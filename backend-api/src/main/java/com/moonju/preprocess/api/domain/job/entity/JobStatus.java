package com.moonju.preprocess.api.domain.job.entity;

public enum JobStatus {
    CREATED,
    QUEUED,
    RUNNING,
    PARTIAL_SUCCEEDED,
    SUCCEEDED,
    FAILED,
    CANCEL_REQUESTED,
    CANCELLED,
    RETRYING
}
