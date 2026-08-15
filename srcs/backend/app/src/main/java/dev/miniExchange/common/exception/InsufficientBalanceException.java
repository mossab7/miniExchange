package dev.miniExchange.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an operation fails due to insufficient balance.
 */
public class InsufficientBalanceException extends BaseExchangeException {
    public InsufficientBalanceException(String message) {
        super(message, "INSUFFICIENT_BALANCE", HttpStatus.BAD_REQUEST);
    }

    public InsufficientBalanceException(String message, Object context) {
        super(message, "INSUFFICIENT_BALANCE", HttpStatus.BAD_REQUEST, context);
    }
}
