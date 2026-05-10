package com.moonju.preprocess.api.domain.job.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JobTests {

    @Test
    void createsJobInCreatedStatus() {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of("targetDpi", "300"), false, JobPriority.NORMAL, 2);

        assertThat(job.getProjectId()).isEqualTo(10L);
        assertThat(job.getStatus()).isEqualTo(JobStatus.CREATED);
        assertThat(job.getTotalCount()).isEqualTo(2);
    }

    @Test
    void marksQueuedAndCancelRequested() {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 2);

        job.markQueued(2);
        job.requestCancel();

        assertThat(job.getQueuedCount()).isEqualTo(2);
        assertThat(job.getStatus()).isEqualTo(JobStatus.CANCEL_REQUESTED);
    }

    @Test
    void refreshesProgressFromItems() {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 2);
        JobItem succeeded = JobItem.queued(1L, 100L);
        JobItem failed = JobItem.queued(1L, 101L);
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 10, 0);

        succeeded.markProcessing("worker-1", 1, now);
        succeeded.markSucceeded("worker-1", "processed/key.png", "preview/key.png", "report/key.json", now);
        failed.markProcessing("worker-1", 1, now);
        failed.markFailed("worker-1", "DECODE_FAILED", "cannot decode", now);
        job.refreshProgress(List.of(succeeded, failed), now);

        assertThat(job.getStatus()).isEqualTo(JobStatus.PARTIAL_SUCCEEDED);
        assertThat(job.getSucceededCount()).isEqualTo(1);
        assertThat(job.getFailedCount()).isEqualTo(1);
        assertThat(job.getCompletedAt()).isEqualTo(now);
    }
}
