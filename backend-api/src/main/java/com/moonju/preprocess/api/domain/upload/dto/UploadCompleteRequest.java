package com.moonju.preprocess.api.domain.upload.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UploadCompleteRequest(
    @NotEmpty
    List<Long> uploadFileIds
) {
}
