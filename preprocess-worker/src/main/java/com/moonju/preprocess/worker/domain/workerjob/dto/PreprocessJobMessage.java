package com.moonju.preprocess.worker.domain.workerjob.dto;

import com.moonju.preprocess.worker.domain.workerjob.exception.InvalidWorkerMessageException;
import java.time.Instant;
import java.util.Map;

public record PreprocessJobMessage(
    String messageId,
    Long jobId,
    Long itemId,
    Long projectId,
    Long imageId,
    Long userId,
    String originalObjectKey,
    String preset,
    Map<String, String> presetParameters,
    boolean debug,
    JobPriority priority,
    int attempt,
    String traceId,
    Instant createdAt
) {

    public Map<String, String> safePresetParameters() {
        return presetParameters == null ? Map.of() : presetParameters;
    }

    public void validateRequiredFields() {
        if (isBlank(messageId) || jobId == null || itemId == null || projectId == null || imageId == null) {
            throw new InvalidWorkerMessageException("Message identity fields are required.");
        }
        if (isBlank(originalObjectKey) || isBlank(preset)) {
            throw new InvalidWorkerMessageException("Original object key and preset are required.");
        }
        if (attempt < 1) {
            throw new InvalidWorkerMessageException("Attempt must be greater than zero.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
