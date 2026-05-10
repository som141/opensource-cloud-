package com.moonju.preprocess.worker.domain.report.dto;

import com.moonju.preprocess.worker.domain.preprocess.model.DebugArtifactDescriptor;
import com.moonju.preprocess.worker.domain.report.model.ProcessingFallbackSummary;
import com.moonju.preprocess.worker.domain.report.model.ProcessingMemoryUsage;
import com.moonju.preprocess.worker.domain.report.model.ProcessingTiming;
import java.util.List;

public record ProcessingReport(
    Long jobId,
    Long itemId,
    String presetName,
    List<ProcessingStepReport> steps,
    ProcessingTiming timing,
    ProcessingMemoryUsage memoryUsage,
    ProcessingFallbackSummary fallbackSummary,
    List<DebugArtifactDescriptor> debugArtifacts,
    boolean success,
    String errorMessage
) {
}
