package com.moonju.preprocess.worker.infra.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.workerjob.dto.JobPriority;
import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RabbitTraceContextExtractorTests {

    private final RabbitTraceContextExtractor extractor = new RabbitTraceContextExtractor();

    @Test
    void extractsTraceIdAndMessageIdFromWorkerMessage() {
        PreprocessJobMessage message = new PreprocessJobMessage(
            "msg-1",
            1L,
            2L,
            3L,
            4L,
            5L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            JobPriority.NORMAL,
            1,
            "trace-1",
            Instant.parse("2026-05-10T00:00:00Z")
        );

        WorkerTraceContext traceContext = extractor.extract(message);

        assertThat(traceContext.messageId()).isEqualTo("msg-1");
        assertThat(traceContext.traceId()).isEqualTo("trace-1");
        assertThat(traceContext.present()).isTrue();
    }
}
