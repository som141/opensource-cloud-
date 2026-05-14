package com.moonju.preprocess.worker.domain.preprocess.preset;

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
            Map.of(
                "targetDpi", "300",
                "binarizationMode", "adaptive",
                "adaptiveBlockSize", "21",
                "adaptiveC", "5.0",
                "contrastClipLimit", "2.0",
                "sharpen", "false",
                "denoiseMode", "median",
                "denoiseKernelSize", "5",
                "morphologyMode", "open_close",
                "morphologyKernelSize", "1"
            )
        );
    }
}
