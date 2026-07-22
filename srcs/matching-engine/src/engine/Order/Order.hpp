#ifndef ORDER_HPP
#define ORDER_HPP

#include <iostream>
#include <stdexcept>
#include <cstdint>
#include <Side.hpp>
#include <OrderStatus.hpp>
#include <list>
#include <PriceLevel/PriceLevel.hpp>

typedef std::list<Order*>::iterator OrderIterator;

class Order
{
private:
	uint64_t id_;
	OrderIterator position;
	uint64_t userId_;

	Side side_;

	double price_;

	uint32_t quantity_;
	uint32_t remainingQuantity_;

	uint64_t timestamp_;

	OrderStatus status_;

public:
	uint64_t getId() const;

	double getPrice() const;

	uint32_t getRemainingQuantity() const;

	Side getSide() const;

	OrderIterator getPosition() const;

	OrderStatus getStatus() const;

	void fill(uint32_t quantity);

	void cancel();
};

#endif // ORDER_HPP
