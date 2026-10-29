package com.bookmyhive.exception;

/** Thrown for business-rule violations in an otherwise well-formed request (HTTP 400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
