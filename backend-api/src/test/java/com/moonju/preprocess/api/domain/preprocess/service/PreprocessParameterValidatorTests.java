package com.moonju.preprocess.api.domain.preprocess.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreprocessParameterValidatorTests {

    private final PreprocessPresetRegistry registry = new PreprocessPresetRegistry();
    private final PreprocessParameterValidator validator = new PreprocessParameterValidator();

    @Test
    void resolvesDefaultParametersWhenRequestIsEmpty() {
        PresetValidateResponse response = validator.validate(
            registry.findByName("A4_SCAN_300DPI"),
            Map.of()
        );

        assertThat(response.valid()).isTrue();
        assertThat(response.resolvedParameters())
            .containsEntry("grayscale", "true")
            .containsEntry("targetDpi", "300")
            .containsEntry("referenceWidthInches", "8.27")
            .containsEntry("referenceHeightInches", "11.69")
            .containsEntry("fallbackSourceDpi", "300")
            .containsEntry("binarizationMode", "otsu")
            .containsEntry("adaptiveBlockSize", "31")
            .containsEntry("adaptiveC", "15.0")
            .containsEntry("contrastNormalize", "false")
            .containsEntry("contrastClipLimit", "2.5")
            .containsEntry("denoiseMode", "median")
            .containsEntry("denoiseDiameter", "7")
            .containsEntry("denoiseSigmaColor", "50.0")
            .containsEntry("denoiseSigmaRange", "50.0")
            .containsEntry("morphologyMode", "open")
            .containsEntry("morphologyKernelSize", "2")
            .containsEntry("sharpenAmount", "0.25")
            .containsEntry("sharpenSigma", "1.2")
            .containsEntry("debugArtifacts", "false");
    }

    @Test
    void rejectsOutOfRangeAndUnknownParameters() {
        PresetValidateResponse response = validator.validate(
            registry.findByName("A4_SCAN_300DPI"),
            Map.of(
                "targetDpi", "1000",
                "unknown", "value"
            )
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors())
            .contains("Unknown parameter: unknown")
            .contains("targetDpi must be between 150 and 600.");
    }

    @Test
    void rejectsInvalidEnumValue() {
        PresetValidateResponse response = validator.validate(
            registry.findByName("LOW_CONTRAST_SCAN"),
            Map.of("binarizationMode", "global")
        );

        assertThat(response.valid()).isFalse();
        assertThat(response.errors().getFirst()).contains("binarizationMode must be one of");
    }
}
