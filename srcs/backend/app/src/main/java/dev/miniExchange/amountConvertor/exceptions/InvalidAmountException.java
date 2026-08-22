package dev.miniExchange.amountConvertor.exceptions;

import org.springframework.http.HttpStatus;

import dev.miniExchange.common.exceptions.BaseExchangeException;

public class InvalidAmountException extends BaseExchangeException {
    public InvalidAmountException(String message) {
        super(message, "INVALID_AMOUNT", HttpStatus.BAD_REQUEST);
    }
}
