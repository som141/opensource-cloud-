package com.moonju.preprocess.api.domain.job.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class JobNotFoundException extends BusinessException {

    public JobNotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND, "Job not found.");
    }
}
