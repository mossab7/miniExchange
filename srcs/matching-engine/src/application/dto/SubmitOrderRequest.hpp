#ifndef SUBMIT_ORDER_REQUEST_HPP
#define SUBMIT_ORDER_REQUEST_HPP

#include <string>

struct SubmitOrderRequest
{
    std::string orderId;
    std::string userId;
    std::string side; // "BUY" or "SELL"
    std::string symbol;
    double price;
    int quantity;
};

#endif // SUBMIT_ORDER_REQUEST_HPP