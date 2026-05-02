package com.moonju.preprocess.api.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.moonju.preprocess.api.global.error.ErrorCode;
import org.junit.jupiter.api.Test;

class ApiResponseTests {

    @Test
    void createsSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.code()).isEqualTo("common200");
        assertThat(response.message()).isEqualTo(ErrorCode.COMMON_OK.getMessage());
        assertThat(response.result()).isEqualTo("ok");
    }

    @Test
    void createsFailResponse() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.COMMON_UNAUTHORIZED);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.code()).isEqualTo("common401");
        assertThat(response.result()).isNull();
    }
}
