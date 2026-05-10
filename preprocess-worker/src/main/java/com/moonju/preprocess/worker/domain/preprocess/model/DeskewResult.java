package com.moonju.preprocess.worker.domain.preprocess.model;

public record DeskewResult(
    double angleDegrees,
    String method,
    boolean applied
) {

    public static DeskewResult skipped(String reason) {
        return new DeskewResult(0.0, reason, false);
    }
}
