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
                entry("grayscale", "true"),
                entry("targetDpi", "300"),
                entry("referenceWidthInches", "8.27"),
                entry("referenceHeightInches", "11.69"),
                entry("fallbackSourceDpi", "300"),
                entry("binarizationMode", "adaptive"),
                entry("adaptiveBlockSize", "31"),
                entry("adaptiveC", "15.0"),
                entry("contrastNormalize", "false"),
                entry("contrastClipLimit", "2.5"),
                entry("contrastTileGridSize", "8"),
                entry("denoiseMode", "bilateral"),
                entry("denoiseKernelSize", "3"),
                entry("denoiseDiameter", "7"),
                entry("denoiseSigmaColor", "50.0"),
                entry("denoiseSigmaRange", "50.0"),
                entry("morphologyMode", "open"),
                entry("morphologyKernelSize", "2"),
                entry("sharpen", "false"),
                entry("sharpenAmount", "0.25"),
                entry("sharpenSigma", "1.2")
            )
        );
    }
}
