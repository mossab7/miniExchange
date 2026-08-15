package dev.miniExchange.asset.exceptions;

import dev.miniExchange.common.exception.ResourceAlreadyExistException;

public class AssetAlreadyExistsException extends ResourceAlreadyExistException {
    public AssetAlreadyExistsException(String message) {
        super("asset " + message + " already exists");
    }
}
