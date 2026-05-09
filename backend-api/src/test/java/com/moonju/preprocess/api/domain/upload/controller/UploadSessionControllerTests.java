package com.moonju.preprocess.api.domain.upload.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.upload.dto.UploadSessionCreateRequest;
import com.moonju.preprocess.api.domain.upload.dto.UploadSessionResponse;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionStatus;
import com.moonju.preprocess.api.domain.upload.service.PresignedUploadService;
import com.moonju.preprocess.api.domain.upload.service.UploadCompleteService;
import com.moonju.preprocess.api.domain.upload.service.UploadSessionService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadSessionControllerTests {

    @Mock
    private UploadSessionService uploadSessionService;

    @Mock
    private PresignedUploadService presignedUploadService;

    @Mock
    private UploadCompleteService uploadCompleteService;

    @Test
    void createsUploadSessionWithCommonCreatedResponse() {
        UploadSessionController controller = new UploadSessionController(
            uploadSessionService,
            presignedUploadService,
            uploadCompleteService
        );
        UploadSessionCreateRequest request = new UploadSessionCreateRequest(3, 4096L);
        UploadSessionResponse serviceResponse = new UploadSessionResponse(
            1L,
            10L,
            20L,
            UploadSessionStatus.CREATED,
            3,
            4096L,
            null,
            null
        );
        when(uploadSessionService.create(20L, 10L, request)).thenReturn(serviceResponse);

        ApiResponse<UploadSessionResponse> response = controller.create(20L, 10L, request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common201");
        assertThat(response.result()).isSameAs(serviceResponse);
    }

    @Test
    void cancelsUploadSessionWithCommonNoContentResponse() {
        UploadSessionController controller = new UploadSessionController(
            uploadSessionService,
            presignedUploadService,
            uploadCompleteService
        );

        ApiResponse<Void> response = controller.cancel(20L, 1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common204");
        verify(uploadSessionService).cancel(20L, 1L);
    }
}
