package com.moonju.preprocess.api.domain.preprocess.service;

import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateResponse;
import com.moonju.preprocess.api.domain.preprocess.entity.PreprocessPreset;
import com.moonju.preprocess.api.domain.preprocess.entity.PresetParameterDefinition;
import com.moonju.preprocess.api.domain.preprocess.entity.PresetParameterType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PreprocessParameterValidator {

    public PresetValidateResponse validate(PreprocessPreset preset, Map<String, String> requestedParameters) {
        List<String> errors = new ArrayList<>();
        Map<String, String> resolvedParameters = new LinkedHashMap<>();
        Set<String> knownParameterNames = preset.getParameters()
            .stream()
            .map(PresetParameterDefinition::name)
            .collect(Collectors.toSet());

        for (String requestedName : requestedParameters.keySet()) {
            if (!knownParameterNames.contains(requestedName)) {
                errors.add("Unknown parameter: " + requestedName);
            }
        }

        for (PresetParameterDefinition definition : preset.getParameters()) {
            String value = requestedParameters.get(definition.name());
            if (!StringUtils.hasText(value)) {
                if (definition.required() && !StringUtils.hasText(definition.defaultValue())) {
                    errors.add("Required parameter is missing: " + definition.name());
                    continue;
                }
                if (StringUtils.hasText(definition.defaultValue())) {
                    resolvedParameters.put(definition.name(), definition.defaultValue());
                }
                continue;
            }
            validateValue(definition, value, errors);
            resolvedParameters.put(definition.name(), value);
        }

        if (errors.isEmpty()) {
            return PresetValidateResponse.valid(preset.getName().name(), resolvedParameters);
        }
        return PresetValidateResponse.invalid(preset.getName().name(), errors, resolvedParameters);
    }

    private void validateValue(PresetParameterDefinition definition, String value, List<String> errors) {
        if (definition.type() == PresetParameterType.INTEGER) {
            validateInteger(definition, value, errors);
            return;
        }
        if (definition.type() == PresetParameterType.DECIMAL) {
            validateDecimal(definition, value, errors);
            return;
        }
        if (definition.type() == PresetParameterType.BOOLEAN) {
            validateBoolean(definition, value, errors);
            return;
        }
        if (definition.type() == PresetParameterType.ENUM) {
            validateEnum(definition, value, errors);
        }
    }

    private void validateInteger(PresetParameterDefinition definition, String value, List<String> errors) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < Integer.parseInt(definition.minValue()) || parsed > Integer.parseInt(definition.maxValue())) {
                errors.add(definition.name() + " must be between " + definition.minValue() + " and "
                    + definition.maxValue() + ".");
            }
        } catch (NumberFormatException exception) {
            errors.add(definition.name() + " must be an integer.");
        }
    }

    private void validateDecimal(PresetParameterDefinition definition, String value, List<String> errors) {
        try {
            BigDecimal parsed = new BigDecimal(value);
            BigDecimal min = new BigDecimal(definition.minValue());
            BigDecimal max = new BigDecimal(definition.maxValue());
            if (parsed.compareTo(min) < 0 || parsed.compareTo(max) > 0) {
                errors.add(definition.name() + " must be between " + definition.minValue() + " and "
                    + definition.maxValue() + ".");
            }
        } catch (NumberFormatException exception) {
            errors.add(definition.name() + " must be a decimal.");
        }
    }

    private void validateBoolean(PresetParameterDefinition definition, String value, List<String> errors) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            errors.add(definition.name() + " must be true or false.");
        }
    }

    private void validateEnum(PresetParameterDefinition definition, String value, List<String> errors) {
        if (!definition.allowedValues().contains(value)) {
            errors.add(definition.name() + " must be one of " + definition.allowedValues() + ".");
        }
    }
}
