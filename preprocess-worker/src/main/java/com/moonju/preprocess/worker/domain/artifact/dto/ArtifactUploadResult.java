package com.moonju.preprocess.worker.domain.artifact.dto;

import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;

public record ArtifactUploadResult(
    ArtifactType artifactType,
    ArtifactPath artifactPath,
    boolean uploaded,
    long sizeBytes,
    String note
) {

    public static ArtifactUploadResult prepared(ArtifactUploadRequest request) {
        return new ArtifactUploadResult(
            request.artifactType(),
            request.artifactPath(),
            false,
            request.sizeBytes(),
            "Artifact upload skeleton prepared; object storage upload is not implemented yet."
        );
    }

    public static ArtifactUploadResult uploaded(ArtifactUploadRequest request) {
        return new ArtifactUploadResult(
            request.artifactType(),
            request.artifactPath(),
            true,
            request.sizeBytes(),
            "Artifact uploaded."
        );
    }
}
