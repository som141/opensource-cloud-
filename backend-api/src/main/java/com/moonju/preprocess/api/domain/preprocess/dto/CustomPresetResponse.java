package com.moonju.preprocess.api.domain.preprocess.dto;

import com.moonju.preprocess.api.domain.preprocess.entity.CustomPreprocessPreset;
import java.time.LocalDateTime;
import java.util.Map;

public record CustomPresetResponse(
    Long id,
    String name,
    String description,
    String basePresetName,
    Map<String, String> parameters,
    LocalDateTime createdAt
) {

    public static CustomPresetResponse from(CustomPreprocessPreset preset) {
        return new CustomPresetResponse(
            preset.getId(),
            preset.getName(),
            preset.getDescription(),
            preset.getBasePresetName().name(),
            preset.getParameters(),
            preset.getCreatedAt()
        );
    }
}
