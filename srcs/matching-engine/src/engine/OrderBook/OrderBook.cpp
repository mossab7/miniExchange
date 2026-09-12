#include "OrderBook.hpp"
#include <algorithm>

OrderBook::OrderBook() {}

OrderBook::~OrderBook() = default;

void OrderBook::rejectOrder(uint64_t id, const std::string& reason)
{
    // TODO: Implement order rejection logic
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
}

PriceLevel* OrderBook::getBestBid()
{   
    return bids.empty() ? nullptr : bids.begin()->second;
}

PriceLevel* OrderBook::getBestAsk()
{
    return asks.empty() ? nullptr : asks.begin()->second;
}

uint64_t OrderBook::getBestAskPrice()
{
    PriceLevel* bestAskPriceLevel = getBestAsk();
    return bestAskPriceLevel ? bestAskPriceLevel->getPrice() : 0;
}

void OrderBook::submitOrder(const SubmitOrderRequest &request)
{
    Order* order = OrderPool_.acquire(request);

    // OPTIMIZATION 1: Match FIRST before adding to the book.
    if (order->getSide() == Side::BUYER)
    {
        matchAgainstAsks(order);
    }
    else
    {
        matchAgainstBids(order);
    }

    // Only add to the tree and hashmap if there is remaining quantity.
    if (order->getQuantity() > 0) [[likely]] 
    {
        addToBook(order);
    }
    else 
    {
        OrderPool_.release(order);
    }
}

void OrderBook::addToBook(Order* order)
{
    PriceLevel* priceLevel = nullptr;
    uint64_t price = order->getPrice();

    if (order->getSide() == Side::BUYER)
    {
        auto [it, inserted] = bids.try_emplace(price, nullptr);
        if (inserted) [[unlikely]] 
        {
            it->second = priceLevelPool.acquire(price);
        }
        priceLevel = it->second;
    }
    else
    {
        auto [it, inserted] = asks.try_emplace(price, nullptr);
        if (inserted) [[unlikely]] 
        {
            it->second = priceLevelPool.acquire(price);
        }
        priceLevel = it->second;
    }

    priceLevel->addOrder(order);
    orderMap.emplace(order->getId(), OrderIterator(priceLevel, order));
}

void OrderBook::matchAgainstAsks(Order* incomingOrder)
{
    while (incomingOrder->getQuantity() > 0 && !asks.empty())
    {
        auto bestAskIt = asks.begin();
        PriceLevel* bestAskLevel = bestAskIt->second;

        if (incomingOrder->getPrice() < bestAskLevel->getPrice())
        {
            break;
        }

        Order* restingOrder = bestAskLevel->getFirstOrder();
        uint64_t tradeQuantity = std::min(incomingOrder->getQuantity(), restingOrder->getQuantity());

        incomingOrder->fill(tradeQuantity);
        restingOrder->fill(tradeQuantity);

        if (restingOrder->getQuantity() == 0) [[likely]]
        {
            bestAskLevel->removeOrder(restingOrder);
            orderMap.erase(restingOrder->getId());
            OrderPool_.release(restingOrder);

            if (bestAskLevel->isEmpty()) [[unlikely]]
            {
                asks.erase(bestAskIt);
                priceLevelPool.release(bestAskLevel);
            }
        }
    }
}

void OrderBook::matchAgainstBids(Order* incomingOrder)
{
    while (incomingOrder->getQuantity() > 0 && !bids.empty())
    {
        auto bestBidIt = bids.begin();
        PriceLevel* bestBidLevel = bestBidIt->second;

        if (incomingOrder->getPrice() > bestBidLevel->getPrice())
        {
            break;
        }

        Order* restingOrder = bestBidLevel->getFirstOrder();
        uint64_t tradeQuantity = std::min(incomingOrder->getQuantity(), restingOrder->getQuantity());

        incomingOrder->fill(tradeQuantity);
        restingOrder->fill(tradeQuantity);

        if (restingOrder->getQuantity() == 0) [[likely]]
        {
            bestBidLevel->removeOrder(restingOrder);
            orderMap.erase(restingOrder->getId());
            OrderPool_.release(restingOrder);

            if (bestBidLevel->isEmpty()) [[unlikely]]
            {
                bids.erase(bestBidIt);
                priceLevelPool.release(bestBidLevel);
            }
        }
    }
}

void OrderBook::removeOrder(Order* order)
{
    auto it = orderMap.find(order->getId());
    if (it == orderMap.end()) [[unlikely]] return; 

    PriceLevel* priceLevel = it->second.first;
    priceLevel->removeOrder(it->second.second);
    
    if (priceLevel->isEmpty()) [[unlikely]]
    {
        if (order->getSide() == Side::BUYER)
            bids.erase(priceLevel->getPrice());
        else
            asks.erase(priceLevel->getPrice());
        
        priceLevelPool.release(priceLevel);
    }
    
    OrderPool_.release(it->second.second);
    orderMap.erase(it);
}