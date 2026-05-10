package com.moonju.preprocess.worker.domain.preprocess.step;

import org.springframework.stereotype.Component;

@Component
public class ContrastNormalizeStep extends AbstractSkeletonPreprocessStep {

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.CONTRAST_NORMALIZE;
    }
}
