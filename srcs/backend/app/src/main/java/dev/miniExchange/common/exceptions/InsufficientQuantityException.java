package dev.miniExchange.common.exceptions;

public class InsufficientQuantityException extends BaseExchangeException {
    public InsufficientQuantityException(String message) {
        super(message, "INSUFFICIENT_QUANTITY", org.springframework.http.HttpStatus.BAD_REQUEST);
    }
}
