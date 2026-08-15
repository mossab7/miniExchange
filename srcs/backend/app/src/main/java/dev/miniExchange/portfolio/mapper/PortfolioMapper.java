package dev.miniExchange.portfolio.mapper;


import dev.miniExchange.portfolio.entity.Portfolio;
import dev.miniExchange.portfolio.dto.PortfolioResponse;

public class PortfolioMapper {
    public static PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.getUuid(),
                portfolio.getUserUuid(),
                portfolio.getBalance(),
                portfolio.getCreatedAt(),
                portfolio.getUpdatedAt()
        );
    }
}
