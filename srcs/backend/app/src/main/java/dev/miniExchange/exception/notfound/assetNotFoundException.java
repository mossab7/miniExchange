package dev.miniExchange.exception.notfound;

import dev.miniExchange.exception.base;
import org.springframework.http.HttpStatus;

public class assetNotFoundException extends base {
    public assetNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "asset " + message + " not found");
    }
}