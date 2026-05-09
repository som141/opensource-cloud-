package com.moonju.preprocess.api.domain.preprocess.service;

import com.moonju.preprocess.api.domain.preprocess.dto.PresetDetailResponse;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetResponse;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateResponse;
import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPreset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreprocessPresetService {

    private final PreprocessPresetRegistry presetRegistry;
    private final PreprocessParameterValidator parameterValidator;

    public PreprocessPresetService(
        PreprocessPresetRegistry presetRegistry,
        PreprocessParameterValidator parameterValidator
    ) {
        this.presetRegistry = presetRegistry;
        this.parameterValidator = parameterValidator;
    }

    @Transactional(readOnly = true)
    public List<PresetResponse> findBuiltInPresets() {
        return presetRegistry.findAll()
            .stream()
            .map(PresetResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public PresetDetailResponse findDetail(String presetName) {
        return PresetDetailResponse.from(presetRegistry.findByName(presetName));
    }

    @Transactional(readOnly = true)
    public PresetValidateResponse validate(PresetValidateRequest request) {
        PreprocessPreset preset = presetRegistry.findByName(request.presetName());
        return parameterValidator.validate(preset, request.parameters());
    }
}
