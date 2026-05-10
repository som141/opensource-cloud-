package com.moonju.preprocess.worker.domain.preprocess.preset;

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
            Map.of(
                "targetDpi", "300",
                "binarizationMode", "otsu",
                "contrastClipLimit", "1.2",
                "sharpen", "false"
            )
        );
    }
}
