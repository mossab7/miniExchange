#include "Trade.hpp"

#include "../matchingEngine/matchingEngine.hpp"

Trade::Trade(uint64_t buyOrderId, uint64_t sellOrderId, uint64_t price, uint64_t quantity)
	:  buyOrderId(buyOrderId), sellOrderId(sellOrderId), price(price), quantity(quantity)
{
	static uint64_t nextTradeId = 1;
	tradeId = g_engineId << 48 | ++nextTradeId;
	timestamp = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
}
