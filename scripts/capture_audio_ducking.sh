#!/usr/bin/env bash
set -euo pipefail

# Captures a Perfetto trace focused on audio ducking/mixer behavior.
# Usage: ./scripts/capture_audio_ducking.sh [duration_seconds] [output_dir]

: "${ADB_BIN:=adb}"
DURATION="${1:-30}"
OUTPUT_DIR="${2:-scripts/perf/audio}"
TRACE_PATH_ON_DEVICE="/sdcard/starborn_audio.pftrace"

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
local_trace="$OUTPUT_DIR/audio-${timestamp}.pftrace"

echo "Capturing audio Perfetto trace for ${DURATION}s..."
"$ADB_BIN" shell perfetto -o "$TRACE_PATH_ON_DEVICE" -t "${DURATION}s" \
  sched freq audio binder_driver hal

echo "Pulling trace to $local_trace"
"$ADB_BIN" pull "$TRACE_PATH_ON_DEVICE" "$local_trace" >/dev/null
echo "Trace saved to $local_trace"
