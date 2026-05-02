package com.moonju.preprocess.api.global.response;

import com.moonju.preprocess.api.global.error.ErrorCode;

public record ApiResponse<T>(
    boolean isSuccess,
    String code,
    String message,
    T result
) {

    public static <T> ApiResponse<T> success(T result) {
        return success(ErrorCode.COMMON_OK, result);
    }

    public static <T> ApiResponse<T> success(ErrorCode code, T result) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), result);
    }

    public static <T> ApiResponse<T> success(String code, String message, T result) {
        return new ApiResponse<>(true, code, message, result);
    }

    public static ApiResponse<Void> fail(ErrorCode code) {
        return fail(code.getCode(), code.getMessage());
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
