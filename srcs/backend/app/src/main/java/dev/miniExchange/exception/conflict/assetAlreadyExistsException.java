package dev.miniExchange.exception.conflict;

import dev.miniExchange.exception.base;
import org.springframework.http.HttpStatus;

public class assetAlreadyExistsException extends base {
    public assetAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, "asset " + message + " already exists");
    }
}