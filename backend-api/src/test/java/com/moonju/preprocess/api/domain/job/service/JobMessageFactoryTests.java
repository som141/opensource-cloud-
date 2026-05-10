package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageFormat;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.infra.rabbitmq.PreprocessJobMessage;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JobMessageFactoryTests {

    private final JobMessageFactory factory = new JobMessageFactory();

    @Test
    void createsMessageWithJobItemAndImageContext() {
        Job job = Job.create(
            10L,
            20L,
            "LOW_CONTRAST_SCAN",
            Map.of("targetDpi", "300"),
            true,
            JobPriority.HIGH,
            1
        );
        ReflectionTestUtils.setField(job, "id", 1L);
        JobItem item = JobItem.queued(1L, 100L);
        ReflectionTestUtils.setField(item, "id", 2L);
        Image image = image(100L);

        PreprocessJobMessage message = factory.create(job, item, image);

        assertThat(message.jobId()).isEqualTo(1L);
        assertThat(message.itemId()).isEqualTo(2L);
        assertThat(message.originalObjectKey()).isEqualTo("originals/project-10/scan.png");
        assertThat(message.preset()).isEqualTo("LOW_CONTRAST_SCAN");
        assertThat(message.presetParameters()).containsEntry("targetDpi", "300");
        assertThat(message.debug()).isTrue();
        assertThat(message.priority()).isEqualTo(JobPriority.HIGH);
        assertThat(message.messageId()).isNotBlank();
        assertThat(message.traceId()).isNotBlank();
    }

    private Image image(Long id) {
        Image image = new Image(
            10L,
            1L,
            2L,
            20L,
            "scan.png",
            "originals/project-10/scan.png",
            "image/png",
            1024L,
            "a".repeat(64),
            ImageFormat.PNG,
            ImageStatus.UPLOADED
        );
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }
}
