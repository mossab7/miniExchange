package dev.miniExchange.portfolio.mapper;

import dev.miniExchange.portfolio.entity.Portfolio;
import org.springframework.stereotype.Component;
import dev.miniExchange.portfolio.dto.PortfolioResponse;
import dev.miniExchange.amountConvertor.AmountConverterService;

@Component
public class PortfolioMapper {
    private final int defaultDecimalPlaces = 2;
    private final AmountConverterService amountConverterService;

    public PortfolioMapper(AmountConverterService amountConverterService) {
        this.amountConverterService = amountConverterService;
    }

    public PortfolioResponse toResponse(Portfolio portfolio) {
        if (portfolio == null) {
            return null;
        }
        return new PortfolioResponse(
                portfolio.getUuid(),
                portfolio.getUserUuid(),
                portfolio.getName(),
                portfolio.getDescription(),
                amountConverterService.convertToBigDecimal(portfolio.getAvailableBalance(), defaultDecimalPlaces),
                amountConverterService.convertToBigDecimal(portfolio.getLockedBalance(), defaultDecimalPlaces),
                amountConverterService.convertToBigDecimal(portfolio.getTotalBalance(), defaultDecimalPlaces),
                portfolio.getCreatedAt(),
                portfolio.getUpdatedAt()
        );
    }
}
