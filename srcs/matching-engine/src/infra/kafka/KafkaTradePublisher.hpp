#ifndef KAFKA_TRADE_PUBLISHER_HPP
#define KAFKA_TRADE_PUBLISHER_HPP

#include "engine/Trade/TradePublisher.hpp"

class KafkaTradePublisher final : public TradePublisher
{
public:
    KafkaTradePublisher(const char* bootstrapServers, const char* topic);
    ~KafkaTradePublisher() override;

    void publish(const Trade& trade, uint64_t instrumentId) override;

private:
    class Impl;
    std::unique_ptr<Impl> impl_;
};

#endif // KAFKA_TRADE_PUBLISHER_HPP
