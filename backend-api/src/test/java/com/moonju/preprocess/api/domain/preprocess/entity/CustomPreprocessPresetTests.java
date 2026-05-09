package com.moonju.preprocess.api.domain.preprocess.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CustomPreprocessPresetTests {

    @Test
    void createsCustomPresetFromBasePreset() {
        CustomPreprocessPreset preset = new CustomPreprocessPreset(
            1L,
            "Library low contrast",
            "For old scans",
            PreprocessPresetName.LOW_CONTRAST_SCAN,
            Map.of("targetDpi", "300")
        );

        assertThat(preset.getUserId()).isEqualTo(1L);
        assertThat(preset.getBasePresetName()).isEqualTo(PreprocessPresetName.LOW_CONTRAST_SCAN);
        assertThat(preset.getParameters()).containsEntry("targetDpi", "300");
        assertThat(preset.isDeleted()).isFalse();
    }

    @Test
    void softDeletesCustomPreset() {
        CustomPreprocessPreset preset = new CustomPreprocessPreset(
            1L,
            "Library low contrast",
            "For old scans",
            PreprocessPresetName.LOW_CONTRAST_SCAN,
            Map.of("targetDpi", "300")
        );

        preset.delete();

        assertThat(preset.isDeleted()).isTrue();
        assertThat(preset.getDeletedAt()).isNotNull();
    }
}
