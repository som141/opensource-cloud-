package com.moonju.preprocess.worker.infra.api;

import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;

public interface BackendApiClient {

    void reportStarted(PreprocessJobMessage message);

    void reportFailed(PreprocessJobMessage message, WorkerFailureCode failureCode, String failureMessage);
}
