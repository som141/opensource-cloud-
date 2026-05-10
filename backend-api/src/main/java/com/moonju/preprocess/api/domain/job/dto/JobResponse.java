package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;
import java.time.LocalDateTime;
import java.util.Map;

public record JobResponse(
    Long id,
    Long projectId,
    Long userId,
    String preset,
    Map<String, String> presetParameters,
    boolean debug,
    JobPriority priority,
    JobStatus status,
    int totalCount,
    int queuedCount,
    int processingCount,
    int succeededCount,
    int failedCount,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {

    public static JobResponse from(Job job) {
        return new JobResponse(
            job.getId(),
            job.getProjectId(),
            job.getUserId(),
            job.getPreset(),
            job.getPresetParameters(),
            job.isDebug(),
            job.getPriority(),
            job.getStatus(),
            job.getTotalCount(),
            job.getQueuedCount(),
            job.getProcessingCount(),
            job.getSucceededCount(),
            job.getFailedCount(),
            job.getCreatedAt(),
            job.getStartedAt(),
            job.getCompletedAt()
        );
    }
}
