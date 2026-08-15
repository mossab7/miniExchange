package dev.miniExchange.amountConvertor;

import dev.miniExchange.amountConvertor.exceptions.InvalidAmountException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.BigInteger;

@Service
public class AmountConverterService {
    public BigInteger toSmallestUnit(
            String input,
            int decimalPlaces
    ) {
        if (input == null || input.isBlank()) {
            throw new InvalidAmountException("Amount cannot be empty");
        }
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException(
                    "Decimal places cannot be negative"
            );
        }
        final BigDecimal amount;
        try {
            amount = new BigDecimal(input);
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Invalid amount: " + input);
        }
        if (amount.signum() < 0) {
            throw new InvalidAmountException(
                    "Amount cannot be negative"
            );
        }
        if (amount.scale() > decimalPlaces) {
            throw new InvalidAmountException(
                    "Too many decimal places"
            );
        }
        return amount
                .movePointRight(decimalPlaces)
                .toBigIntegerExact();
    }
}
