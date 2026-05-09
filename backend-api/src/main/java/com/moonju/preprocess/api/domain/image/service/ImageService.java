package com.moonju.preprocess.api.domain.image.service;

import com.moonju.preprocess.api.domain.image.dto.ImageListResponse;
import com.moonju.preprocess.api.domain.image.dto.ImageResponse;
import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import com.moonju.preprocess.api.domain.image.exception.ImageNotFoundException;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final ProjectPermissionService projectPermissionService;

    public ImageService(
        ImageRepository imageRepository,
        ProjectPermissionService projectPermissionService
    ) {
        this.imageRepository = imageRepository;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ImageListResponse> findProjectImages(Long currentUserId, Long projectId, Pageable pageable) {
        projectPermissionService.validateReadable(projectId, currentUserId);
        return PageResponse.from(imageRepository
            .findAllByProjectIdAndStatusNot(projectId, ImageStatus.DELETED, pageable)
            .map(ImageListResponse::from));
    }

    @Transactional(readOnly = true)
    public ImageResponse findOne(Long currentUserId, Long imageId) {
        Image image = findActiveImage(imageId);
        projectPermissionService.validateReadable(image.getProjectId(), currentUserId);
        return ImageResponse.from(image);
    }

    @Transactional
    public void delete(Long currentUserId, Long imageId) {
        Image image = findActiveImage(imageId);
        projectPermissionService.validateEditable(image.getProjectId(), currentUserId);
        image.delete();
    }

    @Transactional(readOnly = true)
    public Image findActiveImage(Long imageId) {
        return imageRepository.findByIdAndStatusNot(imageId, ImageStatus.DELETED)
            .orElseThrow(ImageNotFoundException::new);
    }
}
