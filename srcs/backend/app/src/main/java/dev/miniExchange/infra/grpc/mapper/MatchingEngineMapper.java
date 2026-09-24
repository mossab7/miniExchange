package dev.miniExchange.infra.grpc.mapper;


import dev.miniExchange.asset.entity.Asset;
import dev.miniExchange.order.entity.Order;
import dev.miniExchange.order.entity.Side;

import dev.miniExchange.infra.grpc.generated.SubmitOrderRequest;
import dev.miniExchange.infra.grpc.generated.CancelOrderRequest;
import dev.miniExchange.infra.grpc.generated.AddInstrumentRequest;

import java.math.BigInteger;

public class MatchingEngineMapper {
    public static SubmitOrderRequest toSubmitOrderRequest(Order order) {
        return SubmitOrderRequest.newBuilder()
                .setInstrumentId(toUnsignedLong(order.getAsset().getId(), "instrument id"))
                .setOrderId(toUnsignedLong(order.getId(), "order id"))
                .setPrice(toUnsignedLong(order.getPrice(), "price"))
                .setQuantity(toUnsignedLong(order.getQuantity(), "quantity"))
                .setSide(order.getSide() == Side.BUY ? dev.miniExchange.infra.grpc.generated.Side.BUY : dev.miniExchange.infra.grpc.generated.Side.SELL)
                .build();
    }

    public static CancelOrderRequest toCancelOrderRequest(Order order) {
        return CancelOrderRequest.newBuilder()
                .setOrderId(toUnsignedLong(order.getId(), "order id"))
                .build();
    }

    public static AddInstrumentRequest toAddInstrumentRequest(Asset asset) {
        return AddInstrumentRequest.newBuilder()
                .setInstrumentId(toUnsignedLong(asset.getId(), "instrument id")).build();

    }

    private static long toUnsignedLong(Number value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value instanceof BigInteger bigInteger) {
            if (bigInteger.signum() <= 0 || bigInteger.bitLength() > 63) {
                throw new IllegalArgumentException(name + " must fit in a positive uint64 value");
            }
            return bigInteger.longValueExact();
        }
        long result = value.longValue();
        if (result <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return result;
    }

}
