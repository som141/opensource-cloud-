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
            .containsEntry("grayscale", "true")
            .containsEntry("referenceWidthInches", "8.27")
            .containsEntry("referenceHeightInches", "11.69")
            .containsEntry("fallbackSourceDpi", "300")
            .containsEntry("contrastNormalize", "false")
            .containsEntry("contrastClipLimit", "2.5")
            .containsEntry("adaptiveBlockSize", "31")
            .containsEntry("adaptiveC", "15.0")
            .containsEntry("denoiseMode", "median")
            .containsEntry("denoiseDiameter", "7")
            .containsEntry("denoiseSigmaColor", "50.0")
            .containsEntry("denoiseSigmaRange", "50.0")
            .containsEntry("morphologyMode", "open")
            .containsEntry("sharpenAmount", "0.25")
            .containsEntry("sharpenSigma", "1.2");

        assertThat(registry.findByName("LOW_CONTRAST_SCAN").defaultParameters())
            .containsEntry("contrastNormalize", "true")
            .containsEntry("contrastClipLimit", "2.5")
            .containsEntry("denoiseMode", "median")
            .containsEntry("morphologyMode", "close")
            .containsEntry("sharpen", "true");

        assertThat(registry.findByName("RECEIPT").defaultParameters())
            .containsEntry("contrastNormalize", "true")
            .containsEntry("contrastClipLimit", "2.5")
            .containsEntry("denoiseMode", "median")
            .containsEntry("morphologyMode", "close")
            .containsEntry("morphologyKernelSize", "2");

        assertThat(registry.findByName("NOISY_SCAN").defaultParameters())
            .containsEntry("contrastNormalize", "false")
            .containsEntry("contrastClipLimit", "2.5")
            .containsEntry("denoiseMode", "bilateral")
            .containsEntry("denoiseSigmaRange", "50.0");
    }
}
