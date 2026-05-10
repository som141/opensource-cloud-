package com.moonju.preprocess.api.domain.preprocess.controller;

import com.moonju.preprocess.api.domain.preprocess.dto.PresetResponse;
import com.moonju.preprocess.api.domain.preprocess.service.PreprocessPresetService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
@RequestMapping("/internal/v1/preprocess/presets")
public class InternalWorkerPresetController {

    private final PreprocessPresetService presetService;

    public InternalWorkerPresetController(PreprocessPresetService presetService) {
        this.presetService = presetService;
    }

    @GetMapping
    public ApiResponse<List<PresetResponse>> findBuiltInPresets() {
        return ApiResponse.success(presetService.findBuiltInPresets());
    }
}
