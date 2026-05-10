package com.moonju.preprocess.api.domain.job.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.job.dto.WorkerItemReportResponse;
import com.moonju.preprocess.api.domain.job.dto.WorkerItemStartedRequest;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.domain.job.service.InternalWorkerJobService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalWorkerJobControllerTests {

    @Mock
    private InternalWorkerJobService workerJobService;

    @Test
    void reportsStartedWithCommonResponse() {
        InternalWorkerJobController controller = new InternalWorkerJobController(workerJobService);
        WorkerItemStartedRequest request = new WorkerItemStartedRequest("worker-1", 1);
        WorkerItemReportResponse serviceResponse = new WorkerItemReportResponse(
            1L,
            10L,
            JobItemStatus.PROCESSING,
            "worker-1",
            LocalDateTime.of(2026, 5, 10, 0, 0)
        );
        when(workerJobService.started(1L, 10L, request)).thenReturn(serviceResponse);

        ApiResponse<WorkerItemReportResponse> response = controller.started(1L, 10L, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.result()).isSameAs(serviceResponse);
        verify(workerJobService).started(1L, 10L, request);
    }
}
