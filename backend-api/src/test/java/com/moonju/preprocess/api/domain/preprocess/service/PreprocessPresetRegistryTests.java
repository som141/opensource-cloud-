package com.moonju.preprocess.api.domain.preprocess.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPresetName;
import com.moonju.preprocess.api.domain.preprocess.exception.PresetNotFoundException;
import org.junit.jupiter.api.Test;

class PreprocessPresetRegistryTests {

    private final PreprocessPresetRegistry registry = new PreprocessPresetRegistry();

    @Test
    void providesRequiredBuiltInPresets() {
        assertThat(registry.findAll())
            .extracting(preset -> preset.getName().name())
            .containsExactly(
                "A4_SCAN_300DPI",
                "LOW_CONTRAST_SCAN",
                "RECEIPT",
                "NOISY_SCAN",
                "AUTO"
            );
    }

    @Test
    void findsPresetByCaseInsensitiveName() {
        assertThat(registry.findByName("low_contrast_scan").getName())
            .isEqualTo(PreprocessPresetName.LOW_CONTRAST_SCAN);
    }

    @Test
    void rejectsUnknownPresetName() {
        assertThatThrownBy(() -> registry.findByName("resize_only"))
            .isInstanceOf(PresetNotFoundException.class);
    }

    @Test
    void exposesWorkerQualityParameters() {
        assertThat(registry.findByName("NOISY_SCAN").getParameters())
            .extracting(parameter -> parameter.name())
            .contains(
                "adaptiveBlockSize",
                "adaptiveC",
                "denoiseMode",
                "denoiseSigmaColor",
                "denoiseSigmaRange",
                "morphologyMode",
                "morphologyKernelSize",
                "sharpenAmount",
                "sharpenSigma"
            );
    }
}
