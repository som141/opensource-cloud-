package com.moonju.preprocess.worker.domain.preprocess.preset;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.preprocess.pipeline.PreprocessContext;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AutoPresetSelectorTests {

    @Test
    void reservesAutoSelectionBoundaryWithoutChangingPipelineContract() {
        AutoPresetSelector selector = new AutoPresetSelector();
        PreprocessContext context = new PreprocessContext(
            1L,
            10L,
            "originals/project/scan.png",
            "AUTO",
            Map.of(),
            false
        );

        PreprocessPresetName selected = selector.select(context);

        assertThat(selected).isEqualTo(PreprocessPresetName.A4_SCAN_300DPI);
    }
}
