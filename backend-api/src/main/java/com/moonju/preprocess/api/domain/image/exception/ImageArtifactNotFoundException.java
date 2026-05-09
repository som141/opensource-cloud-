package com.moonju.preprocess.api.domain.image.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class ImageArtifactNotFoundException extends BusinessException {

    public ImageArtifactNotFoundException(String message) {
        super(ErrorCode.COMMON_NOT_FOUND, message);
    }
}
