package com.moonju.preprocess.api.domain.preprocess.dto;

import com.moonju.preprocess.api.domain.preprocess.entity.PresetParameterDefinition;
import com.moonju.preprocess.api.domain.preprocess.entity.PresetParameterType;
import java.util.Set;

public record PresetParameterDefinitionResponse(
    String name,
    String description,
    PresetParameterType type,
    boolean required,
    String defaultValue,
    String minValue,
    String maxValue,
    Set<String> allowedValues
) {

    public static PresetParameterDefinitionResponse from(PresetParameterDefinition definition) {
        return new PresetParameterDefinitionResponse(
            definition.name(),
            definition.description(),
            definition.type(),
            definition.required(),
            definition.defaultValue(),
            definition.minValue(),
            definition.maxValue(),
            definition.allowedValues()
        );
    }
}
