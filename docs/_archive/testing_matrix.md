# Testing Matrix & Execution Notes

## JVM
- `./gradlew lintDebug testDebugUnitTest`  
  - Covers asset validation, quest/milestone persistence, crafting/event engines, combat math, and dialogue triggers.  
  - Always run after JSON or domain-layer edits.
- `./gradlew runAssetIntegrity`  
  - Fast path for schema/integrity checks (`DataIntegrityTest`).  
  - Use after editing events, cinematics, tutorial scripts, or milestone metadata.

## Instrumentation (Device/Emulator)
- `./gradlew connectedDebugAndroidTest`
  - Exercises `NarrativeSystemsInstrumentedTest`:
    1. `tinkering_screen_entered` → tutorial overlay → `mine_generator_restore` cinematic + exit unlock.
    2. `talk_to` Jed reward loop → component grants + `scene_jed_tinkering_prompt`.
    3. `enter_room` market objective → quest task + milestone updates.
    4. `tinkering_screen_entered` (system tutorial) → verifies the Compose prompt queue shows the Jed tutorial script and marks it seen.
    5. `fishing_success` (player action) → asserts the fishing basics tutorial plays and `ms_fishing_first_catch` is set.
    6. `encounter_defeat` (triggered via `EventPayload`) → ensures defeat tutorial cinematics set `ms_tutorial_encounter_defeat`.
    7. `encounter_retreat` (triggered via `EventPayload`) → ensures retreat tutorials set `ms_tutorial_encounter_retreat`.
    8. Inventory menu overlay (via `ExplorationViewModel`) → verifies selecting the Inventory tab queues the `bag_basics` tutorial script and marks it completed.
  - Run on API 33+ (Pixel hardware preferred). For GH Actions parity, mirror `reactivecircus/android-emulator-runner` settings: `api-level: 33`, `arch: x86_64`.
  - Capture logs: `adb logcat -d > instrumentation.log` for build artifacts, or use `scripts/run_instrumentation.sh` to automate the run + log archive under `scripts/logs/`.

## Perfetto / Profiling
- Follow `docs/perf_capture_playbook.md` for FrameTimeline, GPU counters, memory soak, and audio headroom captures.
- Log findings (device, date, trace path, metrics) in `docs/profiling_reports.md`.

## Release Readiness
- CI (`.github/workflows/android-ci.yml`) gates PRs on lint/unit/instrumentation checks. Local runs should match:
  1. `./gradlew lintDebug testDebugUnitTest runAssetIntegrity assembleDebugAndroidTest`
  2. `./gradlew connectedDebugAndroidTest` (device/emulator)
  3. `./gradlew bundleRelease` (once signing props configured)
