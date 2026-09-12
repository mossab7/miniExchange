#include "PriceLevel.hpp"
#include <iterator>

PriceLevel::PriceLevel(uint64_t price) : price_(price), head_(nullptr), tail_(nullptr) {}

uint64_t PriceLevel::getPrice() const
{
    return price_;
}

void PriceLevel::addOrder(Order *order)
{
    if (head_ == nullptr)
    {
        head_ = order;
        tail_ = order;
        order->next = nullptr;
        order->prev = nullptr;
    }
    else
    {
        tail_->next = order;
        order->prev = tail_;
        order->next = nullptr;
        tail_ = order;
    }
}

void PriceLevel::removeOrder(Order *order)
{
    if (order->prev)
    {
        order->prev->next = order->next;
    }
    else
    {
        head_ = order->next;
    }
}

const Order* PriceLevel::getOrders() const
{
    return head_;
}

bool PriceLevel::isEmpty() const
{
    return head_ == nullptr;
}

Order* PriceLevel::getFirstOrder() const
{
    return head_;
}