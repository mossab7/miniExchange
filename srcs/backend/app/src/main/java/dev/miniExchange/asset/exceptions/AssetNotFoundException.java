package dev.miniExchange.asset.exceptions;

import dev.miniExchange.common.exceptions.ResourceNotFoundException;

public class AssetNotFoundException extends ResourceNotFoundException {
    public AssetNotFoundException(String message) {
        super("asset " + message + " not found");
    }
}