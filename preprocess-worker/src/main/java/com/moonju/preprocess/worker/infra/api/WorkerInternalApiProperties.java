package com.moonju.preprocess.worker.infra.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backend-api")
public class WorkerInternalApiProperties {

    private String baseUrl = "http://localhost:8080";
    private String workerToken = "local-worker-token";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getWorkerToken() {
        return workerToken;
    }

    public void setWorkerToken(String workerToken) {
        this.workerToken = workerToken;
    }
}
