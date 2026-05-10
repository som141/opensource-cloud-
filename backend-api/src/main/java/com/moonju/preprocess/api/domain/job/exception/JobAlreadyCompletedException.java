package com.moonju.preprocess.api.domain.job.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class JobAlreadyCompletedException extends BusinessException {

    public JobAlreadyCompletedException() {
        super(ErrorCode.COMMON_CONFLICT, "Job is already completed.");
    }
}
