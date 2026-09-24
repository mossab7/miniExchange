#include "OrderBook.hpp"

#include <algorithm>
#include <utility>

OrderBook::OrderBook() = default;

OrderBook::~OrderBook()
{
    clear();
}

void OrderBook::rejectOrder(uint64_t id, const std::string& reason)
{
    lastRejectedOrderId_ = id;
    lastRejectionReason_ = reason;
}

void OrderBook::clear()
{
    for (auto& [price, priceLevel] : bids)
    {
        (void)price;
        while (!priceLevel->isEmpty())
        {
            Order* order = priceLevel->getFirstOrder();
            priceLevel->removeOrder(order);
            OrderPool_.release(order);
        }
        priceLevelPool.release(priceLevel);
    }
    bids.clear();

    for (auto& [price, priceLevel] : asks)
    {
        (void)price;
        while (!priceLevel->isEmpty())
        {
            Order* order = priceLevel->getFirstOrder();
            priceLevel->removeOrder(order);
            OrderPool_.release(order);
        }
        priceLevelPool.release(priceLevel);
    }
    asks.clear();
    orderMap.clear();
}

void OrderBook::recordTrade(const Order* incomingOrder, const Order* restingOrder,
                            uint64_t quantity)
{
    const bool incomingIsBuyer = incomingOrder->getSide() == Side::BUYER;
    const uint64_t buyOrderId = incomingIsBuyer ? incomingOrder->getId() : restingOrder->getId();
    const uint64_t sellOrderId = incomingIsBuyer ? restingOrder->getId() : incomingOrder->getId();
    lastTrades_.emplace_back(buyOrderId, sellOrderId, restingOrder->getPrice(), quantity);
}

PriceLevel* OrderBook::getPriceLevel(Order* order)
{
    if (order == nullptr) [[unlikely]]
    {
        return nullptr;
    }

    uint64_t price = order->getPrice();
    if (order->getSide() == Side::BUYER)
    {
        auto it = bids.find(price);
        return (it != bids.end()) ? it->second : nullptr;
    }
    else if (order->getSide() == Side::SELLER)
    {
        auto it = asks.find(price);
        return (it != asks.end()) ? it->second : nullptr;
    }
    return nullptr;
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

bool OrderBook::submitOrder(const SubmitOrderRequest &request)
{
    lastFilledOrderIds_.clear();
    lastTrades_.clear();
    const bool validSide = request.side == Side::BUYER || request.side == Side::SELLER;
    if (request.id == 0 || request.price == 0 || request.quantity == 0 || !validSide || containsOrder(request.id))
    {
        rejectOrder(request.id, "invalid or duplicate order");
        return false;
    }

    Order* order = OrderPool_.acquire(request);
    if (order->getSide() == Side::BUYER)
    {
        matchAgainstAsks(order);
    }
    else
    {
        matchAgainstBids(order);
    }

    if (order->getQuantity() > 0) [[likely]] 
    {
        addToBook(order);
    }
    else 
    {
        OrderPool_.release(order);
    }
    return true;
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

        recordTrade(incomingOrder, restingOrder, tradeQuantity);
        incomingOrder->fill(tradeQuantity);
        restingOrder->fill(tradeQuantity);

        if (restingOrder->getQuantity() == 0) [[likely]]
        {
            lastFilledOrderIds_.push_back(restingOrder->getId());
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

        recordTrade(incomingOrder, restingOrder, tradeQuantity);
        incomingOrder->fill(tradeQuantity);
        restingOrder->fill(tradeQuantity);

        if (restingOrder->getQuantity() == 0) [[likely]]
        {
            lastFilledOrderIds_.push_back(restingOrder->getId());
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
    if (order == nullptr)
    {
        return;
    }

    auto it = orderMap.find(order->getId());
    if (it == orderMap.end()) [[unlikely]] return; 

    PriceLevel* priceLevel = it->second.first;
    Order* storedOrder = it->second.second;
    priceLevel->removeOrder(it->second.second);
    
    if (priceLevel->isEmpty()) [[unlikely]]
    {
        if (storedOrder->getSide() == Side::BUYER)
            bids.erase(priceLevel->getPrice());
        else
            asks.erase(priceLevel->getPrice());
        
        priceLevelPool.release(priceLevel);
    }
    
    OrderPool_.release(storedOrder);
    orderMap.erase(it);
}

bool OrderBook::cancelOrder(uint64_t orderId)
{
    auto it = orderMap.find(orderId);
    if (it == orderMap.end()) [[unlikely]]
    {
        return false;
    }
    removeOrder(it->second.second);
    return true;
}

bool OrderBook::containsOrder(uint64_t orderId) const
{
    return orderMap.find(orderId) != orderMap.end();
}

const std::vector<Trade>& OrderBook::getTrades() const
{
    return lastTrades_;
}

const std::vector<uint64_t>& OrderBook::getLastFilledOrderIds() const
{
    return lastFilledOrderIds_;
}

uint64_t OrderBook::getLastRejectedOrderId() const
{
    return lastRejectedOrderId_;
}

const std::string& OrderBook::getLastRejectionReason() const
{
    return lastRejectionReason_;
}
