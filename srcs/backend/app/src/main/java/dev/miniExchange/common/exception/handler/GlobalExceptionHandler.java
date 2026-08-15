package dev.miniExchange.common.exception.handler;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.miniExchange.common.exception.BaseExchangeException;
import dev.miniExchange.common.exception.ErrorResponse;




@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(BaseExchangeException.class)
    public ResponseEntity<ErrorResponse> handleBaseExchangeException(BaseExchangeException ex, HttpServletRequest request) {

        logger.error("API Exception: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse errorResponse = ErrorResponse.of(ex, request.getRequestURI());
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
}