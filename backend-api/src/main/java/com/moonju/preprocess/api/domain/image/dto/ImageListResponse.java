package com.moonju.preprocess.api.domain.image.dto;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageFormat;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import java.time.LocalDateTime;

public record ImageListResponse(
    Long id,
    Long projectId,
    String originalFileName,
    String contentType,
    long sizeBytes,
    ImageFormat format,
    ImageStatus status,
    LocalDateTime createdAt
) {

    public static ImageListResponse from(Image image) {
        return new ImageListResponse(
            image.getId(),
            image.getProjectId(),
            image.getOriginalFileName(),
            image.getContentType(),
            image.getSizeBytes(),
            image.getFormat(),
            image.getStatus(),
            image.getCreatedAt()
        );
    }
}
