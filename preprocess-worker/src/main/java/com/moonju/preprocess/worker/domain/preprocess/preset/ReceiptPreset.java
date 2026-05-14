package com.moonju.preprocess.worker.domain.preprocess.preset;

import java.util.Map;
import org.springframework.stereotype.Component;
import static java.util.Map.entry;

@Component
public class ReceiptPreset implements PreprocessPresetDefinition {

    @Override
    public PreprocessPreset preset() {
        return new PreprocessPreset(
            PreprocessPresetName.RECEIPT,
            "Receipt",
            "Narrow receipt-like document preset.",
            DocumentStepSequence.standard(),
            Map.ofEntries(
                entry("targetDpi", "300"),
                entry("binarizationMode", "adaptive"),
                entry("adaptiveBlockSize", "15"),
                entry("adaptiveC", "4.0"),
                entry("contrastClipLimit", "2.0"),
                entry("sharpen", "true"),
                entry("sharpenAmount", "0.8"),
                entry("sharpenSigma", "1.0"),
                entry("denoiseMode", "median"),
                entry("denoiseKernelSize", "3"),
                entry("morphologyMode", "open_close"),
                entry("morphologyKernelSize", "1")
            )
        );
    }
}
