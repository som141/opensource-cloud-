package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.api.domain.job.dto.JobEventType;
import com.moonju.preprocess.api.domain.job.dto.JobProgressEvent;
import com.moonju.preprocess.api.domain.job.dto.JobSummaryResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterRegistryTests {

    private final SseEmitterRegistry registry = new SseEmitterRegistry();

    @Test
    void registersAndSendsEventsByJobId() {
        SseEmitter emitter = registry.register(1L);
        JobSummaryResponse summary = new JobSummaryResponse(1L, 10, 1, 2, 6, 1, 70.0);
        JobProgressEvent event = JobProgressEvent.fromSummary(
            JobEventType.JOB_PROGRESS,
            summary,
            Instant.parse("2026-05-10T00:00:00Z")
        );

        int sentCount = registry.sendToJob(1L, event);

        assertThat(emitter).isNotNull();
        assertThat(registry.connectionCount(1L)).isEqualTo(1);
        assertThat(sentCount).isEqualTo(1);
    }

    @Test
    void returnsZeroWhenNoEmitterExistsForJob() {
        JobProgressEvent event = JobProgressEvent.heartbeat(99L, Instant.parse("2026-05-10T00:00:00Z"));

        assertThat(registry.sendToJob(99L, event)).isZero();
    }
}
