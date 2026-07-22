#include "OrderBook.hpp"

OrderBook::OrderBook() {}

OrderBook::~OrderBook()
{
    for (auto &pair : bids)
    {
        delete pair.second;
    }
    for (auto& pair : asks)
    {
        delete pair.second;
    }

}

PriceLevel* OrderBook::getPriceLevel(Order* order)
{

    double price = order->getPrice();
    if (order->getSide() == Side::BUYER)
    {
        auto it = bids.find(price);
        if (it != bids.end())
        {
            return it->second;
        }
    }
    else
    {
        auto it = asks.find(price);
        if (it != asks.end())
        {
            return it->second;
        }
    }
    return nullptr;
}


PriceLevel *OrderBook::getBestBid()
{
    if (bids.empty())
    {
        return nullptr;
    }
    return bids.rbegin()->second;
}

PriceLevel *OrderBook::getBestAsk()
{
    if (asks.empty())
    {
        return nullptr;
    }
    return asks.begin()->second;
}

void OrderBook::addBid(Order* order)
{
    double price = order->getPrice();
    auto it = bids.find(price);
    if (it == bids.end())
    {
        PriceLevel* newLevel = new PriceLevel(price);
        newLevel->addOrder(order);
        bids[price] = newLevel;
    }
    else
    {
        it->second->addOrder(order);
    }
}

void OrderBook::addAsk(Order* order)
{
    double price = order->getPrice();
    auto it = asks.find(price);
    if (it == asks.end())
    {
        PriceLevel* newLevel = new PriceLevel(price);
        newLevel->addOrder(order);
        asks[price] = newLevel;
    }
    else
    {
        it->second->addOrder(order);
    }
}

void OrderBook::addOrder(Order* order)
{
    if (order->getSide() == Side::BUYER)
    {
        addBid(order);
    }
    else
    {
        addAsk(order);
    }
}

void OrderBook::removeBid(Order* order)
{
    double price = order->getPrice();
    auto it = bids.find(price);
    if (it != bids.end())
    {
        PriceLevel* level = it->second;
        level->removeOrder(order->getPosition());
        if (level->isEmpty())
        {
            delete level;
            bids.erase(it);
        }
    }
}

void OrderBook::removeAsk(Order* order)
{
    double price = order->getPrice();
    auto it = asks.find(price);
    if (it != asks.end())
    {
        PriceLevel* level = it->second;
        level->removeOrder(order->getPosition());
        if (level->isEmpty())
        {
            delete level;
            asks.erase(it);
        }
    }
}

void OrderBook::removeOrder(Order* order)
{
    if (order->getSide() == Side::BUYER)
    {
        removeBid(order);
    }
    else
    {
        removeAsk(order);
    }
}
