package com.moonju.preprocess.api.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifact;
import com.moonju.preprocess.api.domain.image.entity.ImageArtifactType;
import com.moonju.preprocess.api.domain.image.model.ImageMetadata;
import com.moonju.preprocess.api.domain.image.repository.ImageArtifactRepository;
import com.moonju.preprocess.api.domain.image.repository.ImageRepository;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ImageCreateServiceTests {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ImageArtifactRepository imageArtifactRepository;

    @InjectMocks
    private ImageCreateService service;

    @Test
    void createsImageAndOriginalArtifactFromCompletedUpload() {
        UploadSession uploadSession = uploadSession();
        UploadSessionFile file = uploadSessionFile(100L);
        ImageMetadata metadata = new ImageMetadata(1240, 1754, 300, 300);
        when(imageRepository.existsByUploadSessionFileId(100L)).thenReturn(false);
        when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> {
            Image image = invocation.getArgument(0);
            ReflectionTestUtils.setField(image, "id", 200L);
            return image;
        });

        List<Image> images = service.createFromCompletedUpload(uploadSession, List.of(file), Map.of(100L, metadata));

        assertThat(images).hasSize(1);
        assertThat(images.getFirst().getUploadSessionFileId()).isEqualTo(100L);
        assertThat(images.getFirst().getWidth()).isEqualTo(1240);
        assertThat(images.getFirst().getHeight()).isEqualTo(1754);
        assertThat(images.getFirst().getDpiX()).isEqualTo(300);
        assertThat(images.getFirst().getDpiY()).isEqualTo(300);
        ArgumentCaptor<ImageArtifact> artifactCaptor = ArgumentCaptor.forClass(ImageArtifact.class);
        verify(imageArtifactRepository).save(artifactCaptor.capture());
        assertThat(artifactCaptor.getValue().getImageId()).isEqualTo(200L);
        assertThat(artifactCaptor.getValue().getType()).isEqualTo(ImageArtifactType.ORIGINAL);
    }

    @Test
    void skipsAlreadyFinalizedUploadFile() {
        UploadSession uploadSession = uploadSession();
        UploadSessionFile file = uploadSessionFile(100L);
        when(imageRepository.existsByUploadSessionFileId(100L)).thenReturn(true);

        List<Image> images = service.createFromCompletedUpload(uploadSession, List.of(file));

        assertThat(images).isEmpty();
        verify(imageRepository, never()).save(any(Image.class));
        verify(imageArtifactRepository, never()).save(any(ImageArtifact.class));
    }

    private UploadSession uploadSession() {
        UploadSession uploadSession = UploadSession.create(10L, 20L, 1, 4096L);
        ReflectionTestUtils.setField(uploadSession, "id", 1L);
        return uploadSession;
    }

    private UploadSessionFile uploadSessionFile(Long id) {
        UploadSessionFile file = UploadSessionFile.issued(
            1L,
            10L,
            "scan_001.png",
            "originals/10/1/file/scan_001.png",
            "image/png",
            1024L,
            "a".repeat(64)
        );
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }
}
