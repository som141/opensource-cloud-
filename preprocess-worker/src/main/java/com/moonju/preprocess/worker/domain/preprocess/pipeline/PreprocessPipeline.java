package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPreset;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStep;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepCatalog;
import java.util.List;

public record PreprocessPipeline(
    PreprocessPreset preset,
    List<PreprocessStep> steps
) {

    public static PreprocessPipeline from(PreprocessPreset preset, PreprocessStepCatalog stepCatalog) {
        return new PreprocessPipeline(preset, stepCatalog.resolveAll(preset.steps()));
    }
}
