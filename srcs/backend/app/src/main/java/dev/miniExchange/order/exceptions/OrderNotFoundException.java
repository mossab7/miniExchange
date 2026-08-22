package dev.miniExchange.order.exceptions;

import dev.miniExchange.common.exceptions.ResourceNotFoundException;

public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(Object identifier) {
        super("Order", identifier);
    }
}
