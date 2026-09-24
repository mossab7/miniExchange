#include "TradePublisher.hpp"

#include <cstdlib>
#include <iostream>

namespace
{
class NullTradePublisher final : public TradePublisher
{
public:
    void publish(const Trade&, uint64_t) override
    {
        // This is intentionally a development fallback for hosts without the
        // librdkafka development package. Docker builds use KafkaPublisher.
    }
};
} // namespace

std::shared_ptr<TradePublisher> createTradePublisherFromEnvironment()
{
    if (const char* servers = std::getenv("MATCHING_ENGINE_KAFKA_BOOTSTRAP_SERVERS");
        servers != nullptr && *servers != '\0')
    {
        std::cerr << "Kafka support is not available in this build; trade events will not be published\n";
    }
    return std::make_shared<NullTradePublisher>();
}
