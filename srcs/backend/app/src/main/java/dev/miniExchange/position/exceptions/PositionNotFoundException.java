package dev.miniExchange.position.exceptions;

import dev.miniExchange.common.exception.ResourceNotFoundException;

public class PositionNotFoundException extends ResourceNotFoundException {
    public PositionNotFoundException(String symbol) {
        super("position ", symbol);
    }
    public PositionNotFoundException(String message, Object Identifier) {
        super(message, Identifier);
    }
}
