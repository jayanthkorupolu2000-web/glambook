package com.salon.exception;

/**
 * Thrown when a customer attempts to submit a second review for the same appointment.
 * Mapped to HTTP 409 Conflict by GlobalExceptionHandler.
 */
public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(String message) {
        super(message);
    }
}
