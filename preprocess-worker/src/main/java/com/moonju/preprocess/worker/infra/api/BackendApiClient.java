package com.moonju.preprocess.worker.infra.api;

import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;

public interface BackendApiClient {

    void reportStarted(PreprocessJobMessage message);

    void reportHeartbeat(PreprocessJobMessage message);

    void reportSucceeded(
        PreprocessJobMessage message,
        String processedObjectKey,
        String previewObjectKey,
        String reportObjectKey
    );

    void reportFailed(
        PreprocessJobMessage message,
        WorkerFailureCode failureCode,
        String failureMessage,
        boolean retryable
    );

    void registerArtifacts(
        PreprocessJobMessage message,
        String processedObjectKey,
        String previewObjectKey,
        String reportObjectKey
    );
}
