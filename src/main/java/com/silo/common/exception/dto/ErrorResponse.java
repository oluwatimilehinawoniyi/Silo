package com.silo.common.exception.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ValidationError> errors
) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<ValidationError> errors) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, errors);
    }
}
