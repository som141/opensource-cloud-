package com.moonju.preprocess.worker.domain.workerjob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.exception.ArtifactUploadFailedException;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import com.moonju.preprocess.worker.domain.artifact.service.DebugArtifactSaveService;
import com.moonju.preprocess.worker.domain.artifact.service.PreviewImageSaveService;
import com.moonju.preprocess.worker.domain.artifact.service.ProcessedImageSaveService;
import com.moonju.preprocess.worker.domain.artifact.service.ProcessingReportSaveService;
import com.moonju.preprocess.worker.domain.preprocess.model.DebugArtifactSnapshot;
import com.moonju.preprocess.worker.domain.preprocess.model.ImageMatHolder;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessPipelineRunner;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessResult;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReport;
import com.moonju.preprocess.worker.domain.report.service.ProcessingReportFactory;
import com.moonju.preprocess.worker.domain.workerjob.dto.JobPriority;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.dto.WorkerJobResult;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerJobStatus;
import com.moonju.preprocess.worker.infra.api.BackendApiClient;
import com.moonju.preprocess.worker.infra.api.BackendApiReportException;
import com.moonju.preprocess.worker.infra.metrics.WorkerMetricsRecorder;
import com.moonju.preprocess.worker.infra.opencv.OpenCvLoader;
import com.moonju.preprocess.worker.infra.storage.ObjectStoragePort;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

class WorkerJobServiceTests {

    @BeforeAll
    static void loadOpenCv() {
        new OpenCvLoader().loadIfPresent();
    }

    private final BackendApiClient backendApiClient = mock(BackendApiClient.class);
    private final ObjectStoragePort objectStoragePort = mock(ObjectStoragePort.class);
    private final PreprocessPipelineRunner preprocessPipelineRunner = mock(PreprocessPipelineRunner.class);
    private final ProcessedImageSaveService processedImageSaveService = mock(ProcessedImageSaveService.class);
    private final PreviewImageSaveService previewImageSaveService = mock(PreviewImageSaveService.class);
    private final DebugArtifactSaveService debugArtifactSaveService = mock(DebugArtifactSaveService.class);
    private final ProcessingReportFactory processingReportFactory = new ProcessingReportFactory();
    private final ProcessingReportSaveService processingReportSaveService = mock(ProcessingReportSaveService.class);
    private final WorkerMetricsRecorder workerMetricsRecorder = mock(WorkerMetricsRecorder.class);
    private final WorkerJobService service = new WorkerJobService(
        backendApiClient,
        objectStoragePort,
        preprocessPipelineRunner,
        processedImageSaveService,
        previewImageSaveService,
        debugArtifactSaveService,
        processingReportFactory,
        processingReportSaveService,
        workerMetricsRecorder
    );

    @Test
    void uploadsArtifactsAndReportsSuccess() {
        PreprocessJobMessage message = validMessage();
        when(objectStoragePort.downloadBytes("originals/scan.png")).thenReturn(new byte[] {1, 2, 3});
        when(processedImageSaveService.save(eq(3L), eq(1L), eq(2L), any(ImageMatHolder.class)))
            .thenReturn(uploaded(ArtifactType.PROCESSED_IMAGE, "processed/3/1/2/processed.png", 10));
        when(previewImageSaveService.save(eq(3L), eq(1L), eq(2L), any(ImageMatHolder.class)))
            .thenReturn(uploaded(ArtifactType.PREVIEW_IMAGE, "processed/3/1/2/preview.png", 5));
        when(processingReportSaveService.save(eq(3L), eq(1L), eq(2L), any(ProcessingReport.class)))
            .thenReturn(uploaded(ArtifactType.PROCESSING_REPORT, "processed/3/1/2/processing-report.json", 50));
        successfulPipelineRun(message);

        WorkerJobResult result = service.process(message);

        assertThat(result.status()).isEqualTo(WorkerJobStatus.SUCCEEDED);
        assertThat(result.failureCode()).isNull();
        assertThat(result.message()).contains("artifacts uploaded");
        verify(backendApiClient).reportStarted(message);
        verify(objectStoragePort).downloadBytes("originals/scan.png");
        verify(backendApiClient).reportHeartbeat(message);
        verify(debugArtifactSaveService).saveAll(argThat(List::isEmpty));
        verify(backendApiClient).reportSucceeded(
            message,
            "processed/3/1/2/processed.png",
            "processed/3/1/2/preview.png",
            "processed/3/1/2/processing-report.json"
        );
        verify(workerMetricsRecorder).recordJobCompleted(
            eq("A4_SCAN_300DPI"),
            eq(WorkerJobStatus.SUCCEEDED),
            isNull(),
            eq(false),
            any(Duration.class)
        );
    }

    @Test
    void uploadsDebugArtifactsWhenDebugEnabled() {
        PreprocessJobMessage message = validMessage(true);
        when(objectStoragePort.downloadBytes("originals/scan.png")).thenReturn(new byte[] {1, 2, 3});
        when(processedImageSaveService.save(eq(3L), eq(1L), eq(2L), any(ImageMatHolder.class)))
            .thenReturn(uploaded(ArtifactType.PROCESSED_IMAGE, "processed/3/1/2/processed.png", 10));
        when(previewImageSaveService.save(eq(3L), eq(1L), eq(2L), any(ImageMatHolder.class)))
            .thenReturn(uploaded(ArtifactType.PREVIEW_IMAGE, "processed/3/1/2/preview.png", 5));
        when(processingReportSaveService.save(eq(3L), eq(1L), eq(2L), any(ProcessingReport.class)))
            .thenReturn(uploaded(ArtifactType.PROCESSING_REPORT, "processed/3/1/2/processing-report.json", 50));
        doAnswer(invocation -> {
            List<DebugArtifactSnapshot> snapshots = invocation.getArgument(0);
            assertThat(snapshots).hasSize(1);
            assertThat(snapshots.getFirst().loaded()).isTrue();
            return List.of();
        }).when(debugArtifactSaveService).saveAll(any());
        successfulPipelineRun(message);

        WorkerJobResult result = service.process(message);

        assertThat(result.status()).isEqualTo(WorkerJobStatus.SUCCEEDED);
        verify(debugArtifactSaveService).saveAll(argThat(snapshots ->
            snapshots.size() == 1
                && snapshots.getFirst().descriptor().objectKey()
                    .equals("processed/3/1/2/debug/00_decoded.png")
        ));
    }

    @Test
    void reportsNotImplementedWhenPipelineHasNoOutputImage() {
        PreprocessJobMessage message = validMessage();
        when(objectStoragePort.downloadBytes("originals/scan.png")).thenReturn(new byte[] {1, 2, 3});
        doAnswer(invocation -> {
            PreprocessContext context = invocation.getArgument(0);
            return PreprocessResult.from(context, true, Duration.ofMillis(3), true, null);
        }).when(preprocessPipelineRunner).run(any(PreprocessContext.class), any());

        WorkerJobResult result = service.process(message);

        assertThat(result.status()).isEqualTo(WorkerJobStatus.FAILED);
        assertThat(result.failureCode()).isEqualTo(WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED);
        assertThat(result.message()).contains("output image is not available");
        assertThat(result.retryable()).isFalse();
        verify(backendApiClient).reportFailed(
            message,
            WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED,
            "Preprocess pipeline skeleton executed 0 steps; output image is not available for artifact upload.",
            false
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
        assertThat(result.retryable()).isFalse();
        verifyNoInteractions(backendApiClient, objectStoragePort);
        verify(workerMetricsRecorder).recordJobCompleted(
            isNull(),
            eq(WorkerJobStatus.FAILED),
            eq(WorkerFailureCode.INVALID_MESSAGE),
            eq(false),
            any(Duration.class)
        );
    }

    @Test
    void marksBackendReportFailureAsRetryable() {
        PreprocessJobMessage message = validMessage();
        doThrow(new RuntimeException("backend unavailable")).when(backendApiClient).reportStarted(message);

        WorkerJobResult result = service.process(message);

        assertThat(result.failureCode()).isEqualTo(WorkerFailureCode.BACKEND_REPORT_FAILED);
        assertThat(result.retryable()).isTrue();
    }

    @Test
    void marksBackendConflictReportFailureAsNonRetryable() {
        PreprocessJobMessage message = validMessage();
        doThrow(new BackendApiReportException(
            "Backend internal API returned status 409 for /internal/v1/jobs/1/items/2/started"
        )).when(backendApiClient).reportStarted(message);

        WorkerJobResult result = service.process(message);

        assertThat(result.failureCode()).isEqualTo(WorkerFailureCode.BACKEND_REPORT_FAILED);
        assertThat(result.retryable()).isFalse();
    }

    @Test
    void reportsStorageDownloadFailureAsRetryable() {
        PreprocessJobMessage message = validMessage();
        doThrow(new RuntimeException("storage unavailable"))
            .when(objectStoragePort)
            .downloadBytes("originals/scan.png");

        WorkerJobResult result = service.process(message);

        assertThat(result.failureCode()).isEqualTo(WorkerFailureCode.STORAGE_DOWNLOAD_FAILED);
        assertThat(result.retryable()).isTrue();
        verify(backendApiClient).reportFailed(
            message,
            WorkerFailureCode.STORAGE_DOWNLOAD_FAILED,
            "storage unavailable",
            true
        );
    }

    @Test
    void reportsPipelineExecutionFailureAsRetryable() {
        PreprocessJobMessage message = validMessage();
        when(objectStoragePort.downloadBytes("originals/scan.png")).thenReturn(new byte[] {1, 2, 3});
        PreprocessContext context = PreprocessContext.fromMessage(message);
        doAnswer(invocation -> PreprocessResult.from(context, true, Duration.ofMillis(3), false, "decode failed"))
            .when(preprocessPipelineRunner)
            .run(any(PreprocessContext.class), any());

        WorkerJobResult result = service.process(message);

        assertThat(result.failureCode()).isEqualTo(WorkerFailureCode.PIPELINE_EXECUTION_FAILED);
        assertThat(result.retryable()).isTrue();
        verify(backendApiClient).reportFailed(
            message,
            WorkerFailureCode.PIPELINE_EXECUTION_FAILED,
            "decode failed",
            true
        );
    }

    @Test
    void reportsArtifactUploadFailureAsRetryable() {
        PreprocessJobMessage message = validMessage();
        when(objectStoragePort.downloadBytes("originals/scan.png")).thenReturn(new byte[] {1, 2, 3});
        when(processedImageSaveService.save(eq(3L), eq(1L), eq(2L), any(ImageMatHolder.class)))
            .thenThrow(new ArtifactUploadFailedException("processed upload failed"));
        successfulPipelineRun(message);

        WorkerJobResult result = service.process(message);

        assertThat(result.failureCode()).isEqualTo(WorkerFailureCode.ARTIFACT_UPLOAD_FAILED);
        assertThat(result.retryable()).isTrue();
        verify(backendApiClient).reportFailed(
            message,
            WorkerFailureCode.ARTIFACT_UPLOAD_FAILED,
            "processed upload failed",
            true
        );
    }

    @SuppressWarnings("unchecked")
    private void successfulPipelineRun(PreprocessJobMessage message) {
        doAnswer(invocation -> {
            PreprocessContext context = invocation.getArgument(0);
            Consumer<ImageMatHolder> outputConsumer = invocation.getArgument(1);
            ImageMatHolder outputImage = ImageMatHolder.decoded(
                "originals/scan.png",
                new Mat(2, 3, CvType.CV_8UC1, new Scalar(255))
            );
            try {
                if (context.debug()) {
                    context.recordDebugSnapshot(PreprocessStepName.DECODE, "00_decoded.png", outputImage);
                }
                outputConsumer.accept(outputImage);
            } finally {
                context.releaseDebugSnapshots();
                outputImage.release();
            }
            return PreprocessResult.from(context, false, Duration.ofMillis(3), true, null);
        }).when(preprocessPipelineRunner).run(any(PreprocessContext.class), any(Consumer.class));
    }

    private ArtifactUploadResult uploaded(ArtifactType type, String objectKey, long sizeBytes) {
        return new ArtifactUploadResult(type, new ArtifactPath(objectKey), true, sizeBytes, "uploaded");
    }

    private PreprocessJobMessage validMessage() {
        return validMessage(false);
    }

    private PreprocessJobMessage validMessage(boolean debug) {
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
            debug,
            JobPriority.NORMAL,
            1,
            "trace",
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
