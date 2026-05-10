package com.moonju.preprocess.worker.domain.preprocess.preset;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LowContrastScanPreset implements PreprocessPresetDefinition {

    @Override
    public PreprocessPreset preset() {
        return new PreprocessPreset(
            PreprocessPresetName.LOW_CONTRAST_SCAN,
            "Low contrast scan",
            "Low contrast document preset with stronger contrast normalization.",
            DocumentStepSequence.standard(),
            Map.of(
                "targetDpi", "300",
                "binarizationMode", "adaptive",
                "contrastClipLimit", "1.6",
                "sharpen", "true"
            )
        );
    }
}
