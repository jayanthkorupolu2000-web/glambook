package com.salon.exception;

/**
 * Thrown when a Salon Owner with the given ID does not exist in the database.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public class SalonOwnerNotFoundException extends RuntimeException {
    public SalonOwnerNotFoundException(Long id) {
        super("Salon Owner not found with id: " + id);
    }
}
