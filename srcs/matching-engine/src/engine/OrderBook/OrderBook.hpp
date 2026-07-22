#ifndef ORDER_BOOK_HPP
#define ORDER_BOOK_HPP

#include "../PriceLevel/PriceLevel.hpp"
#include <map>

typedef std::map<double, PriceLevel*> PriceLevelMap;

class OrderBook
{
    private:
        PriceLevelMap bids;
        PriceLevelMap asks;
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