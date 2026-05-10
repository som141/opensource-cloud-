package com.moonju.preprocess.api.domain.job.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class InvalidWorkerReportException extends BusinessException {

    public InvalidWorkerReportException(String message) {
        super(ErrorCode.WORKER_REPORT_CONFLICT, message);
    }
}
