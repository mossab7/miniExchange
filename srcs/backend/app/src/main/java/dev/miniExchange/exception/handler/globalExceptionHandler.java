package dev.miniExchange.exception.handler;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.miniExchange.exception.base.ApiException;
import dev.miniExchange.exception.dto.ErrorResponse;




@RestControllerAdvice
public class globalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(globalExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {

        logger.error("API Exception: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse errorResponse = buildErrorResponse(ex, request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }
    private ErrorResponse buildErrorResponse(ApiException ex, String path) {
        return new ErrorResponse(
            Instant.now(),
            ex.getStatus().value(),
            ex.getStatus().getReasonPhrase(),
            ex.getMessage(),
            path
        );
    }
}