package dev.miniExchange.trade.mapper;

import org.springframework.stereotype.Component;

import dev.miniExchange.asset.service.AssetService;
import dev.miniExchange.order.service.OrderService;
import dev.miniExchange.portfolio.service.PortfolioService;
import dev.miniExchange.trade.command.ProcessTradeCommand;
import dev.miniExchange.trade.entity.Trade;

@Component
public class ProcessTradeCommandMapper {

    private final PortfolioService portfolioService;
    private final OrderService orderService;
    private final AssetService assetService;

    public ProcessTradeCommandMapper(PortfolioService portfolioService,
                                     OrderService orderService,
                                     AssetService assetService) {
        this.portfolioService = portfolioService;
        this.orderService = orderService;
        this.assetService = assetService;
    }

    public Trade toEntity(ProcessTradeCommand command) {
        Trade trade = new Trade();
        trade.setAmount(command.amount());
        trade.setPrice(command.price());
        trade.setBuyer(portfolioService.getReference(command.buyerPortfolioId()));
        trade.setSeller(portfolioService.getReference(command.sellerPortfolioId()));
        trade.setAsset(assetService.getReferenceBySymbol(command.assetSymbol()));
        trade.setBuyerOrder(orderService.getReference(command.buyerOrderId()));
        trade.setSellerOrder(orderService.getReference(command.sellerOrderId()));
        return trade;
    }
}
