package com.moonju.preprocess.api.domain.image.service;

import com.moonju.preprocess.api.domain.image.dto.DebugArtifactResponse;
import com.moonju.preprocess.api.domain.image.dto.ImageReportResponse;
import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifact;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifactType;
import com.moonju.preprocess.api.domain.image.exception.ImageArtifactNotFoundException;
import com.moonju.preprocess.api.domain.image.repository.ImageArtifactRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageArtifactService {

    private static final Duration ARTIFACT_URL_EXPIRES_IN = Duration.ofMinutes(10);

    private final ImageService imageService;
    private final ImageArtifactRepository imageArtifactRepository;
    private final ProjectPermissionService projectPermissionService;
    private final PresignedDownloadUrlGenerator presignedDownloadUrlGenerator;

    public ImageArtifactService(
        ImageService imageService,
        ImageArtifactRepository imageArtifactRepository,
        ProjectPermissionService projectPermissionService,
        PresignedDownloadUrlGenerator presignedDownloadUrlGenerator
    ) {
        this.imageService = imageService;
        this.imageArtifactRepository = imageArtifactRepository;
        this.projectPermissionService = projectPermissionService;
        this.presignedDownloadUrlGenerator = presignedDownloadUrlGenerator;
    }

    @Transactional(readOnly = true)
    public ImageReportResponse findReport(Long currentUserId, Long imageId) {
        Image image = findReadableImage(currentUserId, imageId);
        ImageArtifact artifact = imageArtifactRepository
            .findFirstByImageIdAndTypeOrderByIdDesc(imageId, ImageArtifactType.PROCESSING_REPORT)
            .orElseThrow(() -> new ImageArtifactNotFoundException("Processing report not found."));
        PresignedDownloadTarget target = createDownloadTarget(artifact);
        return ImageReportResponse.of(image.getId(), artifact.getId(), target);
    }

    @Transactional(readOnly = true)
    public List<DebugArtifactResponse> findDebugArtifacts(Long currentUserId, Long imageId) {
        findReadableImage(currentUserId, imageId);
        return imageArtifactRepository.findAllByImageIdAndTypeOrderByIdAsc(imageId, ImageArtifactType.DEBUG)
            .stream()
            .map(artifact -> DebugArtifactResponse.of(artifact, createDownloadTarget(artifact)))
            .toList();
    }

    private Image findReadableImage(Long currentUserId, Long imageId) {
        Image image = imageService.findActiveImage(imageId);
        projectPermissionService.validateReadable(image.getProjectId(), currentUserId);
        return image;
    }

    private PresignedDownloadTarget createDownloadTarget(ImageArtifact artifact) {
        return presignedDownloadUrlGenerator.generateDownloadUrl(
            new PresignedDownloadCommand(artifact.getObjectKey(), ARTIFACT_URL_EXPIRES_IN)
        );
    }
}
