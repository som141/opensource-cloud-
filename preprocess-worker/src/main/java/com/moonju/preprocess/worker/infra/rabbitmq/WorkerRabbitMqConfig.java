package com.moonju.preprocess.worker.infra.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonju.preprocess.worker.infra.api.WorkerInternalApiProperties;
import com.moonju.preprocess.worker.infra.api.WorkerRuntimeProperties;
import com.moonju.preprocess.worker.infra.storage.StorageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    WorkerQueueProperties.class,
    WorkerInternalApiProperties.class,
    WorkerRuntimeProperties.class,
    StorageProperties.class
})
public class WorkerRabbitMqConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter(new ObjectMapper().findAndRegisterModules());
    }
}
