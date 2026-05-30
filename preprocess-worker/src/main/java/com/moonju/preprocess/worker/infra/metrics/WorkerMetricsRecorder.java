package com.moonju.preprocess.worker.infra.metrics;

import com.moonju.preprocess.worker.domain.workerjob.status.WorkerFailureCode;
import com.moonju.preprocess.worker.domain.workerjob.status.WorkerJobStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class WorkerMetricsRecorder {

    private static final String UNKNOWN = "unknown";
    private static final String NONE = "none";

    private final MeterRegistry meterRegistry;

    public WorkerMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPipelineSkeletonExecution(String presetName, int stepCount) {
        Counter.builder("worker_pipeline_skeleton_executions_total")
            .description("Preprocess pipeline skeleton execution count")
            .tag("preset", safeTag(presetName))
            .tag("step_count", Integer.toString(stepCount))
            .register(meterRegistry)
            .increment();
    }

    public void recordArtifactSkeletonPrepared(String artifactType) {
        Counter.builder("worker_artifact_prepared_total")
            .description("Worker artifact preparation count")
            .tag("artifact_type", safeTag(artifactType))
            .register(meterRegistry)
            .increment();
    }

    public void recordJobCompleted(
        String presetName,
        WorkerJobStatus status,
        WorkerFailureCode failureCode,
        boolean retryable,
        Duration duration
    ) {
        String statusTag = status == null ? UNKNOWN : status.name();
        String failureTag = failureCode == null ? NONE : failureCode.name();
        Counter.builder("worker_job_processed_total")
            .description("Worker processed JobItem count")
            .tag("preset", safeTag(presetName))
            .tag("status", statusTag)
            .tag("failure_code", failureTag)
            .tag("retryable", Boolean.toString(retryable))
            .register(meterRegistry)
            .increment();

        Timer.builder("worker_processing_seconds")
            .description("Worker JobItem processing duration")
            .tag("preset", safeTag(presetName))
            .tag("status", statusTag)
            .tag("failure_code", failureTag)
            .register(meterRegistry)
            .record(duration);
    }

    private String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value;
    }
}
