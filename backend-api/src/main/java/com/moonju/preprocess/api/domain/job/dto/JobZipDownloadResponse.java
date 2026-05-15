package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import java.time.Instant;

public record JobZipDownloadResponse(
    Long jobId,
    int fileCount,
    String objectKey,
    String downloadUrl,
    Instant expiresAt
) {

    public static JobZipDownloadResponse of(Long jobId, int fileCount, PresignedDownloadTarget target) {
        return new JobZipDownloadResponse(
            jobId,
            fileCount,
            target.objectKey(),
            target.downloadUrl(),
            target.expiresAt()
        );
    }
}
