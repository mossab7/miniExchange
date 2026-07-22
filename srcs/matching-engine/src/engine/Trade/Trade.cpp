#include <Trade.hpp>

uint64_t Trade::getTradeId() const
{
	return tradeId;
}

uint64_t Trade::getBuyOrderId() const
{
	return buyOrderId;
}

uint64_t Trade::getSellOrderId() const
{
	return sellOrderId;
}

double Trade::getPrice() const
{
	return price;
}

uint32_t Trade::getQuantity() const
{
	return quantity;
}

uint64_t Trade::getTimestamp() const
{
	return timestamp;
}

