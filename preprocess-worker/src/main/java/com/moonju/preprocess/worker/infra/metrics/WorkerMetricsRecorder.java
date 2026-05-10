package com.moonju.preprocess.worker.infra.metrics;

import org.springframework.stereotype.Component;

@Component
public class WorkerMetricsRecorder {

    public void recordPipelineSkeletonExecution(String presetName, int stepCount) {
        // Micrometer counters will be connected in the observability task.
    }

    public void recordArtifactSkeletonPrepared(String artifactType) {
        // Micrometer counters will be connected in the observability task.
    }
}
