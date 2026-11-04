# End-to-end integration report

## Result

The isolated Docker Compose validation completed a real `REST order -> C++ matching engine -> Kafka trade-events -> Spring Kafka consumer -> PostgreSQL` flow. A crossing BTC/USD pair produced one PostgreSQL trade with a generated UUID, preserved numeric execution identity, and both orders marked `FILLED`.

The Kafka consumer group was then stopped, reset to offset zero, and restarted. The event was replayed; PostgreSQL still contained exactly one trade with one `(engine_namespace, execution_id)` pair. This validates the implemented duplicate guard for replayed deliveries, not exactly-once delivery or crash durability.

## Architecture and changes

The existing path is `OrderBook -> MatchingWorker -> MatchingEngine -> KafkaTradePublisher -> trade-events -> KafkaTradeEventConsumer -> TradeService -> PostgreSQL`. No second publisher or queue was introduced. C++ now publishes `engineNamespace` and `executionId`; protobuf field 1 is reserved, and Spring generates the persistent trade UUID on first insertion. PostgreSQL migration V4 adds nullable identity columns and a composite unique constraint for compatibility with legacy rows.

The namespace is generated once per engine process and the sequence is monotonic within that process. It is constrained to the positive 63-bit range because Java `long` and PostgreSQL `BIGINT` are signed. This avoids the live failure mode where a high-bit `uint64_t` namespace was discarded as negative. It is still not a registry-backed global identity across restarts or instances.

Kafka-enabled CMake builds now fail closed when librdkafka is unavailable; the no-op publisher requires an explicit development option.

## Validation commands

```text
cd /home/blackstar/work/miniExchange
docker compose -p miniexchange-e2e -f srcs/docker-compose.yml up -d
docker exec miniExchange_kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --create --if-not-exists \
  --topic trade-events --partitions 1 --replication-factor 1
cd srcs/backend && ./gradlew :app:test --no-daemon
```

The live database result was one row with `engine_namespace=1632861359964305217`, `execution_id=1`, a non-null UUID, amount `100000000`, and price `5000000`; both orders were filled. Native C++ tests and the backend test suite also passed.

## Reliability findings

Spring uses manual acknowledgment and fixed retries. A transaction rollback prevents a failed portfolio/trade transaction from committing, while the identity lookup and database constraint make replay idempotent. The live stack briefly emitted Kafka coordinator/topic startup errors before converging; no event was lost in this run.

The matching engine has no durable outbox or replay journal. A publish failure after matching can therefore leave committed book state with an unpublished execution. Also, instruments are currently registered in engine memory: restarting only the engine caused `instrument is not configured` until the instrument was registered again. Broker outage, database outage, and crash-recovery fault injection were not performed.

## Next work

Add startup reconciliation of the engine's instrument catalog from the existing backend asset catalog, then define a durable configured engine namespace and sequence policy. Separately, add a durable outbox or replayable execution journal before making any exactly-once or crash-durability claim.
