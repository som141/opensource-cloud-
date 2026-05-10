package com.moonju.preprocess.api.domain.job.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.JobCreateRequest;
import com.moonju.preprocess.api.domain.job.dto.JobCreateResponse;
import com.moonju.preprocess.api.domain.job.dto.JobRetryResponse;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;
import com.moonju.preprocess.api.domain.job.service.JobCancelService;
import com.moonju.preprocess.api.domain.job.service.JobCommandService;
import com.moonju.preprocess.api.domain.job.service.JobQueryService;
import com.moonju.preprocess.api.domain.job.service.JobRetryService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobControllerTests {

    @Mock
    private JobCommandService jobCommandService;

    @Mock
    private JobQueryService jobQueryService;

    @Mock
    private JobCancelService jobCancelService;

    @Mock
    private JobRetryService jobRetryService;

    @Test
    void createsJobWithCommonCreatedResponse() {
        JobController controller = controller();
        JobCreateRequest request = new JobCreateRequest(
            10L,
            List.of(100L),
            "A4_SCAN_300DPI",
            Map.of("targetDpi", "300"),
            false,
            JobPriority.NORMAL,
            null
        );
        JobCreateResponse serviceResponse = new JobCreateResponse(
            1L,
            JobStatus.QUEUED,
            1,
            1,
            LocalDateTime.of(2026, 5, 10, 12, 0)
        );
        when(jobCommandService.create(20L, request)).thenReturn(serviceResponse);

        ApiResponse<JobCreateResponse> response = controller.create(20L, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common201");
        assertThat(response.result()).isSameAs(serviceResponse);
    }

    @Test
    void retriesFailedJobItems() {
        JobController controller = controller();
        JobRetryResponse serviceResponse = new JobRetryResponse(1L, JobStatus.RETRYING, 2);
        when(jobRetryService.retryFailed(20L, 1L)).thenReturn(serviceResponse);

        ApiResponse<JobRetryResponse> response = controller.retry(20L, 1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.result()).isSameAs(serviceResponse);
        verify(jobRetryService).retryFailed(20L, 1L);
    }

    private JobController controller() {
        return new JobController(jobCommandService, jobQueryService, jobCancelService, jobRetryService);
    }
}
