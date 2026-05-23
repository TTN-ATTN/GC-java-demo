#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-churn}"
HEAP_MB="${2:-128}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLASSES_DIR="$ROOT_DIR/out/classes"

# Dat moi lan chay vao thu muc rieng de de doc lai log sau demo.
RUN_ID="$(date +%Y%m%d-%H%M%S)-${SCENARIO}-defaultgc-${HEAP_MB}m"
RUN_DIR="$ROOT_DIR/out/$RUN_ID"

mkdir -p "$CLASSES_DIR" "$RUN_DIR"

# Bien dich truc tiep voi javac; khong can Maven/Gradle cho demo nay.
javac -d "$CLASSES_DIR" "$ROOT_DIR/src/MemoryGcDemo.java"
: > "$RUN_DIR/app.log"

JAVA_ARGS=(
    # Heap co dinh giup de quan sat Eden/Old Gen thay doi.
    "-Xms${HEAP_MB}m"
    "-Xmx${HEAP_MB}m"

    # Khong truyen -XX:+Use...GC: JVM tu chon GC mac dinh.
    # Tren may Java 21 cua ban, -Xlog se ghi "Using G1".
    "-Xlog:gc*,gc+heap=debug:file=$RUN_DIR/gc.log:uptime,level,tags"
)

java "${JAVA_ARGS[@]}" \
    -cp "$CLASSES_DIR" \
    MemoryGcDemo "$SCENARIO" \
    > "$RUN_DIR/app.log" 2>&1 &

APP_PID=$!
JSTAT_PID=""
TAIL_PID=""

cleanup() {
    if [[ -n "$JSTAT_PID" ]] && kill -0 "$JSTAT_PID" 2>/dev/null; then
        kill "$JSTAT_PID" 2>/dev/null || true
    fi
    if [[ -n "$TAIL_PID" ]] && kill -0 "$TAIL_PID" 2>/dev/null; then
        kill "$TAIL_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

echo "PID=$APP_PID"
echo "scenario=$SCENARIO gc=JVM-default heap=${HEAP_MB}m"
echo "output=$RUN_DIR"
echo

# Show Java phase markers live on the terminal and also keep app.log.
tail -n +1 -f "$RUN_DIR/app.log" | sed -u 's/^/[app] /' &
TAIL_PID=$!

# jstat samples Heap generation usage and GC counters once per second.
jstat -gcutil "$APP_PID" 1000 | tee "$RUN_DIR/jstat.log" &
JSTAT_PID=$!

wait "$APP_PID" || true
cleanup
trap - EXIT

# Keep only GC lines that are useful to explain during the presentation.
grep -E 'Using|Pause|Full|Concurrent|Heap' "$RUN_DIR/gc.log" > "$RUN_DIR/gc-summary.log" || true

# Count real pause lines and sum their duration in milliseconds.
PAUSE_COUNT="$(
    awk '
        /Pause/ && $NF ~ /ms$/ { count++ }
        END { print count + 0 }
    ' "$RUN_DIR/gc-summary.log"
)"
PAUSE_TOTAL_MS="$(
    awk '
        /Pause/ && $NF ~ /ms$/ {
            for (i = 1; i <= NF; i++) {
                if ($i ~ /ms$/) {
                    gsub("ms", "", $i)
                    total += $i
                }
            }
        }
        END { printf "%.3f", total }
    ' "$RUN_DIR/gc-summary.log"
)"

echo
echo "===== jstat last samples ====="
tail -n 12 "$RUN_DIR/jstat.log" || true

echo
echo "===== GC pause summary ====="
echo "pause_count=$PAUSE_COUNT"
echo "pause_total_ms=$PAUSE_TOTAL_MS"

echo
echo "===== Last GC events ====="
tail -n 30 "$RUN_DIR/gc-summary.log" || true

echo
echo "files:"
echo "  $RUN_DIR/app.log"
echo "  $RUN_DIR/jstat.log"
echo "  $RUN_DIR/gc.log"
echo "  $RUN_DIR/gc-summary.log"
