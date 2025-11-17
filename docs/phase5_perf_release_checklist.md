# Phase 5 – Perf & Release Checklist

_Living document for profiling passes and release automation tasks._

## Profiling & Performance
- **FrameTimeline**: Capture 60‑second traces on Pixel 6 and Pixel 7 hardware across exploration, dialogue, and combat screens. Use `adb shell am trace-start` or Perfetto UI with SurfaceFlinger/FrameTimeline slices to verify jank < 5% and GPU deadline misses < 1%.
- **GPU / Shader validation**: Enable AGSL debug layers, capture GPU counter profiles (via `adb shell dumpsys SurfaceFlinger --display-id` or AGI) while weather/VFX overlays are active, and document shader fallback paths.
- **Memory soak**: Run a 30‑minute exploration/combat loop with tutorials, cinematics, and inventory screens; monitor heap via `adb shell dumpsys meminfo com.example.starborn` and Perfetto Heap Profiler to ensure RSS < 600 MB and no sustained growth.
- **Audio headroom**: Record a systrace/Perfetto session covering music + dialogue + combat ducking. Verify mixer peaks stay under ‑3 dBFS and ducking restores within 250 ms post‑cinematic.
- **Load testing**: Execute `./gradlew :app:testDebugUnitTest` plus a scripted JSON stress test (load entire asset set twice) to catch serialization regressions before packaging.
- Detailed command sequences live in `docs/perf_capture_playbook.md`; update that file with timestamps + trace file paths as captures complete.

## QA Automation
- **Instrumentation smoke**: Run `./gradlew :app:connectedDebugAndroidTest` on at least one API 33+ device. Ensure `NarrativeSystemsInstrumentedTest` passes (tutorial + Jed reward + market objective loops).
- **Static analysis**: Gate builds on `./gradlew :app:lintDebug :app:detekt` (detekt optional) and JVM suite (`testDebugUnitTest`).
- **CI hooks**: Publish Gradle tasks and required env vars (`ANDROID_HOME`, signing keystore path) in the pipeline docs; capture emulator matrix (API 30, 33, 34).

## Release Automation
- **Signing assets**: Store release keystore + Play Console JSON in encrypted vault; mirror config in `gradle.properties` (`RELEASE_STORE_FILE`, etc.) and reference from `app/build.gradle.kts`.
- **Bundle generation**: Add `./gradlew :app:bundleRelease` to CI with `--scan` for reproducibility. Archive `.aab` artifacts plus mapping files per build.
- **Play tracks**: Script fastlane/Gradle Play Publisher flow with beta & internal tracks; document promotion checklist (QA sign-off, regression matrix, Perfetto captures).
- **Versioning**: Define semantic version steps (major/minor/patch) and automate `versionCode` bump script tied to Git tags.
- **Crash/analytics hooks**: Validate Crashlytics/analytics toggles in release variant and document how to flip staging endpoints before submission.
- **CI visibility**: The `.github/workflows/android-ci.yml` workflow now runs lint/unit tests and spins up an emulator for `connectedDebugAndroidTest`; ensure failures gate merges.
