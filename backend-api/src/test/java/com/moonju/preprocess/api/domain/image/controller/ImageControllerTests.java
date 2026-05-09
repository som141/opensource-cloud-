package com.moonju.preprocess.api.domain.image.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.moonju.preprocess.api.domain.image.service.ImageArtifactService;
import com.moonju.preprocess.api.domain.image.service.ImageDownloadService;
import com.moonju.preprocess.api.domain.image.service.ImageService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageControllerTests {

    @Mock
    private ImageService imageService;

    @Mock
    private ImageDownloadService imageDownloadService;

    @Mock
    private ImageArtifactService imageArtifactService;

    @Test
    void deletesImageWithCommonNoContentResponse() {
        ImageController controller = new ImageController(imageService, imageDownloadService, imageArtifactService);

        ApiResponse<Void> response = controller.delete(20L, 200L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common204");
        verify(imageService).delete(20L, 200L);
    }
}
