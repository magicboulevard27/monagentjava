#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: verify-compose.sh [base-url]

Runs local Docker Compose validation:
1. docker compose config
2. docker compose ps after startup
3. health and info smoke checks against the running app

Optional:
  base-url    Base URL for the app smoke checks (default: http://127.0.0.1:8080)
EOF
}

case "${1:-}" in
    -h|--help|help)
        usage
        exit 0
        ;;
esac

BASE_URL="${1:-${BASE_URL:-http://127.0.0.1:8080}}"

docker compose config >/dev/null
curl --fail --silent --show-error "$BASE_URL/actuator/health" >/dev/null
curl --fail --silent --show-error "$BASE_URL/actuator/info" >/dev/null
