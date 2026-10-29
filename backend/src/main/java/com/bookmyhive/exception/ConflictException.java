package com.bookmyhive.exception;

/** Thrown when the request clashes with current state, e.g. seat already booked (HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
