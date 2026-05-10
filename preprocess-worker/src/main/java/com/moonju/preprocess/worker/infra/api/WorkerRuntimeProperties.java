package com.moonju.preprocess.worker.infra.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker")
public class WorkerRuntimeProperties {

    private String id = "local-worker-1";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
