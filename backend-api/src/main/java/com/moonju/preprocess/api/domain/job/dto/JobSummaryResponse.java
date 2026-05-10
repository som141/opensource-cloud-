package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.Job;

public record JobSummaryResponse(
    Long jobId,
    int total,
    int queued,
    int processing,
    int succeeded,
    int failed,
    double progressPercent
) {

    public static JobSummaryResponse from(Job job) {
        double progress = job.getTotalCount() == 0
            ? 0.0
            : (double) (job.getSucceededCount() + job.getFailedCount()) * 100.0 / job.getTotalCount();
        return new JobSummaryResponse(
            job.getId(),
            job.getTotalCount(),
            job.getQueuedCount(),
            job.getProcessingCount(),
            job.getSucceededCount(),
            job.getFailedCount(),
            progress
        );
    }
}
