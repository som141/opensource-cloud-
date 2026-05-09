package com.moonju.preprocess.api.domain.upload.service;

import com.moonju.preprocess.api.domain.image.service.ImageCreateService;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.domain.upload.dto.UploadCompleteRequest;
import com.moonju.preprocess.api.domain.upload.dto.UploadCompleteResponse;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import com.moonju.preprocess.api.domain.upload.exception.UploadNotCompletedException;
import com.moonju.preprocess.api.domain.upload.repository.UploadSessionFileRepository;
import com.moonju.preprocess.api.infra.storage.ObjectStoragePort;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UploadCompleteService {

    private final UploadSessionService uploadSessionService;
    private final UploadSessionFileRepository uploadSessionFileRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ObjectStoragePort objectStoragePort;
    private final ImageCreateService imageCreateService;

    public UploadCompleteService(
        UploadSessionService uploadSessionService,
        UploadSessionFileRepository uploadSessionFileRepository,
        ProjectPermissionService projectPermissionService,
        ObjectStoragePort objectStoragePort,
        ImageCreateService imageCreateService
    ) {
        this.uploadSessionService = uploadSessionService;
        this.uploadSessionFileRepository = uploadSessionFileRepository;
        this.projectPermissionService = projectPermissionService;
        this.objectStoragePort = objectStoragePort;
        this.imageCreateService = imageCreateService;
    }

    @Transactional
    public UploadCompleteResponse complete(Long currentUserId, Long sessionId, UploadCompleteRequest request) {
        UploadSession uploadSession = uploadSessionService.findOpenSession(sessionId);
        projectPermissionService.validateEditable(uploadSession.getProjectId(), currentUserId);

        List<UploadSessionFile> files = uploadSessionFileRepository.findByUploadSessionIdAndIdIn(
            uploadSession.getId(),
            request.uploadFileIds()
        );
        validateCompletion(uploadSession, request.uploadFileIds(), files);

        files.forEach(this::verifyAndMarkUploaded);
        uploadSession.complete();
        imageCreateService.createFromCompletedUpload(uploadSession, files);
        return UploadCompleteResponse.of(uploadSession, files.size());
    }

    private void validateCompletion(
        UploadSession uploadSession,
        List<Long> requestedUploadFileIds,
        List<UploadSessionFile> files
    ) {
        Set<Long> requestedIds = new HashSet<>(requestedUploadFileIds);
        if (requestedIds.size() != requestedUploadFileIds.size()) {
            throw new UploadNotCompletedException("Upload file ids contain duplicates.");
        }
        if (files.size() != uploadSession.getExpectedFileCount()) {
            throw new UploadNotCompletedException("Uploaded file count does not match upload session expectation.");
        }
        if (files.size() != requestedIds.size()) {
            throw new UploadNotCompletedException("Some requested upload files do not belong to the upload session.");
        }
    }

    private void verifyAndMarkUploaded(UploadSessionFile file) {
        if (!objectStoragePort.exists(file.getObjectKey())) {
            throw new UploadNotCompletedException("Uploaded object does not exist: " + file.getObjectKey());
        }
        file.markUploaded();
    }
}
