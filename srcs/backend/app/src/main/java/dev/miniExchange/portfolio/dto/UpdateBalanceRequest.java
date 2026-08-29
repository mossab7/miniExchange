package dev.miniExchange.portfolio.dto;


public record UpdateBalanceRequest(
    String amount,
    String type,
    String currency
) {
    
}
