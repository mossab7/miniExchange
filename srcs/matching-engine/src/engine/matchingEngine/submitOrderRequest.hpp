#ifndef SUBMIT_ORDER_REQUEST_HPP
#define SUBMIT_ORDER_REQUEST_HPP

#include <string>
#include <iostream>
#include <cstdint>
#include "../Order/Side.hpp"

typedef struct SubmitOrderRequest {
    uint64_t id;
    uint64_t instrumentId;
    uint64_t price;
    uint64_t quantity;
    Side side;
} SubmitOrderRequest;

#endif // SUBMIT_ORDER_REQUEST_HPP
