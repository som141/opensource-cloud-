package com.moonju.preprocess.worker.domain.preprocess.pipeline;

import com.moonju.preprocess.worker.domain.preprocess.model.FallbackNote;
import com.moonju.preprocess.worker.domain.preprocess.step.PreprocessStepName;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PreprocessContext {

    private final Long jobId;
    private final Long itemId;
    private final String originalObjectKey;
    private final String presetName;
    private final Map<String, String> parameters;
    private final boolean debug;
    private final List<PreprocessStepExecution> stepExecutions = new ArrayList<>();
    private final List<FallbackNote> fallbackNotes = new ArrayList<>();
    private final Map<PreprocessStepName, String> stepNotes = new EnumMap<>(PreprocessStepName.class);

    public PreprocessContext(
        Long jobId,
        Long itemId,
        String originalObjectKey,
        String presetName,
        Map<String, String> parameters,
        boolean debug
    ) {
        this.jobId = jobId;
        this.itemId = itemId;
        this.originalObjectKey = originalObjectKey;
        this.presetName = presetName;
        this.parameters = new LinkedHashMap<>(parameters);
        this.debug = debug;
    }

    public static PreprocessContext fromMessage(PreprocessJobMessage message) {
        return new PreprocessContext(
            message.jobId(),
            message.itemId(),
            message.originalObjectKey(),
            message.preset(),
            message.safePresetParameters(),
            message.debug()
        );
    }

    public void recordStep(PreprocessStepName stepName, String note) {
        stepNotes.put(stepName, note);
    }

    public void recordStepExecution(PreprocessStepExecution execution) {
        stepExecutions.add(execution);
    }

    public String consumeStepNote(PreprocessStepName stepName) {
        String note = stepNotes.remove(stepName);
        return note == null ? "Step executed." : note;
    }

    public void recordFallback(PreprocessStepName stepName, String reason, String selectedStrategy) {
        fallbackNotes.add(new FallbackNote(stepName.name(), reason, selectedStrategy));
    }

    public Long jobId() {
        return jobId;
    }

    public Long itemId() {
        return itemId;
    }

    public String originalObjectKey() {
        return originalObjectKey;
    }

    public String presetName() {
        return presetName;
    }

    public Map<String, String> parameters() {
        return Map.copyOf(parameters);
    }

    public boolean debug() {
        return debug;
    }

    public List<PreprocessStepExecution> stepExecutions() {
        return List.copyOf(stepExecutions);
    }

    public List<FallbackNote> fallbackNotes() {
        return List.copyOf(fallbackNotes);
    }
}
