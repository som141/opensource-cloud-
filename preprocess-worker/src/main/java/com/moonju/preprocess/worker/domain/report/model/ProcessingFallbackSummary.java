package com.moonju.preprocess.worker.domain.report.model;

import com.moonju.preprocess.worker.domain.preprocess.model.FallbackNote;
import java.util.List;

public record ProcessingFallbackSummary(
    List<FallbackNote> fallbackNotes
) {

    public static ProcessingFallbackSummary empty() {
        return new ProcessingFallbackSummary(List.of());
    }
}
