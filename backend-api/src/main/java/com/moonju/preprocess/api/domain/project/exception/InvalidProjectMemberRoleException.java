package com.moonju.preprocess.api.domain.project.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class InvalidProjectMemberRoleException extends BusinessException {

    public InvalidProjectMemberRoleException(String message) {
        super(ErrorCode.COMMON_BAD_REQUEST, message);
    }
}
