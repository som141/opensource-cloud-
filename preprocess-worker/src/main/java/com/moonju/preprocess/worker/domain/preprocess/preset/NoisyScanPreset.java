package com.moonju.preprocess.worker.domain.preprocess.preset;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NoisyScanPreset implements PreprocessPresetDefinition {

    @Override
    public PreprocessPreset preset() {
        return new PreprocessPreset(
            PreprocessPresetName.NOISY_SCAN,
            "Noisy scan",
            "Document preset reserved for strong background noise cases.",
            DocumentStepSequence.standard(),
            Map.of(
                "targetDpi", "300",
                "binarizationMode", "adaptive",
                "contrastClipLimit", "1.5",
                "sharpen", "false"
            )
        );
    }
}
