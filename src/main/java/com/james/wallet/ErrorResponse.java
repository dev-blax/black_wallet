package com.james.wallet;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String code,
        String message,
        Instant timestamp,
        List<FieldViolation> errors
) {

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, Instant.now(), List.of());
    }

    public static ErrorResponse of(int status, String code, String message, List<FieldViolation> errors) {
        return new ErrorResponse(status, code, message, Instant.now(), errors);
    }

    public record FieldViolation(String field, String message) { }
}
