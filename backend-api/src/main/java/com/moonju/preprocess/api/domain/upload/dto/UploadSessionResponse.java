package com.moonju.preprocess.api.domain.upload.dto;

import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionStatus;
import java.time.LocalDateTime;

public record UploadSessionResponse(
    Long id,
    Long projectId,
    Long userId,
    UploadSessionStatus status,
    int expectedFileCount,
    long expectedTotalSizeBytes,
    LocalDateTime completedAt,
    LocalDateTime cancelledAt
) {

    public static UploadSessionResponse from(UploadSession uploadSession) {
        return new UploadSessionResponse(
            uploadSession.getId(),
            uploadSession.getProjectId(),
            uploadSession.getUserId(),
            uploadSession.getStatus(),
            uploadSession.getExpectedFileCount(),
            uploadSession.getExpectedTotalSizeBytes(),
            uploadSession.getCompletedAt(),
            uploadSession.getCancelledAt()
        );
    }
}
