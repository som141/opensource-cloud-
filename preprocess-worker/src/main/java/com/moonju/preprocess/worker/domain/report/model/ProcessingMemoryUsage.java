package com.moonju.preprocess.worker.domain.report.model;

public record ProcessingMemoryUsage(
    long sampledPeakMemoryBytes
) {

    public static ProcessingMemoryUsage notSampled() {
        return new ProcessingMemoryUsage(0L);
    }
}
