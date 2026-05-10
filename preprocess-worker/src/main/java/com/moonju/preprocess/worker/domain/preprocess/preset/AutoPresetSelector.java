package com.moonju.preprocess.worker.domain.preprocess.preset;

import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import org.springframework.stereotype.Component;

@Component
public class AutoPresetSelector {

    public PreprocessPresetName select(PreprocessContext context) {
        return PreprocessPresetName.A4_SCAN_300DPI;
    }
}
