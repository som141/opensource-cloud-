package com.moonju.preprocess.worker.domain.preprocess.preset;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import org.junit.jupiter.api.Test;

class PreprocessPresetRegistryTests {

    private final PreprocessPresetRegistry registry = PreprocessPresetRegistry.builtIn();

    @Test
    void containsRequiredBackendPresetNames() {
        assertThat(registry.supportedNames()).containsExactlyInAnyOrder(
            PreprocessPresetName.A4_SCAN_300DPI,
            PreprocessPresetName.LOW_CONTRAST_SCAN,
            PreprocessPresetName.RECEIPT,
            PreprocessPresetName.NOISY_SCAN,
            PreprocessPresetName.AUTO
        );
    }

    @Test
    void a4PresetUsesFullDocumentPreprocessSequence() {
        PreprocessPreset preset = registry.findByName("A4_SCAN_300DPI");

        assertThat(preset.steps()).contains(
            PreprocessStepName.DESKEW,
            PreprocessStepName.CROP,
            PreprocessStepName.DENOISE,
            PreprocessStepName.CONTRAST_NORMALIZE,
            PreprocessStepName.BINARIZATION,
            PreprocessStepName.MORPHOLOGY_CLEANUP,
            PreprocessStepName.DPI_NORMALIZE,
            PreprocessStepName.OPTIONAL_SHARPEN
        );
        assertThat(preset.steps()).hasSize(11);
    }
}
