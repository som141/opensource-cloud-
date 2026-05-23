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
                entry("targetDpi", "300"),
                entry("binarizationMode", "otsu"),
                entry("adaptiveBlockSize", "21"),
                entry("adaptiveC", "5.0"),
                entry("contrastClipLimit", "2.0"),
                entry("contrastTileGridSize", "8"),
                entry("denoiseMode", "median"),
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
