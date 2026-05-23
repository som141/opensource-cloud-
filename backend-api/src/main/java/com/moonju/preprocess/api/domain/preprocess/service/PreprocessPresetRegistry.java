package com.moonju.preprocess.api.domain.preprocess.service;

import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPreset;
import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPresetName;
import com.moonju.preprocess.api.domain.preprocess.entity.PresetParameterDefinition;
import com.moonju.preprocess.api.domain.preprocess.entity.PresetType;
import com.moonju.preprocess.api.domain.preprocess.exception.PresetNotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PreprocessPresetRegistry {

    private static final List<String> DOCUMENT_PIPELINE_STEPS = List.of(
        "DECODE",
        "COLOR_NORMALIZE",
        "ORIENTATION_NORMALIZE",
        "DESKEW",
        "CROP",
        "DENOISE",
        "CONTRAST_NORMALIZE",
        "BINARIZATION",
        "MORPHOLOGY_CLEANUP",
        "DPI_NORMALIZE",
        "OPTIONAL_SHARPEN"
    );

    private final Map<PreprocessPresetName, PreprocessPreset> presets;

    public PreprocessPresetRegistry() {
        this.presets = new EnumMap<>(PreprocessPresetName.class);
        registerBuiltInPresets();
    }

    public List<PreprocessPreset> findAll() {
        return List.copyOf(presets.values());
    }

    public PreprocessPreset findByName(String presetName) {
        try {
            return findByName(PreprocessPresetName.from(presetName));
        } catch (IllegalArgumentException exception) {
            throw new PresetNotFoundException();
        }
    }

    public PreprocessPreset findByName(PreprocessPresetName presetName) {
        PreprocessPreset preset = presets.get(presetName);
        if (preset == null) {
            throw new PresetNotFoundException();
        }
        return preset;
    }

    private void registerBuiltInPresets() {
        register(new PreprocessPreset(
            PreprocessPresetName.A4_SCAN_300DPI,
            PresetType.BUILT_IN,
            "A4 300 DPI scan",
            "General A4 document scan preset for OCR preprocessing.",
            true,
            DOCUMENT_PIPELINE_STEPS,
            commonParameters(300, "otsu", 2.0, "median", false)
        ));
        register(new PreprocessPreset(
            PreprocessPresetName.LOW_CONTRAST_SCAN,
            PresetType.BUILT_IN,
            "Low contrast scan",
            "Improves low contrast scans using stronger contrast normalization before binarization.",
            true,
            DOCUMENT_PIPELINE_STEPS,
            commonParameters(300, "adaptive", 2.4, "median", true)
        ));
        register(new PreprocessPreset(
            PreprocessPresetName.RECEIPT,
            PresetType.BUILT_IN,
            "Receipt",
            "Narrow receipt-like document preset with adaptive thresholding and light morphology cleanup.",
            true,
            DOCUMENT_PIPELINE_STEPS,
            commonParameters(300, "adaptive", 2.2, "median", true)
        ));
        register(new PreprocessPreset(
            PreprocessPresetName.NOISY_SCAN,
            PresetType.BUILT_IN,
            "Noisy scan",
            "Preset for scans with strong background noise and more aggressive denoise settings.",
            true,
            DOCUMENT_PIPELINE_STEPS,
            commonParameters(300, "adaptive", 2.0, "bilateral", false)
        ));
        register(new PreprocessPreset(
            PreprocessPresetName.AUTO,
            PresetType.BUILT_IN,
            "Auto",
            "Delegates preset selection to the worker based on image characteristics.",
            true,
            List.of("DECODE", "COLOR_NORMALIZE", "AUTO_PRESET_SELECT", "RUN_SELECTED_PIPELINE"),
            List.of(PresetParameterDefinition.bool(
                "debugArtifacts",
                "Whether worker should save intermediate debug artifacts.",
                false
            ))
        ));
    }

    private void register(PreprocessPreset preset) {
        presets.put(preset.getName(), preset);
    }

    private List<PresetParameterDefinition> commonParameters(
        int targetDpi,
        String binarizationMode,
        double contrastClipLimit,
        String denoiseMode,
        boolean sharpen
    ) {
        return List.of(
            PresetParameterDefinition.integer(
                "targetDpi",
                "Target DPI for OCR-oriented DPI normalization.",
                true,
                targetDpi,
                150,
                600
            ),
            PresetParameterDefinition.decimal(
                "maxDeskewAngle",
                "Maximum allowed deskew correction angle in degrees.",
                true,
                40.0,
                0.0,
                45.0
            ),
            PresetParameterDefinition.option(
                "binarizationMode",
                "Binarization strategy used by the worker.",
                true,
                binarizationMode,
                Set.of("otsu", "adaptive")
            ),
            PresetParameterDefinition.integer(
                "adaptiveBlockSize",
                "Odd block size for adaptive thresholding.",
                true,
                21,
                3,
                99
            ),
            PresetParameterDefinition.decimal(
                "adaptiveC",
                "Constant subtracted from the adaptive threshold mean.",
                true,
                5.0,
                -20.0,
                20.0
            ),
            PresetParameterDefinition.decimal(
                "contrastClipLimit",
                "CLAHE-like contrast normalization strength.",
                true,
                contrastClipLimit,
                1.0,
                4.0
            ),
            PresetParameterDefinition.integer(
                "contrastTileGridSize",
                "CLAHE tile grid size.",
                true,
                8,
                1,
                32
            ),
            PresetParameterDefinition.option(
                "denoiseMode",
                "Denoise strategy used by the worker.",
                true,
                denoiseMode,
                Set.of("median", "bilateral", "none", "off", "false")
            ),
            PresetParameterDefinition.integer(
                "denoiseKernelSize",
                "Median blur kernel size.",
                true,
                3,
                3,
                15
            ),
            PresetParameterDefinition.integer(
                "denoiseDiameter",
                "Bilateral filter neighborhood diameter.",
                true,
                5,
                3,
                15
            ),
            PresetParameterDefinition.decimal(
                "denoiseSigmaColor",
                "Bilateral filter color sigma. Lower values preserve text edges.",
                true,
                25.0,
                0.1,
                200.0
            ),
            PresetParameterDefinition.decimal(
                "denoiseSigmaRange",
                "Bilateral filter spatial sigma range.",
                true,
                75.0,
                0.1,
                200.0
            ),
            PresetParameterDefinition.option(
                "morphologyMode",
                "Morphology cleanup strategy.",
                true,
                "open_close",
                Set.of("open", "close", "open_close", "none", "off", "false")
            ),
            PresetParameterDefinition.integer(
                "morphologyKernelSize",
                "Morphology cleanup kernel size.",
                true,
                2,
                1,
                7
            ),
            PresetParameterDefinition.bool(
                "sharpen",
                "Whether optional sharpen step should run after DPI normalization.",
                sharpen
            ),
            PresetParameterDefinition.decimal(
                "sharpenAmount",
                "Unsharp mask weight for optional sharpen.",
                true,
                0.8,
                0.1,
                3.0
            ),
            PresetParameterDefinition.decimal(
                "sharpenSigma",
                "Gaussian blur sigma for optional sharpen.",
                true,
                1.5,
                0.1,
                10.0
            ),
            PresetParameterDefinition.bool(
                "debugArtifacts",
                "Whether worker should save intermediate debug artifacts.",
                false
            )
        );
    }
}
