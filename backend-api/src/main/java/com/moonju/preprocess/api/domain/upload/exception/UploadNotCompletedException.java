package com.moonju.preprocess.api.domain.upload.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class UploadNotCompletedException extends BusinessException {

    public UploadNotCompletedException(String message) {
        super(ErrorCode.COMMON_CONFLICT, message);
    }
}
