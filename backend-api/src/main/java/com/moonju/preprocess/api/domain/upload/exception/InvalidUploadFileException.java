package com.moonju.preprocess.api.domain.upload.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class InvalidUploadFileException extends BusinessException {

    public InvalidUploadFileException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
