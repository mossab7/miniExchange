#ifndef TRADE_PUBLISHER_HPP
#define TRADE_PUBLISHER_HPP

#include <cstdint>
#include <memory>

#include "Trade.hpp"

class TradePublisher
{
public:
    virtual ~TradePublisher() = default;
    virtual void publish(const Trade& trade, uint64_t instrumentId) = 0;
};

std::shared_ptr<TradePublisher> createTradePublisherFromEnvironment();

#endif // TRADE_PUBLISHER_HPP
