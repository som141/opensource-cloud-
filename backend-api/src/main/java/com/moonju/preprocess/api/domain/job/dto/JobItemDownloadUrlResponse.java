package com.moonju.preprocess.api.domain.job.dto;

import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import java.time.Instant;
import java.util.Map;

public record JobItemDownloadUrlResponse(
    Long jobId,
    Long itemId,
    String type,
    String objectKey,
    String downloadUrl,
    Instant expiresAt,
    Map<String, String> requiredHeaders
) {

    public static JobItemDownloadUrlResponse of(
        Long jobId,
        Long itemId,
        String type,
        PresignedDownloadTarget target
    ) {
        return new JobItemDownloadUrlResponse(
            jobId,
            itemId,
            type,
            target.objectKey(),
            target.downloadUrl(),
            target.expiresAt(),
            target.requiredHeaders()
        );
    }
}
