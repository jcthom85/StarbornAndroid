#!/usr/bin/env bash
set -euo pipefail

# Captures a FrameTimeline Perfetto trace on a connected device.
# Usage: ./scripts/capture_frametimeline.sh [duration_seconds] [output_dir]

: "${ADB_BIN:=adb}"
DURATION="${1:-60}"
OUTPUT_DIR="${2:-scripts/perf/frametimeline}"
TRACE_PATH_ON_DEVICE="/sdcard/starborn_frametimeline.pftrace"

if ! command -v "$ADB_BIN" >/dev/null 2>&1; then
  echo "adb not found. Set ADB_BIN or install platform-tools." >&2
  exit 1
fi

ONLINE_DEVICES=$("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}')
if [ -z "$ONLINE_DEVICES" ]; then
  echo "No connected devices detected. Connect a Pixel device and retry." >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
timestamp="$(date +%Y%m%d-%H%M%S)"
local_trace="$OUTPUT_DIR/frametimeline-${timestamp}.pftrace"

echo "Capturing FrameTimeline trace for ${DURATION}s..."
"$ADB_BIN" shell perfetto -o "$TRACE_PATH_ON_DEVICE" -t "${DURATION}s" \
  sched freq idle am wm gfx view binder_driver hal dalvik camera input res memory sysui frame_timeline

echo "Pulling trace to $local_trace"
"$ADB_BIN" pull "$TRACE_PATH_ON_DEVICE" "$local_trace" >/dev/null
echo "Trace saved to $local_trace"
