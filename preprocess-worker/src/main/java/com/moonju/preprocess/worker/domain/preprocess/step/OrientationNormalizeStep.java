package com.moonju.preprocess.worker.domain.preprocess.step;

import org.springframework.stereotype.Component;

@Component
public class OrientationNormalizeStep extends AbstractSkeletonPreprocessStep {

    @Override
    public PreprocessStepName name() {
        return PreprocessStepName.ORIENTATION_NORMALIZE;
    }
}
