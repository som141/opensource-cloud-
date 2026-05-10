package com.moonju.preprocess.worker.infra.rabbitmq;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq.queues")
public class WorkerQueueProperties {

    private String preprocessHigh = "image.preprocess.high";
    private String preprocessNormal = "image.preprocess.normal";
    private String preprocessRetry = "image.preprocess.retry";
    private String benchmarkNormal = "image.benchmark.normal";

    public String getPreprocessHigh() {
        return preprocessHigh;
    }

    public void setPreprocessHigh(String preprocessHigh) {
        this.preprocessHigh = preprocessHigh;
    }

    public String getPreprocessNormal() {
        return preprocessNormal;
    }

    public void setPreprocessNormal(String preprocessNormal) {
        this.preprocessNormal = preprocessNormal;
    }

    public String getPreprocessRetry() {
        return preprocessRetry;
    }

    public void setPreprocessRetry(String preprocessRetry) {
        this.preprocessRetry = preprocessRetry;
    }

    public String getBenchmarkNormal() {
        return benchmarkNormal;
    }

    public void setBenchmarkNormal(String benchmarkNormal) {
        this.benchmarkNormal = benchmarkNormal;
    }
}
