package dev.miniExchange.portfolio.Mapper;

import dev.miniExchange.portfolio.dto.PortfolioResponse;
import dev.miniExchange.portfolio.entity.Portfolio;
import org.springframework.stereotype.Component;

@Component
public class PortfolioMapper {
    public PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(
            portfolio.getUserUuid(),
            portfolio.getUuid(),
            portfolio.getBalance()
        );
    }
}
