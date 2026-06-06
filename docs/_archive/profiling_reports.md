# Profiling Reports

Use this log to capture raw data + findings for each profiling pass listed in `docs/perf_capture_playbook.md`.

## FrameTimeline (Device / Date)
- Device: _Pixel 7 (scheduled once hardware is back on the bench)_
- Trace: _Awaiting capture_
- Avg GPU frame time: _TBD_
- SF Jank % / App Jank %: _TBD_
- Notes: _Use `./scripts/capture_frametimeline.sh` once hardware is available (script added Nov 2025)._
- 2025-02-15: CLI session confirmed no attached device / adb access, so capture deferred. Next on-device session should follow Perf Capture Playbook §1 with exploration → dialogue → combat path and log trace path under `scripts/perf/frametimeline/`.

## GPU Counters
- Device / Build: _Pixel 7 / latest debug (scheduled)_
- Trace: _TBD_
- Problem shaders or fallback notes: _To be filled after AGI session._
- 2025-02-15: Pending AGI workstation availability; enable `debug.hwui.profile` + `debug.hwui.render_dirty_regions` prior to capture per playbook.

## Memory Soak
- Scenario: _30 min exploration/combat loop_
- Duration: _Scheduled alongside FrameTimeline run_
- RSS Samples: _Pending (Perfetto + `dumpsys meminfo`)_
- Observations: _Awaiting soak run once hardware session starts._
- 2025-02-15: Need macro or manual driver to alternate exploration/combat every ~5 minutes; log path `scripts/perf/meminfo.log` reserved.

## Audio Headroom
- Mix peak: _Pending_
- Ducking release time: _Pending_
- Trace: _Perfetto audio run queued with FrameTimeline capture._
- 2025-02-15: Command confirmed via `./scripts/capture_audio_ducking.sh`; blocked until adb-capable host is available. Pair run with dialogue+combat ducking scenario and capture AudioFlinger logs for peaks.

## Load / Stress
- `testDebugUnitTest`: _Executed locally as part of perf prep (see CI run #pending)_  
- Asset stress script: _`./gradlew runAssetIntegrity` now available (passes locally)._
- Observations: _Use `runAssetIntegrity` after editing `events.json`, cinematics, or quests to catch missing references early._
