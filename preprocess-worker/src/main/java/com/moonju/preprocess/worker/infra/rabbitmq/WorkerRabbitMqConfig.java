package com.moonju.preprocess.worker.infra.rabbitmq;

import com.moonju.preprocess.worker.infra.api.WorkerInternalApiProperties;
import com.moonju.preprocess.worker.infra.storage.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    WorkerQueueProperties.class,
    WorkerInternalApiProperties.class,
    StorageProperties.class
})
public class WorkerRabbitMqConfig {
}
