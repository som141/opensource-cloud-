package com.moonju.preprocess.worker.domain.preprocess.step;

import org.springframework.stereotype.Component;

@Component
public class ColorNormalizeStep extends AbstractSkeletonPreprocessStep {

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.COLOR_NORMALIZE;
    }
}
