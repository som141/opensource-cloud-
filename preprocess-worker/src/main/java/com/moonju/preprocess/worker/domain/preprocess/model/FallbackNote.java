package com.moonju.preprocess.worker.domain.preprocess.model;

public record FallbackNote(
    String stepName,
    String reason,
    String selectedStrategy
) {
}
