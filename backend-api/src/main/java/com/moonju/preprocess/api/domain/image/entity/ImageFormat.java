package com.moonju.preprocess.api.domain.image.entity;

import java.util.Locale;

public enum ImageFormat {
    PNG,
    JPG,
    JPEG,
    TIF,
    TIFF,
    BMP,
    WEBP;

    public static ImageFormat fromFileName(String fileName) {
        String normalized = fileName.toLowerCase(Locale.ROOT);
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0 || lastDot == normalized.length() - 1) {
            throw new IllegalArgumentException("Image file extension is required.");
        }
        return switch (normalized.substring(lastDot + 1)) {
            case "png" -> PNG;
            case "jpg" -> JPG;
            case "jpeg" -> JPEG;
            case "tif" -> TIF;
            case "tiff" -> TIFF;
            case "bmp" -> BMP;
            case "webp" -> WEBP;
            default -> throw new IllegalArgumentException("Unsupported image file extension.");
        };
    }
}
