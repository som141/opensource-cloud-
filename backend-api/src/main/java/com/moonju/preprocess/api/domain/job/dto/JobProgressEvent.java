package com.moonju.preprocess.api.domain.job.dto;

import java.time.Instant;

public record JobProgressEvent(
    JobEventType eventType,
    Long jobId,
    Integer total,
    Integer queued,
    Integer processing,
    Integer succeeded,
    Integer failed,
    Double progressPercent,
    Instant emittedAt
) {

    public static JobProgressEvent fromSummary(
        JobEventType eventType,
        JobSummaryResponse summary,
        Instant emittedAt
    ) {
        return new JobProgressEvent(
            eventType,
            summary.jobId(),
            summary.total(),
            summary.queued(),
            summary.processing(),
            summary.succeeded(),
            summary.failed(),
            summary.progressPercent(),
            emittedAt
        );
    }

    public static JobProgressEvent heartbeat(Long jobId, Instant emittedAt) {
        return new JobProgressEvent(JobEventType.HEARTBEAT, jobId, null, null, null, null, null, null, emittedAt);
    }
}
