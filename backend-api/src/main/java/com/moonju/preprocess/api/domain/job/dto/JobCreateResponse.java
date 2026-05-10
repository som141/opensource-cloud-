package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;
import java.time.LocalDateTime;

public record JobCreateResponse(
    Long jobId,
    JobStatus status,
    int totalImages,
    int queuedImages,
    LocalDateTime createdAt
) {

    public static JobCreateResponse from(Job job) {
        return new JobCreateResponse(
            job.getId(),
            job.getStatus(),
            job.getTotalCount(),
            job.getQueuedCount(),
            job.getCreatedAt()
        );
    }
}
