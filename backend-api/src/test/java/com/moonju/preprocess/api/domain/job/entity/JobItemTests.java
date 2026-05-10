package com.moonju.preprocess.api.domain.job.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class JobItemTests {

    @Test
    void createsQueuedJobItem() {
        JobItem item = JobItem.queued(1L, 100L);

        assertThat(item.getJobId()).isEqualTo(1L);
        assertThat(item.getImageId()).isEqualTo(100L);
        assertThat(item.getStatus()).isEqualTo(JobItemStatus.QUEUED);
        assertThat(item.getAttempt()).isEqualTo(1);
    }

    @Test
    void retriesFailedItem() {
        JobItem item = new JobItem(1L, 100L, JobItemStatus.FAILED, 1);

        item.retry();

        assertThat(item.getStatus()).isEqualTo(JobItemStatus.RETRYING);
        assertThat(item.getAttempt()).isEqualTo(2);
    }

    @Test
    void cancelsQueuedItem() {
        JobItem item = JobItem.queued(1L, 100L);

        item.requestCancel();

        assertThat(item.getStatus()).isEqualTo(JobItemStatus.CANCELLED);
    }

    @Test
    void marksWorkerProcessingHeartbeatSucceededAndArtifacts() {
        JobItem item = JobItem.queued(1L, 100L);
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 10, 10, 0);
        LocalDateTime heartbeatAt = LocalDateTime.of(2026, 5, 10, 10, 1);
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 10, 10, 2);

        item.markProcessing("worker-1", 2, startedAt);
        item.markHeartbeat("worker-1", heartbeatAt);
        item.registerArtifacts("processed/key.png", "preview/key.png", "report/key.json");
        item.markSucceeded("worker-1", "processed/key.png", "preview/key.png", "report/key.json", completedAt);

        assertThat(item.getStatus()).isEqualTo(JobItemStatus.SUCCEEDED);
        assertThat(item.getAttempt()).isEqualTo(2);
        assertThat(item.getWorkerId()).isEqualTo("worker-1");
        assertThat(item.getLastHeartbeatAt()).isEqualTo(completedAt);
        assertThat(item.getProcessedObjectKey()).isEqualTo("processed/key.png");
        assertThat(item.getReportObjectKey()).isEqualTo("report/key.json");
    }

    @Test
    void marksWorkerFailure() {
        JobItem item = JobItem.queued(1L, 100L);
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 10, 10, 0);
        LocalDateTime failedAt = LocalDateTime.of(2026, 5, 10, 10, 2);

        item.markProcessing("worker-1", 1, startedAt);
        item.markFailed("worker-1", "DECODE_FAILED", "cannot decode", failedAt);

        assertThat(item.getStatus()).isEqualTo(JobItemStatus.FAILED);
        assertThat(item.getErrorCode()).isEqualTo("DECODE_FAILED");
        assertThat(item.getErrorMessage()).isEqualTo("cannot decode");
        assertThat(item.getCompletedAt()).isEqualTo(failedAt);
    }
}
