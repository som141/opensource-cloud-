package com.moonju.preprocess.worker.domain.workerjob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.moonju.preprocess.worker.domain.workerjob.dto.JobPriority;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.dto.WorkerJobResult;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessPipelineRunner;
import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPresetRegistry;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepCatalog;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerJobStatus;
import com.moonju.preprocess.worker.infra.api.BackendApiClient;
import com.moonju.preprocess.worker.infra.storage.ObjectStoragePort;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkerJobServiceTests {

    private final BackendApiClient backendApiClient = org.mockito.Mockito.mock(BackendApiClient.class);
    private final ObjectStoragePort objectStoragePort = org.mockito.Mockito.mock(ObjectStoragePort.class);
    private final PreprocessPipelineRunner preprocessPipelineRunner = new PreprocessPipelineRunner(
        PreprocessPresetRegistry.builtIn(),
        PreprocessStepCatalog.builtIn()
    );
    private final WorkerJobService service = new WorkerJobService(
        backendApiClient,
        objectStoragePort,
        preprocessPipelineRunner
    );

    @Test
    void executesPipelineSkeletonAndReportsNotImplementedBoundary() {
        PreprocessJobMessage message = validMessage();

        WorkerJobResult result = service.process(message);

        assertThat(result.status()).isEqualTo(WorkerJobStatus.FAILED);
        assertThat(result.failureCode()).isEqualTo(WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED);
        assertThat(result.message()).contains("skeleton executed 11 steps");
        verify(backendApiClient).reportStarted(message);
        verify(objectStoragePort).prepareDownload("originals/scan.png");
        verify(backendApiClient).reportFailed(
            message,
            WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED,
            "Preprocess pipeline skeleton executed 11 steps; OpenCV and artifact integration are not implemented yet."
        );
    }

    @Test
    void rejectsInvalidMessageBeforeExternalCalls() {
        PreprocessJobMessage message = new PreprocessJobMessage(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Map.of(),
            false,
            JobPriority.NORMAL,
            0,
            null,
            null
        );

        WorkerJobResult result = service.process(message);

        assertThat(result.status()).isEqualTo(WorkerJobStatus.FAILED);
        assertThat(result.failureCode()).isEqualTo(WorkerFailureCode.INVALID_MESSAGE);
        verifyNoInteractions(backendApiClient, objectStoragePort);
    }

    private PreprocessJobMessage validMessage() {
        return new PreprocessJobMessage(
            "msg",
            1L,
            2L,
            3L,
            4L,
            5L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of("targetDpi", "300"),
            false,
            JobPriority.NORMAL,
            1,
            "trace",
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
