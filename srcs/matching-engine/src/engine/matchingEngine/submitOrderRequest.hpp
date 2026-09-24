#ifndef SUBMIT_ORDER_REQUEST_HPP
#define SUBMIT_ORDER_REQUEST_HPP

#include <cstdint>
#include "../Order/Side.hpp"

struct SubmitOrderRequest
{
    uint64_t id{0};
    uint64_t instrumentId{0};
    uint64_t price{0};
    uint64_t quantity{0};
    Side side{Side::BUYER};
};

#endif // SUBMIT_ORDER_REQUEST_HPP
