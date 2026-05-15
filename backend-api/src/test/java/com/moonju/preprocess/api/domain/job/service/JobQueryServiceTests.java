package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.JobItemDownloadUrlResponse;
import com.moonju.preprocess.api.domain.job.dto.JobResponse;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemArtifactDownloadType;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.response.PageResponse;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.time.Instant;
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
    void createsJobItemArtifactDownloadUrl() {
        Job job = job(1L);
        JobItem item = succeededItem(2L, job.getId());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);
        when(jobItemRepository.findByIdAndJobId(2L, 1L)).thenReturn(Optional.of(item));
        when(presignedDownloadUrlGenerator.generateDownloadUrl(any(PresignedDownloadCommand.class)))
            .thenReturn(new PresignedDownloadTarget(
                "processed/10/1/2/processed.png",
                "http://localhost:9000/image-preprocess-local/processed.png",
                Instant.parse("2026-05-15T09:00:00Z"),
                Map.of()
            ));

        JobItemDownloadUrlResponse response = service.createItemArtifactDownloadUrl(20L, 1L, 2L, "processed");

        assertThat(response.jobId()).isEqualTo(1L);
        assertThat(response.itemId()).isEqualTo(2L);
        assertThat(response.type()).isEqualTo(JobItemArtifactDownloadType.PROCESSED);
        assertThat(response.downloadUrl()).contains("localhost:9000");
    }

    @Test
    void rejectsUnsupportedJobItemArtifactType() {
        Job job = job(1L);
        JobItem item = succeededItem(2L, job.getId());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);
        when(jobItemRepository.findByIdAndJobId(2L, 1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.createItemArtifactDownloadUrl(20L, 1L, 2L, "debug"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("Unsupported job item artifact type.");
    }

    private Job job(Long id) {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 2);
        ReflectionTestUtils.setField(job, "id", id);
        job.markQueued(2);
        return job;
    }

    private JobItem succeededItem(Long id, Long jobId) {
        JobItem item = new JobItem(jobId, 100L, JobItemStatus.SUCCEEDED, 1);
        item.registerArtifacts(
            "processed/10/1/2/processed.png",
            "processed/10/1/2/preview.png",
            "processed/10/1/2/processing-report.json"
        );
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
