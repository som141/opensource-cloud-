package com.moonju.preprocess.api.domain.preprocess.dto;

import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPreset;
import com.moonju.preprocess.api.domain.preprocess.entity.PresetType;
import java.util.List;

public record PresetDetailResponse(
    String name,
    PresetType type,
    String displayName,
    String description,
    boolean supportsDebug,
    List<String> steps,
    List<PresetParameterDefinitionResponse> parameters
) {

    public static PresetDetailResponse from(PreprocessPreset preset) {
        return new PresetDetailResponse(
            preset.getName().name(),
            preset.getType(),
            preset.getDisplayName(),
            preset.getDescription(),
            preset.isSupportsDebug(),
            preset.getSteps(),
            preset.getParameters().stream()
                .map(PresetParameterDefinitionResponse::from)
                .toList()
        );
    }
}
