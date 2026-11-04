#include <chrono>
#include <atomic>
#include <random>
#include <cstdio>

#include "Trade.hpp"


namespace
{
std::atomic<uint64_t> nextExecutionId{1};
uint64_t allocateExecutionId() noexcept { return nextExecutionId.fetch_add(1, std::memory_order_relaxed); }
uint64_t nowMillis() noexcept { return static_cast<uint64_t>(std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count()); }
uint64_t processNamespace() noexcept
{
	static const uint64_t value = [] {
		try
		{
			std::random_device random;
			return (static_cast<uint64_t>(random()) << 32) | random();
		}
		catch (...)
		{
			return static_cast<uint64_t>(std::chrono::steady_clock::now().time_since_epoch().count());
		}
	}();
	return value;
}
}

std::string tradeIdForExecution(uint64_t executionId)
{
	char buffer[37];
	const uint64_t processId = processNamespace();
	std::snprintf(buffer, sizeof(buffer), "%08llx-%04llx-4%03llx-8%03llx-%012llx",
	              static_cast<unsigned long long>(processId >> 32),
	              static_cast<unsigned long long>((processId >> 16) & 0xffff),
	              static_cast<unsigned long long>(processId & 0xfff),
	              static_cast<unsigned long long>((processId >> 4) & 0xfff),
	              static_cast<unsigned long long>(executionId & 0xffffffffffffULL));
	return buffer;
}

Trade::Trade(uint64_t buyOrderId, uint64_t sellOrderId, uint64_t price, uint64_t quantity)
	: Trade(Execution{0, allocateExecutionId(), buyOrderId, sellOrderId, nowMillis(), price, quantity}) {}

Trade::Trade(const Execution& execution)
	: tradeId(tradeIdForExecution(execution.executionId)), buyOrderId(execution.buyOrderId), sellOrderId(execution.sellOrderId), timestamp(execution.timestamp), price(execution.price), quantity(execution.quantity) {}
