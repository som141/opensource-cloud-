package com.moonju.preprocess.api.global.error;

import java.util.List;

public record ErrorResponse(
    boolean isSuccess,
    String code,
    String message,
    Object result,
    List<FieldErrorResponse> errors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(false, errorCode.getCode(), message, null, List.of());
    }

    public static ErrorResponse validation(List<FieldErrorResponse> errors) {
        return new ErrorResponse(
            false,
            ErrorCode.VALIDATION_ERROR.getCode(),
            ErrorCode.VALIDATION_ERROR.getMessage(),
            null,
            errors
        );
    }

    public record FieldErrorResponse(
        String field,
        String rejectedValue,
        String reason
    ) {
    }
}
