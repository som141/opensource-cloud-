package com.moonju.preprocess.worker.domain.artifact.dto;

import com.moonju.preprocess.worker.domain.artifact.model.ArtifactPath;
import com.moonju.preprocess.worker.domain.artifact.model.ArtifactType;

public record ArtifactUploadResult(
    ArtifactType artifactType,
    ArtifactPath artifactPath,
    boolean uploaded,
    String note
) {

    public static ArtifactUploadResult prepared(ArtifactUploadRequest request) {
        return new ArtifactUploadResult(
            request.artifactType(),
            request.artifactPath(),
            false,
            "Artifact upload skeleton prepared; object storage upload is not implemented yet."
        );
    }
}
