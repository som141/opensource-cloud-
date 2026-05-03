package com.moonju.preprocess.api.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 1000)
    String description,

    @Size(max = 100)
    String defaultPreset
) {
}
