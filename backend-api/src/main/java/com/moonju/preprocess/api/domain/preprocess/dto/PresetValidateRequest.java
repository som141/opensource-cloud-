package com.moonju.preprocess.api.domain.preprocess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record PresetValidateRequest(
    @NotBlank
    String presetName,

    @NotNull
    @Size(max = 100)
    Map<String, String> parameters
) {
}
