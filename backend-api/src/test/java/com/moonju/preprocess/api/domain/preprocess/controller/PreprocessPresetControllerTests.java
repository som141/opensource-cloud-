package com.moonju.preprocess.api.domain.preprocess.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.preprocess.dto.CustomPresetCreateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.CustomPresetResponse;
import com.moonju.preprocess.api.domain.preprocess.service.CustomPreprocessPresetService;
import com.moonju.preprocess.api.domain.preprocess.service.PreprocessPresetService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PreprocessPresetControllerTests {

    @Mock
    private PreprocessPresetService presetService;

    @Mock
    private CustomPreprocessPresetService customPresetService;

    @Test
    void createsCustomPresetWithCommonCreatedResponse() {
        PreprocessPresetController controller = new PreprocessPresetController(presetService, customPresetService);
        CustomPresetCreateRequest request = new CustomPresetCreateRequest(
            "Low contrast",
            null,
            "LOW_CONTRAST_SCAN",
            Map.of("targetDpi", "300")
        );
        CustomPresetResponse serviceResponse = new CustomPresetResponse(
            10L,
            "Low contrast",
            null,
            "LOW_CONTRAST_SCAN",
            Map.of("targetDpi", "300"),
            null
        );
        when(customPresetService.create(1L, request)).thenReturn(serviceResponse);

        ApiResponse<CustomPresetResponse> response = controller.createCustomPreset(1L, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common201");
        assertThat(response.result()).isSameAs(serviceResponse);
    }

    @Test
    void deletesCustomPresetWithCommonNoContentResponse() {
        PreprocessPresetController controller = new PreprocessPresetController(presetService, customPresetService);

        ApiResponse<Void> response = controller.deleteCustomPreset(1L, 10L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common204");
        verify(customPresetService).delete(1L, 10L);
    }
}
