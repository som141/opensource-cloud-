package com.moonju.preprocess.worker.domain.artifact.dto;

import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;
import java.util.Arrays;

public record ArtifactUploadRequest(
    ArtifactType artifactType,
    ArtifactPath artifactPath,
    String contentType,
    long sizeBytes,
    byte[] content
) {

    public ArtifactUploadRequest {
        content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
    }

    public static ArtifactUploadRequest skeleton(ArtifactType artifactType, ArtifactPath artifactPath) {
        return new ArtifactUploadRequest(artifactType, artifactPath, artifactType.contentType(), 0L, new byte[0]);
    }

    public static ArtifactUploadRequest upload(
        ArtifactType artifactType,
        ArtifactPath artifactPath,
        byte[] content
    ) {
        byte[] safeContent = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
        return new ArtifactUploadRequest(
            artifactType,
            artifactPath,
            artifactType.contentType(),
            safeContent.length,
            safeContent
        );
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
