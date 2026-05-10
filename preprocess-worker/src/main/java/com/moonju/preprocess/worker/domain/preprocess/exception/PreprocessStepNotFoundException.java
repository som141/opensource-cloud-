package com.moonju.preprocess.worker.domain.preprocess.exception;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;

public class PreprocessStepNotFoundException extends RuntimeException {

    public PreprocessStepNotFoundException(PreprocessStepName stepName) {
        super("Preprocess step is not registered: " + stepName);
    }
}
