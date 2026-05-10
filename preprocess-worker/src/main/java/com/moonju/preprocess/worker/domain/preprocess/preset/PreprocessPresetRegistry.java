package com.moonju.preprocess.worker.domain.preprocess.preset;

import com.moonju.preprocess.worker.domain.preprocess.exception.PresetNotSupportedException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PreprocessPresetRegistry {

    private final Map<PreprocessPresetName, PreprocessPreset> presets;

    public PreprocessPresetRegistry(List<PreprocessPresetDefinition> definitions) {
        this.presets = new EnumMap<>(PreprocessPresetName.class);
        for (PreprocessPresetDefinition definition : definitions) {
            PreprocessPreset preset = definition.preset();
            presets.put(preset.name(), preset);
        }
    }

    public static PreprocessPresetRegistry builtIn() {
        return new PreprocessPresetRegistry(List.of(
            new A4Scan300DpiPreset(),
            new LowContrastScanPreset(),
            new ReceiptPreset(),
            new NoisyScanPreset(),
            new AutoPreprocessPreset()
        ));
    }

    public PreprocessPreset findByName(String presetName) {
        PreprocessPresetName name = parseName(presetName);
        PreprocessPreset preset = presets.get(name);
        if (preset == null) {
            throw new PresetNotSupportedException(presetName);
        }
        return preset;
    }

    public List<PreprocessPresetName> supportedNames() {
        return List.copyOf(presets.keySet());
    }

    private PreprocessPresetName parseName(String presetName) {
        try {
            return PreprocessPresetName.valueOf(presetName);
        } catch (IllegalArgumentException exception) {
            throw new PresetNotSupportedException(presetName);
        }
    }
}
