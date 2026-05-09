package com.moonju.preprocess.api.domain.preprocess.exception;

import com.moonju.preprocess.api.global.error.BusinessException;
import com.moonju.preprocess.api.global.error.ErrorCode;

public class PresetNotFoundException extends BusinessException {

    public PresetNotFoundException() {
        super(ErrorCode.COMMON_NOT_FOUND, "Preprocess preset not found.");
    }
}
