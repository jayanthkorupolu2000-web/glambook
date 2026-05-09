package com.salon.exception;

import com.salon.dto.response.ErrorResponse;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.StringLength;
import org.springframework.http.ResponseEntity;

/**
 * Property-based tests for GlobalExceptionHandler.
 *
 * Validates: Requirement 13.1
 */
public class GlobalExceptionHandlerPropertyTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * Property 20: Any ResourceNotFoundException produces a 404 response with non-empty error body.
     *
     * Validates: Requirement 13.1
     */
    @Property
    void resourceNotFoundAlwaysReturns404WithBody(
            @ForAll @StringLength(min = 1, max = 100) String message) {

        ResourceNotFoundException ex = new ResourceNotFoundException(message);
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        assert response.getStatusCode().value() == 404;
        assert response.getBody() != null;
        assert response.getBody().getMessage() != null && !response.getBody().getMessage().isEmpty();
        assert response.getBody().getError() != null && !response.getBody().getError().isEmpty();
    }
}
