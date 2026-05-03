package com.moonju.preprocess.api.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 1000)
    String description,

    @NotBlank
    @Size(max = 60)
    String defaultPreset
) {
}
