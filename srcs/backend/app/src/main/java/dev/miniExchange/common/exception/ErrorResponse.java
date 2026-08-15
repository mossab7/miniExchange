package dev.miniExchange.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Structured error response returned to clients when an exception occurs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String code,
    String message,
    int status,
    Instant timestamp,
    String path,
    Object context
) {
    public static ErrorResponse of(BaseExchangeException ex, String path) {
        return new ErrorResponse(
            ex.getCode(),
            ex.getMessage(),
            ex.getHttpStatus().value(),
            Instant.now(),
            path,
            ex.getContext()
        );
    }

    public static ErrorResponse of(String message, int status, String path) {
        return new ErrorResponse(
            "INTERNAL_ERROR",
            message,
            status,
            Instant.now(),
            path,
            null
        );
    }
}
