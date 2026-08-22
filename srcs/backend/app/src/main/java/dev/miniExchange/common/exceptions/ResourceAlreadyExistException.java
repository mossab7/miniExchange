package dev.miniExchange.common.exceptions;

import org.springframework.http.HttpStatus;


public class ResourceAlreadyExistException extends  BaseExchangeException {
    public ResourceAlreadyExistException(String message) {
        super(message, "RESOURCE_ALREADY_EXISTS", HttpStatus.CONFLICT);
    }
    
}
