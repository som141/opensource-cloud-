package com.moonju.preprocess.api.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    COMMON_OK(HttpStatus.OK, "common200", "Request succeeded."),
    COMMON_CREATED(HttpStatus.CREATED, "common201", "Resource created."),
    COMMON_NO_CONTENT(HttpStatus.NO_CONTENT, "common204", "Request succeeded."),

    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "common400", "Invalid request."),
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "common401", "Authentication is required."),
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "common403", "Access is denied."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "common404", "Resource not found."),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "common409", "Request conflicts with current resource state."),
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "common500", "Internal server error."),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION400", "Request value is invalid."),

    WORKER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "WORKER401", "Worker token is invalid."),
    WORKER_REPORT_CONFLICT(
        HttpStatus.CONFLICT,
        "WORKER409",
        "Worker report is not allowed for current item state."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
