package com.moonju.preprocess.api.domain.job.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageFormat;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.job.dto.JobCreateRequest;
import com.moonju.preprocess.api.domain.job.dto.JobCreateResponse;
import com.moonju.preprocess.api.domain.job.entity.Job;
import com.moonju.preprocess.api.domain.job.entity.JobItem;
import com.moonju.preprocess.api.domain.job.entity.JobPriority;
import com.moonju.preprocess.api.domain.job.entity.JobStatus;
import com.moonju.preprocess.api.domain.job.exception.InvalidJobRequestException;
import com.moonju.preprocess.api.domain.job.repository.JobItemRepository;
import com.moonju.preprocess.api.domain.job.repository.JobRepository;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateRequest;
import com.moonju.preprocess.api.domain.preprocess.dto.PresetValidateResponse;
import com.moonju.preprocess.api.domain.preprocess.service.PreprocessPresetService;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.infra.rabbitmq.JobMessagePublisher;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JobCommandServiceTests {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobItemRepository jobItemRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private PreprocessPresetService preprocessPresetService;

    @Mock
    private JobMessageFactory jobMessageFactory;

    @Mock
    private JobMessagePublisher jobMessagePublisher;

    @InjectMocks
    private JobCommandService service;

    @Test
    void createsJobItemsAndPublishesOneMessagePerImage() {
        Image image = image(100L, 10L);
        when(projectPermissionService.validateEditable(10L, 20L)).thenReturn(null);
        when(imageRepository.findAllByProjectIdAndIdInAndStatusNot(10L, List.of(100L), ImageStatus.DELETED))
            .thenReturn(List.of(image));
        when(preprocessPresetService.validate(any(PresetValidateRequest.class)))
            .thenReturn(PresetValidateResponse.valid("A4_SCAN_300DPI", Map.of("targetDpi", "300")));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", 1L);
            return job;
        });
        when(jobItemRepository.save(any(JobItem.class))).thenAnswer(invocation -> {
            JobItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 10L);
            return item;
        });

        JobCreateResponse response = service.create(20L, new JobCreateRequest(
            10L,
            List.of(100L),
            "A4_SCAN_300DPI",
            Map.of("targetDpi", "300"),
            false,
            JobPriority.NORMAL,
            null
        ));

        assertThat(response.jobId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(JobStatus.QUEUED);
        assertThat(response.queuedImages()).isEqualTo(1);
        verify(jobMessagePublisher).publishPreprocess(any());
    }

    @Test
    void rejectsDuplicateImageIds() {
        assertThatThrownBy(() -> service.create(20L, new JobCreateRequest(
            10L,
            List.of(100L, 100L),
            "A4_SCAN_300DPI",
            Map.of(),
            false,
            JobPriority.NORMAL,
            null
        )))
            .isInstanceOf(InvalidJobRequestException.class)
            .hasMessage("Duplicate imageIds are not allowed.");
    }

    private Image image(Long imageId, Long projectId) {
        Image image = new Image(
            projectId,
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
        ReflectionTestUtils.setField(image, "id", imageId);
        return image;
    }
}
