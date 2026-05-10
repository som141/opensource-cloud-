package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.exception.PreprocessStepNotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PreprocessStepCatalog {

    private final Map<PreprocessStepName, PreprocessStep> steps;

    public PreprocessStepCatalog(List<PreprocessStep> steps) {
        this.steps = new EnumMap<>(PreprocessStepName.class);
        for (PreprocessStep step : steps) {
            this.steps.put(step.name(), step);
        }
    }

    public static PreprocessStepCatalog builtIn() {
        return new PreprocessStepCatalog(List.of(
            new DecodeStep(),
            new ColorNormalizeStep(),
            new OrientationNormalizeStep(),
            new DeskewStep(),
            new CropStep(),
            new DenoiseStep(),
            new ContrastNormalizeStep(),
            new BinarizationStep(),
            new MorphologyCleanupStep(),
            new DpiNormalizeStep(),
            new SharpenStep()
        ));
    }

    public List<PreprocessStep> resolveAll(List<PreprocessStepName> names) {
        return names.stream().map(this::findByName).toList();
    }

    public List<PreprocessStepName> registeredNames() {
        return List.copyOf(steps.keySet());
    }

    private PreprocessStep findByName(PreprocessStepName name) {
        PreprocessStep step = steps.get(name);
        if (step == null) {
            throw new PreprocessStepNotFoundException(name);
        }
        return step;
    }
}
