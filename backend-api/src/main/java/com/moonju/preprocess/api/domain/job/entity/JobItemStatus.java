package com.moonju.preprocess.api.domain.job.entity;

public enum JobItemStatus {
    PENDING,
    QUEUED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    SKIPPED,
    CANCELLED,
    RETRYING,
    DEAD_LETTERED
}
