package com.moonju.preprocess.api.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    COMMON_OK(HttpStatus.OK, "common200", "요청에 성공했습니다."),
    COMMON_CREATED(HttpStatus.CREATED, "common201", "생성에 성공했습니다."),
    COMMON_NO_CONTENT(HttpStatus.NO_CONTENT, "common204", "요청에 성공했습니다."),

    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "common400", "잘못된 요청입니다."),
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "common401", "인증이 필요합니다."),
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "common403", "접근 권한이 없습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "common404", "요청한 리소스를 찾을 수 없습니다."),
    COMMON_CONFLICT(HttpStatus.CONFLICT, "common409", "요청 상태가 현재 리소스 상태와 충돌합니다."),
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "common500", "서버 내부 오류가 발생했습니다."),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION400", "요청 값이 올바르지 않습니다.");

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
