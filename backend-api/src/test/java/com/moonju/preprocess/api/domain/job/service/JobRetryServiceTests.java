package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageFormat;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.job.dto.JobRetryResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobItemStatus;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.infra.rabbitmq.JobMessagePublisher;
import com.moonju.preprocess.api.infra.rabbitmq.PreprocessJobMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobRetryServiceTests {

    @Mock
    private JobQueryService jobQueryService;

    @Mock
    private JobItemRepository jobItemRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private JobMessageFactory jobMessageFactory;

    @Mock
    private JobMessagePublisher jobMessagePublisher;

    @Test
    void retriesFailedAndDeadLetteredItemsOnly() {
        JobRetryService service = service();
        Job job = job(1L);
        JobItem failed = item(10L, JobItemStatus.FAILED);
        JobItem deadLettered = item(11L, JobItemStatus.DEAD_LETTERED);
        Image image100 = image(100L);
        Image image101 = image(101L);
        when(jobQueryService.findEditableJob(20L, 1L)).thenReturn(job);
        when(jobItemRepository.findAllByJobIdAndStatusIn(
            1L,
            List.of(JobItemStatus.FAILED, JobItemStatus.DEAD_LETTERED)
        )).thenReturn(List.of(failed, deadLettered));
        when(imageRepository.findAllByProjectIdAndIdInAndStatusNot(
            10L,
            List.of(100L, 101L),
            ImageStatus.DELETED
        )).thenReturn(List.of(image100, image101));
        when(jobMessageFactory.create(any(Job.class), any(JobItem.class), any(Image.class)))
            .thenReturn(message());

        JobRetryResponse response = service.retryFailed(20L, 1L);

        assertThat(response.status()).isEqualTo(JobStatus.RETRYING);
        assertThat(response.queuedItems()).isEqualTo(2);
        assertThat(failed.getStatus()).isEqualTo(JobItemStatus.RETRYING);
        assertThat(deadLettered.getAttempt()).isEqualTo(2);
        verify(jobMessagePublisher, org.mockito.Mockito.times(2)).publishRetry(any());
    }

    @Test
    void doesNotPublishWhenThereAreNoRetryableItems() {
        JobRetryService service = service();
        Job job = job(1L);
        when(jobQueryService.findEditableJob(20L, 1L)).thenReturn(job);
        when(jobItemRepository.findAllByJobIdAndStatusIn(
            1L,
            List.of(JobItemStatus.FAILED, JobItemStatus.DEAD_LETTERED)
        )).thenReturn(List.of());

        JobRetryResponse response = service.retryFailed(20L, 1L);

        assertThat(response.queuedItems()).isZero();
        verify(jobMessagePublisher, never()).publishRetry(any());
    }

    private JobRetryService service() {
        return new JobRetryService(
            jobQueryService,
            jobItemRepository,
            imageRepository,
            jobMessageFactory,
            jobMessagePublisher
        );
    }

    private Job job(Long id) {
        Job job = Job.create(10L, 20L, "A4_SCAN_300DPI", Map.of(), false, JobPriority.NORMAL, 2);
        ReflectionTestUtils.setField(job, "id", id);
        job.markQueued(2);
        return job;
    }

    private JobItem item(Long id, JobItemStatus status) {
        JobItem item = new JobItem(1L, id + 90L, status, 1);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private Image image(Long id) {
        Image image = new Image(
            10L,
            1L,
            2L,
            20L,
            "scan.png",
            "originals/scan.png",
            "image/png",
            1024L,
            "a".repeat(64),
            ImageFormat.PNG,
            ImageStatus.UPLOADED
        );
        ReflectionTestUtils.setField(image, "id", id);
        return image;
    }

    private PreprocessJobMessage message() {
        return new PreprocessJobMessage(
            "msg",
            1L,
            10L,
            10L,
            100L,
            20L,
            "originals/scan.png",
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            JobPriority.NORMAL,
            2,
            "trace",
            java.time.Instant.parse("2026-05-10T00:00:00Z")
        );
    }
}
