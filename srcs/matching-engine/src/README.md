# MiniExchange matching engine

The engine separates the transport layer from the matching core. gRPC maps
protobuf messages to `SubmitOrderRequest` values; the core routes each request
by instrument to exactly one worker; and that worker is the only thread that
mutates the instrument's `OrderBook`.

Workers use bounded mutex/condition-variable queues. A full queue rejects the
request with a deterministic error; commands are never overwritten or silently
dropped. The public engine API waits for the owning worker to process the
command, so gRPC responses describe completed matching operations.

## Native core build

The core and its tests do not require protobuf or gRPC:

```sh
cmake -S . -B build/matching-engine-core -DBUILD_GRPC=OFF
cmake --build build/matching-engine-core --parallel
ctest --test-dir build/matching-engine-core --output-on-failure
```

The gRPC build generates C++ sources from `../proto/miniExchange.proto` in the
build directory. It requires compatible protobuf headers/runtime/compiler and
the gRPC C++ plugin:

```sh
cmake -S . -B build/matching-engine -DBUILD_GRPC=ON
cmake --build build/matching-engine --parallel
```

The server listens on `0.0.0.0:50051` by default. Its second argument selects
the worker count, for example:

```sh
./build/matching-engine/matching-engine/matching_engine_server 0.0.0.0:50051 3
```

When librdkafka is available, set `MATCHING_ENGINE_KAFKA_BOOTSTRAP_SERVERS`
and optionally `TRADE_EVENTS_TOPIC` to publish protobuf `TradeEvent` messages.
The native build uses a no-op publisher only as a local fallback when the
librdkafka development package is absent; the Alpine Docker image includes it.

The order book retains only the trades produced by its most recent command as a
temporary matching result. It is not a backend trade store. The application
layer forwards that result to the Kafka publisher after the worker completes.
