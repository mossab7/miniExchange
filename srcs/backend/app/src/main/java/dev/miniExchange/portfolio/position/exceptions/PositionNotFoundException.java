package dev.miniExchange.portfolio.position.exceptions;

import dev.miniExchange.common.exceptions.ResourceNotFoundException;

public class PositionNotFoundException extends ResourceNotFoundException {
    public PositionNotFoundException(String symbol) {
        super("position ", symbol);
    }
    public PositionNotFoundException(String message, Object Identifier) {
        super(message, Identifier);
    }
}
 