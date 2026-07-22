#ifndef SUBMIT_ORDER_RESPONSE_HPP
#define SUBMIT_ORDER_RESPONSE_HPP

#include <string>

struct SubmitOrderResponse
{
    bool success;
    std::string orderId;
    std::string message;
};

#endif // SUBMIT_ORDER_RESPONSE_HPP