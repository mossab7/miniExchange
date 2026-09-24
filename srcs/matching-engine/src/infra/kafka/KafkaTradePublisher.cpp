#include "KafkaTradePublisher.hpp"

#include <miniExchange.pb.h>
#include <librdkafka/rdkafkacpp.h>

#include <chrono>
#include <cstdlib>
#include <memory>
#include <stdexcept>
#include <string>

namespace
{
class NullTradePublisher final : public TradePublisher
{
public:
    void publish(const Trade&, uint64_t) override {}
};
} // namespace

class KafkaTradePublisher::Impl
{
public:
    Impl(const char* bootstrapServers, const char* topic)
        : topic_(topic)
    {
        std::string error;
        auto configuration = std::unique_ptr<RdKafka::Conf>(
            RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL));
        if (!configuration ||
            configuration->set("bootstrap.servers", bootstrapServers, error) != RdKafka::Conf::CONF_OK ||
            configuration->set("enable.idempotence", "true", error) != RdKafka::Conf::CONF_OK ||
            configuration->set("acks", "all", error) != RdKafka::Conf::CONF_OK)
        {
            throw std::runtime_error("failed to configure Kafka producer: " + error);
        }

        producer_.reset(RdKafka::Producer::create(configuration.release(), error));
        if (!producer_)
        {
            throw std::runtime_error("failed to create Kafka producer: " + error);
        }
    }

    ~Impl()
    {
        producer_->flush(5000);
    }

    void publish(const Trade& trade, uint64_t instrumentId)
    {
        miniExchange::TradeEvent event;
        event.set_tradeid(trade.tradeId);
        event.set_instrumentid(instrumentId);
        event.set_buyorderid(trade.buyOrderId);
        event.set_sellorderid(trade.sellOrderId);
        event.set_price(trade.price);
        event.set_quantity(trade.quantity);
        event.set_timestamp_epoch_millis(static_cast<int64_t>(trade.timestamp));

        std::string payload;
        if (!event.SerializeToString(&payload))
        {
            throw std::runtime_error("failed to serialize trade event");
        }

        const auto result = producer_->produce(
            topic_, RdKafka::Topic::PARTITION_UA, RdKafka::Producer::RK_MSG_COPY,
            payload.data(), payload.size(), nullptr, 0, 0, nullptr);
        producer_->poll(0);
        if (result != RdKafka::ERR_NO_ERROR)
        {
            throw std::runtime_error("failed to enqueue trade event in Kafka: " +
                                     RdKafka::err2str(result));
        }

        const auto deadline = std::chrono::steady_clock::now() + std::chrono::seconds(5);
        while (producer_->outq_len() > 0 && std::chrono::steady_clock::now() < deadline)
        {
            producer_->poll(100);
        }
        if (producer_->outq_len() > 0)
        {
            throw std::runtime_error("timed out waiting for Kafka trade event delivery");
        }
    }

private:
    std::string topic_;
    std::unique_ptr<RdKafka::Producer> producer_;
};

std::shared_ptr<TradePublisher> createTradePublisherFromEnvironment()
{
    const char* servers = std::getenv("MATCHING_ENGINE_KAFKA_BOOTSTRAP_SERVERS");
    const char* topic = std::getenv("TRADE_EVENTS_TOPIC");
    if (servers == nullptr || *servers == '\0')
    {
        return std::make_shared<NullTradePublisher>();
    }
    return std::make_shared<KafkaTradePublisher>(servers, topic == nullptr || *topic == '\0'
                                                           ? "trade-events"
                                                           : topic);
}

KafkaTradePublisher::KafkaTradePublisher(const char* bootstrapServers, const char* topic)
    : impl_(std::make_unique<Impl>(bootstrapServers, topic))
{
}

KafkaTradePublisher::~KafkaTradePublisher() = default;

void KafkaTradePublisher::publish(const Trade& trade, uint64_t instrumentId)
{
    impl_->publish(trade, instrumentId);
}
