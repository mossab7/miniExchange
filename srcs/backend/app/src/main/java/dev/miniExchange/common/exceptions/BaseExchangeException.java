package dev.miniExchange.common.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all business exceptions in the miniExchange application.
 * All domain-specific exceptions should extend this class.
 */
public abstract class BaseExchangeException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;
    private final Object context;

    protected BaseExchangeException(String message, String code, HttpStatus httpStatus) {
        this(message, code, httpStatus, null);
    }

    protected BaseExchangeException(String message, String code, HttpStatus httpStatus, Object context) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.context = context;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Object getContext() {
        return context;
    }
}
