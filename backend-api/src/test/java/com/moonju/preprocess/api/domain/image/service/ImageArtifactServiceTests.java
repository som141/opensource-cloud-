package com.moonju.preprocess.api.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.image.dto.DebugArtifactResponse;
import com.moonju.preprocess.api.domain.image.dto.ImageReportResponse;
import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifact;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifactType;
import com.moonju.preprocess.api.domain.image.entity.ImageFormat;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.repository.ImageArtifactRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ImageArtifactServiceTests {

    @Mock
    private ImageService imageService;

    @Mock
    private ImageArtifactRepository imageArtifactRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private PresignedDownloadUrlGenerator presignedDownloadUrlGenerator;

    @InjectMocks
    private ImageArtifactService service;

    @Test
    void findsProcessingReportDownloadUrl() {
        Image image = image();
        ImageArtifact artifact = artifact(300L, ImageArtifactType.PROCESSING_REPORT, null);
        when(imageService.findActiveImage(200L)).thenReturn(image);
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);
        when(imageArtifactRepository.findFirstByImageIdAndTypeOrderByIdDesc(200L, ImageArtifactType.PROCESSING_REPORT))
            .thenReturn(Optional.of(artifact));
        when(presignedDownloadUrlGenerator.generateDownloadUrl(any(PresignedDownloadCommand.class)))
            .thenReturn(downloadTarget(artifact));

        ImageReportResponse response = service.findReport(20L, 200L);

        assertThat(response.artifactId()).isEqualTo(300L);
        assertThat(response.downloadUrl()).contains("/local-download/");
    }

    @Test
    void listsDebugArtifactDownloadUrls() {
        Image image = image();
        ImageArtifact artifact = artifact(301L, ImageArtifactType.DEBUG, "02_deskew");
        when(imageService.findActiveImage(200L)).thenReturn(image);
        when(projectPermissionService.validateReadable(10L, 20L)).thenReturn(null);
        when(imageArtifactRepository.findAllByImageIdAndTypeOrderByIdAsc(200L, ImageArtifactType.DEBUG))
            .thenReturn(List.of(artifact));
        when(presignedDownloadUrlGenerator.generateDownloadUrl(any(PresignedDownloadCommand.class)))
            .thenReturn(downloadTarget(artifact));

        List<DebugArtifactResponse> response = service.findDebugArtifacts(20L, 200L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().debugStep()).isEqualTo("02_deskew");
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
            ImageStatus.PROCESSED
        );
        ReflectionTestUtils.setField(image, "id", 200L);
        return image;
    }

    private ImageArtifact artifact(Long artifactId, ImageArtifactType type, String debugStep) {
        ImageArtifact artifact = new ImageArtifact(
            200L,
            10L,
            100L,
            type,
            "processed/10/100/artifact",
            "application/json",
            512L,
            debugStep
        );
        ReflectionTestUtils.setField(artifact, "id", artifactId);
        return artifact;
    }

    private PresignedDownloadTarget downloadTarget(ImageArtifact artifact) {
        return new PresignedDownloadTarget(
            artifact.getObjectKey(),
            "http://localhost:9000/local-download/" + artifact.getObjectKey(),
            Instant.parse("2026-05-09T10:00:00Z"),
            Map.of()
        );
    }
}
