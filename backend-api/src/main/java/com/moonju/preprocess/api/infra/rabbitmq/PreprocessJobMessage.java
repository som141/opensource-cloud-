package com.moonju.preprocess.api.infra.rabbitmq;

import com.moonju.preprocess.api.domain.job.entity.JobPriority;
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
}
