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

    @Test
    void builtInPresetsExposeQualityTuningParameters() {
        assertThat(registry.findByName("A4_SCAN_300DPI").defaultParameters())
            .containsEntry("contrastClipLimit", "2.0")
            .containsEntry("adaptiveBlockSize", "21")
            .containsEntry("adaptiveC", "5.0")
            .containsEntry("denoiseMode", "median")
            .containsEntry("denoiseSigmaColor", "25.0")
            .containsEntry("morphologyMode", "open_close")
            .containsEntry("sharpenAmount", "0.8")
            .containsEntry("sharpenSigma", "1.5");

        assertThat(registry.findByName("LOW_CONTRAST_SCAN").defaultParameters())
            .containsEntry("contrastClipLimit", "2.4")
            .containsEntry("denoiseMode", "median")
            .containsEntry("sharpen", "true");

        assertThat(registry.findByName("RECEIPT").defaultParameters())
            .containsEntry("contrastClipLimit", "2.2")
            .containsEntry("denoiseMode", "median")
            .containsEntry("morphologyKernelSize", "2");

        assertThat(registry.findByName("NOISY_SCAN").defaultParameters())
            .containsEntry("contrastClipLimit", "2.0")
            .containsEntry("denoiseMode", "bilateral")
            .containsEntry("denoiseSigmaRange", "75.0");
    }
}
