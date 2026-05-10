package com.moonju.preprocess.worker.domain.report.dto;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import com.moonju.preprocess.worker.domain.report.model.ProcessingTiming;

public record ProcessingStepReport(
    PreprocessStepName stepName,
    String note,
    ProcessingTiming timing
) {
}
