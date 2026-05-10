package com.moonju.preprocess.worker.domain.artifact.dto;

import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;

public record ArtifactUploadRequest(
    ArtifactType artifactType,
    ArtifactPath artifactPath,
    String contentType,
    long sizeBytes
) {

    public static ArtifactUploadRequest skeleton(ArtifactType artifactType, ArtifactPath artifactPath) {
        return new ArtifactUploadRequest(artifactType, artifactPath, artifactType.contentType(), 0L);
    }
}
