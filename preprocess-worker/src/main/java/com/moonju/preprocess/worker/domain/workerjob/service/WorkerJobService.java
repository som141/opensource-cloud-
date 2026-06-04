package com.moonju.preprocess.worker.domain.workerjob.service;

import com.moonju.preprocess.worker.domain.artifact.dto.ArtifactUploadResult;
import com.moonju.preprocess.worker.domain.artifact.exception.ArtifactUploadFailedException;
import com.moonju.preprocess.worker.domain.artifact.service.DebugArtifactSaveService;
import com.moonju.preprocess.worker.domain.artifact.service.PreviewImageSaveService;
import com.moonju.preprocess.worker.domain.artifact.service.ProcessedImageSaveService;
import com.moonju.preprocess.worker.domain.artifact.service.ProcessingReportSaveService;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessPipelineRunner;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessResult;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReport;
import com.moonju.preprocess.worker.domain.report.service.ProcessingReportFactory;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.dto.WorkerJobResult;
import com.moonju.preprocess.worker.domain.workerjob.exception.InvalidWorkerMessageException;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import com.moonju.preprocess.worker.infra.api.BackendApiClient;
import com.moonju.preprocess.worker.infra.api.BackendApiReportException;
import com.moonju.preprocess.worker.infra.metrics.WorkerMetricsRecorder;
import com.moonju.preprocess.worker.infra.storage.ObjectStoragePort;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class WorkerJobService {

    private final BackendApiClient backendApiClient;
    private final ObjectStoragePort objectStoragePort;
    private final PreprocessPipelineRunner preprocessPipelineRunner;
    private final ProcessedImageSaveService processedImageSaveService;
    private final PreviewImageSaveService previewImageSaveService;
    private final DebugArtifactSaveService debugArtifactSaveService;
    private final ProcessingReportFactory processingReportFactory;
    private final ProcessingReportSaveService processingReportSaveService;
    private final WorkerMetricsRecorder workerMetricsRecorder;

    public WorkerJobService(
        BackendApiClient backendApiClient,
        ObjectStoragePort objectStoragePort,
        PreprocessPipelineRunner preprocessPipelineRunner,
        ProcessedImageSaveService processedImageSaveService,
        PreviewImageSaveService previewImageSaveService,
        DebugArtifactSaveService debugArtifactSaveService,
        ProcessingReportFactory processingReportFactory,
        ProcessingReportSaveService processingReportSaveService,
        WorkerMetricsRecorder workerMetricsRecorder
    ) {
        this.backendApiClient = backendApiClient;
        this.objectStoragePort = objectStoragePort;
        this.preprocessPipelineRunner = preprocessPipelineRunner;
        this.processedImageSaveService = processedImageSaveService;
        this.previewImageSaveService = previewImageSaveService;
        this.debugArtifactSaveService = debugArtifactSaveService;
        this.processingReportFactory = processingReportFactory;
        this.processingReportSaveService = processingReportSaveService;
        this.workerMetricsRecorder = workerMetricsRecorder;
    }

    public WorkerJobResult process(PreprocessJobMessage message) {
        Instant startedAt = Instant.now();
        WorkerJobResult result = processInternal(message);
        workerMetricsRecorder.recordJobCompleted(
            message == null ? null : message.preset(),
            result.status(),
            result.failureCode(),
            result.retryable(),
            Duration.between(startedAt, Instant.now())
        );
        return result;
    }

    private WorkerJobResult processInternal(PreprocessJobMessage message) {
        try {
            message.validateRequiredFields();
        } catch (InvalidWorkerMessageException exception) {
            return WorkerJobResult.invalid(exception.getMessage());
        }

        try {
            backendApiClient.reportStarted(message);
        } catch (RuntimeException exception) {
            return backendReportFailed(message, exception);
        }

        byte[] sourceImageBytes;
        try {
            sourceImageBytes = objectStoragePort.downloadBytes(message.originalObjectKey());
        } catch (RuntimeException exception) {
            return reportFailure(message, WorkerFailureCode.STORAGE_DOWNLOAD_FAILED, exception.getMessage(), true);
        }

        try {
            backendApiClient.reportHeartbeat(message);
        } catch (RuntimeException exception) {
            return backendReportFailed(message, exception);
        }

        try {
            PreprocessContext context = PreprocessContext.fromMessage(message)
                .withSourceImageBytes(sourceImageBytes);
            AtomicReference<ArtifactUploadResult> processedArtifact = new AtomicReference<>();
            AtomicReference<ArtifactUploadResult> previewArtifact = new AtomicReference<>();
            PreprocessResult result = preprocessPipelineRunner.run(context, outputImage -> {
                processedArtifact.set(processedImageSaveService.save(
                    message.projectId(),
                    message.jobId(),
                    message.itemId(),
                    outputImage
                ));
                previewArtifact.set(previewImageSaveService.save(
                    message.projectId(),
                    message.jobId(),
                    message.itemId(),
                    outputImage
                ));
                debugArtifactSaveService.saveAll(context.debugSnapshots());
            });
            if (result.skeletonOnly()) {
                workerMetricsRecorder.recordPipelineSkeletonExecution(
                    result.presetName(),
                    result.executedStepNames().size()
                );
            }
            if (!result.success()) {
                return reportFailure(
                    message,
                    WorkerFailureCode.PIPELINE_EXECUTION_FAILED,
                    result.errorMessage(),
                    true
                );
            }
            if (result.skeletonOnly()) {
                String failureMessage = "Preprocess pipeline skeleton executed "
                    + result.executedStepNames().size()
                    + " steps; output image is not available for artifact upload.";
                return reportFailure(message, WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED, failureMessage, false);
            }

            ProcessingReport report = processingReportFactory.createReport(result);
            ArtifactUploadResult reportArtifact = processingReportSaveService.save(
                message.projectId(),
                message.jobId(),
                message.itemId(),
                report
            );
            return reportSuccess(
                message,
                requiredArtifact(processedArtifact, "processed image"),
                requiredArtifact(previewArtifact, "preview image"),
                reportArtifact
            );
        } catch (ArtifactUploadFailedException exception) {
            return reportFailure(message, WorkerFailureCode.ARTIFACT_UPLOAD_FAILED, exception.getMessage(), true);
        } catch (RuntimeException exception) {
            return reportFailure(message, WorkerFailureCode.PIPELINE_EXECUTION_FAILED, exception.getMessage(), true);
        }
    }

    private WorkerJobResult reportSuccess(
        PreprocessJobMessage message,
        ArtifactUploadResult processedArtifact,
        ArtifactUploadResult previewArtifact,
        ArtifactUploadResult reportArtifact
    ) {
        try {
            backendApiClient.reportSucceeded(
                message,
                processedArtifact.artifactPath().value(),
                previewArtifact.artifactPath().value(),
                reportArtifact.artifactPath().value()
            );
            return WorkerJobResult.succeeded(message, "Preprocess pipeline completed and artifacts uploaded.");
        } catch (RuntimeException exception) {
            return backendReportFailed(message, exception);
        }
    }

    private ArtifactUploadResult requiredArtifact(
        AtomicReference<ArtifactUploadResult> artifact,
        String artifactName
    ) {
        ArtifactUploadResult result = artifact.get();
        if (result == null) {
            throw new ArtifactUploadFailedException("Missing uploaded artifact: " + artifactName);
        }
        return result;
    }

    private WorkerJobResult reportFailure(
        PreprocessJobMessage message,
        WorkerFailureCode failureCode,
        String failureMessage,
        boolean retryable
    ) {
        try {
            backendApiClient.reportFailed(message, failureCode, failureMessage, retryable);
            return WorkerJobResult.failed(message, failureCode, failureMessage, retryable);
        } catch (RuntimeException exception) {
            return WorkerJobResult.failed(
                message,
                WorkerFailureCode.BACKEND_REPORT_FAILED,
                exception.getMessage(),
                isRetryableBackendReportFailure(exception)
            );
        }
    }

    private WorkerJobResult backendReportFailed(PreprocessJobMessage message, RuntimeException exception) {
        return WorkerJobResult.failed(
            message,
            WorkerFailureCode.BACKEND_REPORT_FAILED,
            exception.getMessage(),
            isRetryableBackendReportFailure(exception)
        );
    }

    private boolean isRetryableBackendReportFailure(RuntimeException exception) {
        if (exception instanceof BackendApiReportException
            && exception.getMessage() != null
            && exception.getMessage().contains("status 409")) {
            return false;
        }
        return true;
    }
}
