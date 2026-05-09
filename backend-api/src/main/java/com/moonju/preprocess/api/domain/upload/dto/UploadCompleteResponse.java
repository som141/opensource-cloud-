package com.moonju.preprocess.api.domain.upload.dto;

import com.moonju.preprocess.api.domain.upload.entity.UploadSession;
import com.moonju.preprocess.api.domain.upload.entity.UploadSessionStatus;

public record UploadCompleteResponse(
    Long sessionId,
    UploadSessionStatus status,
    int uploadedFileCount
) {

    public static UploadCompleteResponse of(UploadSession uploadSession, int uploadedFileCount) {
        return new UploadCompleteResponse(uploadSession.getId(), uploadSession.getStatus(), uploadedFileCount);
    }
}
