package dev.miniExchange.user.exceptions;

import dev.miniExchange.common.exceptions.ResourceAlreadyExistException;

public class UserAlreadyExistException extends ResourceAlreadyExistException {
    public UserAlreadyExistException(String message) {
        super("User " + message + " already exists");
    }
}
