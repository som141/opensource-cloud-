package com.moonju.preprocess.api.domain.upload.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class UploadSessionNotFoundException extends BusinessException {

    public UploadSessionNotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND, "Upload session not found.");
    }
}
