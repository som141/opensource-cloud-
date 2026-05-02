package com.moonju.preprocess.api.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorResponseTests {

    @Test
    void createsBusinessErrorResponse() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.COMMON_FORBIDDEN);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.code()).isEqualTo("common403");
        assertThat(response.message()).isEqualTo(ErrorCode.COMMON_FORBIDDEN.getMessage());
        assertThat(response.result()).isNull();
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void createsValidationErrorResponse() {
        ErrorResponse.FieldErrorResponse fieldError = new ErrorResponse.FieldErrorResponse(
            "name",
            "",
            "must not be blank"
        );

        ErrorResponse response = ErrorResponse.validation(List.of(fieldError));

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.code()).isEqualTo("VALIDATION400");
        assertThat(response.errors()).containsExactly(fieldError);
    }
}
