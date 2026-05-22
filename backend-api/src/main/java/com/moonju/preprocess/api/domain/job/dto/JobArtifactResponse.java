package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import java.time.Instant;
import java.util.List;

public record JobArtifactResponse(
    Long jobId,
    int totalItems,
    int processedReadyCount,
    List<ProcessedArtifact> processedArtifacts
) {

    public static JobArtifactResponse of(Long jobId, int totalItems, List<ProcessedArtifact> processedArtifacts) {
        return new JobArtifactResponse(jobId, totalItems, processedArtifacts.size(), processedArtifacts);
    }

    public record ProcessedArtifact(
        Long itemId,
        Long imageId,
        JobItemStatus status,
        String objectKey,
        String downloadUrl,
        Instant expiresAt
    ) {

        public static ProcessedArtifact of(JobItem item, PresignedDownloadTarget target) {
            return new ProcessedArtifact(
                item.getId(),
                item.getImageId(),
                item.getStatus(),
                target.objectKey(),
                target.downloadUrl(),
                target.expiresAt()
            );
        }
    }
}
