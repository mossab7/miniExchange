#ifndef PRICELEVEL_HPP
#define PRICELEVEL_HPP

#include <list>
#include "../Order/Order.hpp"


class PriceLevel
{
    private:
        uint64_t price_;
        Order *head_;
        Order *tail_;
    public:
        explicit PriceLevel(uint64_t price);
        uint64_t getPrice() const;
        void addOrder(Order *order);
        void removeOrder(Order *order);
        const Order* getOrders() const;
        bool isEmpty() const;
        Order* getFirstOrder() const;
};

#endif // PRICELEVEL_HPP