package com.moonju.preprocess.api.domain.job.entity;

import static org.assertj.core.api.Assertions.assertThat;

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
}
