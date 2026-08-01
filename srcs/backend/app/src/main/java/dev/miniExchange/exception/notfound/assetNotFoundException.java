package dev.miniExchange.exception.notfound;

import dev.miniExchange.exception.base.ApiException;
import org.springframework.http.HttpStatus;

public class assetNotFoundException extends ApiException {
    public assetNotFoundException(String message) {
        super("asset " + message + " not found",HttpStatus.NOT_FOUND);
    }
}