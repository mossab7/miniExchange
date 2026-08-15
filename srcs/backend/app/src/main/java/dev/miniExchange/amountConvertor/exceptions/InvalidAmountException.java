package dev.miniExchange.amountConvertor.exceptions;

import dev.miniExchange.common.exception.BaseExchangeException;
import org.springframework.http.HttpStatus;

public class InvalidAmountException extends BaseExchangeException {
    public InvalidAmountException(String message) {
        super(message, "INVALID_AMOUNT", HttpStatus.BAD_REQUEST);
    }
}
