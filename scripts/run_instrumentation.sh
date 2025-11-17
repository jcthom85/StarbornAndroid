#!/usr/bin/env bash
set -euo pipefail

# Runs the instrumentation smoke suite on a connected device/emulator.
# Mirrors the emulator settings used in CI (API 33+) but works with any attached target.

APP_ID="com.example.starborn"
ADB_BIN="${ADB_BIN:-adb}"

if ! command -v "$ADB_BIN" >/dev/null 2>&1; then
  echo "adb not found in PATH. Install platform-tools or point ADB_BIN to your adb binary." >&2
  exit 1
fi

echo "Verifying connected devices..."
mapfile -t ONLINE_DEVICES < <("$ADB_BIN" devices | tail -n +2 | tr -d '\r' | awk '$2=="device" {print $1}')
if [ ${#ONLINE_DEVICES[@]} -eq 0 ]; then
  echo "No online devices detected. Start an emulator (API 33+) or connect hardware and try again." >&2
  exit 1
fi

if [ -n "${TARGET_SERIAL:-}" ]; then
  if ! printf '%s\n' "${ONLINE_DEVICES[@]}" | grep -qx "$TARGET_SERIAL"; then
    echo "Specified TARGET_SERIAL '$TARGET_SERIAL' not found among connected devices." >&2
    exit 1
  fi
  SELECTED_DEVICE="$TARGET_SERIAL"
else
  SELECTED_DEVICE=""
  for dev in "${ONLINE_DEVICES[@]}"; do
    if [[ "$dev" == emulator-* ]]; then
      SELECTED_DEVICE="$dev"
      break
    fi
  done
  if [ -z "$SELECTED_DEVICE" ]; then
    SELECTED_DEVICE="${ONLINE_DEVICES[0]}"
  fi
fi

echo "Using device: $SELECTED_DEVICE"
export ANDROID_SERIAL="$SELECTED_DEVICE"

echo "Building and running connectedDebugAndroidTest..."
./gradlew connectedDebugAndroidTest

echo "Collecting instrumentation logs..."
timestamp="$(date +%Y%m%d-%H%M%S)"
log_dir="scripts/logs"
mkdir -p "$log_dir"
log_path="$log_dir/instrumentation-${SELECTED_DEVICE}-${timestamp}.log"
"$ADB_BIN" -s "$SELECTED_DEVICE" logcat -d > "$log_path"
echo "ADB log saved to $log_path"

echo "To attach Perfetto/FrameTimeline traces, see docs/perf_capture_playbook.md."
