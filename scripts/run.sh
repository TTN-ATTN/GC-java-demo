#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-churn}"
HEAP_MB="${2:-128}"

"$(dirname "$0")/demo.sh" "$SCENARIO" "$HEAP_MB"
