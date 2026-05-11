package com.moonju.preprocess.api.domain.job.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class JobItemNotFoundException extends BusinessException {

    public JobItemNotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND, "Job item not found.");
    }
}
