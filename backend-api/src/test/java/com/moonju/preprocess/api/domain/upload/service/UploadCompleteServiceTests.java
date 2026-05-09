package com.moonju.preprocess.api.domain.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.image.service.ImageCreateService;
import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.domain.upload.dto.UploadCompleteRequest;
import com.moonju.preprocess.api.domain.upload.dto.UploadCompleteResponse;
import com.moonju.preprocess.api.domain.upload.entity.UploadFileStatus;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionStatus;
import com.moonju.preprocess.api.domain.upload.exception.UploadNotCompletedException;
import com.moonju.preprocess.api.domain.upload.repository.UploadSessionFileRepository;
import com.moonju.preprocess.api.infra.storage.ObjectStoragePort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UploadCompleteServiceTests {

    @Mock
    private UploadSessionService uploadSessionService;

    @Mock
    private UploadSessionFileRepository uploadSessionFileRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private ImageCreateService imageCreateService;

    @InjectMocks
    private UploadCompleteService service;

    @Test
    void completesUploadSessionAfterObjectExistenceVerification() {
        UploadSession uploadSession = uploadSession(1L, 10L, 20L, 1, 4096L);
        UploadSessionFile file = uploadSessionFile(100L, 1L, 10L, "originals/10/1/file/scan_001.png");

        when(uploadSessionService.findOpenSession(1L)).thenReturn(uploadSession);
        when(projectPermissionService.validateEditable(10L, 20L)).thenReturn(null);
        when(uploadSessionFileRepository.findByUploadSessionIdAndIdIn(1L, List.of(100L))).thenReturn(List.of(file));
        when(objectStoragePort.exists(file.getObjectKey())).thenReturn(true);

        UploadCompleteResponse response = service.complete(20L, 1L, new UploadCompleteRequest(List.of(100L)));

        assertThat(response.sessionId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(UploadSessionStatus.COMPLETED);
        assertThat(response.uploadedFileCount()).isEqualTo(1);
        assertThat(uploadSession.getStatus()).isEqualTo(UploadSessionStatus.COMPLETED);
        assertThat(file.getStatus()).isEqualTo(UploadFileStatus.UPLOADED);
        verify(imageCreateService).createFromCompletedUpload(uploadSession, List.of(file));
    }

    @Test
    void rejectsCompletionWhenObjectDoesNotExist() {
        UploadSession uploadSession = uploadSession(1L, 10L, 20L, 1, 4096L);
        UploadSessionFile file = uploadSessionFile(100L, 1L, 10L, "originals/10/1/file/scan_001.png");

        when(uploadSessionService.findOpenSession(1L)).thenReturn(uploadSession);
        when(projectPermissionService.validateEditable(10L, 20L)).thenReturn(null);
        when(uploadSessionFileRepository.findByUploadSessionIdAndIdIn(1L, List.of(100L))).thenReturn(List.of(file));
        when(objectStoragePort.exists(file.getObjectKey())).thenReturn(false);

        assertThatThrownBy(() -> service.complete(20L, 1L, new UploadCompleteRequest(List.of(100L))))
            .isInstanceOf(UploadNotCompletedException.class)
            .hasMessage("Uploaded object does not exist: originals/10/1/file/scan_001.png");
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
        uploadSession.markUploadUrlIssued();
        return uploadSession;
    }

    private UploadSessionFile uploadSessionFile(
        Long id,
        Long uploadSessionId,
        Long projectId,
        String objectKey
    ) {
        UploadSessionFile file = UploadSessionFile.issued(
            uploadSessionId,
            projectId,
            "scan_001.png",
            objectKey,
            "image/png",
            1024L,
            "a".repeat(64)
        );
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }
}
