package com.moonju.preprocess.worker.domain.preprocess.preset;

import static java.util.Map.entry;

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
            Map.ofEntries(
                entry("targetDpi", "300"),
                entry("binarizationMode", "adaptive"),
                entry("adaptiveBlockSize", "21"),
                entry("adaptiveC", "5.0"),
                entry("contrastClipLimit", "2.4"),
                entry("contrastTileGridSize", "8"),
                entry("denoiseMode", "median"),
                entry("denoiseKernelSize", "3"),
                entry("denoiseDiameter", "5"),
                entry("denoiseSigmaColor", "25.0"),
                entry("denoiseSigmaRange", "75.0"),
                entry("morphologyMode", "open_close"),
                entry("morphologyKernelSize", "2"),
                entry("sharpen", "true"),
                entry("sharpenAmount", "0.8"),
                entry("sharpenSigma", "1.5")
            )
        );
    }
}
