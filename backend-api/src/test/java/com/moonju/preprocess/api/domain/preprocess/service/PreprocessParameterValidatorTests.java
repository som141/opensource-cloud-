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
            .containsEntry("targetDpi", "300")
            .containsEntry("binarizationMode", "otsu")
            .containsEntry("adaptiveBlockSize", "21")
            .containsEntry("adaptiveC", "5.0")
            .containsEntry("contrastClipLimit", "2.0")
            .containsEntry("denoiseMode", "median")
            .containsEntry("denoiseSigmaColor", "25.0")
            .containsEntry("denoiseSigmaRange", "75.0")
            .containsEntry("morphologyMode", "open_close")
            .containsEntry("morphologyKernelSize", "2")
            .containsEntry("sharpenAmount", "0.8")
            .containsEntry("sharpenSigma", "1.5")
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
