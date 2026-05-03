package com.moonju.preprocess.api.domain.project.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class ProjectNotFoundException extends BusinessException {

    public ProjectNotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND, "Project not found.");
    }
}
