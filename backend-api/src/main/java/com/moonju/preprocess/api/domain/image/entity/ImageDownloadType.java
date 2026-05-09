package com.moonju.preprocess.api.domain.image.entity;

import java.util.Locale;

public enum ImageDownloadType {
    ORIGINAL(ImageArtifactType.ORIGINAL),
    PROCESSED(ImageArtifactType.PROCESSED),
    PREVIEW(ImageArtifactType.PREVIEW);

    private final ImageArtifactType artifactType;

    ImageDownloadType(ImageArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public static ImageDownloadType from(String value) {
        try {
            return ImageDownloadType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Unsupported image download type.");
        }
    }

    public ImageArtifactType getArtifactType() {
        return artifactType;
    }
}
