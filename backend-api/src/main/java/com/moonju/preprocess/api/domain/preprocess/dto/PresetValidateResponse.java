package com.moonju.preprocess.api.domain.preprocess.dto;

import java.util.List;
import java.util.Map;

public record PresetValidateResponse(
    String presetName,
    boolean valid,
    List<String> errors,
    Map<String, String> resolvedParameters
) {

    public static PresetValidateResponse valid(String presetName, Map<String, String> resolvedParameters) {
        return new PresetValidateResponse(presetName, true, List.of(), resolvedParameters);
    }

    public static PresetValidateResponse invalid(
        String presetName,
        List<String> errors,
        Map<String, String> resolvedParameters
    ) {
        return new PresetValidateResponse(presetName, false, errors, resolvedParameters);
    }
}
