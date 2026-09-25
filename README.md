# MiniExchange

A compact trading platform built around a **C++ price-time-priority matching engine**, a **Spring Boot backend**, **gRPC**, **Kafka**, and **PostgreSQL**.

MiniExchange is designed as a learning and portfolio project for exploring how a trading system can separate **low-latency in-memory matching** from **durable application state**.

It is intentionally small enough to understand, but structured around boundaries you would find in larger distributed systems.

---

## Architecture

```text
                         Order Command
┌──────────────┐        REST / JWT        ┌──────────────────┐
│ Client / CLI │ ───────────────────────▶ │ Spring Boot API  │
└──────────────┘                          │                  │
                                         │ PostgreSQL       │
                                         │ Orders/Portfolios│
                                         └────────┬─────────┘
                                                  │
                                                  │ gRPC
                                                  ▼
                                         ┌──────────────────┐
                                         │ C++ Matching     │
                                         │ Engine           │
                                         │                  │
                                         │ Worker Routing   │
                                         │ Order Books      │
                                         │ Price/Time Match │
                                         └────────┬─────────┘
                                                  │
                                                  │ protobuf
                                                  ▼
                                         ┌──────────────────┐
                                         │      Kafka       │
                                         │   trade-events   │
                                         └────────┬─────────┘
                                                  │
                                                  │ consumer
                                                  ▼
                                         ┌──────────────────┐
                                         │ Spring Boot API  │
                                         │                  │
                                         │ Trade Settlement │
                                         │ Idempotency      │
                                         └────────┬─────────┘
                                                  │
                                                  ▼
                                         ┌──────────────────┐
                                         │    PostgreSQL    │
                                         │ Orders / Trades  │
                                         │ Balances / State │
                                         └──────────────────┘
```

The system is split into clear responsibilities:

| Responsibility                 | Component       | Technology                          |
| ------------------------------ | --------------- | ----------------------------------- |
| Authentication & HTTP API      | Backend         | Java 21, Spring Boot, JWT           |
| Orders & portfolios            | Backend         | Spring Data JPA, PostgreSQL, Flyway |
| Order matching                 | Matching engine | C++20                               |
| Backend ↔ engine communication | gRPC            | Protobuf                            |
| Trade event transport          | Kafka           | Protobuf                            |
| Trade persistence & settlement | Backend         | PostgreSQL                          |
| Local orchestration            | Infrastructure  | Docker Compose                      |

---

## Why this project?

A trading system is a useful example because it forces several very different problems to coexist.

The backend needs to handle:

* authentication;
* validation;
* persistence;
* portfolio state;
* order lifecycle;
* reliable event processing.

The matching engine has a different job:

* maintain in-memory order books;
* route orders to workers;
* apply price-time priority;
* generate trades with minimal coordination.

MiniExchange keeps those responsibilities separate instead of putting the entire system behind a single application.

The interesting boundary is:

```text
           Durable application state
                    │
                    │ gRPC command
                    ▼
             ┌─────────────┐
             │ C++ Engine  │
             └──────┬──────┘
                    │
                    │ TradeEvent
                    ▼
                ┌───────┐
                │ Kafka │
                └───┬───┘
                    │
                    ▼
           Durable trade settlement
```

The matching engine does not need to understand how portfolios are stored.

The backend does not need to implement the matching algorithm.

Kafka provides the boundary between the two.

---

# Core Features

### Backend

* JWT authentication
* User and portfolio management
* Asset management
* Order creation and cancellation
* PostgreSQL persistence
* Flyway database migrations
* Validation and authorization
* Trade-event consumption
* Idempotent trade processing

### Matching Engine

* C++20 implementation
* Price-time-priority matching
* In-memory order books
* Multiple instruments
* Worker-based instrument routing
* Efficient order and price-level management
* gRPC server
* Protobuf-based command contract

### Messaging

* Kafka trade events
* Protobuf serialization
* Manual consumer acknowledgement
* Retry handling
* UUID-based trade idempotency

### Development

* Docker Compose environment
* Native CMake build
* Gradle backend build
* Shared protobuf contract
* Automated tests
* Local engine/backend run targets

---

# Trade Lifecycle

A submitted order moves through the system roughly as follows:

```text
Client
  │
  │ POST /api/orders
  ▼
Spring Boot
  │
  ├── validate request
  ├── persist order
  │
  │ gRPC
  ▼
Matching Engine
  │
  ├── route instrument to worker
  ├── access order book
  ├── match orders
  └── create trade event
       │
       │ Kafka
       ▼
Spring Kafka Consumer
       │
       ├── check trade idempotency
       ├── persist trade
       ├── update order state
       └── update settlement state
```

The API is deliberately asynchronous after the order has been accepted.

An order may therefore be returned as `OPEN` before its matching result has been consumed from Kafka.

---

# Matching Engine

The matching engine is implemented in C++20 and keeps order books in memory.

The current matching model uses **price-time priority**:

* better prices match first;
* when multiple orders have the same price, the earlier order has priority.

Conceptually:

```text
BUY BOOK                      SELL BOOK

102.00  [B, B, B]       103.00  [A, A]
101.50  [B, B]          103.50  [A]
100.00  [B]

                 │
                 ▼

        Best bid = 102.00
        Best ask = 103.00
```

When a new order crosses the spread, the engine consumes liquidity from the opposite side until:

* the incoming quantity is filled;
* available liquidity is exhausted; or
* the order is otherwise completed.

The engine is intentionally independent from PostgreSQL and does not perform portfolio persistence itself.

---

# Worker Architecture

Instrument processing is separated across workers so that independent order books do not need to share the same matching path.

A simplified model is:

```text
                 Dispatcher
                     │
          ┌──────────┼──────────┐
          │          │          │
          ▼          ▼          ▼
       Worker 0   Worker 1   Worker 2
          │          │          │
       ┌──┴──┐    ┌──┴──┐    ┌──┴──┐
       BTC   ETH  AAPL  TSLA  ...  ...
```

Orders are routed according to their instrument.

This keeps each order book owned by a single worker rather than introducing locks around every matching operation.

---

# gRPC

The backend communicates with the matching engine through gRPC.

The protocol is defined in the shared protobuf source:

```text
proto/
└── miniExchange.proto
```

The same contract is used to generate bindings for Java and C++.

This avoids maintaining separate DTO definitions for the two services.

Typical flow:

```text
Java
  │
  │ OrderRequest
  ▼
gRPC
  │
  ▼
C++
  │
  │ matching
  ▼
TradeResult / TradeEvent
```

Generated sources are build artifacts. The `.proto` file is the source of truth.

---

# Kafka Trade Events

The matching engine publishes completed trades as protobuf messages.

Example:

```protobuf
message TradeEvent {
    string tradeId = 1;
    uint64 instrumentId = 2;
    uint64 buyOrderId = 3;
    uint64 sellOrderId = 4;
    uint64 price = 5;
    uint64 quantity = 6;
    int64 timestamp_epoch_millis = 7;
}
```

Kafka acts as the transport between matching and settlement.

Instead of keeping completed trades only in memory, the engine publishes them as events:

```text
Matching
   │
   ▼
TradeEvent
   │
   ▼
 Kafka
   │
   ▼
Backend consumer
   │
   ▼
PostgreSQL
```

This also means the backend can process trade events independently from the matching operation itself.

---

# At-Least-Once Processing

Kafka consumers are treated as **at-least-once**.

A message may therefore be delivered more than once.

MiniExchange handles this at the trade boundary using the trade UUID:

```text
Kafka message
     │
     ▼
tradeId already processed?
     │
   ┌─┴───────────────┐
   │                 │
  yes                no
   │                 │
ignore          persist trade
                     │
                     ▼
                update state
```

The important invariant is that the same trade cannot be settled twice simply because its Kafka message was delivered again.

---

# Data Model

The backend owns the durable business state.

The main domain objects include:

```text
User
 │
 ├── Portfolio
 │      │
 │      └── Position
 │
 └── Orders
         │
         └── Trades

Asset
```

PostgreSQL is the system of record for persisted application state.

The matching engine only needs the data required to match orders.

---

# Money and Quantities

Financial values are not represented internally as floating-point numbers.

Prices, quantities, and balances are represented using integer units based on the asset's precision.

For example, an asset with:

```text
decimalPlaces = 2
```

can represent:

```text
12.34
```

as:

```text
1234
```

This avoids the rounding problems associated with binary floating-point arithmetic.

The backend can accept decimal strings at the API boundary and convert them into the integer representation used internally.

Example:

```json
{
  "market": "BTC/USD",
  "price": "50000",
  "quantity": "0.1",
  "side": "BUY"
}
```

---

# Repository Structure

```text
.
├── backend/
│   └── app/                 Spring Boot backend
│
├── matching-engine/
│   └── ...                  C++20 matching engine
│
├── proto/
│   └── miniExchange.proto   Shared protobuf contract
│
├── CMakeLists.txt           Root development/build targets
├── docker-compose.yml       Local multi-service environment
└── README.md
```

Generated gRPC/protobuf bindings are generated during the build and are not the source of truth.

---

# Running with Docker

## Requirements

* Docker
* Docker Compose v2
* Git

Build and start the complete environment:

```bash
docker compose up --build
```

Typical endpoints:

| Service         | Address                 |
| --------------- | ----------------------- |
| Backend API     | `http://localhost:8080` |
| Matching engine | `localhost:50051`       |
| PostgreSQL      | `localhost:5433`        |
| Kafka           | `localhost:29092`       |

Stop the environment:

```bash
docker compose down
```

Remove containers and the local PostgreSQL volume:

```bash
docker compose down -v
```

> This removes the local database data.

---

# Native Development

## Build the matching engine

```bash
cmake -S . -B build \
  -DBUILD_GRPC=ON \
  -DBUILD_TESTING=ON

cmake --build build --parallel
```

Run tests:

```bash
ctest --test-dir build --output-on-failure
```

---

## Generate protobuf / gRPC bindings

```bash
cmake --build build --target generate-proto
```

The protobuf definition remains:

```text
proto/miniExchange.proto
```

The build system generates the required C++ and Java bindings from that contract.

---

## Run the matching engine

```bash
cmake --build build --target run-matching-engine
```

---

## Run the backend

```bash
cmake --build build --target run-backend
```

Or run Spring Boot directly:

```bash
cd backend

./gradlew :app:bootRun --no-daemon
```

Example local configuration:

```bash
MATCHING_ENGINE_HOST=localhost \
MATCHING_ENGINE_PORT=50051 \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
./gradlew :app:bootRun --no-daemon
```

---

## Root CMake Targets

Useful development targets include:

```bash
cmake --build build --target generate-proto
cmake --build build --target run-matching-engine
cmake --build build --target run-backend
cmake --build build --target run-all
```

`run-all` delegates to the Docker Compose environment.

---

# HTTP API

Authentication:

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "secret123"
  }'
```

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "alice",
    "password": "secret123"
  }'
```

Main endpoint groups:

| Endpoint                | Purpose                                  |
| ----------------------- | ---------------------------------------- |
| `POST /api/auth/signup` | Create an account                        |
| `POST /api/auth/login`  | Authenticate and obtain a JWT            |
| `/api/assets`           | Asset management                         |
| `/api/portfolios`       | Portfolio management                     |
| `/api/orders`           | Create, inspect, list, and cancel orders |

Example order request:

```json
{
  "portfolioId": "00000000-0000-0000-0000-000000000000",
  "market": "BTC/USD",
  "price": "50000",
  "quantity": "0.1",
  "side": "BUY"
}
```

---

# Configuration

The application defaults to localhost for native development.

Docker Compose overrides these values with container service names.

| Variable                                  | Default                | Purpose                          |
| ----------------------------------------- | ---------------------- | -------------------------------- |
| `MATCHING_ENGINE_HOST`                    | `localhost`            | Matching engine hostname         |
| `MATCHING_ENGINE_PORT`                    | `50051`                | Matching engine gRPC port        |
| `KAFKA_BOOTSTRAP_SERVERS`                 | `localhost:9092`       | Backend Kafka address            |
| `MATCHING_ENGINE_KAFKA_BOOTSTRAP_SERVERS` | unset                  | Kafka address used by the engine |
| `TRADE_EVENTS_TOPIC`                      | `trade-events`         | Trade event topic                |
| `TRADE_EVENTS_CONSUMER_GROUP`             | `mini-exchange-trades` | Kafka consumer group             |
| `SPRING_DATASOURCE_URL`                   | development default    | PostgreSQL JDBC URL              |
| `SPRING_DATASOURCE_USERNAME`              | `postgres`             | PostgreSQL username              |
| `SPRING_DATASOURCE_PASSWORD`              | `postgres`             | PostgreSQL password              |

Use an environment file for local configuration when appropriate.

Never commit real credentials, tokens, or secrets.

---

# Verification

The main build and test workflow is:

```bash
cmake -S . -B build \
  -DBUILD_GRPC=ON \
  -DBUILD_TESTING=ON

cmake --build build --parallel

ctest --test-dir build --output-on-failure
```

Backend tests:

```bash
cd backend

./gradlew :app:test --no-daemon
```

Validate the Compose configuration:

```bash
docker compose config
```

Build the complete environment:

```bash
docker compose build
```

---

# Design Decisions

### Matching and persistence are separated

The C++ engine focuses on matching orders in memory.

The Java backend owns users, portfolios, orders, trades, and persistent state.

This keeps the matching path independent from database operations.

### Orders are persisted before matching

The backend creates the order record before sending the command to the engine.

That gives the engine a stable order identifier that already exists in the system of record.

### Kafka is the trade boundary

The matching engine does not directly modify PostgreSQL.

Instead:

```text
Engine
  │
  ▼
TradeEvent
  │
  ▼
Kafka
  │
  ▼
Backend
  │
  ▼
PostgreSQL
```

This makes event processing an explicit application boundary.

### Idempotency is part of settlement

Kafka delivery is assumed to be at-least-once.

The backend therefore uses the trade identifier as an idempotency key so repeated delivery does not create duplicate trades.

### Shared protobuf contract

Both services generate their bindings from the same `.proto` source.

That keeps the wire contract explicit and versionable.

---

# Project Scope

MiniExchange is intentionally a compact reference implementation and learning project rather than a production exchange.

It does not attempt to provide:

* production-grade exchange connectivity;
* high-availability failover;
* distributed order-book replication;
* exchange-grade market-data feeds;
* hardened Kafka security;
* complete risk management;
* sophisticated dead-letter/recovery infrastructure;
* regulatory or compliance functionality.

The goal is to explore the architecture and implementation of the core components without hiding the important parts behind frameworks.

---

# Technology Stack

```text
Backend
  Java 21
  Spring Boot
  Spring Security
  Spring Data JPA
  Gradle
  PostgreSQL
  Flyway

Matching Engine
  C++20
  CMake
  gRPC
  Protobuf
  In-memory order books

Messaging
  Apache Kafka
  Protobuf

Infrastructure
  Docker
  Docker Compose
```

---

# Learning Focus

The project is primarily an exploration of:

* low-level C++ systems programming;
* order-book data structures;
* price-time-priority matching;
* worker-based concurrency;
* gRPC service boundaries;
* protobuf contracts;
* Kafka event-driven architecture;
* at-least-once message processing;
* idempotent consumers;
* transactional persistence;
* Java/Spring backend architecture;
* Dockerized multi-service development.

---

# License

This project is provided for learning, experimentation, and extension.

Add an explicit open-source license before redistributing the project as a library or hosted service.
