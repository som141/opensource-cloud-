package com.moonju.preprocess.api.domain.upload.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PresignedUploadUrlRequest(
    @Valid
    @NotEmpty
    @Size(max = 500)
    List<PresignedUploadFileRequest> files
) {
}
