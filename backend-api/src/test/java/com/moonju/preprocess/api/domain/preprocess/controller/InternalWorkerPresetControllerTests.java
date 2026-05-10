package com.moonju.preprocess.api.domain.preprocess.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.preprocess.dto.PresetResponse;
import com.moonju.preprocess.api.domain.preprocess.entity.PresetType;
import com.moonju.preprocess.api.domain.preprocess.service.PreprocessPresetService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalWorkerPresetControllerTests {

    @Mock
    private PreprocessPresetService presetService;

    @Test
    void findsBuiltInPresetsForWorker() {
        InternalWorkerPresetController controller = new InternalWorkerPresetController(presetService);
        List<PresetResponse> presets = List.of(
            new PresetResponse("A4_SCAN_300DPI", PresetType.BUILT_IN, "A4 scan", "A4 scan preset", true)
        );
        when(presetService.findBuiltInPresets()).thenReturn(presets);

        ApiResponse<List<PresetResponse>> response = controller.findBuiltInPresets();

        assertThat(response.result()).isSameAs(presets);
        verify(presetService).findBuiltInPresets();
    }
}
