#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-all}"
GC_KIND="${2:-g1}"
HEAP_MB="${3:-128}"

#   ./run.sh all g1 128
"$(dirname "$0")/demo.sh" "$SCENARIO" "$GC_KIND" "$HEAP_MB"
