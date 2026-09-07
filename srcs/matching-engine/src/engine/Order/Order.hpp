#ifndef ORDER_HPP
#define ORDER_HPP

#include <iostream>
#include <stdexcept>
#include <cstdint>
#include <list>
#include "../matchingEngine/submitOrderRequest.hpp"

class Order
{
private:
	uint64_t id;
	uint64_t price;
	uint64_t quantity;
	Side side;
	
	public:

	Order* prev;
	Order* next;

	explicit Order(const SubmitOrderRequest &submitOrderRequest);
	~Order() = default;
	uint64_t getId() const;
	
	uint64_t getPrice() const;

	uint64_t getQuantity() const;

	Side getSide() const;

	void fill(uint64_t fillQuantity);

};

#endif // ORDER_HPP
