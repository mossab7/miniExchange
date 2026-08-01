package dev.miniExchange.exception.conflict;

import dev.miniExchange.exception.base.ApiException;
import org.springframework.http.HttpStatus;

public class assetAlreadyExistsException extends ApiException {
    public assetAlreadyExistsException(String message) {
        super("asset " + message + " already exists", HttpStatus.CONFLICT);
    }
}