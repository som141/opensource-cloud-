package com.moonju.preprocess.api.domain.preprocess.controller;

import com.moonju.preprocess.api.domain.preprocess.dto.CustomPresetCreateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.CustomPresetResponse;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetDetailResponse;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetResponse;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateResponse;
import com.moonju.preprocess.api.domain.preprocess.service.CustomPreprocessPresetService;
import com.moonju.preprocess.api.domain.preprocess.service.PreprocessPresetService;
import com.moonju.preprocess.api.global.error.ErrorCode;
import com.moonju.preprocess.api.global.response.ApiResponse;
import com.moonju.preprocess.api.global.support.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preprocess/presets")
public class PreprocessPresetController {

    private final PreprocessPresetService presetService;
    private final CustomPreprocessPresetService customPresetService;

    public PreprocessPresetController(
        PreprocessPresetService presetService,
        CustomPreprocessPresetService customPresetService
    ) {
        this.presetService = presetService;
        this.customPresetService = customPresetService;
    }

    @GetMapping
    public ApiResponse<List<PresetResponse>> findBuiltInPresets() {
        return ApiResponse.success(presetService.findBuiltInPresets());
    }

    @GetMapping("/{presetName}")
    public ApiResponse<PresetDetailResponse> findDetail(@PathVariable String presetName) {
        return ApiResponse.success(presetService.findDetail(presetName));
    }

    @PostMapping("/validate")
    public ApiResponse<PresetValidateResponse> validate(@Valid @RequestBody PresetValidateRequest request) {
        return ApiResponse.success(presetService.validate(request));
    }

    @PostMapping("/custom")
    public ApiResponse<CustomPresetResponse> createCustomPreset(
        @CurrentUser Long currentUserId,
        @Valid @RequestBody CustomPresetCreateRequest request
    ) {
        return ApiResponse.success(ErrorCode.COMMON_CREATED, customPresetService.create(currentUserId, request));
    }

    @GetMapping("/custom")
    public ApiResponse<List<CustomPresetResponse>> findMyCustomPresets(@CurrentUser Long currentUserId) {
        return ApiResponse.success(customPresetService.findMyPresets(currentUserId));
    }

    @DeleteMapping("/custom/{presetId}")
    public ApiResponse<Void> deleteCustomPreset(
        @CurrentUser Long currentUserId,
        @PathVariable Long presetId
    ) {
        customPresetService.delete(currentUserId, presetId);
        return ApiResponse.success(ErrorCode.COMMON_NO_CONTENT, null);
    }
}
