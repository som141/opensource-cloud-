package com.moonju.preprocess.api.domain.preprocess.entity;

import java.util.List;

public class PreprocessPreset {

    private final PreprocessPresetName name;
    private final PresetType type;
    private final String displayName;
    private final String description;
    private final boolean supportsDebug;
    private final List<String> steps;
    private final List<PresetParameterDefinition> parameters;

    public PreprocessPreset(
        PreprocessPresetName name,
        PresetType type,
        String displayName,
        String description,
        boolean supportsDebug,
        List<String> steps,
        List<PresetParameterDefinition> parameters
    ) {
        this.name = name;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.supportsDebug = supportsDebug;
        this.steps = List.copyOf(steps);
        this.parameters = List.copyOf(parameters);
    }

    public PreprocessPresetName getName() {
        return name;
    }

    public PresetType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSupportsDebug() {
        return supportsDebug;
    }

    public List<String> getSteps() {
        return steps;
    }

    public List<PresetParameterDefinition> getParameters() {
        return parameters;
    }
}
