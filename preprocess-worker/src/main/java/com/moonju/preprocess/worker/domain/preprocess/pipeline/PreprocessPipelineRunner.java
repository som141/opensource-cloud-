package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPreset;
import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPresetRegistry;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStep;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepCatalog;
import org.springframework.stereotype.Service;

@Service
public class PreprocessPipelineRunner {

    private final PreprocessPresetRegistry presetRegistry;
    private final PreprocessStepCatalog stepCatalog;

    public PreprocessPipelineRunner(PreprocessPresetRegistry presetRegistry, PreprocessStepCatalog stepCatalog) {
        this.presetRegistry = presetRegistry;
        this.stepCatalog = stepCatalog;
    }

    public PreprocessResult run(PreprocessContext context) {
        PreprocessPreset preset = presetRegistry.findByName(context.presetName());
        PreprocessPipeline pipeline = PreprocessPipeline.from(preset, stepCatalog);
        for (PreprocessStep step : pipeline.steps()) {
            step.execute(context);
        }
        return PreprocessResult.from(context, true);
    }
}
