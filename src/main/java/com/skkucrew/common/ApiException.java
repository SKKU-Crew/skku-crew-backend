package com.skkucrew.common;

import org.springframework.http.HttpStatus;

import java.util.List;

/** 서비스에서 던지는 오류. GlobalExceptionHandler가 SRS 표 3-29 형식으로 바꿔 응답한다. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<ErrorResponse.FieldError> fieldErrors;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(HttpStatus status, String code, String message, List<ErrorResponse.FieldError> fieldErrors) {
        super(message);
        this.status = status;
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public List<ErrorResponse.FieldError> getFieldErrors() { return fieldErrors; }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    public static ApiException badRequest(String message, List<ErrorResponse.FieldError> fieldErrors) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, fieldErrors);
    }

    public static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + "을(를) 찾을 수 없습니다.");
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }
}
