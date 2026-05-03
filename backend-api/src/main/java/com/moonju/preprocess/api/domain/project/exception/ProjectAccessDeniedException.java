package com.moonju.preprocess.api.domain.project.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class ProjectAccessDeniedException extends BusinessException {

    public ProjectAccessDeniedException() {
        super(ErrorCode.COMMON_FORBIDDEN, "Project access denied.");
    }
}
