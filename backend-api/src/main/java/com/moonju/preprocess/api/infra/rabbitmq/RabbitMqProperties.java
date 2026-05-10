package com.moonju.preprocess.api.infra.rabbitmq;

import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq.queues")
public class RabbitMqProperties {

    private String preprocessHigh = "image.preprocess.high";
    private String preprocessNormal = "image.preprocess.normal";
    private String preprocessRetry = "image.preprocess.retry";
    private String preprocessDlq = "image.preprocess.dlq";

    public String queueFor(JobPriority priority) {
        if (priority == JobPriority.HIGH) {
            return preprocessHigh;
        }
        return preprocessNormal;
    }

    public String retryQueue() {
        return preprocessRetry;
    }

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

    public String getPreprocessDlq() {
        return preprocessDlq;
    }

    public void setPreprocessDlq(String preprocessDlq) {
        this.preprocessDlq = preprocessDlq;
    }
}
