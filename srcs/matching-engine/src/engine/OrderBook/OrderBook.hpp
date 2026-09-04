#ifndef ORDER_BOOK_HPP
#define ORDER_BOOK_HPP

#include "../PriceLevel/PriceLevel.hpp"
#include <map>



class OrderBook
{
    private:
        std::map<uint64_t, PriceLevel,std::greater<uint64_t>> bids;
        std::map<uint64_t, PriceLevel> asks;
        std::map<uint64_t, std::list<Order>::iterator> orderMap;
        void addBid(Order* order);
        void addAsk(Order* order);
        void removeBid(Order* order);
        void removeAsk(Order* order);
    public:
        OrderBook();
        ~OrderBook();
        PriceLevel* getPriceLevel(Order* order);
        void addOrder(Order* order);
        void removeOrder(Order* order);
        PriceLevel *getBestBid();
        PriceLevel *getBestAsk();
};



#endif // ORDER_BOOK_HPP