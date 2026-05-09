package com.moonju.preprocess.api.domain.image.dto;

import com.moonju.preprocess.api.domain.image.entity.Image;
import com.moonju.preprocess.api.domain.image.entity.ImageFormat;
import com.moonju.preprocess.api.domain.image.entity.ImageStatus;
import java.time.LocalDateTime;

public record ImageResponse(
    Long id,
    Long projectId,
    Long uploadSessionId,
    Long uploadSessionFileId,
    Long uploaderId,
    String originalFileName,
    String originalObjectKey,
    String contentType,
    long sizeBytes,
    String checksumSha256,
    ImageFormat format,
    ImageStatus status,
    Integer width,
    Integer height,
    Integer dpiX,
    Integer dpiY,
    LocalDateTime createdAt
) {

    public static ImageResponse from(Image image) {
        return new ImageResponse(
            image.getId(),
            image.getProjectId(),
            image.getUploadSessionId(),
            image.getUploadSessionFileId(),
            image.getUploaderId(),
            image.getOriginalFileName(),
            image.getOriginalObjectKey(),
            image.getContentType(),
            image.getSizeBytes(),
            image.getChecksumSha256(),
            image.getFormat(),
            image.getStatus(),
            image.getWidth(),
            image.getHeight(),
            image.getDpiX(),
            image.getDpiY(),
            image.getCreatedAt()
        );
    }
}
