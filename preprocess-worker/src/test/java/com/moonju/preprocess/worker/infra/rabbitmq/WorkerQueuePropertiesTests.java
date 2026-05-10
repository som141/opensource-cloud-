package com.moonju.preprocess.worker.infra.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorkerQueuePropertiesTests {

    @Test
    void providesSafeLocalQueueDefaults() {
        WorkerQueueProperties properties = new WorkerQueueProperties();

        assertThat(properties.getPreprocessHigh()).isEqualTo("image.preprocess.high");
        assertThat(properties.getPreprocessNormal()).isEqualTo("image.preprocess.normal");
        assertThat(properties.getPreprocessRetry()).isEqualTo("image.preprocess.retry");
        assertThat(properties.getBenchmarkNormal()).isEqualTo("image.benchmark.normal");
    }
}
