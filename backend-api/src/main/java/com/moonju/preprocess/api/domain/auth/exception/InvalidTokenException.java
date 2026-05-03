package com.moonju.preprocess.api.domain.auth.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException(String message) {
        super(ErrorCode.COMMON_UNAUTHORIZED, message);
    }
}
