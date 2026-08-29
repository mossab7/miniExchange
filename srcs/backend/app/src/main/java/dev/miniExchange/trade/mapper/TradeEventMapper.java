package dev.miniExchange.trade.mapper;

import org.springframework.stereotype.Component;

import dev.miniExchange.order.service.OrderService;
import dev.miniExchange.trade.command.ProcessTradeCommand;
import dev.miniExchange.trade.event.TradeEvent;

@Component
public class TradeEventMapper {

    private final OrderService orderService;

    public TradeEventMapper(OrderService orderService) {
        this.orderService = orderService;
    }

    public ProcessTradeCommand toCommand(TradeEvent event) {
        Long sellerPortfolioId = orderService.getOrderPortfolioId(event.sellerOrderId());
        Long buyerPortfolioId  = orderService.getOrderPortfolioId(event.buyerOderId());

        return new ProcessTradeCommand(
            event.quantity(),
            event.price(),
            event.assetSymbol(),
            sellerPortfolioId,
            buyerPortfolioId,
            event.buyerOderId(),
            event.sellerOrderId()
        );
    }
}
