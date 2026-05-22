package com.moonju.preprocess.api.domain.upload.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import com.moonju.preprocess.api.domain.upload.exception.InvalidUploadFileException;
import org.junit.jupiter.api.Test;

class UploadedImageMagicNumberValidatorTests {

    private final UploadedImageMagicNumberValidator validator = new UploadedImageMagicNumberValidator();

    @Test
    void acceptsSupportedImageSignatures() {
        validator.validate(file("scan.png", "image/png"), pngBytes());
        validator.validate(file("scan.jpg", "image/jpeg"), jpegBytes());
        validator.validate(file("scan.jpeg", "image/jpeg"), jpegBytes());
        validator.validate(file("scan.webp", "image/webp"), webpBytes());
        validator.validate(file("scan.bmp", "image/x-ms-bmp"), bmpBytes());
        validator.validate(file("scan.tif", "image/tiff"), tiffLittleEndianBytes());
        validator.validate(file("scan.tiff", "image/tiff"), tiffBigEndianBytes());
    }

    @Test
    void rejectsSignatureThatDoesNotMatchExtension() {
        assertThatThrownBy(() -> validator.validate(file("scan.png", "image/png"), jpegBytes()))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("Uploaded image signature does not match the file extension.");
    }

    @Test
    void rejectsSignatureThatDoesNotMatchContentType() {
        assertThatThrownBy(() -> validator.validate(file("scan.jpg", "image/png"), jpegBytes()))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("Uploaded image signature does not match the content type.");
    }

    @Test
    void rejectsUnsupportedOrCorruptedSignature() {
        assertThatThrownBy(() -> validator.validate(file("scan.png", "image/png"), "not-image".getBytes()))
            .isInstanceOf(InvalidUploadFileException.class)
            .hasMessage("Unsupported or corrupted image signature.");
    }

    private UploadSessionFile file(String name, String contentType) {
        return UploadSessionFile.issued(
            1L,
            10L,
            name,
            "originals/10/1/file/" + name,
            contentType,
            1024L,
            "a".repeat(64)
        );
    }

    private byte[] pngBytes() {
        return new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
    }

    private byte[] jpegBytes() {
        return new byte[] {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0
        };
    }

    private byte[] webpBytes() {
        return new byte[] {
            0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50
        };
    }

    private byte[] bmpBytes() {
        return new byte[] {
            0x42, 0x4D, 0x1A, 0x00
        };
    }

    private byte[] tiffLittleEndianBytes() {
        return new byte[] {
            0x49, 0x49, 0x2A, 0x00
        };
    }

    private byte[] tiffBigEndianBytes() {
        return new byte[] {
            0x4D, 0x4D, 0x00, 0x2A
        };
    }
}
