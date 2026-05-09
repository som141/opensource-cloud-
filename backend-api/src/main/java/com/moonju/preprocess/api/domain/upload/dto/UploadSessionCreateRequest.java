package com.moonju.preprocess.api.domain.upload.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UploadSessionCreateRequest(
    @NotNull
    @Min(1)
    @Max(100_000)
    Integer expectedFileCount,

    @NotNull
    @Min(1)
    Long expectedTotalSizeBytes
) {
}
