package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.JobResponse;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.response.PageResponse;
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

    private Job job(Long id) {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 2);
        ReflectionTestUtils.setField(job, "id", id);
        job.markQueued(2);
        return job;
    }
}
