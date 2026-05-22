package com.moonju.preprocess.api.domain.image.model;

public record ImageMetadata(
    Integer width,
    Integer height,
    Integer dpiX,
    Integer dpiY
) {

    public static ImageMetadata empty() {
        return new ImageMetadata(null, null, null, null);
    }
}
