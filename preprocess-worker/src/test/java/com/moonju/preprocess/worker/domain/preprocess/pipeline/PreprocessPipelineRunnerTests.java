package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.preprocess.preset.PreprocessPresetRegistry;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepCatalog;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreprocessPipelineRunnerTests {

    private final PreprocessPipelineRunner runner = new PreprocessPipelineRunner(
        PreprocessPresetRegistry.builtIn(),
        PreprocessStepCatalog.builtIn()
    );

    @Test
    void runsDocumentPipelineStepsInOcrPreprocessOrder() {
        PreprocessContext context = new PreprocessContext(
            1L,
            10L,
            "originals/project/scan.png",
            "LOW_CONTRAST_SCAN",
            Map.of("targetDpi", "300"),
            false
        );

        PreprocessResult result = runner.run(context);

        assertThat(result.skeletonOnly()).isTrue();
        assertThat(result.executedStepNames()).containsExactly(
            PreprocessStepName.DECODE,
            PreprocessStepName.COLOR_NORMALIZE,
            PreprocessStepName.ORIENTATION_NORMALIZE,
            PreprocessStepName.DESKEW,
            PreprocessStepName.CROP,
            PreprocessStepName.DENOISE,
            PreprocessStepName.CONTRAST_NORMALIZE,
            PreprocessStepName.BINARIZATION,
            PreprocessStepName.MORPHOLOGY_CLEANUP,
            PreprocessStepName.DPI_NORMALIZE,
            PreprocessStepName.OPTIONAL_SHARPEN
        );
    }
}
