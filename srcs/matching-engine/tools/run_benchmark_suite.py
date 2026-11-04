#!/usr/bin/env python3
"""Run the isolated matching-engine benchmark and assemble a reproducible report.

The C++ executable remains the source of throughput and latency measurements.
This script only orchestrates independent runs and records host/profiler data.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import json
import os
import platform
import resource
import re
import shutil
import subprocess
import sys
import time
import uuid
from pathlib import Path
from typing import Iterable


BASE_COLUMNS = [
    "workload_size", "scenario", "mode", "setup_orders", "initial_book_size",
    "measured_submissions", "measured_operations", "successful_operations",
    "failed_operations", "trades", "executed_quantity", "final_book_size",
    "repetitions", "seed", "price_levels", "latency_samples", "elapsed_ms",
    "attempted_operations_per_second", "operations_per_second", "submissions_per_second",
    "trades_per_second", "median_operations_per_second", "min_operations_per_second",
    "max_operations_per_second", "p50_latency_ns", "p90_latency_ns", "p95_latency_ns",
    "p99_latency_ns", "p99_9_latency_ns", "max_latency_ns", "correctness_failures",
]


def command_result(command: list[str], cwd: Path | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, text=True, capture_output=True, check=False)


def write_text(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")


def parse_key_value_lines(text: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in text.splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            result[key.strip()] = value.strip()
    return result


def parse_time_output(path: Path) -> dict[str, str]:
    if not path.exists():
        return {"status": "unavailable", "reason": "time output was not created"}
    result = parse_key_value_lines(path.read_text(encoding="utf-8", errors="replace"))
    elapsed = result.get("Elapsed (wall clock) time (h:mm:ss or m:ss)")
    if elapsed:
        parts = elapsed.split(":")
        try:
            if len(parts) == 3:
                result["elapsed_seconds"] = str(int(parts[0]) * 3600 + int(parts[1]) * 60 + float(parts[2]))
            elif len(parts) == 2:
                result["elapsed_seconds"] = str(int(parts[0]) * 60 + float(parts[1]))
            else:
                result["elapsed_seconds"] = str(float(parts[0]))
        except ValueError:
            pass
    result["status"] = "available"
    return result


def lscpu_info() -> dict[str, str]:
    tool = shutil.which("lscpu")
    if not tool:
        return {"status": "unavailable", "reason": "lscpu not installed"}
    completed = command_result([tool])
    info = parse_key_value_lines(completed.stdout)
    info["status"] = "available" if completed.returncode == 0 else "unavailable"
    if completed.returncode:
        info["reason"] = completed.stderr.strip()
    return info


def memory_info() -> dict[str, str]:
    path = Path("/proc/meminfo")
    if not path.exists():
        return {"status": "unavailable", "reason": "/proc/meminfo is unavailable"}
    info: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            info[key] = value.strip()
    info["status"] = "available"
    return info


def build_info(benchmark: Path) -> dict[str, str]:
    result: dict[str, str] = {
        "benchmark": str(benchmark),
        "compiler": "unavailable",
        "compiler_version": "unavailable",
        "build_type": "unknown",
        "cxx_flags": "unknown",
        "cxx_standard": "c++20",
    }
    build_dir = None
    for parent in [benchmark.parent, *benchmark.parents]:
        if (parent / "CMakeCache.txt").exists():
            build_dir = parent
            break
    if build_dir:
        cache = (build_dir / "CMakeCache.txt").read_text(encoding="utf-8", errors="replace")
        for key in ("CMAKE_BUILD_TYPE", "CMAKE_CXX_COMPILER", "CMAKE_CXX_FLAGS"):
            match = re.search(rf"^{re.escape(key)}:[^=]*=(.*)$", cache, re.MULTILINE)
            if match:
                result["build_type" if key == "CMAKE_BUILD_TYPE" else "cxx_flags" if key == "CMAKE_CXX_FLAGS" else "compiler"] = match.group(1)
        flags = build_dir / "srcs/matching-engine/CMakeFiles/matching_engine_bench.dir/flags.make"
        if flags.exists():
            text = flags.read_text(encoding="utf-8", errors="replace")
            match = re.search(r"^CXX_FLAGS = (.*)$", text, re.MULTILINE)
            if match:
                result["cxx_flags"] = match.group(1)
    compiler = result["compiler"] if result["compiler"] != "unavailable" else shutil.which("c++")
    if compiler:
        result["compiler"] = compiler
        version = command_result([compiler, "--version"])
        result["compiler_version"] = version.stdout.splitlines()[0] if version.stdout else version.stderr.strip()
    return result


def parse_perf_events(perf_list: str) -> list[str]:
    candidates = [
        ("cycles", r"\b(cpu-cycles|cycles)\b"),
        ("instructions", r"\binstructions\b"),
        ("branches", r"\bbranches\b"),
        ("branch-misses", r"\bbranch-misses\b"),
        ("cache-references", r"\bcache-references\b"),
        ("cache-misses", r"\bcache-misses\b"),
        ("L1-dcache-loads", r"L1-dcache-loads"),
        ("L1-dcache-load-misses", r"L1-dcache-load-misses"),
        ("LLC-loads", r"LLC-loads"),
        ("LLC-load-misses", r"LLC-load-misses"),
        ("task-clock", r"\btask-clock\b"),
        ("context-switches", r"\bcontext-switches\b"),
        ("cpu-migrations", r"\bcpu-migrations\b"),
        ("page-faults", r"\bpage-faults\b"),
    ]
    return [name for name, pattern in candidates if re.search(pattern, perf_list, re.IGNORECASE)]


def csv_rows(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as stream:
        return list(csv.DictReader(stream))


def write_rows(path: Path, rows: list[dict[str, str]], columns: list[str]) -> None:
    with path.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=columns, extrasaction="ignore")
        writer.writeheader()
        writer.writerows({column: row.get(column, "") for column in columns} for row in rows)


def numeric(row: dict[str, str], key: str) -> float | None:
    try:
        value = row.get(key, "")
        return float(value) if value != "" else None
    except ValueError:
        return None


def slug(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", value)


def run_benchmark(
    benchmark: Path,
    args: list[str],
    raw_prefix: Path,
    raw_dir: Path,
    use_time: bool,
) -> tuple[int, str, str, Path | None, dict[str, str], dict[str, str]]:
    stdout_path = raw_dir / f"{raw_prefix}.stdout.txt"
    stderr_path = raw_dir / f"{raw_prefix}.stderr.txt"
    time_path = raw_dir / f"{raw_prefix}.time.txt"
    command = [str(benchmark), *args]
    external_time = use_time and Path("/usr/bin/time").is_file()
    if external_time:
        command = ["/usr/bin/time", "-v", "-o", str(time_path), *command]
    before_usage = resource.getrusage(resource.RUSAGE_CHILDREN)
    start = time.monotonic()
    completed = command_result(command)
    elapsed = time.monotonic() - start
    after_usage = resource.getrusage(resource.RUSAGE_CHILDREN)
    write_text(stdout_path, completed.stdout)
    write_text(stderr_path, completed.stderr)
    usage = {
        "status": "available",
        "peak_rss_kb": str(after_usage.ru_maxrss),
        "user_time_s": str(after_usage.ru_utime - before_usage.ru_utime),
        "system_time_s": str(after_usage.ru_stime - before_usage.ru_stime),
        "elapsed_wall_s": str(elapsed),
        "cpu_percent": str((after_usage.ru_utime - before_usage.ru_utime + after_usage.ru_stime - before_usage.ru_stime) / elapsed * 100 if elapsed else 0.0),
        "minor_page_faults": str(after_usage.ru_minflt - before_usage.ru_minflt),
        "major_page_faults": str(after_usage.ru_majflt - before_usage.ru_majflt),
        "voluntary_context_switches": str(after_usage.ru_nvcsw - before_usage.ru_nvcsw),
        "involuntary_context_switches": str(after_usage.ru_nivcsw - before_usage.ru_nivcsw),
        "source": "GNU time -v" if external_time else "Python resource.RUSAGE_CHILDREN",
    }
    return completed.returncode, completed.stdout, completed.stderr, time_path if external_time else None, {
        "command": " ".join(command),
        "stdout": str(stdout_path),
        "stderr": str(stderr_path),
    }, usage


def svg_plot(path: Path, title: str, x_label: str, y_label: str,
             series: dict[str, list[tuple[float, float]]]) -> None:
    width, height = 900, 480
    left, top, right, bottom = 90, 45, 35, 70
    points = [point for values in series.values() for point in values]
    if not points:
        return
    xmin, xmax = min(point[0] for point in points), max(point[0] for point in points)
    ymin, ymax = min(point[1] for point in points), max(point[1] for point in points)
    if xmin == xmax:
        xmin, xmax = 0, max(1, xmax * 1.1)
    if ymin == ymax:
        ymin, ymax = 0, max(1, ymax * 1.1)

    def xy(point: tuple[float, float]) -> tuple[float, float]:
        x = left + (point[0] - xmin) / (xmax - xmin) * (width - left - right)
        y = height - bottom - (point[1] - ymin) / (ymax - ymin) * (height - top - bottom)
        return x, y

    colors = ["#2563eb", "#dc2626", "#16a34a", "#9333ea", "#ea580c", "#0891b2"]
    lines = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
             '<rect width="100%" height="100%" fill="white"/>',
             f'<text x="{width / 2}" y="25" text-anchor="middle" font-family="sans-serif" font-size="18">{title}</text>',
             f'<line x1="{left}" y1="{height-bottom}" x2="{width-right}" y2="{height-bottom}" stroke="black"/>',
             f'<line x1="{left}" y1="{top}" x2="{left}" y2="{height-bottom}" stroke="black"/>',
             f'<text x="{width / 2}" y="{height-18}" text-anchor="middle" font-family="sans-serif">{x_label}</text>',
             f'<text x="18" y="{height / 2}" transform="rotate(-90 18 {height / 2})" text-anchor="middle" font-family="sans-serif">{y_label}</text>']
    for index, (name, values) in enumerate(sorted(series.items())):
        color = colors[index % len(colors)]
        coords = [xy(point) for point in sorted(values)]
        lines.append('<polyline fill="none" stroke="%s" points="%s"/>' %
                     (color, " ".join(f"{x:.1f},{y:.1f}" for x, y in coords)))
        for x, y in coords:
            lines.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="3" fill="{color}"/>')
        lines.append(f'<text x="{width-right-120}" y="{top+18*index}" fill="{color}" font-family="sans-serif">{name}</text>')
    lines.append("</svg>\n")
    write_text(path, "\n".join(lines))


def markdown_table(headers: list[str], rows: Iterable[Iterable[object]]) -> str:
    lines = ["| " + " | ".join(headers) + " |", "| " + " | ".join("---" for _ in headers) + " |"]
    lines.extend("| " + " | ".join(str(value) for value in row) + " |" for row in rows)
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--benchmark", default="build-tests/srcs/matching-engine/matching_engine_bench")
    parser.add_argument("--output-root", default="benchmark-results")
    parser.add_argument("--sizes", default="10000", help="comma-separated measured operation counts")
    parser.add_argument("--scenarios", default="all", help="comma-separated scenarios or all")
    parser.add_argument("--iterations", type=int, default=5)
    parser.add_argument("--warmup", type=int, default=1)
    parser.add_argument("--seed", type=int, default=12648430)
    parser.add_argument("--price-levels", type=int, default=100)
    parser.add_argument("--price-distribution", choices=["cyclic", "uniform"], default="cyclic")
    parser.add_argument("--no-perf", action="store_true", help="skip perf stat collection")
    parser.add_argument("--perf-all-sizes", action="store_true")
    parser.add_argument("--force", action="store_true", help="allow an explicitly selected run directory")
    parser.add_argument("--run-dir", help="exact output directory; otherwise a unique directory is created")
    ns = parser.parse_args()

    benchmark = Path(ns.benchmark).resolve()
    if not benchmark.is_file():
        print(f"benchmark executable not found: {benchmark}", file=sys.stderr)
        return 2
    if ns.iterations < 1 or ns.warmup < 0 or ns.price_levels < 1:
        print("iterations must be positive, warmup non-negative, and price-levels positive", file=sys.stderr)
        return 2
    try:
        sizes = [int(value) for value in ns.sizes.split(",") if value]
        if not sizes or any(size < 1 for size in sizes):
            raise ValueError
    except ValueError:
        print("--sizes must contain positive integers", file=sys.stderr)
        return 2
    scenarios = [value for value in ("no-match-insertion,immediate-matching,partial-fills,multi-order-matching,price-level-traversal,cancellation,cancellation-mixed,mixed-workload" if ns.scenarios == "all" else ns.scenarios).split(",") if value]

    if ns.run_dir:
        run_dir = Path(ns.run_dir).resolve()
        if run_dir.exists() and not ns.force:
            print(f"run directory exists; use --force explicitly: {run_dir}", file=sys.stderr)
            return 2
    else:
        stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        run_dir = (Path(ns.output_root).resolve() / f"{stamp}-{os.getpid()}-{uuid.uuid4().hex[:8]}")
    run_dir.mkdir(parents=True, exist_ok=False)
    raw_dir = run_dir / "raw"
    plots_dir = run_dir / "plots"
    raw_dir.mkdir()
    plots_dir.mkdir()

    lscpu = lscpu_info()
    perf_path = shutil.which("perf")
    perf_list = ""
    perf_events: list[str] = []
    perf_reason = "disabled by --no-perf" if ns.no_perf else "perf not installed"
    if perf_path and not ns.no_perf:
        perf_probe = command_result([perf_path, "list"])
        perf_list = perf_probe.stdout + perf_probe.stderr
        write_text(raw_dir / "perf-list.txt", perf_list)
        perf_events = parse_perf_events(perf_list)
        perf_reason = "no recognized events in perf list" if not perf_events else ""
    env = {
        "run_id": run_dir.name,
        "timestamp_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "host": platform.node(),
        "kernel": platform.platform(),
        "uname": command_result(["uname", "-a"]).stdout.strip(),
        "cpu": lscpu,
        "logical_cpus": os.cpu_count(),
        "physical_cpus": None,
        "memory": memory_info(),
        "build": build_info(benchmark),
        "perf": {"installed": bool(perf_path), "events_discovered": perf_events, "reason": perf_reason},
        "python": sys.version,
    }
    try:
        env["physical_cpus"] = int(lscpu.get("Core(s) per socket", "0")) * int(lscpu.get("Socket(s)", "0"))
    except ValueError:
        pass

    common = ["--orders", "0", "--iterations", str(ns.iterations), "--warmup", str(ns.warmup),
              "--seed", str(ns.seed), "--price-levels", str(ns.price_levels),
              "--price-distribution", ns.price_distribution]
    throughput_rows: list[dict[str, str]] = []
    latency_rows: list[dict[str, str]] = []
    memory_rows: list[dict[str, str]] = []
    hardware_rows: list[dict[str, str]] = []
    commands: list[str] = []
    correctness: list[str] = []
    all_valid = True
    perf_failures: list[str] = []

    for size in sizes:
        selected_perf_sizes = ns.perf_all_sizes or size == sizes[0]
        for scenario in scenarios:
            stem = slug(f"{scenario}-{size}")
            csv_path = raw_dir / f"{stem}-throughput.csv"
            args = ["--scenario", scenario, *common[:1], str(size), *common[2:], "--csv", str(csv_path)]
            rc, stdout, stderr, time_path, command_meta, usage_stats = run_benchmark(
                benchmark, args, Path(f"{stem}-throughput"), raw_dir, True)
            commands.append(command_meta["command"])
            valid = rc == 0 and csv_path.exists()
            if valid:
                rows = csv_rows(csv_path)
                valid = len(rows) == 1 and rows[0].get("correctness_failures", "0") == "0"
                for row in rows:
                    row.update({"workload_size": str(size), "mode": "throughput", "scenario": scenario})
                    throughput_rows.append(row)
                if not rows:
                    correctness.append(f"INVALID {scenario} size={size}: benchmark emitted no CSV row")
            if not valid:
                all_valid = False
                correctness.append(f"INVALID {scenario} size={size}: returncode={rc}; stderr={stderr.strip()}")
            else:
                correctness.append(f"PASS {scenario} size={size}")
            if time_path:
                stats = parse_time_output(time_path)
            else:
                stats = usage_stats
            memory_row = {
                "scenario": scenario, "workload_size": str(size), "mode": "throughput",
                "peak_rss_kb": stats.get("Maximum resident set size (kbytes)", stats.get("peak_rss_kb", "")),
                "user_time_s": stats.get("User time (seconds)", stats.get("user_time_s", "")),
                "system_time_s": stats.get("System time (seconds)", stats.get("system_time_s", "")),
                "elapsed_wall_s": stats.get("elapsed_seconds", stats.get("elapsed_wall_s", "")),
                "cpu_percent": stats.get("Percent of CPU this job got", stats.get("cpu_percent", "")),
                "minor_page_faults": stats.get("Minor (reclaiming a frame) page faults", stats.get("minor_page_faults", "")),
                "major_page_faults": stats.get("Major (requiring I/O) page faults", stats.get("major_page_faults", "")),
                "voluntary_context_switches": stats.get("Voluntary context switches", stats.get("voluntary_context_switches", "")),
                "involuntary_context_switches": stats.get("Involuntary context switches", stats.get("involuntary_context_switches", "")),
                "source": stats.get("source", "GNU time -v" if time_path else "Python resource.RUSAGE_CHILDREN"),
                "status": stats.get("status", "unavailable"),
            }
            successful = numeric(throughput_rows[-1], "successful_operations") if throughput_rows and throughput_rows[-1].get("scenario") == scenario and throughput_rows[-1].get("workload_size") == str(size) else None
            wall_seconds = numeric(memory_row, "elapsed_wall_s")
            cpu_seconds = (numeric(memory_row, "user_time_s") or 0.0) + (numeric(memory_row, "system_time_s") or 0.0)
            memory_row["wall_time_per_successful_operation_ns"] = str(wall_seconds * 1e9 / successful) if wall_seconds is not None and successful else ""
            memory_row["cpu_time_per_successful_operation_ns"] = str(cpu_seconds * 1e9 / successful) if successful else ""
            memory_rows.append(memory_row)

            latency_csv = raw_dir / f"{stem}-latency.csv"
            latency_args = ["--scenario", scenario, "--orders", str(size), "--iterations", str(ns.iterations),
                            "--warmup", str(ns.warmup), "--seed", str(ns.seed), "--price-levels",
                            str(ns.price_levels), "--price-distribution", ns.price_distribution,
                            "--latency", "--csv", str(latency_csv)]
            rc_latency, _, stderr_latency, _, latency_meta, _ = run_benchmark(
                benchmark, latency_args, Path(f"{stem}-latency"), raw_dir, False)
            commands.append(latency_meta["command"])
            if rc_latency == 0 and latency_csv.exists():
                for row in csv_rows(latency_csv):
                    row.update({"workload_size": str(size), "mode": "latency", "scenario": scenario})
                    latency_rows.append(row)
            else:
                all_valid = False
                correctness.append(f"INVALID latency {scenario} size={size}: returncode={rc_latency}; stderr={stderr_latency.strip()}")

            if perf_events and selected_perf_sizes:
                perf_csv = raw_dir / f"{stem}-perf-child.csv"
                perf_output = raw_dir / f"{stem}-perf.stat"
                perf_stderr = raw_dir / f"{stem}-perf.stderr.txt"
                perf_command = [perf_path, "stat", "-x,", "-r", "1", "-e", ",".join(perf_events),
                                "-o", str(perf_output), "--", str(benchmark), "--scenario", scenario,
                                "--orders", str(size), "--iterations", "1", "--warmup", "1",
                                "--seed", str(ns.seed), "--price-levels", str(ns.price_levels),
                                "--price-distribution", ns.price_distribution, "--csv", str(perf_csv)]
                perf_result = subprocess.run(perf_command, text=True, capture_output=True, check=False)
                write_text(raw_dir / f"{stem}-perf.stdout.txt", perf_result.stdout)
                write_text(perf_stderr, perf_result.stderr)
                commands.append(" ".join(perf_command))
                if perf_result.returncode == 0 and perf_output.exists():
                    for line in perf_output.read_text(encoding="utf-8", errors="replace").splitlines():
                        fields = line.split(",")
                        if len(fields) >= 3 and fields[0] not in ("<not counted>", "<not supported>"):
                            try:
                                value = float(fields[0].replace(" ", ""))
                            except ValueError:
                                continue
                            hardware_rows.append({"scenario": scenario, "workload_size": str(size),
                                                  "metric": fields[2], "value": str(value),
                                                  "unit": fields[1], "status": "available",
                                                  "source": str(perf_output)})
                else:
                    perf_failures.append(perf_result.stderr.strip() or f"returncode={perf_result.returncode}")
                    hardware_rows.append({"scenario": scenario, "workload_size": str(size),
                                          "metric": "perf_stat", "value": "", "unit": "",
                                          "status": "unavailable",
                                          "source": str(perf_stderr)})

    # A profiler may be installed but inaccessible to this kernel/container.
    if perf_failures:
        env["perf"]["reason"] = " ".join(perf_failures[0].splitlines()[:4])
    if not perf_events:
        for scenario in scenarios:
            hardware_rows.append({"scenario": scenario, "workload_size": str(sizes[0]),
                                  "metric": "perf_stat", "value": "", "unit": "",
                                  "status": "unavailable", "source": perf_reason})

    # Normalize only counters that were actually measured. These are derived
    # rows, never estimates from wall-clock throughput.
    throughput_by_key = {(row["scenario"], row["workload_size"]): row for row in throughput_rows}
    measured = {}
    for row in hardware_rows:
        if row["status"] == "available":
            try:
                measured.setdefault((row["scenario"], row["workload_size"]), {})[row["metric"]] = float(row["value"])
            except ValueError:
                pass
    for key, counters in measured.items():
        base = throughput_by_key.get(key, {})
        orders = numeric(base, "measured_submissions") or 0.0
        trades = numeric(base, "trades") or 0.0
        cycles = counters.get("cycles")
        instructions = counters.get("instructions")
        derived: list[tuple[str, float | None, str]] = [
            ("cycles_per_submitted_order", cycles / orders if cycles is not None and orders else None, "cycles/order"),
            ("instructions_per_submitted_order", instructions / orders if instructions is not None and orders else None, "instructions/order"),
            ("cycles_per_executed_trade", cycles / trades if cycles is not None and trades else None, "cycles/trade"),
            ("instructions_per_executed_trade", instructions / trades if instructions is not None and trades else None, "instructions/trade"),
            ("instructions_per_cycle", instructions / cycles if instructions is not None and cycles else None, "IPC"),
        ]
        if "branch-misses" in counters and "branches" in counters and counters["branches"]:
            derived.append(("branch_miss_rate", counters["branch-misses"] / counters["branches"] * 100.0, "%"))
        if "cache-misses" in counters and "cache-references" in counters and counters["cache-references"]:
            derived.append(("cache_miss_rate", counters["cache-misses"] / counters["cache-references"] * 100.0, "%"))
        for metric, value, unit in derived:
            if value is not None:
                hardware_rows.append({"scenario": key[0], "workload_size": key[1], "metric": metric,
                                      "value": str(value), "unit": unit, "status": "derived",
                                      "source": "hardware-counters.csv"})

    write_rows(run_dir / "throughput.csv", throughput_rows, BASE_COLUMNS)
    write_rows(run_dir / "latency.csv", latency_rows, BASE_COLUMNS)
    write_rows(run_dir / "memory.csv", memory_rows,
                ["scenario", "workload_size", "mode", "peak_rss_kb", "user_time_s", "system_time_s",
                "elapsed_wall_s", "cpu_percent", "minor_page_faults", "major_page_faults",
                "voluntary_context_switches", "involuntary_context_switches",
                "wall_time_per_successful_operation_ns", "cpu_time_per_successful_operation_ns",
                "source", "status"])
    write_rows(run_dir / "hardware-counters.csv", hardware_rows,
               ["scenario", "workload_size", "metric", "value", "unit", "status", "source"])
    write_text(run_dir / "correctness.txt", "\n".join(correctness) + "\n")
    write_text(run_dir / "commands.txt", "\n".join(commands) + "\n")
    env["arguments"] = vars(ns)
    env["commands"] = commands
    env["status"] = "valid" if all_valid else "invalid"
    write_text(run_dir / "metadata.json", json.dumps(env, indent=2, sort_keys=True) + "\n")

    throughput_series: dict[str, list[tuple[float, float]]] = {}
    latency_series: dict[str, list[tuple[float, float]]] = {}
    for row in throughput_rows:
        size, value = numeric(row, "workload_size"), numeric(row, "median_operations_per_second")
        if size is not None and value is not None:
            throughput_series.setdefault(row["scenario"], []).append((size, value))
    for row in latency_rows:
        size, value = numeric(row, "workload_size"), numeric(row, "p99_latency_ns")
        if size is not None and value is not None:
            latency_series.setdefault(row["scenario"], []).append((size, value))
    svg_plot(plots_dir / "throughput-vs-size.svg", "Matching-engine throughput", "operations", "successful operations/s", throughput_series)
    svg_plot(plots_dir / "p99-latency-vs-size.svg", "Matching-engine p99 latency", "operations", "nanoseconds", latency_series)

    report: list[str] = [f"# Matching-engine benchmark report: `{run_dir.name}`", "", "## Executive summary", "",
                         f"Run status: **{env['status']}**. The benchmark called the in-process `OrderBook` directly; no gRPC, Kafka, database, or backend service was involved.", "",
                         f"Measured scenarios: {len(scenarios)}; sizes: {', '.join(map(str, sizes))}; seed: `{ns.seed}`; repetitions: `{ns.iterations}`; warmups: `{ns.warmup}`.", ""]
    report += ["## Hardware and software environment", "", f"- Host: `{env['host']}`", f"- Kernel: `{env['kernel']}`",
               f"- CPU: `{lscpu.get('Model name', 'unavailable')}`; logical CPUs: `{env['logical_cpus']}`; physical CPUs: `{env['physical_cpus']}`",
               f"- Compiler: `{env['build'].get('compiler_version', 'unavailable')}`", f"- Build type: `{env['build'].get('build_type', 'unknown')}`",
               f"- C++ standard: `{env['build'].get('cxx_standard', 'unknown')}`; flags: `{env['build'].get('cxx_flags', 'unknown')}`"]
    perf_summary = ("events discovered: " + ", ".join(perf_events)) if perf_events else "unavailable: " + perf_reason
    if env["perf"].get("reason"):
        perf_summary += "; collection result: " + env["perf"]["reason"]
    report += [f"- perf: {perf_summary}", ""]
    report += ["## Benchmark methodology", "", "The primary throughput pass measures a fresh `OrderBook` after setup and warmup. Setup is outside the timed region. Each repetition recreates the book. `operations_per_second` counts successful operations only; `attempted_operations_per_second` includes expected failed cancellations. Latency is a separate pass with one `steady_clock` sample around each operation.", ""]
    report += ["## Workload definitions", "", "- `no-match-insertion`: non-crossing buys, with configurable cyclic or uniform price levels.", "- `immediate-matching`: one incoming buy per resting ask.", "- `partial-fills`: incoming quantity one against quantity-two asks.", "- `multi-order-matching`: each incoming order consumes two FIFO asks.", "- `price-level-traversal`: one incoming order sweeps many ask levels.", "- `cancellation`: all cancellations succeed.", "- `cancellation-mixed`: alternating successful and unknown-order cancellations.", "- `mixed-workload`: insert, insert, match, cancel cycle; generated with the recorded seed.", ""]
    report += ["## Correctness validation", "", "```text", *correctness, "```", ""]
    report += ["## Throughput", "", markdown_table(
        ["Scenario", "Size", "Successful ops/s", "Median ops/s", "Trades", "Failed ops", "Final book"],
        ((row["scenario"], row["workload_size"], row["operations_per_second"], row["median_operations_per_second"], row["trades"], row["failed_operations"], row["final_book_size"]) for row in throughput_rows)), ""]
    report += ["## Latency", "", "Percentiles use the dedicated instrumented latency pass. Sample counts are shown; p99.9 is not treated as statistically strong when the sample count is small.", "",
               markdown_table(["Scenario", "Size", "Samples", "p50 ns", "p90 ns", "p95 ns", "p99 ns", "p99.9 ns", "Max ns"],
                              ((row["scenario"], row["workload_size"], row["latency_samples"], row["p50_latency_ns"], row["p90_latency_ns"], row["p95_latency_ns"], row["p99_latency_ns"], row["p99_9_latency_ns"], row["max_latency_ns"]) for row in latency_rows)), ""]
    report += ["## CPU counters", "", "Hardware counters are collected by external `perf stat` runs and are not part of the primary timing. Missing or permission-denied counters remain unavailable.", "", markdown_table(["Scenario", "Size", "Metric", "Value", "Unit", "Status"], ((row["scenario"], row["workload_size"], row["metric"], row["value"], row["unit"], row["status"]) for row in hardware_rows)), ""]
    report += ["## Memory measurements", "", markdown_table(["Scenario", "Size", "Peak RSS KB", "User s", "System s", "CPU %", "Wall ns/op", "CPU ns/op", "Minor faults", "Major faults", "Source"], ((row["scenario"], row["workload_size"], row["peak_rss_kb"], row["user_time_s"], row["system_time_s"], row["cpu_percent"], row["wall_time_per_successful_operation_ns"], row["cpu_time_per_successful_operation_ns"], row["minor_page_faults"], row["major_page_faults"], row["source"]) for row in memory_rows)), ""]
    report += ["## Scaling analysis", "", "See `plots/throughput-vs-size.svg` and `plots/p99-latency-vs-size.svg`. A scaling conclusion is only drawn when more than one workload size was measured; single-size runs are a baseline, not a scaling result.", ""]
    if len(sizes) > 1 and throughput_rows:
        report.append("Measured sizes include more than one point; compare rows in `throughput.csv` and the plotted series rather than comparing workloads with different trade counts as if they were equivalent.")
    else:
        report.append("Only one workload size was requested for this run, so no scaling claim is made.")
    report += ["", "## Measurement limitations", "", "The benchmark includes the existing trade UUID and timestamp construction for matching workloads. The latency pass adds clock and sample-storage overhead. CPU frequency scaling, scheduler noise, virtualization, and perf multiplexing can affect results. The engine is synchronously called through the `OrderBook`; this report is not a claim about transport or multi-thread scaling.", ""]
    report += ["## Reproduction commands", "", "```text", *commands, "```", ""]
    report += ["## Suggested future profiling", "", "Use the saved raw `perf` output for a focused `perf record`/flame-graph pass if counters become available. Candidate investigations are trade construction cost, order/price-level allocation growth, map traversal, and cancellation lookup; these are hypotheses for future measurement, not conclusions from unavailable counters.", ""]
    report += ["## Verified Resume Metrics", ""]
    if env["status"] == "valid" and throughput_rows:
        for row in throughput_rows:
            report.append(f"- Measured `{row['operations_per_second']}` successful operations/s for `{row['scenario']}` at `{row['workload_size']}` operations, seed `{row['seed']}`, with `{row['failed_operations']}` failed operations. Verify in `throughput.csv`; this is a synthetic single-book in-process benchmark on the recorded host.")
        report.append("")
        report.append("Suggested CV wording: `Benchmarked a C++ price-time-priority matching engine under reproducible in-process workloads; report exact throughput and latency from this run, with the host and workload qualification.`")
    else:
        report.append("No resume metric is offered because the run was invalid or produced no validated throughput rows.")
    write_text(run_dir / "report.md", "\n".join(report) + "\n")
    print(run_dir)
    print(f"status={env['status']} throughput_rows={len(throughput_rows)} latency_rows={len(latency_rows)} perf_events={len(perf_events)}")
    return 0 if all_valid else 1


if __name__ == "__main__":
    raise SystemExit(main())
