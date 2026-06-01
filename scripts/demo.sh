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
TAIL_PID=""

cleanup() {
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

app_is_alive() {
    local stat
    stat="$(ps -p "$APP_PID" -o stat= 2>/dev/null || true)"
    [[ -n "$stat" && "$stat" != Z* ]]
}

# Take compact jstat snapshots after important Java log markers.
# This avoids noisy periodic output and maps each row to a demo step.
: > "$RUN_DIR/jstat.log"

snapshot_jstat() {
    local label="$1"
    local raw compact

    if ! app_is_alive; then
        return 0
    fi

    raw="$(jstat -gcutil "$APP_PID" 2>/dev/null || true)"
    compact="$(
        printf '%s\n' "$raw" | awk -v label="$label" '
            NR == 1 {
                for (i = 1; i <= NF; i++) indexOf[$i] = i
                next
            }
            NR == 2 {
                e = indexOf["E"] ? $(indexOf["E"]) : "?"
                o = indexOf["O"] ? $(indexOf["O"]) : "?"
                ygc = indexOf["YGC"] ? $(indexOf["YGC"]) : "?"
                fgc = indexOf["FGC"] ? $(indexOf["FGC"]) : "?"
                gct = indexOf["GCT"] ? $(indexOf["GCT"]) : "?"
                printf "%-34s E=%6s%% O=%6s%% YGC=%s FGC=%s GCT=%ss", label, e, o, ygc, fgc, gct
            }
        '
    )"

    if [[ -n "$compact" ]]; then
        echo "[jstat] $compact" | tee -a "$RUN_DIR/jstat.log"
    fi
}

wait_for_log() {
    local text="$1"
    local waited=0

    while ! grep -qF "$text" "$RUN_DIR/app.log"; do
        if ! app_is_alive; then
            return 1
        fi
        sleep 0.1
        waited=$((waited + 1))
        if (( waited > 300 )); then
            return 1
        fi
    done
}

if [[ "$SCENARIO" == "churn" ]]; then
    wait_for_log "Batch 1:" && snapshot_jstat "after batch 1"
    wait_for_log "Batch 2:" && snapshot_jstat "after batch 2"
    wait_for_log "Batch 3:" && snapshot_jstat "after batch 3"
    wait_for_log "Batch 4:" && snapshot_jstat "after batch 4"
    wait_for_log "object đã unreachable" && snapshot_jstat "after churn.clear()"
    wait_for_log "churn size sau GC" && snapshot_jstat "after System.gc()"
elif [[ "$SCENARIO" == "leak" ]]; then
    wait_for_log "Batch 1:" && snapshot_jstat "after leak batch 1"
    wait_for_log "Batch 2:" && snapshot_jstat "after leak batch 2"
    wait_for_log "Batch 3:" && snapshot_jstat "after leak batch 3"
    wait_for_log "Batch 4:" && snapshot_jstat "after leak batch 4"
    wait_for_log "LEAK vẫn giữ reference" && snapshot_jstat "before GC, references kept"
    wait_for_log "Quan sát:" && snapshot_jstat "after GC, references kept"
elif [[ "$SCENARIO" == "oom" ]]; then
    batch=1
    while wait_for_log "Batch $batch:"; do
        snapshot_jstat "after oom batch $batch"
        batch=$((batch + 1))
    done
fi

wait "$APP_PID" || true
cleanup
trap - EXIT

# Keep only GC lines that are useful to explain during the presentation.
grep -E 'Using|Pause|Full|Concurrent|Heap' "$RUN_DIR/gc.log" > "$RUN_DIR/gc-summary.log" || true

echo
echo "===== GC events compact ====="
awk '
    /Pause/ && $NF ~ /ms$/ { events[++count] = $0 }
    END {
        for (i = 1; i <= count; i++) {
            if (count <= 12 || i <= 6 || i > count - 6) {
                print events[i]
            } else if (i == 7) {
                printf "... %d GC pause lines omitted ...\n", count - 12
            }
        }
    }
' "$RUN_DIR/gc-summary.log" || true

echo
echo "files:"
echo "  $RUN_DIR/app.log"
echo "  $RUN_DIR/jstat.log"
echo "  $RUN_DIR/gc.log"
echo "  $RUN_DIR/gc-summary.log"
