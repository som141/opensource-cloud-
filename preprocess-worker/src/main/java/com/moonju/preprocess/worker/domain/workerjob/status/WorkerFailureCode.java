package com.moonju.preprocess.worker.domain.workerjob.status;

public enum WorkerFailureCode {
    INVALID_MESSAGE,
    PIPELINE_NOT_IMPLEMENTED,
    STORAGE_DOWNLOAD_FAILED,
    ARTIFACT_UPLOAD_FAILED,
    PIPELINE_EXECUTION_FAILED,
    BACKEND_REPORT_FAILED
}
