#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"

echo "Checking health..."
curl --silent --show-error "$BASE_URL/actuator/health"
echo
echo "Checking info..."
curl --silent --show-error "$BASE_URL/actuator/info"
echo

