package com.moonju.preprocess.worker.domain.workerjob.service;

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

    public WorkerJobService(BackendApiClient backendApiClient, ObjectStoragePort objectStoragePort) {
        this.backendApiClient = backendApiClient;
        this.objectStoragePort = objectStoragePort;
    }

    public WorkerJobResult process(PreprocessJobMessage message) {
        try {
            message.validateRequiredFields();
            backendApiClient.reportStarted(message);
            objectStoragePort.prepareDownload(message.originalObjectKey());
            backendApiClient.reportFailed(
                message,
                WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED,
                "Preprocess pipeline is not implemented yet."
            );
            return WorkerJobResult.failed(
                message,
                WorkerFailureCode.PIPELINE_NOT_IMPLEMENTED,
                "Preprocess pipeline is not implemented yet."
            );
        } catch (InvalidWorkerMessageException exception) {
            return WorkerJobResult.invalid(exception.getMessage());
        }
    }
}
