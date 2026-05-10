package com.moonju.preprocess.api.infra.rabbitmq;

import static org.mockito.Mockito.verify;

import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class JobMessagePublisherTests {

    @Test
    void publishesHighPriorityMessageToHighQueue() {
        RabbitTemplate rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
        RabbitMqProperties properties = new RabbitMqProperties();
        JobMessagePublisher publisher = new JobMessagePublisher(rabbitTemplate, properties);
        PreprocessJobMessage message = message(JobPriority.HIGH);

        publisher.publishPreprocess(message);

        verify(rabbitTemplate).convertAndSend("image.preprocess.high", message);
    }

    @Test
    void publishesRetryMessageToRetryQueue() {
        RabbitTemplate rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
        RabbitMqProperties properties = new RabbitMqProperties();
        JobMessagePublisher publisher = new JobMessagePublisher(rabbitTemplate, properties);
        PreprocessJobMessage message = message(JobPriority.NORMAL);

        publisher.publishRetry(message);

        verify(rabbitTemplate).convertAndSend("image.preprocess.retry", message);
    }

    private PreprocessJobMessage message(JobPriority priority) {
        return new PreprocessJobMessage(
            "msg",
            1L,
            2L,
            3L,
            4L,
            5L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            priority,
            1,
            "trace",
            Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
