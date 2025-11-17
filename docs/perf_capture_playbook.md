# Perf Capture Playbook

This expands on the Phase 5 checklist with concrete command sequences. Run captures on production-like hardware (Pixel 6/7) unless noted.

## 1. FrameTimeline / Jank
1. Connect device via USB with developer options enabled.
2. Run the helper script (defaults to 60s and writes to `scripts/perf/frametimeline/`):
   ```bash
   ./scripts/capture_frametimeline.sh 60
   ```
3. Drive the app through exploration → dialogue → combat while recording.
4. Open the pulled `.pftrace` in Perfetto and mark jank % and missed GPU deadlines.
5. Document stats (SF VSYNC deadline %, app frame drops) in `docs/profiling_reports.md`.

## 2. GPU / Shader Validation
1. Enable AGSL validator:
   ```bash
   adb shell setprop debug.hwui.profile visual_bars
   adb shell setprop debug.hwui.render_dirty_regions false
   ```
2. Capture AGI/Perfetto GPU counters while weather/VFX overlays play:
   ```bash
   adb shell am trace-start gfx gpu frequency sched freq idle -o /sdcard/starborn_gpu.perfetto
   ```
3. Note shader fallbacks or warnings surfaced in Logcat (`adb logcat '*:E' hwui:*`).

## 3. Memory Soak
1. Launch the app, load autosave, and script a 30-minute loop (e.g., via `adb shell input` commands or a macro).
2. Every five minutes, snapshot memory:
   ```bash
   adb shell dumpsys meminfo com.example.starborn >> scripts/perf/meminfo.log
   ```
3. After 30 minutes, pull the log and chart RSS/PSS trends; open a bug if RSS > 600 MB or growth > 10% between samples.

## 4. Audio Headroom
1. Use the helper script (defaults to 30 s) to record the ducking interaction while you trigger dialogue/combat cues:
   ```bash
   ./scripts/capture_audio_ducking.sh 30
   ```
   Traces land under `scripts/perf/audio/`.
2. Verify mixer peaks in AudioFlinger logs stay under ‑3 dBFS and that ducking releases < 250 ms after cinematics end.

## 5. Load / Data Stress
1. Run JVM suite + asset stress:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:runAssetIntegrity --rerun-tasks
   ```
   `runAssetIntegrity` executes the `DataIntegrityTest` subset (cinematics/tutorials/milestones/timeline references) without rerunning the whole unit suite.
2. Optional Python check:
   ```bash
   python tools/asset_stress.py --repeat 2
   ```

## 6. Release Automation Dry Run
1. Execute new CI workflow locally:
   ```bash
   act workflow_run --job unit-tests
   ```
2. Build release bundle with placeholder signing:
   ```bash
   ./gradlew :app:bundleRelease -Pandroid.injected.signing.store.file=/tmp/debug.keystore
   ```
3. Archive `.aab` + mapping file under `build/artifacts/`.
