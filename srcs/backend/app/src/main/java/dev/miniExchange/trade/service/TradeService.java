package dev.miniExchange.trade.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.miniExchange.portfolio.service.PortfolioService;
import dev.miniExchange.trade.command.ProcessTradeCommand;
import dev.miniExchange.trade.entity.Trade;
import dev.miniExchange.trade.event.TradeEvent;
import dev.miniExchange.trade.mapper.ProcessTradeCommandMapper;
import dev.miniExchange.trade.mapper.TradeEventMapper;
import dev.miniExchange.trade.repository.TradeRepository;

@Service
public class TradeService {

    private final PortfolioService portfolioService;
    private final TradeRepository tradeRepository;
    private final TradeEventMapper tradeEventMapper;
    private final ProcessTradeCommandMapper processTradeCommandMapper;

    TradeService(PortfolioService portfolioService,
                 TradeRepository tradeRepository,
                 TradeEventMapper tradeEventMapper,
                 ProcessTradeCommandMapper processTradeCommandMapper)
    {
        this.portfolioService = portfolioService;
        this.tradeRepository = tradeRepository;
        this.tradeEventMapper = tradeEventMapper;
        this.processTradeCommandMapper = processTradeCommandMapper;
    }

    @Transactional
    public void executeTrade(TradeEvent tradeEvent)
    {
        ProcessTradeCommand command = tradeEventMapper.toCommand(tradeEvent);
        portfolioService.processTrade(command);
        Trade trade = processTradeCommandMapper.toEntity(command);
        trade.setUuid(tradeEvent.tradeUuid());
        tradeRepository.save(trade);
    }
}
