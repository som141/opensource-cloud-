package com.moonju.preprocess.api.domain.job.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobProgressEventTests {

    @Test
    void createsProgressEventFromSummary() {
        JobSummaryResponse summary = new JobSummaryResponse(1L, 10, 2, 1, 6, 1, 70.0);
        Instant emittedAt = Instant.parse("2026-05-10T00:00:00Z");

        JobProgressEvent event = JobProgressEvent.fromSummary(JobEventType.JOB_PROGRESS, summary, emittedAt);

        assertThat(event.eventType()).isEqualTo(JobEventType.JOB_PROGRESS);
        assertThat(event.jobId()).isEqualTo(1L);
        assertThat(event.total()).isEqualTo(10);
        assertThat(event.queued()).isEqualTo(2);
        assertThat(event.processing()).isEqualTo(1);
        assertThat(event.succeeded()).isEqualTo(6);
        assertThat(event.failed()).isEqualTo(1);
        assertThat(event.progressPercent()).isEqualTo(70.0);
        assertThat(event.emittedAt()).isEqualTo(emittedAt);
    }

    @Test
    void createsHeartbeatWithoutProgressCounters() {
        Instant emittedAt = Instant.parse("2026-05-10T00:00:00Z");

        JobProgressEvent event = JobProgressEvent.heartbeat(1L, emittedAt);

        assertThat(event.eventType()).isEqualTo(JobEventType.HEARTBEAT);
        assertThat(event.jobId()).isEqualTo(1L);
        assertThat(event.total()).isNull();
        assertThat(event.progressPercent()).isNull();
        assertThat(event.emittedAt()).isEqualTo(emittedAt);
    }
}
