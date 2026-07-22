#ifndef GET_ORDER_RESPONSE_HPP
#define GET_ORDER_RESPONSE_HPP

#include <string>

struct GetOrderResponse
{
    std::string orderId;
    std::string symbol;
};

#endif // GET_ORDER_RESPONSE_HPP