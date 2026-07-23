#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: stop-linux.sh [minimal|observability|ai|full]

Modes:
  minimal         Stop the base stack
  observability   Stop the base stack and observability services
  ai              Stop the base stack and Ollama
  full            Stop the full stack
EOF
}

mode="${1:-minimal}"

case "$mode" in
    -h|--help|help)
        usage
        exit 0
        ;;
    minimal|observability|ai|full)
        docker compose down
        ;;
    *)
        echo "Unknown mode: $mode" >&2
        usage >&2
        exit 1
        ;;
esac

