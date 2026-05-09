package com.moonju.preprocess.api.domain.preprocess.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.preprocess.dto.CustomPresetCreateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.CustomPresetResponse;
import com.moonju.preprocess.api.domain.preprocess.entity.CustomPreprocessPreset;
import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPresetName;
import com.moonju.preprocess.api.domain.preprocess.exception.InvalidPresetParameterException;
import com.moonju.preprocess.api.domain.preprocess.repository.CustomPreprocessPresetRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomPreprocessPresetServiceTests {

    @Mock
    private CustomPreprocessPresetRepository customPresetRepository;

    private final PreprocessPresetRegistry presetRegistry = new PreprocessPresetRegistry();

    private final PreprocessParameterValidator parameterValidator = new PreprocessParameterValidator();

    private CustomPreprocessPresetService service;

    @Test
    void createsCustomPresetWithValidatedParameters() {
        service = new CustomPreprocessPresetService(customPresetRepository, presetRegistry, parameterValidator);
        when(customPresetRepository.save(any(CustomPreprocessPreset.class))).thenAnswer(invocation -> {
            CustomPreprocessPreset preset = invocation.getArgument(0);
            ReflectionTestUtils.setField(preset, "id", 10L);
            return preset;
        });

        CustomPresetResponse response = service.create(1L, new CustomPresetCreateRequest(
            " Low contrast ",
            "For old scans",
            "LOW_CONTRAST_SCAN",
            Map.of("targetDpi", "300")
        ));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Low contrast");
        assertThat(response.parameters()).containsEntry("targetDpi", "300");
    }

    @Test
    void rejectsInvalidCustomPresetParameters() {
        service = new CustomPreprocessPresetService(customPresetRepository, presetRegistry, parameterValidator);

        assertThatThrownBy(() -> service.create(1L, new CustomPresetCreateRequest(
            "Invalid",
            null,
            "A4_SCAN_300DPI",
            Map.of("targetDpi", "1000")
        )))
            .isInstanceOf(InvalidPresetParameterException.class)
            .hasMessageContaining("targetDpi must be between 150 and 600");
    }

    @Test
    void listsMyCustomPresets() {
        service = new CustomPreprocessPresetService(customPresetRepository, presetRegistry, parameterValidator);
        CustomPreprocessPreset preset = preset(10L);
        when(customPresetRepository.findAllByUserIdAndDeletedFalseOrderByIdDesc(1L)).thenReturn(List.of(preset));

        List<CustomPresetResponse> responses = service.findMyPresets(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(10L);
    }

    @Test
    void softDeletesMyCustomPreset() {
        service = new CustomPreprocessPresetService(customPresetRepository, presetRegistry, parameterValidator);
        CustomPreprocessPreset preset = preset(10L);
        when(customPresetRepository.findByIdAndUserIdAndDeletedFalse(10L, 1L)).thenReturn(Optional.of(preset));

        service.delete(1L, 10L);

        assertThat(preset.isDeleted()).isTrue();
        verify(customPresetRepository).findByIdAndUserIdAndDeletedFalse(10L, 1L);
    }

    private CustomPreprocessPreset preset(Long id) {
        CustomPreprocessPreset preset = new CustomPreprocessPreset(
            1L,
            "Low contrast",
            "For old scans",
            PreprocessPresetName.LOW_CONTRAST_SCAN,
            Map.of("targetDpi", "300")
        );
        ReflectionTestUtils.setField(preset, "id", id);
        return preset;
    }
}
