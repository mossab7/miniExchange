# Isolated matching-engine tests and benchmark

The matching engine has a dependency-free core target, `matching_engine_core`.
It contains the pooled orders and price levels, the `OrderBook`, trade creation,
and the worker-based `MatchingEngine` dispatcher. gRPC, protobuf, Kafka, the
Java backend, PostgreSQL, logging, and network transport are not linked by the
test or benchmark targets.

## Architecture and test approach

`MatchingEngine` assigns each instrument to one worker. Public submissions and
cancellations wait synchronously for that worker, while the worker owns the
instrument's `OrderBook`. An order uses unsigned 64-bit price and quantity,
and the book applies price-time priority: highest bid/lowest ask first, then
FIFO within a price level. Trades execute at the resting order's price.

The tests exercise the `OrderBook` directly for deterministic matching and
state inspection, and exercise `MatchingEngine` separately for routing,
duplicate-active-order handling, lifecycle, and concurrent callers. The
randomized test uses seed `0x5eed1234` and an independent vector-based oracle;
when it fails, the seed and order id are included in the diagnostic.

## Build and run the isolated suite

From the repository root:

```sh
cmake -S . -B build-tests \
  -DBUILD_GRPC=OFF \
  -DBUILD_TESTING=ON \
  -DBUILD_BENCHMARK=ON \
  -DCMAKE_BUILD_TYPE=Release
cmake --build build-tests --parallel
ctest --test-dir build-tests --output-on-failure
```

The test executable is also available as:

```sh
build-tests/srcs/matching-engine/matching_engine_tests
```

The current validation contract rejects zero ids, zero prices, zero
quantities, invalid sides, and duplicate ids that are still active in an
`OrderBook`. Cancellation of an unknown order returns `false`. The tests use
those existing behaviors rather than imposing a new policy. A completed order
id is removed from the engine's active-id map and may therefore be reused.

## Benchmark

Build the standalone benchmark in a Release configuration as shown above and
run all workloads with:

```sh
build-tests/srcs/matching-engine/matching_engine_bench \
  --scenario all \
  --orders 10000 \
  --iterations 7 \
  --warmup 1 \
  --seed 12648430 \
  --csv /tmp/mini-exchange-benchmark.csv
```

Available scenarios are:

| Scenario | Timed operation | Setup and validation |
| --- | --- | --- |
| `no-match-insertion` | Orders that rest without an opposing side | Confirms zero trades |
| `immediate-matching` | Buys consuming one-for-one resting asks | Confirms one trade per submission |
| `partial-fills` | Buys consuming one unit from quantity-two asks | Confirms trades and remaining liquidity |
| `multi-order-matching` | Each buy consumes two FIFO asks | Confirms multiple trades per submission |
| `price-level-traversal` | One buy traversing many ask prices | Confirms every level is consumed |
| `cancellation` | Cancellation of resting buys | Confirms every cancellation succeeds |
| `mixed-workload` | Repeating insert, insert, match, cancel | Uses the supplied seed and confirms both matches and cancels |

Use `--scenario NAME` to run one workload. `--orders` controls the workload
size, `--iterations` controls independent measured repetitions, `--warmup`
controls unmeasured repetitions, and `--seed` controls reproducible mixed
workload generation. Setup orders are created outside the timed region, so the
primary timing is the throughput of the named operation. Fresh order-book state
is used for every repetition.

The console and CSV reports distinguish:

- `operations_per_second`: timed matching-engine operations per second;
- `submissions_per_second`: timed order submissions per second;
- `trades_per_second`: trades produced by timed submissions per second;
- `executed_quantity`: total quantity executed by timed submissions; and
- elapsed time, repetitions, seed, and min/median/max operation throughput.

Pass `--latency` to collect one `steady_clock` sample around each timed
operation and report p50, p95, and p99 nanoseconds. These samples are an
instrumented diagnostic workload, not the primary throughput result: clock
calls and vector storage add overhead, and the reported percentiles describe
the complete in-process `OrderBook` call including trade construction. Without
`--latency`, no individual-order latency claim is made.

The benchmark intentionally measures the core directly. It does not measure
gRPC, Kafka, protobuf serialization, database work, sockets, or backend
request handling. Matching workloads include the existing `Trade` object's
UUID and timestamp construction, because those are part of the in-process
matching path. Console output is outside the timed region.

CSV output is supplied by `--csv PATH`; it is overwritten for that invocation.
Keep result files outside the source and build trees when possible. No result
numbers are committed to this repository because they depend on CPU, compiler,
operating-system scheduling, and build flags.

## Reproducible suite reports

The standard-library-only orchestration script runs separate throughput and
instrumented-latency passes, captures machine/build metadata, records raw
stdout/stderr, attempts external `perf stat` collection, measures child-process
resource usage, and writes a unique run directory. It refuses to overwrite an
existing explicit run directory unless `--force` is supplied.

```sh
python3 srcs/matching-engine/tools/run_benchmark_suite.py \
  --benchmark build-tests/srcs/matching-engine/matching_engine_bench \
  --sizes 10000,100000 \
  --iterations 5 \
  --warmup 1 \
  --seed 12648430 \
  --price-levels 100
```

Use `--sizes 10000,100000,1000000` for a larger configurable scaling run when
the host can complete it. Use `--perf-all-sizes` to collect external counters
for every size; by default counters are attempted for the first size of each
scenario. Pass `--no-perf` when hardware-counter collection is known to be
unavailable. A successful benchmark run prints the generated directory, for
example:

```text
benchmark-results/<timestamp>-<pid>-<unique-id>/
├── metadata.json
├── throughput.csv
├── latency.csv
├── hardware-counters.csv
├── memory.csv
├── correctness.txt
├── commands.txt
├── report.md
├── plots/
└── raw/
```

`operations_per_second` counts successful operations. The separate
`attempted_operations_per_second` includes expected failed cancellation
requests, while `failed_operations` makes those requests visible. Throughput
and latency are never combined into one result. Latency uses
`std::chrono::steady_clock` around each operation in a dedicated run and
reports p50, p90, p95, p99, p99.9, maximum, and sample count. Small sample sets
are called out in the report rather than presented as strong tail-latency
evidence.

The report's CPU table contains only counters returned by external `perf stat`.
It adds cycles/order, instructions/order, cycles/trade, instructions/trade,
IPC, branch-miss rate, and cache-miss rate only when the underlying counters
were actually measured. `/usr/bin/time -v` is used for memory and process
statistics when installed; otherwise the script records Linux
`resource.RUSAGE_CHILDREN` values and labels that source. This fallback does
not provide allocator counts or bytes allocated; its RSS value is the child
process high-water reading and should be interpreted cautiously when several
sizes are run in one suite process. The report derives wall-clock and CPU time
per successful operation from the child timing values; these are software
timing metrics, not hardware-counter measurements. No global allocation
override is used.

On this host `perf` is installed and the generic cycles/instructions/branches/
cache events are discoverable, but `kernel.perf_event_paranoid=3` prevents
collection without additional capabilities. The suite saves the exact error in
`raw/*-perf.stderr.txt`, marks those counters unavailable, and does not weaken
kernel security settings or claim derived counter metrics.

## Sanitizers

Use separate build directories. AddressSanitizer and UndefinedBehaviorSanitizer:

```sh
cmake -S . -B build-asan-ubsan \
  -DBUILD_GRPC=OFF \
  -DBUILD_TESTING=ON \
  -DBUILD_BENCHMARK=OFF \
  -DCMAKE_BUILD_TYPE=Debug \
  -DCMAKE_CXX_FLAGS="-fsanitize=address,undefined -fno-omit-frame-pointer" \
  -DCMAKE_EXE_LINKER_FLAGS="-fsanitize=address,undefined"
cmake --build build-asan-ubsan --parallel
ctest --test-dir build-asan-ubsan --output-on-failure
```

Some ptrace-restricted runners cannot start LeakSanitizer even though the
AddressSanitizer and UndefinedBehaviorSanitizer checks work. In that
environment, rerun the final command as
`ASAN_OPTIONS=detect_leaks=0 ctest --test-dir build-asan-ubsan --output-on-failure`.

For the engine's concurrent-caller test, a separate ThreadSanitizer build can
be run with `-fsanitize=thread` in both `CMAKE_CXX_FLAGS` and
`CMAKE_EXE_LINKER_FLAGS`:

```sh
cmake -S . -B build-tsan \
  -DBUILD_GRPC=OFF -DBUILD_TESTING=ON -DBUILD_BENCHMARK=OFF \
  -DCMAKE_BUILD_TYPE=Debug \
  -DCMAKE_CXX_FLAGS="-fsanitize=thread -fno-omit-frame-pointer" \
  -DCMAKE_EXE_LINKER_FLAGS="-fsanitize=thread"
cmake --build build-tsan --parallel
ctest --test-dir build-tsan --output-on-failure
```

A clean sanitizer run only means that the exercised paths did not report a
sanitizer violation. It does not establish general thread safety, fairness,
or production readiness.

## Limitations

The benchmark uses the current synchronous public core and does not model
transport concurrency. Workloads are intentionally synthetic and do not claim
real-market performance. Small workloads can be dominated by clock resolution,
allocator/object-pool growth, CPU frequency changes, or scheduler noise. The
engine currently exposes the trades from the most recent matching command, not
a persistent trade history; the tests account for that interface.
