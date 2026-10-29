package com.bookmyhive.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Uniform error body returned by every failing endpoint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path,
                            Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }
}
