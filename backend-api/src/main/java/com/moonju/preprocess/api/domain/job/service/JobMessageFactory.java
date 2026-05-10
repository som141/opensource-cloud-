package com.moonju.preprocess.api.domain.job.service;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.infra.rabbitmq.PreprocessJobMessage;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JobMessageFactory {

    public PreprocessJobMessage create(Job job, JobItem item, Image image) {
        return new PreprocessJobMessage(
            UUID.randomUUID().toString(),
            job.getId(),
            item.getId(),
            job.getProjectId(),
            image.getId(),
            job.getUserId(),
            image.getOriginalObjectKey(),
            job.getPreset(),
            job.getPresetParameters(),
            job.isDebug(),
            job.getPriority(),
            item.getAttempt(),
            UUID.randomUUID().toString(),
            Instant.now()
        );
    }
}
