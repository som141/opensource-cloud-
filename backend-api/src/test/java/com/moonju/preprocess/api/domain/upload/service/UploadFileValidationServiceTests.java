package com.moonju.preprocess.api.domain.upload.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadFileRequest;
import com.moonju.preprocess.api.domain.upload.exception.InvalidUploadFileException;
import org.junit.jupiter.api.Test;

class UploadFileValidationServiceTests {

    private final UploadFileValidationService service = new UploadFileValidationService();

    @Test
    void acceptsSupportedImageFile() {
        service.validate(new PresignedUploadFileRequest("scan_001.png", "image/png", 1024L, "a".repeat(64)));
    }

    @Test
    void rejectsUnsupportedExtension() {
        PresignedUploadFileRequest request = new PresignedUploadFileRequest(
            "scan_001.txt",
            "text/plain",
            1024L,
            "a".repeat(64)
        );

        assertThatThrownBy(() -> service.validate(request))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("Unsupported image file extension.");
    }

    @Test
    void rejectsMismatchedContentType() {
        PresignedUploadFileRequest request = new PresignedUploadFileRequest(
            "scan_001.png",
            "image/jpeg",
            1024L,
            "a".repeat(64)
        );

        assertThatThrownBy(() -> service.validate(request))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("Content type does not match the image file extension.");
    }

    @Test
    void rejectsOversizedImage() {
        PresignedUploadFileRequest request = new PresignedUploadFileRequest(
            "scan_001.png",
            "image/png",
            101L * 1024L * 1024L,
            "a".repeat(64)
        );

        assertThatThrownBy(() -> service.validate(request))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("Image file size exceeds the per-file limit.");
    }
}
