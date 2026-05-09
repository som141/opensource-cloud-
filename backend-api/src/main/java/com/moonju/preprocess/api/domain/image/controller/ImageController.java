package com.moonju.preprocess.api.domain.image.controller;

import com.moonju.preprocess.api.domain.image.dto.DebugArtifactResponse;
import com.moonju.preprocess.api.domain.image.dto.ImageDownloadUrlResponse;
import com.moonju.preprocess.api.domain.image.dto.ImageListResponse;
import com.moonju.preprocess.api.domain.image.dto.ImageReportResponse;
import com.moonju.preprocess.api.domain.image.dto.ImageResponse;
import com.moonju.preprocess.api.domain.image.service.ImageArtifactService;
import com.moonju.preprocess.api.domain.image.service.ImageDownloadService;
import com.moonju.preprocess.api.domain.image.service.ImageService;
import com.moonju.preprocess.api.global.error.ErrorCode;
import com.moonju.preprocess.api.global.response.ApiResponse;
import com.moonju.preprocess.api.global.response.PageResponse;
import com.moonju.preprocess.api.global.support.CurrentUser;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ImageController {

    private final ImageService imageService;
    private final ImageDownloadService imageDownloadService;
    private final ImageArtifactService imageArtifactService;

    public ImageController(
        ImageService imageService,
        ImageDownloadService imageDownloadService,
        ImageArtifactService imageArtifactService
    ) {
        this.imageService = imageService;
        this.imageDownloadService = imageDownloadService;
        this.imageArtifactService = imageArtifactService;
    }

    @GetMapping("/projects/{projectId}/images")
    public ApiResponse<PageResponse<ImageListResponse>> findProjectImages(
        @CurrentUser Long currentUserId,
        @PathVariable Long projectId,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(imageService.findProjectImages(currentUserId, projectId, pageable));
    }

    @GetMapping("/images/{imageId}")
    public ApiResponse<ImageResponse> findOne(
        @CurrentUser Long currentUserId,
        @PathVariable Long imageId
    ) {
        return ApiResponse.success(imageService.findOne(currentUserId, imageId));
    }

    @DeleteMapping("/images/{imageId}")
    public ApiResponse<Void> delete(
        @CurrentUser Long currentUserId,
        @PathVariable Long imageId
    ) {
        imageService.delete(currentUserId, imageId);
        return ApiResponse.success(ErrorCode.COMMON_NO_CONTENT, null);
    }

    @GetMapping("/images/{imageId}/download")
    public ApiResponse<ImageDownloadUrlResponse> createDownloadUrl(
        @CurrentUser Long currentUserId,
        @PathVariable Long imageId,
        @RequestParam(defaultValue = "original") String type
    ) {
        return ApiResponse.success(imageDownloadService.createDownloadUrl(currentUserId, imageId, type));
    }

    @GetMapping("/images/{imageId}/report")
    public ApiResponse<ImageReportResponse> findReport(
        @CurrentUser Long currentUserId,
        @PathVariable Long imageId
    ) {
        return ApiResponse.success(imageArtifactService.findReport(currentUserId, imageId));
    }

    @GetMapping("/images/{imageId}/debug-artifacts")
    public ApiResponse<List<DebugArtifactResponse>> findDebugArtifacts(
        @CurrentUser Long currentUserId,
        @PathVariable Long imageId
    ) {
        return ApiResponse.success(imageArtifactService.findDebugArtifacts(currentUserId, imageId));
    }
}
