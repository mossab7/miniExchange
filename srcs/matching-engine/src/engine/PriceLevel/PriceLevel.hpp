#ifndef PRICELEVEL_HPP
#include <list>
#include <Order/Order.hpp>

typedef std::list<Order>::iterator  OrderIterator;

class PriceLevel
{
    private:
        uint64_t price_;
        std::list<Order> orders_;
    public:
        PriceLevel(uint64_t price);
        uint64_t getPrice() const;
        void addOrder(Order* order);
        void removeOrder(OrderIterator orderIterator);
        std::list<Order>& getOrders();
        bool isEmpty() const;
};

#define PRICELEVEL_HPP
#endif // PRICELEVEL_HPP