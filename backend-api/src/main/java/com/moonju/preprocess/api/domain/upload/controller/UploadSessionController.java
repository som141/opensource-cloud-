package com.moonju.preprocess.api.domain.upload.controller;

import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadUrlRequest;
import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadUrlResponse;
import com.moonju.preprocess.api.domain.upload.dto.UploadCompleteRequest;
import com.moonju.preprocess.api.domain.upload.dto.UploadCompleteResponse;
import com.moonju.preprocess.api.domain.upload.dto.UploadSessionCreateRequest;
import com.moonju.preprocess.api.domain.upload.dto.UploadSessionResponse;
import com.moonju.preprocess.api.domain.upload.service.PresignedUploadService;
import com.moonju.preprocess.api.domain.upload.service.UploadCompleteService;
import com.moonju.preprocess.api.domain.upload.service.UploadSessionService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import com.moonju.preprocess.api.global.support.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UploadSessionController {

    private final UploadSessionService uploadSessionService;
    private final PresignedUploadService presignedUploadService;
    private final UploadCompleteService uploadCompleteService;

    public UploadSessionController(
        UploadSessionService uploadSessionService,
        PresignedUploadService presignedUploadService,
        UploadCompleteService uploadCompleteService
    ) {
        this.uploadSessionService = uploadSessionService;
        this.presignedUploadService = presignedUploadService;
        this.uploadCompleteService = uploadCompleteService;
    }

    @PostMapping("/projects/{projectId}/upload-sessions")
    public ApiResponse<UploadSessionResponse> create(
        @CurrentUser Long currentUserId,
        @PathVariable Long projectId,
        @Valid @RequestBody UploadSessionCreateRequest request
    ) {
        return ApiResponse.success(uploadSessionService.create(currentUserId, projectId, request));
    }

    @GetMapping("/upload-sessions/{sessionId}")
    public ApiResponse<UploadSessionResponse> findOne(
        @CurrentUser Long currentUserId,
        @PathVariable Long sessionId
    ) {
        return ApiResponse.success(uploadSessionService.findOne(currentUserId, sessionId));
    }

    @PostMapping("/upload-sessions/{sessionId}/files/presigned-url")
    public ApiResponse<PresignedUploadUrlResponse> createPresignedUrls(
        @CurrentUser Long currentUserId,
        @PathVariable Long sessionId,
        @Valid @RequestBody PresignedUploadUrlRequest request
    ) {
        return ApiResponse.success(presignedUploadService.createUploadUrls(currentUserId, sessionId, request));
    }

    @PostMapping("/upload-sessions/{sessionId}/complete")
    public ApiResponse<UploadCompleteResponse> complete(
        @CurrentUser Long currentUserId,
        @PathVariable Long sessionId,
        @Valid @RequestBody UploadCompleteRequest request
    ) {
        return ApiResponse.success(uploadCompleteService.complete(currentUserId, sessionId, request));
    }

    @DeleteMapping("/upload-sessions/{sessionId}")
    public ApiResponse<Void> cancel(
        @CurrentUser Long currentUserId,
        @PathVariable Long sessionId
    ) {
        uploadSessionService.cancel(currentUserId, sessionId);
        return ApiResponse.success("common204", "Upload session cancelled.", null);
    }
}
