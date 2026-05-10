package com.moonju.preprocess.worker.domain.workerjob.listener;

import com.moonju.preprocess.worker.domain.workerjob.dto.PreprocessJobMessage;
import com.moonju.preprocess.worker.domain.workerjob.service.WorkerJobService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "worker.listener", name = "enabled", havingValue = "true")
public class PreprocessJobListener {

    private final WorkerJobService workerJobService;

    public PreprocessJobListener(WorkerJobService workerJobService) {
        this.workerJobService = workerJobService;
    }

    @RabbitListener(queues = {
        "${rabbitmq.queues.preprocess-high}",
        "${rabbitmq.queues.preprocess-normal}",
        "${rabbitmq.queues.preprocess-retry}"
    })
    public void handle(PreprocessJobMessage message) {
        workerJobService.process(message);
    }
}
