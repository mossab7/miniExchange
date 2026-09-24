#ifndef TRADE_HPP
#define TRADE_HPP

#include <cstdint>
#include <string>

struct Trade
{
	std::string tradeId;
	uint64_t buyOrderId;
	uint64_t sellOrderId;
	uint64_t timestamp;
	uint64_t price;
	uint64_t quantity;

	Trade(uint64_t buyOrderId, uint64_t sellOrderId, uint64_t price, uint64_t quantity);
};
#endif // TRADE_HPP
