#include <Order.hpp>


uint64_t Order::getId() const
{
	return id_;
}

double Order::getPrice() const
{
	return price_;
}

uint32_t Order::getRemainingQuantity() const
{
	return remainingQuantity_;
}

Side Order::getSide() const
{
	return side_;
}

OrderStatus Order::getStatus() const
{
	return status_;
}

void Order::fill(uint32_t quantity)
{
	if (quantity > remainingQuantity_)
	{
		throw std::invalid_argument("Fill quantity exceeds remaining quantity");
	}

	remainingQuantity_ -= quantity;

	if (remainingQuantity_ == 0)
	{
		status_ = OrderStatus::FILLED;
	}
	else
	{
		status_ = OrderStatus::PARTIALLY_FILLED;
	}
}

void Order::cancel()
{
	if (status_ == OrderStatus::FILLED)
	{
		throw std::logic_error("Cannot cancel a filled order");
	}

	status_ = OrderStatus::CANCELLED;
}

OrderIterator Order::getPosition() const
{
	return position;
}