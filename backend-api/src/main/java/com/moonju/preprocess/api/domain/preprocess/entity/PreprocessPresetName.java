package com.moonju.preprocess.api.domain.preprocess.entity;

import java.util.Locale;

public enum PreprocessPresetName {
    A4_SCAN_300DPI,
    LOW_CONTRAST_SCAN,
    RECEIPT,
    NOISY_SCAN,
    AUTO;

    public static PreprocessPresetName from(String value) {
        return PreprocessPresetName.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
