package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.domain.job.entity.JobItemArtifactDownloadType;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import java.time.Instant;

public record JobItemDownloadUrlResponse(
    Long jobId,
    Long itemId,
    JobItemArtifactDownloadType type,
    String objectKey,
    String downloadUrl,
    Instant expiresAt
) {

    public static JobItemDownloadUrlResponse of(
        Long jobId,
        Long itemId,
        JobItemArtifactDownloadType type,
        PresignedDownloadTarget target
    ) {
        return new JobItemDownloadUrlResponse(
            jobId,
            itemId,
            type,
            target.objectKey(),
            target.downloadUrl(),
            target.expiresAt()
        );
    }
}
