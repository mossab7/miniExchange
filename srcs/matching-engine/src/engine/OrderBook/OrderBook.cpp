#include "OrderBook.hpp"
#include <stdexcept>

OrderBook::OrderBook() {}

OrderBook::~OrderBook() = default;

void OrderBook::rejectOrder(uint64_t id, const std::string& reason)
{
    // TODO: Implement order rejection logic, e.g., logging or notifying the user  
}

PriceLevel* OrderBook::getPriceLevel(Order* order)
{

    uint64_t price = order->getPrice();
    if (order->getSide() == Side::BUYER)
    {
        auto it = bids.find(price);
        return (it != bids.end()) ? it->second : nullptr;
    }
    else
    {
        auto it = asks.find(price);
        return (it != asks.end()) ? it->second : nullptr;
    }
    return nullptr;
}

PriceLevel *OrderBook::getBestBid()
{   
    return bids.empty() ? nullptr : bids.begin()->second;
}

PriceLevel *OrderBook::getBestAsk()
{
    return asks.empty() ? nullptr : asks.begin()->second;
}


void OrderBook::addOrder(const SubmitOrderRequest &request)
{
    Order* order = OrderPool_.acquire(request);
    PriceLevel* priceLevel = nullptr;
    if (request.side == Side::BUYER)
    {
        auto [it, inserted] = bids.try_emplace(request.price, nullptr);
        if (inserted)
        {
            it->second = priceLevelPool.acquire(request.price);
        }
        priceLevel = it->second;
    }
    else
    {
        auto [it, inserted] = asks.try_emplace(request.price, nullptr);
        if (inserted)
        {
            it->second = priceLevelPool.acquire(request.price);
        }
        priceLevel = it->second;
    }
    priceLevel->addOrder(order);
    orderMap.emplace(request.id, OrderIterator(priceLevel, order));
}

void OrderBook::removeOrder(Order* order)
{
    
    auto it = orderMap.find(order->getId());
    if (it == orderMap.end()) return; 

    OrderIterator orderIterator = it->second;
    PriceLevel* priceLevel = it->second.first;

    priceLevel->removeOrder(it->second.second);
    
    if (priceLevel->isEmpty())
    {
        if (order->getSide() == Side::BUYER)
            bids.erase(priceLevel->getPrice());
        else
            asks.erase(priceLevel->getPrice());
        priceLevelPool.release(priceLevel);
    }
    orderMap.erase(it);
    OrderPool_.release(order);
}
