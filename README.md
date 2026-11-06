# MiniExchange

### A compact exchange stack with a real matching engine, durable trade events, and production-shaped boundaries.

MiniExchange is a small but complete trading platform built to make the hard parts visible:

- a C++20 matching engine with price/time order-book behavior;
- a Spring Boot API that owns users, portfolios, orders, and persistence;
- gRPC for low-latency command flow;
- Kafka + protobuf for durable, replayable trade settlement; and
- PostgreSQL for the system of record.

The result is intentionally focused: small enough to understand, structured enough to extend.

## Why this project stands out

MiniExchange is not just CRUD around an order table. It demonstrates the boundary between fast in-memory matching and durable financial state:

| Concern | Owner | Technology |
| --- | --- | --- |
| Authentication and API | Backend | Java 21, Spring Boot, JWT |
| Orders and portfolios | Backend | PostgreSQL, JPA, Flyway |
| Matching | Engine | C++20, order books, worker queues |
| Command transport | Backend ↔ Engine | gRPC + protobuf |
| Trade settlement | Engine → Backend | Kafka + protobuf `TradeEvent` |
| Delivery safety | Backend | At-least-once consumer with execution-identity idempotency |

The matching engine never needs to know how a portfolio is persisted. The backend never needs to implement price matching. Each service has one job and one explicit contract.

## Architecture

```text
                         commands                         events
┌───────────────┐   REST/JWT   ┌────────────────┐  gRPC  ┌────────────────────┐
│ Client / CLI  │ ────────────▶ │ Spring backend │ ─────▶ │ C++ matching engine │
└───────────────┘              │                │        │                    │
                               │ PostgreSQL     │        │ Order books        │
                               │ Kafka consumer  │◀────── │ Protobuf publisher │
                               └───────┬────────┘ Kafka  └────────────────────┘
                                       │
                                       ▼
                               ┌──────────────┐
                               │  PostgreSQL   │
                               │ orders/trades │
                               └──────────────┘
```

### Trade lifecycle

1. The backend validates and persists an order.
2. The backend sends the numeric order command to the engine over gRPC.
3. The engine matches against the instrument's order book.
4. Every fill becomes a protobuf `TradeEvent` and is published to Kafka.
5. The backend consumes the event and updates trades, balances, and order state.
6. If Kafka redelivers the event, the `(engineNamespace, executionId)` identity prevents duplicate settlement; Spring generates the persistent trade UUID on first insertion.

The API is therefore intentionally asynchronous after submission: an order can be returned as `OPEN` before the matching result has been consumed.

## Core features

- JWT signup and login.
- Asset and market management.
- Portfolio creation and balance updates.
- Buy and sell orders with cancellation.
- In-memory order books with pooled order and price-level allocation.
- Worker-based instrument routing in the matching engine.
- Shared protobuf contract for Java and C++.
- Kafka trade events with manual acknowledgement and retry handling.
- Idempotent trade persistence.
- Docker Compose environment with PostgreSQL, Kafka, backend, and engine.
- Native CMake and Gradle workflows for local development.

## Repository layout

```text
.
├── backend/                 Spring Boot API and Kafka consumer
├── matching-engine/         C++20 order book and gRPC server
├── proto/                   Shared protobuf contract
├── CMakeLists.txt           Cross-component developer targets
├── docker-compose.yml       Full local infrastructure
└── README.md
```

### Frontend

MiniExchange includes a React + TypeScript trading terminal in [`frontend/`](frontend/). It uses Vite, React Router, TanStack Query, Axios, React Hook Form, Zod, Lucide icons, and a dark responsive terminal layout.

The frontend provides:

- JWT login, registration, protected routes, logout, and session-expiration handling;
- a trading terminal with backend-backed asset selection and limit-order placement;
- order history and cancellation for cancelable orders;
- portfolio balance and account views;
- an admin workspace at `/admin` for creating assets and enabling or disabling assets when the JWT contains the `ADMIN` role;
- explicit unavailable states for backend capabilities that are not currently exposed, including order-book data, historical prices, positions, trade history, WebSocket/SSE feeds, and operational analytics.

The API base URL is configured with `VITE_API_BASE_URL` and defaults to `/api`. Vite proxies `/api` to `http://localhost:8080` during development. Frontend source, tests, and setup instructions are documented in [`frontend/README.md`](frontend/README.md).

Generated bindings are build outputs. The source of truth is [`proto/miniExchange.proto`](proto/miniExchange.proto).

## Quick start with Docker

Requirements:

- Docker Engine with Compose v2
- Git

Start the complete stack:

```bash
docker compose up --build
```

The services are available at:

| Service | Address |
| --- | --- |
| Backend API | `http://localhost:8080` |
| Matching engine gRPC | `localhost:50051` |
| PostgreSQL | `localhost:5433` |
| Kafka host listener | `localhost:29092` |

Stop the stack:

```bash
docker compose down
```

### One-command development

From the repository root, `make` starts PostgreSQL, Kafka, the matching engine, and the Spring Boot backend through Docker Compose, then starts the Vite frontend at `http://localhost:5173`:

```bash
make
```

Useful Make targets:

```bash
make up               # Start backend infrastructure in the background
make frontend         # Start the frontend only
make frontend-build   # Build the frontend for production
make frontend-test    # Run frontend tests
make logs             # Follow Docker Compose logs
make down             # Stop Docker services
```

To remove the local database volume as well, use `docker compose down -v`. This deletes local PostgreSQL data.

## Native development

### Build and test the engine

```bash
cmake -S . -B build -DBUILD_GRPC=ON -DBUILD_TESTING=ON
cmake --build build --parallel
ctest --test-dir build --output-on-failure
```

For dependency-free matching-engine correctness tests, sanitizer builds, and
the standalone in-process benchmark and reproducible profiling reports, see
[`srcs/matching-engine/TESTING.md`](srcs/matching-engine/TESTING.md).

### Generate both language bindings

```bash
cmake --build build --target generate-proto
```

CMake generates the C++ bindings. Gradle generates the Java bindings from the same root proto file.

### Run components locally

Start PostgreSQL and Kafka separately, then run the engine and backend in different terminals:

```bash
cmake --build build --target run-matching-engine
cmake --build build --target run-backend
```

Equivalent backend configuration can be supplied explicitly:

```bash
MATCHING_ENGINE_HOST=localhost \
MATCHING_ENGINE_PORT=50051 \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
./backend/gradlew :app:bootRun --no-daemon
```

Useful root targets:

```bash
cmake --build build --target generate-proto
cmake --build build --target run-matching-engine
cmake --build build --target run-backend
cmake --build build --target run-all
```

`run-all` delegates to Docker Compose.

## HTTP API

All protected endpoints use the JWT returned by signup or login:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"secret123"}'
```

Main endpoint groups:

| Endpoint | Purpose |
| --- | --- |
| `POST /api/auth/signup` | Create a user |
| `POST /api/auth/login` | Obtain a JWT |
| `/api/assets` | Create, list, inspect, and update assets |
| `/api/portfolios` | Create portfolios and update balances |
| `/api/orders` | Submit, list, inspect, and cancel orders |

Create an order with decimal strings so the backend can apply the asset's precision rules:

```json
{
  "portfolioId": "00000000-0000-0000-0000-000000000000",
  "market": "BTC/USD",
  "price": "50000",
  "quantity": "0.1",
  "side": "BUY"
}
```

## The shared event contract

`proto/miniExchange.proto` defines both the gRPC service and the Kafka payload. A completed match is represented as:

```protobuf
message TradeEvent {
  reserved 1;
  uint64 instrumentId = 2;
  uint64 buyOrderId = 3;
  uint64 sellOrderId = 4;
  uint64 price = 5;
  uint64 quantity = 6;
  int64 timestamp_epoch_millis = 7;
  uint64 engineNamespace = 8;
  uint64 executionId = 9;
}
```

Using protobuf keeps the C++ producer and Java consumer on one versioned binary contract without introducing JSON parsing or duplicated DTO definitions.

## Configuration

Local defaults point to `localhost`. Docker Compose overrides service-to-service addresses.

| Variable | Default | Description |
| --- | --- | --- |
| `MATCHING_ENGINE_HOST` | `localhost` | gRPC engine host |
| `MATCHING_ENGINE_PORT` | `50051` | gRPC engine port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Backend Kafka address |
| `MATCHING_ENGINE_KAFKA_BOOTSTRAP_SERVERS` | unset | Engine Kafka address; production builds require Kafka support and a configured address |
| `TRADE_EVENTS_TOPIC` | `trade-events` | Kafka topic |
| `TRADE_EVENTS_CONSUMER_GROUP` | `mini-exchange-trades` | Backend consumer group |
| `SPRING_DATASOURCE_URL` | dev profile default | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | PostgreSQL user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | PostgreSQL password |

Copy `.env.example` to `.env` when one is provided for your environment. Never commit credentials or tokens.

## Verification

The repository has been validated with:

```bash
cmake -S . -B build -DBUILD_GRPC=ON -DBUILD_TESTING=ON
cmake --build build --parallel
ctest --test-dir build --output-on-failure

cd backend
./gradlew :app:test --no-daemon

cd ..
docker compose config
docker compose build
```

The full Compose flow was validated with Docker; see `docs/END_TO_END_INTEGRATION_REPORT.md` for commands, results, and remaining reliability limitations.

## Design notes

- The engine keeps only the most recent command's transient trade results; Kafka is the cross-service transport and PostgreSQL is the durable record.
- The backend persists orders before sending them to the engine, ensuring engine commands carry stable database IDs.
- Kafka delivery is treated as at-least-once. Idempotency is enforced at the `(engineNamespace, executionId)` boundary.
- Native development builds may explicitly enable the no-op publisher with `ALLOW_NOOP_TRADE_PUBLISHER=ON`; production Kafka builds fail closed when librdkafka is unavailable.
- Authentication, Kafka security, rate limiting, and a production dead-letter policy are intentionally outside this compact reference implementation.

## License

This repository is provided for learning, experimentation, and extension. Add a project license before distributing it as a library or hosted service.
