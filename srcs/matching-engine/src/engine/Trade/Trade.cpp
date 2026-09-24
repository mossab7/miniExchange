#include <chrono>
#include <array>
#include <iomanip>
#include <random>
#include <sstream>

#include "Trade.hpp"


Trade::Trade(uint64_t buyOrderId, uint64_t sellOrderId, uint64_t price, uint64_t quantity)
	: buyOrderId(buyOrderId), sellOrderId(sellOrderId), price(price), quantity(quantity)
{
	std::random_device random;
	std::array<unsigned char, 16> bytes{};
	for (auto& byte : bytes)
	{
		byte = static_cast<unsigned char>(random());
	}
	bytes[6] = static_cast<unsigned char>((bytes[6] & 0x0f) | 0x40);
	bytes[8] = static_cast<unsigned char>((bytes[8] & 0x3f) | 0x80);

	std::ostringstream id;
	id << std::hex << std::setfill('0');
	for (std::size_t index = 0; index < bytes.size(); ++index)
	{
		if (index == 4 || index == 6 || index == 8 || index == 10)
		{
			id << '-';
		}
		id << std::setw(2) << static_cast<unsigned int>(bytes[index]);
	}
	tradeId = id.str();
	timestamp = static_cast<uint64_t>(std::chrono::duration_cast<std::chrono::milliseconds>(
		std::chrono::system_clock::now().time_since_epoch()).count());
}
