# MiniExchange post-optimization benchmark report

## Scope

This report covers the current matching-engine checkout after the trade-handling refactor. The measured path stores compact `Execution` records in `lastExecutions_`; it does not construct UUID-backed `Trade` objects. The legacy `getTrades()` compatibility method can lazily materialize `Trade` objects, but the benchmark never calls it. Kafka, gRPC, Spring Boot, and PostgreSQL are not included.

Run artifacts: `benchmark-results/20261104T114852Z-111412-4f7f3267/`.

## Method

- Release build: `-O3 -DNDEBUG`, GCC 12.2, C++20.
- CPU affinity: process and children pinned to CPU 2 with `taskset -c 2`.
- Workload sizes: 100,000 and 1,000,000 operations.
- Repetitions: 5 measured, 2 warmups.
- Seed: `12648430`.
- Price levels: 100, cyclic distribution.
- Setup was outside the timed operation interval.
- Latency used a separate `steady_clock` pass.
- `perf stat` collected cycles, instructions, branches, cache, TLB/page-fault, scheduling, and migration counters.
- Focused `perf record -F 999 -g` captured 8,287 samples with zero lost samples for one-million-operation multi-order matching.

Correctness passed for all eight scenarios at both sizes: insertion, immediate matching, partial fills, multi-order matching, price-level traversal, cancellation, mixed cancellation, and mixed workload.

## Throughput

| Scenario | Size | Orders/sec | Fills/sec | Median orders/sec |
|---|---:|---:|---:|---:|
| No-match insertion | 100k | 5.63M | — | 5.65M |
| Immediate matching | 100k | 5.30M | 5.30M | 5.67M |
| Partial fills | 100k | 5.66M | 5.66M | 5.59M |
| Multi-order matching | 100k | 3.39M | 6.78M | 3.43M |
| Price-level traversal | 100k levels | 37.4 | 3.74M | 37.8 |
| Cancellation | 100k | 11.36M | — | 11.44M |
| Mixed workload | 100k | 7.67M | 1.92M | 8.11M |
| No-match insertion | 1M | 6.79M | — | 6.95M |
| Immediate matching | 1M | 6.10M | 6.10M | 6.21M |
| Partial fills | 1M | 7.24M | 7.24M | 7.29M |
| Multi-order matching | 1M | 3.47M | 6.94M | 3.48M |
| Price-level traversal | 1M levels | 3.86 | 3.86M | 3.83 |
| Cancellation | 1M | 11.41M | — | 11.41M |
| Mixed workload | 1M | 7.85M | 1.96M | 7.87M |

The prior report showed roughly 33k immediate matches/sec because it measured the old UUID/trade-construction path. The current post-refactor run measures approximately 6.1M immediate matches/sec at one million operations. This is not a small noise-level change; it confirms that UUID generation and string formatting were a dominant cost in the old implementation.

## Latency

At one million operations, with five million latency samples per scenario:

| Scenario | p50 | p95 | p99 | p99.9 |
|---|---:|---:|---:|---:|
| No-match insertion | 176 ns | 271 ns | 345 ns | 4.59 µs |
| Immediate matching | 191 ns | 281 ns | 383 ns | 615 ns |
| Partial fills | 186 ns | 229 ns | 327 ns | 557 ns |
| Multi-order matching | 318 ns | 632 ns | 920 ns | 1.17 µs |
| Cancellation | 129 ns | 180 ns | 225 ns | 471 ns |
| Mixed workload | 144 ns | 218 ns | 322 ns | 532 ns |

These are instrumented in-process call latencies and include clock/sample-storage overhead. Price-level traversal has only five incoming-order samples and its p99 is not statistically meaningful. Its one-million-level sample took approximately 342 ms and consumed one million fills, or about 342 ns amortized per fill in the instrumented run.

## Hardware counters

For the one-million-operation multi-order workload, `perf stat` reported:

| Metric | Per incoming order | Per fill |
|---|---:|---:|
| Cycles | 7,082 | 3,541 |
| Instructions | 8,074 | 4,037 |
| IPC | 1.14 | — |
| Branch miss rate | 0.24% | — |
| L1 data-load miss rate | 5.89% | — |
| Aggregate cache-reference miss rate | 56.63% | — |

The aggregate cache-reference event is architecture/tool dependent and should not be interpreted as an LLC miss rate. LLC-load misses were approximately 3.03M over the run; the raw CSV contains all counters.

## Focused profile

The focused multi-fill `perf record` profile captured no lost samples. The largest visible symbols were:

- `std::_Rb_tree::_M_insert_unique`: approximately 18% of sampled children time.
- `OrderBook::addToBook`: approximately 9% self and 8% additional child contribution.
- `OrderBook::submitOrder`: approximately 7% self/child contribution.
- `OrderBook::recordTrade`: approximately 4.7% self and approximately 10% including children; `clock_gettime` appeared at approximately 4.4% of samples.
- `OrderBook::matchAgainstAsks`: approximately 4.6% in the visible profile.
- libc `malloc`/`_int_malloc`/`free`: approximately 15% combined visible samples.

These percentages overlap because the report shows inclusive and self samples. The command profiled the complete benchmark process, so its profile includes prepared-book setup and teardown as well as timed operations. It is therefore evidence for candidate costs, not a precise attribution of the timed multi-fill interval.

## Conclusions

1. UUID generation and string formatting are no longer on the measured matching path.
2. Immediate matching improved from approximately 33k/sec in the old report to approximately 6.1M/sec in the current one-million-operation run.
3. Multi-fill matching now sustains approximately 3.47M incoming orders/sec and 6.94M fills/sec.
4. The next measurable candidates are price-level tree operations, order/book allocation behavior, and timestamp generation in `recordTrade`.
5. No evidence currently justifies replacing `std::map`, the hash table, or the FIFO structure. A timed-only profile or microbenchmark is needed to isolate those costs from setup.
6. No production end-to-end throughput claim should be made from these numbers because Kafka and persistence are excluded.

## Reproduction

```sh
cmake -S . -B build-tests -DBUILD_GRPC=OFF -DBUILD_TESTING=ON \
  -DBUILD_BENCHMARK=ON -DCMAKE_BUILD_TYPE=Release
cmake --build build-tests --parallel
ctest --test-dir build-tests --output-on-failure

taskset -c 2 python3 srcs/matching-engine/tools/run_benchmark_suite.py \
  --benchmark build-tests/srcs/matching-engine/matching_engine_bench \
  --sizes 100000,1000000 --iterations 5 --warmup 2 \
  --seed 12648430 --price-levels 100 --perf-all-sizes
```

The raw focused profile was collected with `perf record -F 999 -g` on the one-million-operation `multi-order-matching` scenario. The kernel permitted hardware counters during this run; no security settings were changed.
