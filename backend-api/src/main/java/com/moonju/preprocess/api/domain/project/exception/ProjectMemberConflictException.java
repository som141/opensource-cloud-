package com.moonju.preprocess.api.domain.project.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class ProjectMemberConflictException extends BusinessException {

    public ProjectMemberConflictException() {
        super(ErrorCode.COMMON_CONFLICT, "Project member already exists.");
    }
}
