#include <PriceLevel/PriceLevel.hpp>

PriceLevel::PriceLevel(double price) : price_(price) {}

double PriceLevel::getPrice() const
{
    return price_;
}

void PriceLevel::addOrder(Order* order)
{
    orders_.push_back(order);
}

void PriceLevel::removeOrder(OrderIterator orderIterator)
{
    orders_.erase(orderIterator);
}

std::list<Order*>& PriceLevel::getOrders()
{
    return orders_;
}

bool PriceLevel::isEmpty() const
{
    return orders_.empty();
}