#ifndef ORDER_HPP
#define ORDER_HPP

#include <iostream>
#include <stdexcept>
#include <cstdint>
#include <Side.hpp>
#include <OrderStatus.hpp>
#include <list>
#include <PriceLevel/PriceLevel.hpp>

typedef std::list<Order>::iterator OrderIterator;

class Order
{
private:
	uint64_t timestamp;
	uint64_t price;
	uint32_t Id;
	uint32_t instrumentId;
	//first bit is for side, second bit is for status, third bit is for reserved
	uint32_t info;
	uint32_t quantity;
	OrderIterator position;

public:
	uint32_t getId() const;
	
	uint64_t getPrice() const;

	uint32_t getRemainingQuantity() const;

	Side getSide() const;

	OrderIterator getPosition() const;

	OrderStatus getStatus() const;

	void fill(uint32_t quantity);

	void cancel();
};

#endif // ORDER_HPP
