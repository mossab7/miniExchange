package dev.miniExchange.asset.exceptions;

import dev.miniExchange.common.exceptions.ResourceAlreadyExistException;

public class AssetAlreadyExistsException extends ResourceAlreadyExistException {
    public AssetAlreadyExistsException(String message) {
        super("asset " + message + " already exists");
    }
}
