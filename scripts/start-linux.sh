#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: start-linux.sh [minimal|observability|ai|full]

Modes:
  minimal         Build the app and start the base stack
  observability   Start the base stack with observability services
  ai              Start the base stack with Ollama
  full            Start observability and Ollama together
EOF
}

mode="${1:-minimal}"

case "$mode" in
    -h|--help|help)
        usage
        exit 0
        ;;
    minimal)
        docker compose up --build
        ;;
    observability)
        docker compose --profile observability up --build
        ;;
    ai)
        docker compose --profile ai up --build
        ;;
    full)
        docker compose --profile observability --profile ai up --build
        ;;
    *)
        echo "Unknown mode: $mode" >&2
        usage >&2
        exit 1
        ;;
esac

