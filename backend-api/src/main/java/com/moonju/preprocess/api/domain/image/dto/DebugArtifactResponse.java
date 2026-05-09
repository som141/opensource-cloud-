package com.moonju.preprocess.api.domain.image.dto;

import com.moonju.preprocess.api.domain.image.entity.ImageArtifact;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import java.time.Instant;
import java.util.Map;

public record DebugArtifactResponse(
    Long artifactId,
    String debugStep,
    String objectKey,
    String downloadUrl,
    Instant expiresAt,
    Map<String, String> requiredHeaders
) {

    public static DebugArtifactResponse of(ImageArtifact artifact, PresignedDownloadTarget target) {
        return new DebugArtifactResponse(
            artifact.getId(),
            artifact.getDebugStep(),
            target.objectKey(),
            target.downloadUrl(),
            target.expiresAt(),
            target.requiredHeaders()
        );
    }
}
