package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;

public record JobRetryResponse(
    Long jobId,
    JobStatus status,
    int queuedItems
) {

    public static JobRetryResponse of(Job job, int queuedItems) {
        return new JobRetryResponse(job.getId(), job.getStatus(), queuedItems);
    }
}
