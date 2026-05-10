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
        List<ProcessingStepReport> steps = result.stepExecutions().stream()
            .map(step -> new ProcessingStepReport(step.stepName(), step.note(), ProcessingTiming.skeleton()))
            .toList();
        return new ProcessingReport(
            result.jobId(),
            result.itemId(),
            result.presetName(),
            steps,
            ProcessingTiming.skeleton(),
            ProcessingMemoryUsage.notSampled(),
            ProcessingFallbackSummary.empty(),
            !result.skeletonOnly(),
            result.skeletonOnly() ? "PIPELINE_NOT_IMPLEMENTED" : null
        );
    }
}
