package com.moonju.preprocess.api.domain.image.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ImageArtifactTests {

    @Test
    void createsOriginalArtifactFromImage() {
        Image image = new Image(
            10L,
            1L,
            100L,
            20L,
            "scan_001.png",
            "originals/10/1/file/scan_001.png",
            "image/png",
            1024L,
            "a".repeat(64),
            ImageFormat.PNG,
            ImageStatus.UPLOADED
        );
        ReflectionTestUtils.setField(image, "id", 200L);

        ImageArtifact artifact = ImageArtifact.original(image);

        assertThat(artifact.getImageId()).isEqualTo(200L);
        assertThat(artifact.getType()).isEqualTo(ImageArtifactType.ORIGINAL);
        assertThat(artifact.getObjectKey()).isEqualTo("originals/10/1/file/scan_001.png");
        assertThat(artifact.getContentType()).isEqualTo("image/png");
        assertThat(artifact.getSizeBytes()).isEqualTo(1024L);
    }
}
