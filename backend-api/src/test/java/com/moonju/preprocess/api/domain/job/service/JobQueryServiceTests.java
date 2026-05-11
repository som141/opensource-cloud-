package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.JobItemDownloadUrlResponse;
import com.moonju.preprocess.api.domain.job.dto.JobResponse;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.response.PageResponse;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobQueryServiceTests {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobItemRepository jobItemRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private PresignedDownloadUrlGenerator presignedDownloadUrlGenerator;

    @InjectMocks
    private JobQueryService service;

    @Test
    void listsJobsCreatedByCurrentUser() {
        Job job = job(1L);
        when(jobRepository.findAllByUserId(20L, PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(java.util.List.of(job)));

        PageResponse<JobResponse> response = service.findMyJobs(20L, PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(1L);
    }

    @Test
    void readsJobAfterProjectReadPermissionValidation() {
        Job job = job(1L);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);

        JobResponse response = service.findOne(20L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        verify(projectPermissionService).validateReadable(10L, 20L);
    }

    @Test
    void summarizesJobProgressFromPersistedCounters() {
        Job job = job(1L);
        ReflectionTestUtils.setField(job, "succeededCount", 1);
        ReflectionTestUtils.setField(job, "failedCount", 1);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);

        JobSummaryResponse response = service.summary(20L, 1L);

        assertThat(response.progressPercent()).isEqualTo(100.0);
    }

    @Test
    void createsPresignedDownloadUrlForProcessedJobItemArtifact() {
        Job job = job(1L);
        JobItem item = JobItem.queued(1L, 2L);
        ReflectionTestUtils.setField(item, "id", 3L);
        item.markProcessing("worker-1", 1, LocalDateTime.now());
        item.markSucceeded(
            "worker-1",
            "processed/10/1/3/processed.png",
            "processed/10/1/3/preview.png",
            "processed/10/1/3/processing-report.json",
            LocalDateTime.now()
        );
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);
        when(jobItemRepository.findByIdAndJobId(3L, 1L)).thenReturn(Optional.of(item));
        when(presignedDownloadUrlGenerator.generateDownloadUrl(any(PresignedDownloadCommand.class)))
            .thenReturn(new PresignedDownloadTarget(
                "processed/10/1/3/processed.png",
                "http://localhost:9000/image-preprocess-local/processed.png",
                Instant.now(),
                Map.of()
            ));

        JobItemDownloadUrlResponse response = service.createItemDownloadUrl(20L, 1L, 3L, "processed");

        assertThat(response.jobId()).isEqualTo(1L);
        assertThat(response.itemId()).isEqualTo(3L);
        assertThat(response.type()).isEqualTo("processed");
        assertThat(response.objectKey()).isEqualTo("processed/10/1/3/processed.png");
        assertThat(response.downloadUrl()).startsWith("http://localhost:9000/");
    }

    private Job job(Long id) {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 2);
        ReflectionTestUtils.setField(job, "id", id);
        job.markQueued(2);
        return job;
    }
}
