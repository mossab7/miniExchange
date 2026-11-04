#ifndef TRADE_HPP
#define TRADE_HPP

#include <cstdint>
#include <string>

struct Execution
{
    uint64_t engineNamespace;
    uint64_t executionId;
    uint64_t buyOrderId;
    uint64_t sellOrderId;
    uint64_t timestamp;
    uint64_t price;
    uint64_t quantity;
};

std::string tradeIdForExecution(uint64_t executionId);

struct Trade
{
	std::string tradeId;
	uint64_t buyOrderId;
	uint64_t sellOrderId;
	uint64_t timestamp;
	uint64_t price;
	uint64_t quantity;

	Trade(uint64_t buyOrderId, uint64_t sellOrderId, uint64_t price, uint64_t quantity);
	explicit Trade(const Execution& execution);
};
#endif // TRADE_HPP
