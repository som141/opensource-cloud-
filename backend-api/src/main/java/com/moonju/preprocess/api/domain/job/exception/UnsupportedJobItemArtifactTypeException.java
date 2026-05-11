package com.moonju.preprocess.api.domain.job.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class UnsupportedJobItemArtifactTypeException extends BusinessException {

    public UnsupportedJobItemArtifactTypeException() {
        super(ErrorCode.COMMON_BAD_REQUEST, "Supported job item artifact types are processed, preview, and report.");
    }
}
