#ifndef CANCEL_ORDER_RESPONSE_HPP
#define CANCEL_ORDER_RESPONSE_HPP

#include <string>

struct CancelOrderResponse
{
    bool success;
    std::string message;
};

#endif // CANCEL_ORDER_RESPONSE_HPP