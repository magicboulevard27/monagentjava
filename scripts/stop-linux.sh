#!/usr/bin/env bash
set -euo pipefail

PID_FILE="${PID_FILE:-/run/monagent/monagent.pid}"

if [[ ! -f "$PID_FILE" ]]; then
    echo "PID file not found: $PID_FILE" >&2
    exit 1
fi

kill -TERM "$(cat "$PID_FILE")"

