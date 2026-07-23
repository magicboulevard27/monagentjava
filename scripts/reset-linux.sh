#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: reset-linux.sh [minimal|observability|ai|full]

Modes:
  minimal         Stop the base stack and remove volumes
  observability   Stop the base stack and observability services, then remove volumes
  ai              Stop the base stack and Ollama, then remove volumes
  full            Stop the full stack and remove volumes
EOF
}

mode="${1:-minimal}"

case "$mode" in
    -h|--help|help)
        usage
        exit 0
        ;;
    minimal|observability|ai|full)
        docker compose down -v
        ;;
    *)
        echo "Unknown mode: $mode" >&2
        usage >&2
        exit 1
        ;;
esac
