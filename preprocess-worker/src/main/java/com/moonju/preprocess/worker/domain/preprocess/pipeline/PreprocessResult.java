package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.model.FallbackNote;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import java.time.Duration;
import java.util.List;

public record PreprocessResult(
    Long jobId,
    Long itemId,
    String presetName,
    List<PreprocessStepExecution> stepExecutions,
    List<FallbackNote> fallbackNotes,
    Duration wallTime,
    boolean success,
    String errorMessage,
    boolean skeletonOnly
) {

    public static PreprocessResult from(PreprocessContext context, boolean skeletonOnly) {
        return from(context, skeletonOnly, Duration.ZERO, true, null);
    }

    public static PreprocessResult from(
        PreprocessContext context,
        boolean skeletonOnly,
        Duration wallTime,
        boolean success,
        String errorMessage
    ) {
        return new PreprocessResult(
            context.jobId(),
            context.itemId(),
            context.presetName(),
            context.stepExecutions(),
            context.fallbackNotes(),
            wallTime,
            success,
            errorMessage,
            skeletonOnly
        );
    }

    public List<PreprocessStepName> executedStepNames() {
        return stepExecutions.stream()
            .map(PreprocessStepExecution::stepName)
            .toList();
    }
}
