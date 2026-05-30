package com.moonju.preprocess.worker.infra.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerJobStatus;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WorkerMetricsRecorderTests {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final WorkerMetricsRecorder recorder = new WorkerMetricsRecorder(meterRegistry);

    @Test
    void recordsJobCompletionCounterAndTimer() {
        recorder.recordJobCompleted(
            "LOW_CONTRAST_SCAN",
            WorkerJobStatus.SUCCEEDED,
            null,
            false,
            Duration.ofMillis(42)
        );

        double processedCount = meterRegistry
            .get("worker_job_processed_total")
            .tag("preset", "LOW_CONTRAST_SCAN")
            .tag("status", "SUCCEEDED")
            .tag("failure_code", "none")
            .tag("retryable", "false")
            .counter()
            .count();
        Timer timer = meterRegistry
            .get("worker_processing_seconds")
            .tag("preset", "LOW_CONTRAST_SCAN")
            .tag("status", "SUCCEEDED")
            .tag("failure_code", "none")
            .timer();

        assertThat(processedCount).isEqualTo(1.0);
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(42.0);
    }

    @Test
    void recordsFailureAndUnknownTags() {
        recorder.recordJobCompleted(
            null,
            WorkerJobStatus.FAILED,
            WorkerFailureCode.INVALID_MESSAGE,
            false,
            Duration.ofMillis(1)
        );

        double processedCount = meterRegistry
            .get("worker_job_processed_total")
            .tag("preset", "unknown")
            .tag("status", "FAILED")
            .tag("failure_code", "INVALID_MESSAGE")
            .tag("retryable", "false")
            .counter()
            .count();

        assertThat(processedCount).isEqualTo(1.0);
    }

    @Test
    void recordsPipelineSkeletonAndArtifactPreparation() {
        recorder.recordPipelineSkeletonExecution("A4_SCAN_300DPI", 10);
        recorder.recordArtifactSkeletonPrepared("processed");

        assertThat(meterRegistry
            .get("worker_pipeline_skeleton_executions_total")
            .tag("preset", "A4_SCAN_300DPI")
            .tag("step_count", "10")
            .counter()
            .count()).isEqualTo(1.0);
        assertThat(meterRegistry
            .get("worker_artifact_prepared_total")
            .tag("artifact_type", "processed")
            .counter()
            .count()).isEqualTo(1.0);
    }
}
