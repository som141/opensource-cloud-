package com.moonju.preprocess.worker.domain.workerjob.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.worker.domain.workerjob.dto.JobPriority;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.dto.WorkerJobResult;
import com.moonju.preprocess.worker.domain.workerjob.service.WorkerJobService;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import java.time.Instant;
import java.util.Map;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.junit.jupiter.api.Test;

class PreprocessJobListenerTests {

    @Test
    void delegatesMessageToWorkerJobService() {
        WorkerJobService service = org.mockito.Mockito.mock(WorkerJobService.class);
        PreprocessJobListener listener = new PreprocessJobListener(service);
        PreprocessJobMessage message = message();
        when(service.process(message)).thenReturn(WorkerJobResult.succeeded(message, "ok"));

        listener.handle(message);

        verify(service).process(message);
    }

    @Test
    void requeuesRetryableFailure() {
        WorkerJobService service = org.mockito.Mockito.mock(WorkerJobService.class);
        PreprocessJobListener listener = new PreprocessJobListener(service);
        PreprocessJobMessage message = message();
        when(service.process(message)).thenReturn(WorkerJobResult.failed(
            message,
            WorkerFailureCode.BACKEND_REPORT_FAILED,
            "backend unavailable",
            true
        ));

        assertThatThrownBy(() -> listener.handle(message))
            .isInstanceOf(ImmediateRequeueAmqpException.class);
    }

    @Test
    void rejectsNonRetryableFailure() {
        WorkerJobService service = org.mockito.Mockito.mock(WorkerJobService.class);
        PreprocessJobListener listener = new PreprocessJobListener(service);
        PreprocessJobMessage message = message();
        when(service.process(message)).thenReturn(WorkerJobResult.failed(
            message,
            WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED,
            "not implemented",
            false
        ));

        assertThatThrownBy(() -> listener.handle(message))
            .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    private PreprocessJobMessage message() {
        return new PreprocessJobMessage(
            "msg",
            1L,
            2L,
            3L,
            4L,
            5L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            JobPriority.NORMAL,
            1,
            "trace",
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
