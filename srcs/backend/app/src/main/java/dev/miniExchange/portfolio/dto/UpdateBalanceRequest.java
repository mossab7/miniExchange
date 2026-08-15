package dev.miniExchange.portfolio.dto;

import java.util.UUID;

public record UpdateBalanceRequest(
    String amount,
    String type,
    String currency
) {
    
}
