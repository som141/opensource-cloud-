package com.moonju.preprocess.worker.infra.tracing;

import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;

public record WorkerTraceContext(
    String traceId,
    String messageId
) {

    public static WorkerTraceContext from(PreprocessJobMessage message) {
        return new WorkerTraceContext(message.traceId(), message.messageId());
    }

    public boolean present() {
        return traceId != null && !traceId.isBlank();
    }
}
