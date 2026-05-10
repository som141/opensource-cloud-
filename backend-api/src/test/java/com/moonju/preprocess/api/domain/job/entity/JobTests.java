package com.moonju.preprocess.api.domain.job.entity;

import static org.assertj.core.api.Assertions.assertThat;

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
}
