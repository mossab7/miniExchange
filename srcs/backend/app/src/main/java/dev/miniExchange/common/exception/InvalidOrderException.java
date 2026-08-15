package dev.miniExchange.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an order validation fails.
 */
public class InvalidOrderException extends BaseExchangeException {
    public InvalidOrderException(String message) {
        super(message, "INVALID_ORDER", HttpStatus.BAD_REQUEST);
    }

    public InvalidOrderException(String message, Object context) {
        super(message, "INVALID_ORDER", HttpStatus.BAD_REQUEST, context);
    }
}
