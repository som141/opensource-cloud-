package com.moonju.preprocess.api.domain.preprocess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CustomPresetCreateRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 1000)
    String description,

    @NotBlank
    String basePresetName,

    @NotNull
    @Size(max = 100)
    Map<String, String> parameters
) {
}
