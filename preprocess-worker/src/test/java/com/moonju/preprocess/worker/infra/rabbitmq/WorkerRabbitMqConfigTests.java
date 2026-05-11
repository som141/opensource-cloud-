package com.moonju.preprocess.worker.infra.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerRabbitMqConfigTests {

    @Test
    void createsJacksonMessageConverterForPreprocessMessages() {
        WorkerRabbitMqConfig config = new WorkerRabbitMqConfig();

        assertThat(config.rabbitMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
