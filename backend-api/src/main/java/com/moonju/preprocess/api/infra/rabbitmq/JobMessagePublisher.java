package com.moonju.preprocess.api.infra.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;

    public JobMessagePublisher(RabbitTemplate rabbitTemplate, RabbitMqProperties rabbitMqProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqProperties = rabbitMqProperties;
    }

    public void publishPreprocess(PreprocessJobMessage message) {
        rabbitTemplate.convertAndSend(rabbitMqProperties.queueFor(message.priority()), message);
    }

    public void publishRetry(PreprocessJobMessage message) {
        rabbitTemplate.convertAndSend(rabbitMqProperties.retryQueue(), message);
    }
}
