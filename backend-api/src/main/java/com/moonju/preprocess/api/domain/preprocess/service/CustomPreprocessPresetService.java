package com.moonju.preprocess.api.domain.preprocess.service;

import com.moonju.preprocess.api.domain.preprocess.dto.CustomPresetCreateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.CustomPresetResponse;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateResponse;
import com.moonju.preprocess.api.domain.preprocess.entity.CustomPreprocessPreset;
import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPreset;
import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPresetName;
import com.moonju.preprocess.api.domain.preprocess.exception.InvalidPresetParameterException;
import com.moonju.preprocess.api.domain.preprocess.exception.PresetNotFoundException;
import com.moonju.preprocess.api.domain.preprocess.repository.CustomPreprocessPresetRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomPreprocessPresetService {

    private final CustomPreprocessPresetRepository customPresetRepository;
    private final PreprocessPresetRegistry presetRegistry;
    private final PreprocessParameterValidator parameterValidator;

    public CustomPreprocessPresetService(
        CustomPreprocessPresetRepository customPresetRepository,
        PreprocessPresetRegistry presetRegistry,
        PreprocessParameterValidator parameterValidator
    ) {
        this.customPresetRepository = customPresetRepository;
        this.presetRegistry = presetRegistry;
        this.parameterValidator = parameterValidator;
    }

    @Transactional
    public CustomPresetResponse create(Long currentUserId, CustomPresetCreateRequest request) {
        PreprocessPreset basePreset = presetRegistry.findByName(request.basePresetName());
        PresetValidateResponse validation = parameterValidator.validate(basePreset, request.parameters());
        if (!validation.valid()) {
            throw new InvalidPresetParameterException(String.join(", ", validation.errors()));
        }

        CustomPreprocessPreset customPreset = customPresetRepository.save(new CustomPreprocessPreset(
            currentUserId,
            normalizeName(request.name()),
            request.description(),
            parseBasePresetName(request.basePresetName()),
            validation.resolvedParameters()
        ));
        return CustomPresetResponse.from(customPreset);
    }

    @Transactional(readOnly = true)
    public List<CustomPresetResponse> findMyPresets(Long currentUserId) {
        return customPresetRepository.findAllByUserIdAndDeletedFalseOrderByIdDesc(currentUserId)
            .stream()
            .map(CustomPresetResponse::from)
            .toList();
    }

    @Transactional
    public void delete(Long currentUserId, Long presetId) {
        CustomPreprocessPreset preset = customPresetRepository
            .findByIdAndUserIdAndDeletedFalse(presetId, currentUserId)
            .orElseThrow(PresetNotFoundException::new);
        preset.delete();
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new InvalidPresetParameterException("Custom preset name must not be blank.");
        }
        return name.trim();
    }

    private PreprocessPresetName parseBasePresetName(String basePresetName) {
        try {
            return PreprocessPresetName.from(basePresetName);
        } catch (IllegalArgumentException exception) {
            throw new PresetNotFoundException();
        }
    }
}
