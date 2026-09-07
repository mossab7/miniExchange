#ifndef MATCHING_ENGINE_HPP
#define MATCHING_ENGINE_HPP

#include <vector>
#include "../OrderBook/OrderBook.hpp"
#include "submitOrderRequest.hpp"

extern uint64_t g_engineId;

class MatchingEngine
{
    private:
    std::vector<OrderBook> orderBooks;
    
    public:
        MatchingEngine();
        void submit(const SubmitOrderRequest &submitOrderRequest);
        ~MatchingEngine();
};


inline void MatchingEngine::submit(const SubmitOrderRequest &submitOrderRequest)
{
    orderBooks[submitOrderRequest.instrumentId % orderBooks.size()].addOrder(submitOrderRequest);
}
#endif // MATCHING_ENGINE_HPP