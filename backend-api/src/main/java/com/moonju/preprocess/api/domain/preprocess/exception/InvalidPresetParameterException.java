package com.moonju.preprocess.api.domain.preprocess.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class InvalidPresetParameterException extends BusinessException {

    public InvalidPresetParameterException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
