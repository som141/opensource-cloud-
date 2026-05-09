package com.moonju.preprocess.api.domain.preprocess.dto;

import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPreset;
import com.moonju.preprocess.api.domain.preprocess.entity.PresetType;

public record PresetResponse(
    String name,
    PresetType type,
    String displayName,
    String description,
    boolean supportsDebug
) {

    public static PresetResponse from(PreprocessPreset preset) {
        return new PresetResponse(
            preset.getName().name(),
            preset.getType(),
            preset.getDisplayName(),
            preset.getDescription(),
            preset.isSupportsDebug()
        );
    }
}
