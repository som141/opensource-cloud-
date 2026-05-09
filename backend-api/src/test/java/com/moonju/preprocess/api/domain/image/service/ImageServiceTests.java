package com.moonju.preprocess.api.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.image.dto.ImageListResponse;
import com.moonju.preprocess.api.domain.image.dto.ImageResponse;
import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageFormat;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.response.PageResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ImageServiceTests {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @InjectMocks
    private ImageService service;

    @Test
    void listsProjectImagesAfterReadPermissionValidation() {
        Image image = image(200L, 10L);
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);
        when(imageRepository.findAllByProjectIdAndStatusNot(10L, ImageStatus.DELETED, PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(java.util.List.of(image)));

        PageResponse<ImageListResponse> response = service.findProjectImages(20L, 10L, PageRequest.of(0, 20));

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(200L);
        verify(projectPermissionService).validateReadable(10L, 20L);
    }

    @Test
    void findsImageDetailAfterReadPermissionValidation() {
        Image image = image(200L, 10L);
        when(imageRepository.findByIdAndStatusNot(200L, ImageStatus.DELETED)).thenReturn(Optional.of(image));
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);

        ImageResponse response = service.findOne(20L, 200L);

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.originalFileName()).isEqualTo("scan_001.png");
        verify(projectPermissionService).validateReadable(10L, 20L);
    }

    @Test
    void softDeletesImageAfterEditPermissionValidation() {
        Image image = image(200L, 10L);
        when(imageRepository.findByIdAndStatusNot(200L, ImageStatus.DELETED)).thenReturn(Optional.of(image));
        when(projectPermissionService.validateEditable(10L, 20L)).thenReturn(null);

        service.delete(20L, 200L);

        assertThat(image.isDeleted()).isTrue();
        verify(projectPermissionService).validateEditable(10L, 20L);
    }

    private Image image(Long imageId, Long projectId) {
        Image image = new Image(
            projectId,
            1L,
            100L,
            20L,
            "scan_001.png",
            "originals/10/1/file/scan_001.png",
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
