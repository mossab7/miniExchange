#!/usr/bin/env bash
set -euo pipefail

if [ "${RUN_TESTS:-false}" = "true" ]; then
    echo "=== Starting matching_engine_server for testing ==="
    matching_engine_server &
    SERVER_PID=$!

    # Wait for the gRPC port to be ready
    for i in $(seq 1 30); do
        if ss -tlnp | grep -q ":50051"; then
            break
        fi
        echo "Waiting for server to start... ($i/30)"
        sleep 1
    done

    echo "=== Running mock_client tests ==="
    if mock_client localhost:50051; then
        echo "=== Tests PASSED ==="
        TEST_EXIT=0
    else
        echo "=== Tests FAILED ==="
        TEST_EXIT=1
    fi

    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
    exit "$TEST_EXIT"
fi

exec matching_engine_server "$@"
