package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import java.time.Duration;
import java.time.Instant;

public record PreprocessStepExecution(
    PreprocessStepName stepName,
    String note,
    Instant startedAt,
    Instant endedAt,
    Duration wallTime,
    boolean success,
    String errorMessage
) {

    public static PreprocessStepExecution succeeded(
        PreprocessStepName stepName,
        String note,
        Instant startedAt,
        Instant endedAt,
        Duration wallTime
    ) {
        return new PreprocessStepExecution(stepName, note, startedAt, endedAt, wallTime, true, null);
    }

    public static PreprocessStepExecution failed(
        PreprocessStepName stepName,
        String note,
        Instant startedAt,
        Instant endedAt,
        Duration wallTime,
        String errorMessage
    ) {
        return new PreprocessStepExecution(stepName, note, startedAt, endedAt, wallTime, false, errorMessage);
    }
}
