package com.moonju.preprocess.api.domain.upload.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UploadSessionTests {

    @Test
    void createsOpenUploadSession() {
        UploadSession uploadSession = UploadSession.create(1L, 2L, 3, 4096L);

        assertThat(uploadSession.getProjectId()).isEqualTo(1L);
        assertThat(uploadSession.getUserId()).isEqualTo(2L);
        assertThat(uploadSession.getExpectedFileCount()).isEqualTo(3);
        assertThat(uploadSession.getExpectedTotalSizeBytes()).isEqualTo(4096L);
        assertThat(uploadSession.getStatus()).isEqualTo(UploadSessionStatus.CREATED);
        assertThat(uploadSession.isOpen()).isTrue();
    }

    @Test
    void marksUploadUrlIssued() {
        UploadSession uploadSession = UploadSession.create(1L, 2L, 3, 4096L);

        uploadSession.markUploadUrlIssued();

        assertThat(uploadSession.getStatus()).isEqualTo(UploadSessionStatus.UPLOAD_URL_ISSUED);
        assertThat(uploadSession.isOpen()).isTrue();
    }

    @Test
    void completesUploadSession() {
        UploadSession uploadSession = UploadSession.create(1L, 2L, 3, 4096L);

        uploadSession.complete();

        assertThat(uploadSession.getStatus()).isEqualTo(UploadSessionStatus.COMPLETED);
        assertThat(uploadSession.getCompletedAt()).isNotNull();
        assertThat(uploadSession.isOpen()).isFalse();
    }

    @Test
    void cancelsUploadSession() {
        UploadSession uploadSession = UploadSession.create(1L, 2L, 3, 4096L);

        uploadSession.cancel();

        assertThat(uploadSession.getStatus()).isEqualTo(UploadSessionStatus.CANCELLED);
        assertThat(uploadSession.getCancelledAt()).isNotNull();
        assertThat(uploadSession.isOpen()).isFalse();
    }
}
