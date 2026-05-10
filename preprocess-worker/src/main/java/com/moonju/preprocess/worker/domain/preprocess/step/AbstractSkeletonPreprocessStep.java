package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;

public abstract class AbstractSkeletonPreprocessStep implements PreprocessStep {

    @Override
    public void execute(PreprocessContext context) {
        context.recordStep(name(), "Skeleton step executed; OpenCV implementation is deferred.");
    }
}
