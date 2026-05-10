package com.moonju.preprocess.worker.domain.report.model;

import java.time.Duration;

public record ProcessingTiming(
    Duration wallTime,
    Duration cpuTime
) {

    public static ProcessingTiming skeleton() {
        return new ProcessingTiming(Duration.ZERO, Duration.ZERO);
    }
}
