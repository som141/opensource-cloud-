package com.moonju.preprocess.api.domain.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadFileRequest;
import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadUrlRequest;
import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadUrlResponse;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionStatus;
import com.moonju.preprocess.api.domain.upload.exception.InvalidUploadFileException;
import com.moonju.preprocess.api.domain.upload.repository.UploadSessionFileRepository;
import com.moonju.preprocess.api.infra.storage.PresignedUploadCommand;
import com.moonju.preprocess.api.infra.storage.PresignedUploadTarget;
import com.moonju.preprocess.api.infra.storage.PresignedUrlGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PresignedUploadServiceTests {

    @Mock
    private UploadSessionService uploadSessionService;

    @Mock
    private UploadSessionFileRepository uploadSessionFileRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private UploadFileValidationService uploadFileValidationService;

    @Mock
    private PresignedUrlGenerator presignedUrlGenerator;

    @InjectMocks
    private PresignedUploadService service;

    @Test
    void createsPresignedUploadTarget() {
        UploadSession uploadSession = uploadSession(1L, 10L, 20L, 1, 4096L);
        PresignedUploadFileRequest file = uploadFile("scan_001.png", "image/png", 1024L, "a".repeat(64));

        when(uploadSessionService.findOpenSession(1L)).thenReturn(uploadSession);
        doNothing().when(projectPermissionService).validateEditable(20L, 10L);
        doNothing().when(uploadFileValidationService).validate(file);
        when(uploadSessionFileRepository.existsByProjectIdAndChecksumSha256(10L, file.checksumSha256()))
            .thenReturn(false);
        when(uploadSessionFileRepository.save(any(UploadSessionFile.class))).thenAnswer(invocation -> {
            UploadSessionFile saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        when(presignedUrlGenerator.generateUploadUrl(any(PresignedUploadCommand.class)))
            .thenReturn(new PresignedUploadTarget(
                "originals/10/1/file/scan_001.png",
                "http://localhost:9000/upload",
                Instant.parse("2026-05-03T08:00:00Z"),
                Map.of("Content-Type", "image/png")
            ));

        PresignedUploadUrlResponse response = service.createUploadUrls(
            20L,
            1L,
            new PresignedUploadUrlRequest(List.of(file))
        );

        assertThat(uploadSession.getStatus()).isEqualTo(UploadSessionStatus.UPLOAD_URL_ISSUED);
        assertThat(response.sessionId()).isEqualTo(1L);
        assertThat(response.uploadTargets()).hasSize(1);
        assertThat(response.uploadTargets().getFirst().uploadFileId()).isEqualTo(100L);
        assertThat(response.uploadTargets().getFirst().uploadUrl()).isEqualTo("http://localhost:9000/upload");
    }

    @Test
    void rejectsDuplicateChecksumInProject() {
        UploadSession uploadSession = uploadSession(1L, 10L, 20L, 1, 4096L);
        PresignedUploadFileRequest file = uploadFile("scan_001.png", "image/png", 1024L, "a".repeat(64));

        when(uploadSessionService.findOpenSession(1L)).thenReturn(uploadSession);
        doNothing().when(projectPermissionService).validateEditable(20L, 10L);
        doNothing().when(uploadFileValidationService).validate(file);
        when(uploadSessionFileRepository.existsByProjectIdAndChecksumSha256(10L, file.checksumSha256()))
            .thenReturn(true);

        assertThatThrownBy(() -> service.createUploadUrls(
            20L,
            1L,
            new PresignedUploadUrlRequest(List.of(file))
        ))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("Duplicate file checksum already exists in this project.");
    }

    private UploadSession uploadSession(
        Long id,
        Long projectId,
        Long userId,
        int expectedFileCount,
        long expectedTotalSizeBytes
    ) {
        UploadSession uploadSession = UploadSession.create(
            projectId,
            userId,
            expectedFileCount,
            expectedTotalSizeBytes
        );
        ReflectionTestUtils.setField(uploadSession, "id", id);
        return uploadSession;
    }

    private PresignedUploadFileRequest uploadFile(
        String fileName,
        String contentType,
        long sizeBytes,
        String checksumSha256
    ) {
        return new PresignedUploadFileRequest(fileName, contentType, sizeBytes, checksumSha256);
    }
}
