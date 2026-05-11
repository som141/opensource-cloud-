package com.moonju.preprocess.api.infra.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqConfigTests {

    @Test
    void createsJacksonMessageConverterForWorkerMessages() {
        RabbitMqConfig config = new RabbitMqConfig();

        assertThat(config.rabbitMessageConverter(new ObjectMapper().findAndRegisterModules()))
            .isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
