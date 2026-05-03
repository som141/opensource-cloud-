package com.moonju.preprocess.api.domain.project.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class ProjectMemberNotFoundException extends BusinessException {

    public ProjectMemberNotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND, "Project member not found.");
    }
}
