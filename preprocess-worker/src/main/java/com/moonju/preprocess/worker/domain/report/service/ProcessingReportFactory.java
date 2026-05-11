package com.moonju.preprocess.worker.domain.report.service;

import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessResult;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReport;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingStepReport;
import com.moonju.preprocess.worker.domain.report.model.ProcessingFallbackSummary;
import com.moonju.preprocess.worker.domain.report.model.ProcessingMemoryUsage;
import com.moonju.preprocess.worker.domain.report.model.ProcessingTiming;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProcessingReportFactory {

    public ProcessingReport createSkeletonReport(PreprocessResult result) {
        return createReport(result);
    }

    public ProcessingReport createReport(PreprocessResult result) {
        List<ProcessingStepReport> steps = result.stepExecutions().stream()
            .map(step -> new ProcessingStepReport(
                step.stepName(),
                step.note(),
                ProcessingTiming.wallOnly(step.wallTime())
            ))
            .toList();
        return new ProcessingReport(
            result.jobId(),
            result.itemId(),
            result.presetName(),
            steps,
            ProcessingTiming.wallOnly(result.wallTime()),
            ProcessingMemoryUsage.notSampled(),
            new ProcessingFallbackSummary(result.fallbackNotes()),
            result.debugArtifacts(),
            result.success() && !result.skeletonOnly(),
            reportErrorMessage(result)
        );
    }

    private String reportErrorMessage(PreprocessResult result) {
        if (!result.success()) {
            return result.errorMessage();
        }
        return result.skeletonOnly() ? "PIPELINE_NOT_IMPLEMENTED" : null;
    }
}
