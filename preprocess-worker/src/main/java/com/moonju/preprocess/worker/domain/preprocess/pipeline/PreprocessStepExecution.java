package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;

public record PreprocessStepExecution(
    PreprocessStepName stepName,
    String note
) {
}
