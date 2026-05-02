package com.moonju.preprocess.api.global.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception
    ) {
        List<ErrorResponse.FieldErrorResponse> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldErrorResponse)
            .toList();

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
            .body(ErrorResponse.validation(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
        ConstraintViolationException exception
    ) {
        List<ErrorResponse.FieldErrorResponse> errors = exception.getConstraintViolations()
            .stream()
            .map(violation -> new ErrorResponse.FieldErrorResponse(
                violation.getPropertyPath().toString(),
                String.valueOf(violation.getInvalidValue()),
                violation.getMessage()
            ))
            .toList();

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
            .body(ErrorResponse.validation(errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        ErrorCode errorCode = ErrorCode.COMMON_INTERNAL_SERVER_ERROR;
        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(ErrorResponse.of(errorCode));
    }

    private ErrorResponse.FieldErrorResponse toFieldErrorResponse(FieldError fieldError) {
        return new ErrorResponse.FieldErrorResponse(
            fieldError.getField(),
            String.valueOf(fieldError.getRejectedValue()),
            fieldError.getDefaultMessage()
        );
    }
}
