package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import java.util.List;

public record PreprocessResult(
    Long jobId,
    Long itemId,
    String presetName,
    List<PreprocessStepExecution> stepExecutions,
    boolean skeletonOnly
) {

    public static PreprocessResult from(PreprocessContext context, boolean skeletonOnly) {
        return new PreprocessResult(
            context.jobId(),
            context.itemId(),
            context.presetName(),
            context.stepExecutions(),
            skeletonOnly
        );
    }

    public List<PreprocessStepName> executedStepNames() {
        return stepExecutions.stream()
            .map(PreprocessStepExecution::stepName)
            .toList();
    }
}
