package com.moonju.preprocess.api.domain.upload.service;

import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import com.moonju.preprocess.api.domain.upload.exception.InvalidUploadFileException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UploadedImageMagicNumberValidator {

    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public void validate(UploadSessionFile file, byte[] bytes) {
        DetectedImageFormat detected = detect(bytes);
        String extension = extensionOf(file.getOriginalFileName());
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);

        if (!detected.extensions.contains(extension)) {
            throw new InvalidUploadFileException("Uploaded image signature does not match the file extension.");
        }
        if (!detected.contentTypes.contains(contentType)) {
            throw new InvalidUploadFileException("Uploaded image signature does not match the content type.");
        }
    }

    private DetectedImageFormat detect(byte[] bytes) {
        if (startsWith(bytes, PNG_SIGNATURE)) {
            return DetectedImageFormat.PNG;
        }
        if (bytes.length >= 3
            && unsigned(bytes[0]) == 0xFF
            && unsigned(bytes[1]) == 0xD8
            && unsigned(bytes[2]) == 0xFF) {
            return DetectedImageFormat.JPEG;
        }
        if (bytes.length >= 12
            && asciiEquals(bytes, 0, "RIFF")
            && asciiEquals(bytes, 8, "WEBP")) {
            return DetectedImageFormat.WEBP;
        }
        if (bytes.length >= 2 && bytes[0] == 0x42 && bytes[1] == 0x4D) {
            return DetectedImageFormat.BMP;
        }
        if (bytes.length >= 4
            && ((bytes[0] == 0x49 && bytes[1] == 0x49 && bytes[2] == 0x2A && bytes[3] == 0x00)
            || (bytes[0] == 0x4D && bytes[1] == 0x4D && bytes[2] == 0x00 && bytes[3] == 0x2A))) {
            return DetectedImageFormat.TIFF;
        }
        throw new InvalidUploadFileException("Unsupported or corrupted image signature.");
    }

    private String extensionOf(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0 || lastDot == normalized.length() - 1) {
            throw new InvalidUploadFileException("Image file extension is required.");
        }
        return normalized.substring(lastDot);
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean asciiEquals(byte[] bytes, int offset, String expected) {
        if (bytes.length < offset + expected.length()) {
            return false;
        }
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != (byte) expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private enum DetectedImageFormat {
        PNG(Set.of(".png"), Set.of("image/png")),
        JPEG(Set.of(".jpg", ".jpeg"), Set.of("image/jpeg")),
        WEBP(Set.of(".webp"), Set.of("image/webp")),
        BMP(Set.of(".bmp"), Set.of("image/bmp", "image/x-ms-bmp")),
        TIFF(Set.of(".tif", ".tiff"), Set.of("image/tiff"));

        private final Set<String> extensions;
        private final Set<String> contentTypes;

        DetectedImageFormat(Set<String> extensions, Set<String> contentTypes) {
            this.extensions = extensions;
            this.contentTypes = contentTypes;
        }
    }
}
