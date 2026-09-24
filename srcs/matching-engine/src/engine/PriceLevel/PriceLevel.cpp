#include "PriceLevel.hpp"
#include <iterator>

PriceLevel::PriceLevel(uint64_t price) : price_(price), head_(nullptr), tail_(nullptr) {}

uint64_t PriceLevel::getPrice() const
{
    return price_;
}

void PriceLevel::addOrder(Order *order)
{
    if (order == nullptr || order == head_ || order->prev != nullptr || order->next != nullptr)
    {
        return;
    }

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
	if (order == nullptr || head_ == nullptr)
	{
		return;
	}

	if (order != head_ && order->prev == nullptr)
	{
		return;
	}

	if (order->prev && order->prev->next != order)
	{
		return;
	}

	if (order->next && order->next->prev != order)
	{
		return;
	}

	if (order->prev)
	{
		order->prev->next = order->next;
    }
    else
	{
		head_ = order->next;
	}

	if (order->next)
	{
		order->next->prev = order->prev;
	}
	else
	{
		tail_ = order->prev;
	}

	order->prev = nullptr;
	order->next = nullptr;
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
