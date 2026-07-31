package dev.miniExchange.exception.base;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    HttpStatus getStatus() {
        return status;
    }
}