package com.moonju.preprocess.api.domain.image.service;

import com.moonju.preprocess.api.domain.image.dto.ImageDownloadUrlResponse;
import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifact;
import com.moonju.preprocess.api.domain.image.entity.ImageDownloadType;
import com.moonju.preprocess.api.domain.image.exception.ImageArtifactNotFoundException;
import com.moonju.preprocess.api.domain.image.exception.UnsupportedImageDownloadTypeException;
import com.moonju.preprocess.api.domain.image.repository.ImageArtifactRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedDownloadUrlGenerator;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageDownloadService {

    private static final Duration DOWNLOAD_URL_EXPIRES_IN = Duration.ofMinutes(10);

    private final ImageService imageService;
    private final ImageArtifactRepository imageArtifactRepository;
    private final ProjectPermissionService projectPermissionService;
    private final PresignedDownloadUrlGenerator presignedDownloadUrlGenerator;

    public ImageDownloadService(
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
    public ImageDownloadUrlResponse createDownloadUrl(Long currentUserId, Long imageId, String typeValue) {
        ImageDownloadType type = parseDownloadType(typeValue);
        Image image = imageService.findActiveImage(imageId);
        projectPermissionService.validateReadable(image.getProjectId(), currentUserId);

        ImageArtifact artifact = imageArtifactRepository
            .findFirstByImageIdAndTypeOrderByIdDesc(imageId, type.getArtifactType())
            .orElseThrow(() -> new ImageArtifactNotFoundException("Image artifact not found."));
        PresignedDownloadTarget target = presignedDownloadUrlGenerator.generateDownloadUrl(
            new PresignedDownloadCommand(artifact.getObjectKey(), DOWNLOAD_URL_EXPIRES_IN)
        );
        return ImageDownloadUrlResponse.of(imageId, type, target);
    }

    private ImageDownloadType parseDownloadType(String typeValue) {
        try {
            return ImageDownloadType.from(typeValue);
        } catch (IllegalArgumentException exception) {
            throw new UnsupportedImageDownloadTypeException();
        }
    }
}
