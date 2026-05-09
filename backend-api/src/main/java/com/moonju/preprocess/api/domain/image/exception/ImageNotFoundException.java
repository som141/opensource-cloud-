package com.moonju.preprocess.api.domain.image.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class ImageNotFoundException extends BusinessException {

    public ImageNotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND, "Image not found.");
    }
}
