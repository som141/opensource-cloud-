package com.moonju.preprocess.api.domain.upload.service;

import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.domain.upload.dto.UploadSessionCreateRequest;
import com.moonju.preprocess.api.domain.upload.dto.UploadSessionResponse;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.exception.UploadNotCompletedException;
import com.moonju.preprocess.api.domain.upload.exception.UploadSessionNotFoundException;
import com.moonju.preprocess.api.domain.upload.repository.UploadSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadSessionService {

    private final UploadSessionRepository uploadSessionRepository;
    private final ProjectPermissionService projectPermissionService;

    public UploadSessionService(
        UploadSessionRepository uploadSessionRepository,
        ProjectPermissionService projectPermissionService
    ) {
        this.uploadSessionRepository = uploadSessionRepository;
        this.projectPermissionService = projectPermissionService;
    }

    @Transactional
    public UploadSessionResponse create(Long currentUserId, Long projectId, UploadSessionCreateRequest request) {
        projectPermissionService.validateEditable(currentUserId, projectId);
        UploadSession uploadSession = uploadSessionRepository.save(UploadSession.create(
            projectId,
            currentUserId,
            request.expectedFileCount(),
            request.expectedTotalSizeBytes()
        ));
        return UploadSessionResponse.from(uploadSession);
    }

    @Transactional(readOnly = true)
    public UploadSessionResponse findOne(Long currentUserId, Long sessionId) {
        UploadSession uploadSession = findByIdAndUserId(sessionId, currentUserId);
        projectPermissionService.validateReadable(currentUserId, uploadSession.getProjectId());
        return UploadSessionResponse.from(uploadSession);
    }

    @Transactional
    public void cancel(Long currentUserId, Long sessionId) {
        UploadSession uploadSession = findByIdAndUserId(sessionId, currentUserId);
        projectPermissionService.validateEditable(currentUserId, uploadSession.getProjectId());
        uploadSession.cancel();
    }

    @Transactional(readOnly = true)
    public UploadSession findOpenSession(Long sessionId) {
        UploadSession uploadSession = uploadSessionRepository.findById(sessionId)
            .orElseThrow(UploadSessionNotFoundException::new);
        if (!uploadSession.isOpen()) {
            throw new UploadNotCompletedException("Upload session is not open.");
        }
        return uploadSession;
    }

    private UploadSession findByIdAndUserId(Long sessionId, Long currentUserId) {
        return uploadSessionRepository.findByIdAndUserId(sessionId, currentUserId)
            .orElseThrow(UploadSessionNotFoundException::new);
    }
}
