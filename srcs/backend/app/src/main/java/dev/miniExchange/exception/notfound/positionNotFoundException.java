package dev.miniExchange.exception.notfound;

import dev.miniExchange.exception.base.ApiException;
import org.springframework.http.HttpStatus;

public class positionNotFoundException extends ApiException {
    public positionNotFoundException(String symbol) {
        super("Position not found for symbol: " + symbol, HttpStatus.NOT_FOUND);
    }
}
