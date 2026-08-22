package dev.miniExchange.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends BaseExchangeException {
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(
            String.format("%s not found: %s", resourceType, identifier),
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            identifier
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
