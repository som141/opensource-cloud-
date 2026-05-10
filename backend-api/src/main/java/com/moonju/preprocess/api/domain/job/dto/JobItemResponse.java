package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import java.time.LocalDateTime;

public record JobItemResponse(
    Long id,
    Long jobId,
    Long imageId,
    JobItemStatus status,
    int attempt,
    String workerId,
    String processedObjectKey,
    String previewObjectKey,
    String reportObjectKey,
    String errorCode,
    String errorMessage,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {

    public static JobItemResponse from(JobItem item) {
        return new JobItemResponse(
            item.getId(),
            item.getJobId(),
            item.getImageId(),
            item.getStatus(),
            item.getAttempt(),
            item.getWorkerId(),
            item.getProcessedObjectKey(),
            item.getPreviewObjectKey(),
            item.getReportObjectKey(),
            item.getErrorCode(),
            item.getErrorMessage(),
            item.getCreatedAt(),
            item.getStartedAt(),
            item.getCompletedAt()
        );
    }
}
