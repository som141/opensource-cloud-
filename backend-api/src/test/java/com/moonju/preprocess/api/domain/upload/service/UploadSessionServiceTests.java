package com.moonju.preprocess.api.domain.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.project.service.ProjectPermissionService;
import com.moonju.preprocess.api.domain.upload.dto.UploadSessionCreateRequest;
import com.moonju.preprocess.api.domain.upload.dto.UploadSessionResponse;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionStatus;
import com.moonju.preprocess.api.domain.upload.exception.UploadNotCompletedException;
import com.moonju.preprocess.api.domain.upload.repository.UploadSessionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UploadSessionServiceTests {

    @Mock
    private UploadSessionRepository uploadSessionRepository;

    @Mock
    private ProjectPermissionService projectPermissionService;

    @InjectMocks
    private UploadSessionService service;

    @Test
    void createsUploadSessionAfterProjectEditPermissionValidation() {
        UploadSession session = uploadSession(1L, 10L, 20L, 3, 4096L);
        when(projectPermissionService.validateEditable(10L, 20L)).thenReturn(null);
        when(uploadSessionRepository.save(any(UploadSession.class))).thenReturn(session);

        UploadSessionResponse response = service.create(20L, 10L, new UploadSessionCreateRequest(3, 4096L));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.projectId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(UploadSessionStatus.CREATED);
        verify(projectPermissionService).validateEditable(10L, 20L);
    }

    @Test
    void readsUploadSessionByProjectReadablePermission() {
        UploadSession session = uploadSession(1L, 10L, 20L, 3, 4096L);
        when(uploadSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(projectPermissionService.validateReadable(10L, 30L)).thenReturn(null);

        UploadSessionResponse response = service.findOne(30L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        verify(projectPermissionService).validateReadable(10L, 30L);
    }

    @Test
    void rejectsCancelWhenSessionIsAlreadyCompleted() {
        UploadSession session = uploadSession(1L, 10L, 20L, 1, 4096L);
        session.complete();
        when(uploadSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(projectPermissionService.validateEditable(10L, 20L)).thenReturn(null);

        assertThatThrownBy(() -> service.cancel(20L, 1L))
            .isInstanceOf(UploadNotCompletedException.class)
            .hasMessage("Upload session cannot be cancelled.");
    }

    private UploadSession uploadSession(
        Long id,
        Long projectId,
        Long userId,
        int expectedFileCount,
        long expectedTotalSizeBytes
    ) {
        UploadSession session = UploadSession.create(projectId, userId, expectedFileCount, expectedTotalSizeBytes);
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
