package com.moonju.preprocess.api.domain.upload.service;

import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadFileRequest;
import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadUrlRequest;
import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadUrlResponse;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import com.moonju.preprocess.api.domain.upload.exception.InvalidUploadFileException;
import com.moonju.preprocess.api.domain.upload.repository.UploadSessionFileRepository;
import com.moonju.preprocess.api.infra.storage.PresignedUploadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedUploadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedUrlGenerator;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresignedUploadService {

    private static final Duration UPLOAD_URL_EXPIRES_IN = Duration.ofMinutes(15);

    private final UploadSessionService uploadSessionService;
    private final UploadSessionFileRepository uploadSessionFileRepository;
    private final ProjectPermissionService projectPermissionService;
    private final UploadFileValidationService uploadFileValidationService;
    private final PresignedUrlGenerator presignedUrlGenerator;

    public PresignedUploadService(
        UploadSessionService uploadSessionService,
        UploadSessionFileRepository uploadSessionFileRepository,
        ProjectPermissionService projectPermissionService,
        UploadFileValidationService uploadFileValidationService,
        PresignedUrlGenerator presignedUrlGenerator
    ) {
        this.uploadSessionService = uploadSessionService;
        this.uploadSessionFileRepository = uploadSessionFileRepository;
        this.projectPermissionService = projectPermissionService;
        this.uploadFileValidationService = uploadFileValidationService;
        this.presignedUrlGenerator = presignedUrlGenerator;
    }

    @Transactional
    public PresignedUploadUrlResponse createUploadUrls(
        Long currentUserId,
        Long sessionId,
        PresignedUploadUrlRequest request
    ) {
        UploadSession uploadSession = uploadSessionService.findOpenSession(sessionId);
        projectPermissionService.validateEditable(currentUserId, uploadSession.getProjectId());
        validateRequest(uploadSession, request.files());

        List<PresignedUploadUrlResponse.UploadTargetResponse> targets = request.files()
            .stream()
            .map(file -> createUploadTarget(uploadSession, file))
            .toList();

        uploadSession.markUploadUrlIssued();
        return PresignedUploadUrlResponse.of(uploadSession.getId(), targets);
    }

    private void validateRequest(UploadSession uploadSession, List<PresignedUploadFileRequest> files) {
        if (files.size() > uploadSession.getExpectedFileCount()) {
            throw new InvalidUploadFileException("Requested file count exceeds upload session expectation.");
        }

        Set<String> checksums = new HashSet<>();
        long totalSizeBytes = 0L;
        for (PresignedUploadFileRequest file : files) {
            uploadFileValidationService.validate(file);
            if (!checksums.add(file.checksumSha256())) {
                throw new InvalidUploadFileException("Duplicate checksum exists in the upload request.");
            }
            if (uploadSessionFileRepository.existsByProjectIdAndChecksumSha256(
                uploadSession.getProjectId(),
                file.checksumSha256()
            )) {
                throw new InvalidUploadFileException("Duplicate file checksum already exists in this project.");
            }
            totalSizeBytes += file.sizeBytes();
        }

        if (totalSizeBytes > uploadSession.getExpectedTotalSizeBytes()) {
            throw new InvalidUploadFileException("Requested file size exceeds upload session expectation.");
        }
    }

    private PresignedUploadUrlResponse.UploadTargetResponse createUploadTarget(
        UploadSession uploadSession,
        PresignedUploadFileRequest file
    ) {
        String objectKey = buildOriginalObjectKey(uploadSession, file.fileName());
        UploadSessionFile uploadFile = uploadSessionFileRepository.save(UploadSessionFile.issued(
            uploadSession.getId(),
            uploadSession.getProjectId(),
            file.fileName(),
            objectKey,
            file.contentType(),
            file.sizeBytes(),
            file.checksumSha256()
        ));

        PresignedUploadTarget target = presignedUrlGenerator.generateUploadUrl(new PresignedUploadCommand(
            objectKey,
            file.contentType(),
            file.sizeBytes(),
            UPLOAD_URL_EXPIRES_IN
        ));
        return PresignedUploadUrlResponse.UploadTargetResponse.of(uploadFile, target);
    }

    private String buildOriginalObjectKey(UploadSession uploadSession, String fileName) {
        String safeFileName = fileName.replace("\\", "_").replace("/", "_");
        return "originals/%d/%d/%s/%s".formatted(
            uploadSession.getProjectId(),
            uploadSession.getId(),
            UUID.randomUUID(),
            safeFileName
        );
    }
}
