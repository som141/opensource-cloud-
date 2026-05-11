package com.moonju.preprocess.api.domain.job.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class JobItemArtifactNotFoundException extends BusinessException {

    public JobItemArtifactNotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND, "Job item artifact is not available.");
    }
}
