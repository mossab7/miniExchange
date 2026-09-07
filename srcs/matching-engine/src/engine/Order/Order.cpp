#include "Order.hpp"


uint64_t Order::getId() const
{
	return id;
}

uint64_t Order::getPrice() const
{
	return price;
}

uint64_t Order::getQuantity() const
{
	return quantity;
}

Side Order::getSide() const
{
	return side;
}

void Order::fill(uint64_t const FillQuantity)
{
	quantity -= FillQuantity;
}

Order::Order(const SubmitOrderRequest &submitOrderRequest)
	: id(submitOrderRequest.id), price(submitOrderRequest.price), quantity(submitOrderRequest.quantity), side(submitOrderRequest.side), prev(nullptr), next(nullptr) {}

