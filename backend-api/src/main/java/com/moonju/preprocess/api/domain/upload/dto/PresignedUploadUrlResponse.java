package com.moonju.preprocess.api.domain.upload.dto;

import com.moonju.preprocess.api.domain.upload.entity.UploadSessionFile;
import com.moonju.preprocess.api.infra.storage.PresignedUploadTarget;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PresignedUploadUrlResponse(
    Long sessionId,
    List<UploadTargetResponse> uploadTargets
) {

    public static PresignedUploadUrlResponse of(Long sessionId, List<UploadTargetResponse> uploadTargets) {
        return new PresignedUploadUrlResponse(sessionId, uploadTargets);
    }

    public record UploadTargetResponse(
        Long uploadFileId,
        String objectKey,
        String uploadUrl,
        Instant expiresAt,
        Map<String, String> requiredHeaders
    ) {

        public static UploadTargetResponse of(UploadSessionFile file, PresignedUploadTarget target) {
            return new UploadTargetResponse(
                file.getId(),
                target.objectKey(),
                target.uploadUrl(),
                target.expiresAt(),
                target.requiredHeaders()
            );
        }
    }
}
