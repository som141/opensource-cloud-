package com.moonju.preprocess.api.domain.job.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class InvalidJobRequestException extends BusinessException {

    public InvalidJobRequestException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
