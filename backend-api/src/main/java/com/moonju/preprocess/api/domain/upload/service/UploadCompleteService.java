package com.moonju.preprocess.api.domain.upload.service;

import com.moonju.preprocess.api.domain.image.service.ImageCreateService;
import com.moonju.preprocess.api.domain.image.model.ImageMetadata;
import com.moonju.preprocess.api.domain.image.service.ImageMetadataExtractor;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.domain.upload.dto.UploadCompleteRequest;
import com.moonju.preprocess.api.domain.upload.dto.UploadCompleteResponse;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import com.moonju.preprocess.api.domain.upload.exception.UploadNotCompletedException;
import com.moonju.preprocess.api.domain.upload.repository.UploadSessionFileRepository;
import com.moonju.preprocess.api.infra.storage.ObjectStoragePort;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final UploadedImageMagicNumberValidator magicNumberValidator;
    private final ImageMetadataExtractor imageMetadataExtractor;

    public UploadCompleteService(
        UploadSessionService uploadSessionService,
        UploadSessionFileRepository uploadSessionFileRepository,
        ProjectPermissionService projectPermissionService,
        ObjectStoragePort objectStoragePort,
        ImageCreateService imageCreateService,
        UploadedImageMagicNumberValidator magicNumberValidator,
        ImageMetadataExtractor imageMetadataExtractor
    ) {
        this.uploadSessionService = uploadSessionService;
        this.uploadSessionFileRepository = uploadSessionFileRepository;
        this.projectPermissionService = projectPermissionService;
        this.objectStoragePort = objectStoragePort;
        this.imageCreateService = imageCreateService;
        this.magicNumberValidator = magicNumberValidator;
        this.imageMetadataExtractor = imageMetadataExtractor;
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

        Map<Long, ImageMetadata> metadataByUploadFileId = new HashMap<>();
        files.forEach(file -> metadataByUploadFileId.put(file.getId(), verifyAndMarkUploaded(file)));
        uploadSession.complete();
        imageCreateService.createFromCompletedUpload(uploadSession, files, metadataByUploadFileId);
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

    private ImageMetadata verifyAndMarkUploaded(UploadSessionFile file) {
        if (!objectStoragePort.exists(file.getObjectKey())) {
            throw new UploadNotCompletedException("Uploaded object does not exist: " + file.getObjectKey());
        }
        byte[] bytes = objectStoragePort.downloadBytes(file.getObjectKey());
        magicNumberValidator.validate(file, bytes);
        ImageMetadata metadata = imageMetadataExtractor.extract(bytes);
        file.markUploaded();
        return metadata;
    }
}
