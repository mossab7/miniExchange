#ifndef ORDER_BOOK_HPP
#define ORDER_BOOK_HPP

#include <cstddef>
#include <cstdint>
#include <map>
#include <string>
#include <unordered_map>
#include <vector>
#include "../PriceLevel/PriceLevel.hpp"
#include "../ObjectPool/ObjectPool.hpp"
#include "../Trade/Trade.hpp"
#include "../matchingEngine/submitOrderRequest.hpp"

inline constexpr std::size_t ORDERS_MAX_PER_ASSET = 1000;
inline constexpr std::size_t PRICE_LEVELS_MAX_PER_ASSET = 1000;

using OrderIterator = std::pair<PriceLevel*, Order*>;

class OrderBook
{
    private:
        std::map<uint64_t, PriceLevel*,std::greater<uint64_t>> bids;
        std::map<uint64_t, PriceLevel*> asks;
        ObjectPool<Order> OrderPool_{ORDERS_MAX_PER_ASSET};
        ObjectPool<PriceLevel> priceLevelPool{PRICE_LEVELS_MAX_PER_ASSET};
        std::unordered_map<uint64_t, OrderIterator> orderMap;
        // Trades are only the result of the most recent matching command.
        // They are handed to the application/infrastructure layer and are not
        // an in-memory trade store.
        std::vector<Trade> lastTrades_;
        std::vector<uint64_t> lastFilledOrderIds_;
        uint64_t lastRejectedOrderId_{0};
        std::string lastRejectionReason_;
        
        void rejectOrder(uint64_t id, const std::string& reason);
        void clear();
        void recordTrade(const Order* incomingOrder, const Order* restingOrder,
                         uint64_t quantity);
    
    public:
        OrderBook();
        ~OrderBook();
        OrderBook(const OrderBook&) = delete;
        OrderBook& operator=(const OrderBook&) = delete;
        OrderBook(OrderBook&&) = delete;
        OrderBook& operator=(OrderBook&&) = delete;

        PriceLevel* getPriceLevel(Order* order);
        void addToBook(Order *order);
        void matchAgainstAsks(Order* incomingOrder);
        void matchAgainstBids(Order* incomingOrder);
        bool submitOrder(const SubmitOrderRequest &request);
        void removeOrder(Order* order);
        bool cancelOrder(uint64_t orderId);
        PriceLevel *getBestBid();
        PriceLevel *getBestAsk();
        uint64_t getBestAskPrice();

        bool containsOrder(uint64_t orderId) const;
        const std::vector<Trade>& getTrades() const;
        const std::vector<uint64_t>& getLastFilledOrderIds() const;
        uint64_t getLastRejectedOrderId() const;
        const std::string& getLastRejectionReason() const;
};

#endif // ORDER_BOOK_HPP
