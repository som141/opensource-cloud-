package com.moonju.preprocess.worker.infra.tracing;

import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import org.springframework.stereotype.Component;

@Component
public class RabbitTraceContextExtractor {

    public WorkerTraceContext extract(PreprocessJobMessage message) {
        return WorkerTraceContext.from(message);
    }
}
