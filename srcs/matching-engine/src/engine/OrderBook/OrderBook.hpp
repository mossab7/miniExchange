#ifndef ORDER_BOOK_HPP
#define ORDER_BOOK_HPP

#include <map>
#include <unordered_map>
#include "../PriceLevel/PriceLevel.hpp"
#include "../ObjectPool/ObjectPool.hpp"
#include "../matchingEngine/submitOrderRequest.hpp"

#define ORDERS_MAX_PER_ASSET 1000
#define PRICE_LEVELS_MAX_PER_ASSET 1000

using OrderIterator = std::pair<PriceLevel*, Order*>;

class OrderBook
{
    private:
        std::map<uint64_t, PriceLevel*,std::greater<uint64_t>> bids;
        std::map<uint64_t, PriceLevel*> asks;
        ObjectPool<Order> OrderPool_{ORDERS_MAX_PER_ASSET};
        ObjectPool<PriceLevel> priceLevelPool{PRICE_LEVELS_MAX_PER_ASSET};
        std::unordered_map<uint64_t, OrderIterator> orderMap;
        
        void rejectOrder(uint64_t id, const std::string& reason);
    
    public:
        OrderBook();
        ~OrderBook();
        PriceLevel* getPriceLevel(Order* order);
        void addOrder(const SubmitOrderRequest &request);
        void removeOrder(Order* order);
        PriceLevel *getBestBid();
        PriceLevel *getBestAsk();
};



#endif // ORDER_BOOK_HPP