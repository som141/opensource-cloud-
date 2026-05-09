package com.moonju.preprocess.api.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.image.dto.ImageDownloadUrlResponse;
import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifact;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifactType;
import com.moonju.preprocess.api.domain.image.entity.ImageDownloadType;
import com.moonju.preprocess.api.domain.image.entity.ImageFormat;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.exception.UnsupportedImageDownloadTypeException;
import com.moonju.preprocess.api.domain.image.repository.ImageArtifactRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ImageDownloadServiceTests {

    @Mock
    private ImageService imageService;

    @Mock
    private ImageArtifactRepository imageArtifactRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private PresignedDownloadUrlGenerator presignedDownloadUrlGenerator;

    @InjectMocks
    private ImageDownloadService service;

    @Test
    void createsOriginalDownloadUrlWithLowercaseType() {
        Image image = image();
        ImageArtifact artifact = artifact(300L, ImageArtifactType.ORIGINAL);
        when(imageService.findActiveImage(200L)).thenReturn(image);
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);
        when(imageArtifactRepository.findFirstByImageIdAndTypeOrderByIdDesc(200L, ImageArtifactType.ORIGINAL))
            .thenReturn(Optional.of(artifact));
        when(presignedDownloadUrlGenerator.generateDownloadUrl(any(PresignedDownloadCommand.class)))
            .thenReturn(new PresignedDownloadTarget(
                artifact.getObjectKey(),
                "http://localhost:9000/local-download/originals/scan_001.png",
                Instant.parse("2026-05-09T10:00:00Z"),
                Map.of()
            ));

        ImageDownloadUrlResponse response = service.createDownloadUrl(20L, 200L, "original");

        assertThat(response.imageId()).isEqualTo(200L);
        assertThat(response.type()).isEqualTo(ImageDownloadType.ORIGINAL);
        assertThat(response.downloadUrl()).contains("/local-download/");
    }

    @Test
    void rejectsUnsupportedDownloadType() {
        assertThatThrownBy(() -> service.createDownloadUrl(20L, 200L, "report"))
            .isInstanceOf(UnsupportedImageDownloadTypeException.class)
            .hasMessage("Unsupported image download type.");
    }

    private Image image() {
        Image image = new Image(
            10L,
            1L,
            100L,
            20L,
            "scan_001.png",
            "originals/scan_001.png",
            "image/png",
            1024L,
            "a".repeat(64),
            ImageFormat.PNG,
            ImageStatus.UPLOADED
        );
        ReflectionTestUtils.setField(image, "id", 200L);
        return image;
    }

    private ImageArtifact artifact(Long artifactId, ImageArtifactType type) {
        ImageArtifact artifact = new ImageArtifact(
            200L,
            null,
            null,
            type,
            "originals/scan_001.png",
            "image/png",
            1024L,
            null
        );
        ReflectionTestUtils.setField(artifact, "id", artifactId);
        return artifact;
    }
}
