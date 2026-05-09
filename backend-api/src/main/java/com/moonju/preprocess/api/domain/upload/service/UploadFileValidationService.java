package com.moonju.preprocess.api.domain.upload.service;

import com.moonju.preprocess.api.domain.upload.dto.PresignedUploadFileRequest;
import com.moonju.preprocess.api.domain.upload.exception.InvalidUploadFileException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UploadFileValidationService {

    private static final long MAX_IMAGE_SIZE_BYTES = 100L * 1024L * 1024L;
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES_BY_EXTENSION = Map.of(
        ".png", Set.of("image/png"),
        ".jpg", Set.of("image/jpeg"),
        ".jpeg", Set.of("image/jpeg"),
        ".tif", Set.of("image/tiff"),
        ".tiff", Set.of("image/tiff"),
        ".bmp", Set.of("image/bmp", "image/x-ms-bmp"),
        ".webp", Set.of("image/webp")
    );

    public void validate(PresignedUploadFileRequest file) {
        String extension = extractExtension(file.fileName());
        if (!ALLOWED_CONTENT_TYPES_BY_EXTENSION.containsKey(extension)) {
            throw new InvalidUploadFileException("Unsupported image file extension.");
        }
        if (!ALLOWED_CONTENT_TYPES_BY_EXTENSION.get(extension).contains(file.contentType().toLowerCase(Locale.ROOT))) {
            throw new InvalidUploadFileException("Content type does not match the image file extension.");
        }
        if (file.sizeBytes() > MAX_IMAGE_SIZE_BYTES) {
            throw new InvalidUploadFileException("Image file size exceeds the per-file limit.");
        }
    }

    private String extractExtension(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0 || lastDot == normalized.length() - 1) {
            throw new InvalidUploadFileException("Image file extension is required.");
        }
        return normalized.substring(lastDot);
    }
}
