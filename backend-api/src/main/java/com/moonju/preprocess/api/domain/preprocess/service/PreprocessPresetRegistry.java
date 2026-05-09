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
            commonParameters(300, "otsu", 1.2, false)
        ));
        register(new PreprocessPreset(
            PreprocessPresetName.LOW_CONTRAST_SCAN,
            PresetType.BUILT_IN,
            "Low contrast scan",
            "Improves low contrast scans using stronger contrast normalization before binarization.",
            true,
            DOCUMENT_PIPELINE_STEPS,
            commonParameters(300, "adaptive", 1.6, true)
        ));
        register(new PreprocessPreset(
            PreprocessPresetName.RECEIPT,
            PresetType.BUILT_IN,
            "Receipt",
            "Narrow receipt-like document preset with adaptive thresholding and light morphology cleanup.",
            true,
            DOCUMENT_PIPELINE_STEPS,
            commonParameters(300, "adaptive", 1.4, true)
        ));
        register(new PreprocessPreset(
            PreprocessPresetName.NOISY_SCAN,
            PresetType.BUILT_IN,
            "Noisy scan",
            "Preset for scans with strong background noise and more aggressive denoise settings.",
            true,
            DOCUMENT_PIPELINE_STEPS,
            commonParameters(300, "adaptive", 1.5, false)
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
            PresetParameterDefinition.decimal(
                "contrastClipLimit",
                "CLAHE-like contrast normalization strength.",
                true,
                contrastClipLimit,
                1.0,
                4.0
            ),
            PresetParameterDefinition.bool(
                "sharpen",
                "Whether optional sharpen step should run after DPI normalization.",
                sharpen
            ),
            PresetParameterDefinition.bool(
                "debugArtifacts",
                "Whether worker should save intermediate debug artifacts.",
                false
            )
        );
    }
}
