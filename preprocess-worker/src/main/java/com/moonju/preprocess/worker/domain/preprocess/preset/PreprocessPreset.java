package com.moonju.preprocess.worker.domain.preprocess.preset;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import java.util.List;
import java.util.Map;

public record PreprocessPreset(
    PreprocessPresetName name,
    String displayName,
    String description,
    List<PreprocessStepName> steps,
    Map<String, String> defaultParameters
) {
}
