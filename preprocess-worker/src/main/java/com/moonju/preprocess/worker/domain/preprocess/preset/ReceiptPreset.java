package com.moonju.preprocess.worker.domain.preprocess.preset;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReceiptPreset implements PreprocessPresetDefinition {

    @Override
    public PreprocessPreset preset() {
        return new PreprocessPreset(
            PreprocessPresetName.RECEIPT,
            "Receipt",
            "Narrow receipt-like document preset.",
            DocumentStepSequence.standard(),
            Map.of(
                "targetDpi", "300",
                "binarizationMode", "adaptive",
                "contrastClipLimit", "1.4",
                "sharpen", "true"
            )
        );
    }
}
