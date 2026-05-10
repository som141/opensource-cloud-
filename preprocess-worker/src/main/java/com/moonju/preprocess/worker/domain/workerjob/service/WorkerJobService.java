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
            backendApiClient.reportStarted(message);
            objectStoragePort.prepareDownload(message.originalObjectKey());
            PreprocessResult result = preprocessPipelineRunner.run(PreprocessContext.fromMessage(message));
            String failureMessage = "Preprocess pipeline skeleton executed "
                + result.executedStepNames().size()
                + " steps; OpenCV and artifact integration are not implemented yet.";
            backendApiClient.reportFailed(
                message,
                WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED,
                failureMessage
            );
            return WorkerJobResult.failed(
                message,
                WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED,
                failureMessage
            );
        } catch (InvalidWorkerMessageException exception) {
            return WorkerJobResult.invalid(exception.getMessage());
        }
    }
}
