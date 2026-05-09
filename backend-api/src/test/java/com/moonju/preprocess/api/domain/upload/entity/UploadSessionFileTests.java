package com.moonju.preprocess.api.domain.upload.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UploadSessionFileTests {

    @Test
    void createsIssuedUploadSessionFile() {
        UploadSessionFile file = UploadSessionFile.issued(
            10L,
            20L,
            "scan_001.png",
            "originals/20/10/file/scan_001.png",
            "image/png",
            1024L,
            "a".repeat(64)
        );

        assertThat(file.getUploadSessionId()).isEqualTo(10L);
        assertThat(file.getProjectId()).isEqualTo(20L);
        assertThat(file.getOriginalFileName()).isEqualTo("scan_001.png");
        assertThat(file.getObjectKey()).isEqualTo("originals/20/10/file/scan_001.png");
        assertThat(file.getStatus()).isEqualTo(UploadFileStatus.UPLOAD_URL_ISSUED);
    }

    @Test
    void marksUploaded() {
        UploadSessionFile file = UploadSessionFile.issued(
            10L,
            20L,
            "scan_001.png",
            "originals/20/10/file/scan_001.png",
            "image/png",
            1024L,
            "a".repeat(64)
        );

        file.markUploaded();

        assertThat(file.getStatus()).isEqualTo(UploadFileStatus.UPLOADED);
        assertThat(file.isUploaded()).isTrue();
    }
}
