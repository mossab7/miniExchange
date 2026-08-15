package dev.miniExchange.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when input validation fails.
 */
public class ValidationException extends BaseExchangeException {
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message, Object context) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST, context);
    }
}
