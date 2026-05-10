package com.moonju.preprocess.worker.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessPipelineRunner;
import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessResult;
import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPresetRegistry;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepCatalog;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReport;
import com.moonju.preprocess.worker.domain.report.dto.ProcessingReportJson;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessingReportFactoryTests {

    private final PreprocessPipelineRunner runner = new PreprocessPipelineRunner(
        PreprocessPresetRegistry.builtIn(),
        PreprocessStepCatalog.builtIn()
    );
    private final ProcessingReportFactory factory = new ProcessingReportFactory();
    private final ProcessingReportWriter writer = new ProcessingReportWriter();

    @Test
    void createsSkeletonReportFromPipelineResult() {
        PreprocessContext context = new PreprocessContext(
            1L,
            2L,
            "originals/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            true
        );
        PreprocessResult result = runner.run(context);

        ProcessingReport report = factory.createSkeletonReport(result);
        ProcessingReportJson reportJson = writer.prepareJson(report);

        assertThat(report.jobId()).isEqualTo(1L);
        assertThat(report.itemId()).isEqualTo(2L);
        assertThat(report.presetName()).isEqualTo("LOW_CONTRAST_SCAN");
        assertThat(report.steps()).hasSize(11);
        assertThat(report.steps().getFirst().timing().wallTime()).isNotNull();
        assertThat(report.timing().wallTime()).isNotNull();
        assertThat(report.fallbackSummary().fallbackNotes()).isEmpty();
        assertThat(report.debugArtifacts()).isEmpty();
        assertThat(report.success()).isFalse();
        assertThat(report.errorMessage()).isEqualTo("PIPELINE_NOT_IMPLEMENTED");
        assertThat(reportJson.fileName()).isEqualTo("processing-report.json");
        assertThat(reportJson.schemaVersion()).isEqualTo("1.0");
    }

    @Test
    void carriesDebugArtifactMetadataIntoReport() {
        PreprocessContext context = new PreprocessContext(
            3L,
            1L,
            2L,
            "originals/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of(),
            true
        );
        context.recordDebugArtifact(PreprocessStepName.CROP, "03_crop.png");
        PreprocessResult result = PreprocessResult.from(
            context,
            true,
            java.time.Duration.ofMillis(1),
            true,
            null
        );

        ProcessingReport report = factory.createSkeletonReport(result);

        assertThat(report.debugArtifacts()).hasSize(1);
        assertThat(report.debugArtifacts().getFirst().objectKey()).isEqualTo("processed/3/1/2/debug/03_crop.png");
    }
}
