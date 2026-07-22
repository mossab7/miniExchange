#ifndef TRADE_HPP
#define TRADE_HPP

#include <cstdint>

class Trade
{
private:
	uint64_t tradeId;

	uint64_t buyOrderId;
	uint64_t sellOrderId;

	double price;

	uint32_t quantity;

	uint64_t timestamp;

public:
	uint64_t getTradeId() const;
	uint64_t getBuyOrderId() const;
	uint64_t getSellOrderId() const;
	double getPrice() const;
	uint32_t getQuantity() const;
	uint64_t getTimestamp() const;
};
#endif // TRADE_HPP
