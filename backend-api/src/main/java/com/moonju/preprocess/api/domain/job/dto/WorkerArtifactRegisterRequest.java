package com.moonju.preprocess.api.domain.job.dto;

public record WorkerArtifactRegisterRequest(
    String processedObjectKey,
    String previewObjectKey,
    String reportObjectKey
) {
}
