package com.moonju.preprocess.api.domain.image.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class UnsupportedImageDownloadTypeException extends BusinessException {

    public UnsupportedImageDownloadTypeException() {
        super(ErrorCode.VALIDATION_ERROR, "Unsupported image download type.");
    }
}
