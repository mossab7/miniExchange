package dev.miniExchange.exception.dto;

import java.time.Instant;

public record ErrorResponse(
    Instant time,
    int status,
    String error,
    String message,
    String path

) {
    
}
