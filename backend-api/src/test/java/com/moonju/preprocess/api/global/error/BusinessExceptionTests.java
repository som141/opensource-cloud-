package com.moonju.preprocess.api.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BusinessExceptionTests {

    @Test
    void keepsErrorCodeAndDefaultMessage() {
        BusinessException exception = new BusinessException(ErrorCode.COMMON_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo(ErrorCode.COMMON_NOT_FOUND.getMessage());
    }

    @Test
    void supportsCustomMessage() {
        BusinessException exception = new BusinessException(ErrorCode.COMMON_CONFLICT, "custom conflict");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("custom conflict");
    }
}
