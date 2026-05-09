package com.moonju.preprocess.api.domain.upload.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PresignedUploadFileRequest(
    @NotBlank
    @Size(max = 255)
    String fileName,

    @NotBlank
    @Size(max = 100)
    String contentType,

    @Min(1)
    long sizeBytes,

    @NotBlank
    @Pattern(regexp = "^[a-fA-F0-9]{64}$")
    String checksumSha256
) {
}
