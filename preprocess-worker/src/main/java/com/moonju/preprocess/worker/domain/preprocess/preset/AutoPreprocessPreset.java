package com.moonju.preprocess.worker.domain.preprocess.preset;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AutoPreprocessPreset implements PreprocessPresetDefinition {

    @Override
    public PreprocessPreset preset() {
        return new PreprocessPreset(
            PreprocessPresetName.AUTO,
            "Auto preset",
            "Skeleton preset that reserves Worker-side preset selection based on image characteristics.",
            DocumentStepSequence.standard(),
            Map.of("debugArtifacts", "false")
        );
    }
}
