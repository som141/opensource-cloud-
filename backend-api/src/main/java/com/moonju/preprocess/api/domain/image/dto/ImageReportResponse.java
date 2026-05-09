package com.moonju.preprocess.api.domain.image.dto;

import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import java.time.Instant;
import java.util.Map;

public record ImageReportResponse(
    Long imageId,
    Long artifactId,
    String objectKey,
    String downloadUrl,
    Instant expiresAt,
    Map<String, String> requiredHeaders
) {

    public static ImageReportResponse of(Long imageId, Long artifactId, PresignedDownloadTarget target) {
        return new ImageReportResponse(
            imageId,
            artifactId,
            target.objectKey(),
            target.downloadUrl(),
            target.expiresAt(),
            target.requiredHeaders()
        );
    }
}
