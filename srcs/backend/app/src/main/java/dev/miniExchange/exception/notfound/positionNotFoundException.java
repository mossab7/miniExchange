package dev.miniExchange.exception.notfound;

import dev.miniExchange.exception.base.ApiException;
import org.springframework.http.HttpStatus;

public class positionNotFoundException extends ApiException {
    public positionNotFoundException(Long assetId, Long portfolioId) {
        super("Position not found for assetId: " + assetId + " and portfolioId: " + portfolioId,
                HttpStatus.NOT_FOUND);
    }
}
