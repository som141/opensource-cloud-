package com.moonju.preprocess.worker.domain.preprocess.step;

import org.springframework.stereotype.Component;

@Component
public class BinarizationStep extends AbstractSkeletonPreprocessStep {

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.BINARIZATION;
    }
}
