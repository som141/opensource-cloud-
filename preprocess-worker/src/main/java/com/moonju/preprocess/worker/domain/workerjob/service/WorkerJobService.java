package com.moonju.preprocess.worker.domain.workerjob.service;

import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessPipelineRunner;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessResult;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.dto.WorkerJobResult;
import com.moonju.preprocess.worker.domain.workerjob.exception.InvalidWorkerMessageException;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import com.moonju.preprocess.worker.infra.api.BackendApiClient;
import com.moonju.preprocess.worker.infra.storage.ObjectStoragePort;
import org.springframework.stereotype.Service;

@Service
public class WorkerJobService {

    private final BackendApiClient backendApiClient;
    private final ObjectStoragePort objectStoragePort;
    private final PreprocessPipelineRunner preprocessPipelineRunner;

    public WorkerJobService(
        BackendApiClient backendApiClient,
        ObjectStoragePort objectStoragePort,
        PreprocessPipelineRunner preprocessPipelineRunner
    ) {
        this.backendApiClient = backendApiClient;
        this.objectStoragePort = objectStoragePort;
        this.preprocessPipelineRunner = preprocessPipelineRunner;
    }

    public WorkerJobResult process(PreprocessJobMessage message) {
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

        try {
            objectStoragePort.prepareDownload(message.originalObjectKey());
        } catch (RuntimeException exception) {
            return reportFailure(message, WorkerFailureCode.STORAGE_DOWNLOAD_FAILED, exception.getMessage(), true);
        }

        try {
            backendApiClient.reportHeartbeat(message);
        } catch (RuntimeException exception) {
            return backendReportFailed(message, exception);
        }

        try {
            PreprocessResult result = preprocessPipelineRunner.run(PreprocessContext.fromMessage(message));
            String failureMessage = "Preprocess pipeline skeleton executed "
                + result.executedStepNames().size()
                + " steps; OpenCV and artifact integration are not implemented yet.";
            return reportFailure(message, WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED, failureMessage, false);
        } catch (RuntimeException exception) {
            return reportFailure(message, WorkerFailureCode.PIPELINE_EXECUTION_FAILED, exception.getMessage(), true);
        }
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
                true
            );
        }
    }

    private WorkerJobResult backendReportFailed(PreprocessJobMessage message, RuntimeException exception) {
        return WorkerJobResult.failed(
            message,
            WorkerFailureCode.BACKEND_REPORT_FAILED,
            exception.getMessage(),
            true
        );
    }
}
