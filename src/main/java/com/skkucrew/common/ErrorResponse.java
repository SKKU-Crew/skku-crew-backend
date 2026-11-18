package com.skkucrew.common;

import java.time.OffsetDateTime;
import java.util.List;

/** SRS 표 3-29 오류 응답 형식 */
public record ErrorResponse(
        String code,
        String message,
        List<FieldError> fieldErrors,
        OffsetDateTime timestamp
) {
    public record FieldError(String field, String message) {}

    public static ErrorResponse of(String code, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, fieldErrors == null ? List.of() : fieldErrors, Times.now());
    }
}
