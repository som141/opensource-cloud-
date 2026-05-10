package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;

public record JobCancelResponse(
    Long jobId,
    JobStatus status
) {

    public static JobCancelResponse from(Job job) {
        return new JobCancelResponse(job.getId(), job.getStatus());
    }
}
