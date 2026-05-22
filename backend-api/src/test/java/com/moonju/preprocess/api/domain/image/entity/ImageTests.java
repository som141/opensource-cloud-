package com.moonju.preprocess.api.domain.image.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.api.domain.image.model.ImageMetadata;
import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ImageTests {

    @Test
    void createsImageFromUploadedFile() {
        UploadSession uploadSession = UploadSession.create(10L, 20L, 1, 4096L);
        ReflectionTestUtils.setField(uploadSession, "id", 1L);
        UploadSessionFile file = UploadSessionFile.issued(
            1L,
            10L,
            "scan_001.png",
            "originals/10/1/file/scan_001.png",
            "image/png",
            1024L,
            "a".repeat(64)
        );
        ReflectionTestUtils.setField(file, "id", 100L);

        Image image = Image.fromUpload(uploadSession, file, new ImageMetadata(1240, 1754, 300, 300));

        assertThat(image.getProjectId()).isEqualTo(10L);
        assertThat(image.getUploadSessionId()).isEqualTo(1L);
        assertThat(image.getUploadSessionFileId()).isEqualTo(100L);
        assertThat(image.getUploaderId()).isEqualTo(20L);
        assertThat(image.getFormat()).isEqualTo(ImageFormat.PNG);
        assertThat(image.getStatus()).isEqualTo(ImageStatus.UPLOADED);
        assertThat(image.getWidth()).isEqualTo(1240);
        assertThat(image.getHeight()).isEqualTo(1754);
        assertThat(image.getDpiX()).isEqualTo(300);
        assertThat(image.getDpiY()).isEqualTo(300);
    }

    @Test
    void marksImageAsDeleted() {
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

        image.delete();

        assertThat(image.isDeleted()).isTrue();
        assertThat(image.getDeletedAt()).isNotNull();
    }
}
