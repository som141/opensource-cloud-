package com.moonju.preprocess.api.domain.job.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkerItemSucceededRequest(
    @NotBlank String workerId,
    @NotBlank String processedObjectKey,
    String previewObjectKey,
    @NotBlank String reportObjectKey
) {
}
