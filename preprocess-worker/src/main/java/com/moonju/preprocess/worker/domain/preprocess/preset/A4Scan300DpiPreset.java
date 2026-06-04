package com.moonju.preprocess.worker.domain.preprocess.preset;

import static java.util.Map.entry;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class A4Scan300DpiPreset implements PreprocessPresetDefinition {

    @Override
    public PreprocessPreset preset() {
        return new PreprocessPreset(
            PreprocessPresetName.A4_SCAN_300DPI,
            "A4 300 DPI scan",
            "General A4 document scan preset for OCR preprocessing.",
            DocumentStepSequence.standard(),
            Map.ofEntries(
                entry("grayscale", "true"),
                entry("targetDpi", "300"),
                entry("referenceWidthInches", "8.27"),
                entry("referenceHeightInches", "11.69"),
                entry("fallbackSourceDpi", "300"),
                entry("binarizationMode", "otsu"),
                entry("adaptiveBlockSize", "31"),
                entry("adaptiveC", "15.0"),
                entry("contrastNormalize", "false"),
                entry("contrastClipLimit", "2.5"),
                entry("contrastTileGridSize", "8"),
                entry("denoiseMode", "median"),
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
