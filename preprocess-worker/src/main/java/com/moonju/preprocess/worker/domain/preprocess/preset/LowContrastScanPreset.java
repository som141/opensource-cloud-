package com.moonju.preprocess.worker.domain.preprocess.preset;

import java.util.Map;
import org.springframework.stereotype.Component;
import static java.util.Map.entry;

@Component
public class LowContrastScanPreset implements PreprocessPresetDefinition {

    @Override
    public PreprocessPreset preset() {
        return new PreprocessPreset(
            PreprocessPresetName.LOW_CONTRAST_SCAN,
            "Low contrast scan",
            "Low contrast document preset with stronger contrast normalization.",
            DocumentStepSequence.standard(),
            Map.ofEntries(
                entry("targetDpi", "300"),
                entry("binarizationMode", "adaptive"),
                entry("adaptiveBlockSize", "21"),
                entry("adaptiveC", "5.0"),
                entry("contrastClipLimit", "3.0"),
                entry("sharpen", "true"),
                entry("sharpenAmount", "1.0"),
                entry("sharpenSigma", "1.5"),
                entry("denoiseMode", "bilateral"),
                entry("morphologyMode", "open_close"),
                entry("morphologyKernelSize", "1")
            )
        );
    }
}
