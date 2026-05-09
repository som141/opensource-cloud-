package com.moonju.preprocess.api.domain.preprocess.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.api.domain.preprocess.dto.PresetDetailResponse;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetResponse;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PreprocessPresetServiceTests {

    private final PreprocessPresetService service = new PreprocessPresetService(
        new PreprocessPresetRegistry(),
        new PreprocessParameterValidator()
    );

    @Test
    void listsBuiltInPresets() {
        List<PresetResponse> responses = service.findBuiltInPresets();

        assertThat(responses).hasSize(5);
        assertThat(responses).extracting(PresetResponse::name).contains("NOISY_SCAN", "AUTO");
    }

    @Test
    void findsPresetDetailWithDocumentPipelineSteps() {
        PresetDetailResponse response = service.findDetail("A4_SCAN_300DPI");

        assertThat(response.steps()).contains("DESKEW", "DPI_NORMALIZE", "OPTIONAL_SHARPEN");
        assertThat(response.parameters()).extracting(parameter -> parameter.name()).contains("targetDpi");
    }

    @Test
    void validatesPresetParameters() {
        PresetValidateResponse response = service.validate(new PresetValidateRequest(
            "RECEIPT",
            Map.of("targetDpi", "300", "sharpen", "true")
        ));

        assertThat(response.valid()).isTrue();
        assertThat(response.resolvedParameters()).containsEntry("targetDpi", "300");
    }
}
