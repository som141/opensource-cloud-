package com.moonju.preprocess.api.domain.auth.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class OAuth2LoginFailedException extends BusinessException {

    public OAuth2LoginFailedException(String message) {
        super(ErrorCode.COMMON_UNAUTHORIZED, message);
    }
}
