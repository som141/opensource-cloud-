package com.moonju.preprocess.worker.domain.preprocess.preset;

import static java.util.Map.entry;

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
            Map.ofEntries(
                entry("targetDpi", "300"),
                entry("binarizationMode", "adaptive"),
                entry("adaptiveBlockSize", "21"),
                entry("adaptiveC", "5.0"),
                entry("contrastClipLimit", "2.0"),
                entry("contrastTileGridSize", "8"),
                entry("denoiseMode", "bilateral"),
                entry("denoiseKernelSize", "3"),
                entry("denoiseDiameter", "5"),
                entry("denoiseSigmaColor", "25.0"),
                entry("denoiseSigmaRange", "75.0"),
                entry("morphologyMode", "open_close"),
                entry("morphologyKernelSize", "2"),
                entry("sharpen", "false"),
                entry("sharpenAmount", "0.8"),
                entry("sharpenSigma", "1.5")
            )
        );
    }
}
