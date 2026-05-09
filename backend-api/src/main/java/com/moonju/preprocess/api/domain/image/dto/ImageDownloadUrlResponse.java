package com.moonju.preprocess.api.domain.image.dto;

import com.moonju.preprocess.api.domain.image.entity.ImageDownloadType;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import java.time.Instant;
import java.util.Map;

public record ImageDownloadUrlResponse(
    Long imageId,
    ImageDownloadType type,
    String objectKey,
    String downloadUrl,
    Instant expiresAt,
    Map<String, String> requiredHeaders
) {

    public static ImageDownloadUrlResponse of(
        Long imageId,
        ImageDownloadType type,
        PresignedDownloadTarget target
    ) {
        return new ImageDownloadUrlResponse(
            imageId,
            type,
            target.objectKey(),
            target.downloadUrl(),
            target.expiresAt(),
            target.requiredHeaders()
        );
    }
}
