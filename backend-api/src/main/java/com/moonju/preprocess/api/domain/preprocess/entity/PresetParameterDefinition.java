package com.moonju.preprocess.api.domain.preprocess.entity;

import java.util.Set;

public record PresetParameterDefinition(
    String name,
    String description,
    PresetParameterType type,
    boolean required,
    String defaultValue,
    String minValue,
    String maxValue,
    Set<String> allowedValues
) {

    public static PresetParameterDefinition integer(
        String name,
        String description,
        boolean required,
        int defaultValue,
        int minValue,
        int maxValue
    ) {
        return new PresetParameterDefinition(
            name,
            description,
            PresetParameterType.INTEGER,
            required,
            String.valueOf(defaultValue),
            String.valueOf(minValue),
            String.valueOf(maxValue),
            Set.of()
        );
    }

    public static PresetParameterDefinition decimal(
        String name,
        String description,
        boolean required,
        double defaultValue,
        double minValue,
        double maxValue
    ) {
        return new PresetParameterDefinition(
            name,
            description,
            PresetParameterType.DECIMAL,
            required,
            String.valueOf(defaultValue),
            String.valueOf(minValue),
            String.valueOf(maxValue),
            Set.of()
        );
    }

    public static PresetParameterDefinition bool(String name, String description, boolean defaultValue) {
        return new PresetParameterDefinition(
            name,
            description,
            PresetParameterType.BOOLEAN,
            false,
            String.valueOf(defaultValue),
            null,
            null,
            Set.of()
        );
    }

    public static PresetParameterDefinition option(
        String name,
        String description,
        boolean required,
        String defaultValue,
        Set<String> allowedValues
    ) {
        return new PresetParameterDefinition(
            name,
            description,
            PresetParameterType.ENUM,
            required,
            defaultValue,
            null,
            null,
            allowedValues
        );
    }
}
