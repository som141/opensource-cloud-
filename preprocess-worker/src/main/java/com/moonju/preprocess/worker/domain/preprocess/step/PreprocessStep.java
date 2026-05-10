package com.moonju.preprocess.worker.domain.preprocess.step;

import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;

public interface PreprocessStep {

    PreprocessStepName name();

    void execute(PreprocessContext context);
}
