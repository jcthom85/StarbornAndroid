# Environmental effects: implementation and review

Implemented October 8, 2026 for Android and desktop exploration and room-backed combat.

## What ships

- One shared JSON catalog, resolver, simulation and Compose drawing source for Android/JVM.
- 33 presets: all 12 legacy names plus authored dust, rain/storm/snow, fog/gas/smoke, vent mist, pressure release, sparks/arcs, embers/ash/spores, gravity drift, resonance/starfall, pulse/flicker/scanner/light shaft.
- Full viewport atmosphere; image-relative source coordinates. Desktop portrait artwork and stationary reflected sides retain their existing composition.
- 114 explicit room profiles. All 91 resonance rooms have reviewed restrained intensities; all 12 steam rooms have source positions; five Astra rooms have equipment/living-space treatments.
- All 477 rooms are listed in [ROOM_MATRIX.csv](ROOM_MATRIX.csv). The other 229 originally quiet rooms remain deliberately quiet; authored weather is retained elsewhere through compatibility presets.
- Existing room descriptions, quests, actions, state, weather fields and artwork are preserved. A byte-backed JSON comparison against HEAD verifies all non-effect room data is unchanged.
- Full/Reduced/Off persisted through both existing settings stores. Reduced uses half the particle budget and 35% movement speed, suppressing bright accents/field movement. Existing flash, shake and high-contrast preferences also apply.
- State/milestone predicates, combat participation, region clipping and dark visibility. Combat contribution is 0.65; dim/emissive dark contributions are 0.15/0.35; hidden contributes zero.
- Pooled seeded particles, 350ms binding fades, clamped delta, pause on foreground loss and blocking overlays, bounded two-scene navigation memory. Transition scenes share particle, jet and burst budgets. Image sources follow the artwork; viewport atmosphere crossfades without directional translation.
- Missing/unsupported catalogs retain built-in legacy compatibility. Invalid bindings skip with diagnostics. Missing source-frame assets use a procedural fallback.

## Resonance

The old effect drew three repeating horizontal sine curves. The new effect uses sparse cyan/violet suspended motes with slow shimmer and two very faint drifting vertical light veils. It has no global image warp or screen-wide wave pattern. Most outdoor passages and quiet rooms use 0.12?0.23; selected pictured tears/cores use 0.32. The Campfire uses 0.18. Reduced/disable-shake freezes the veils' movement, and dark/combat attenuation remains independent.

## Authoring preview

Desktop: press F8 during exploration. Android debug: Field Menu ? Settings ? Environmental preview; the Weather Lab title also opens it.

The preview uses the production resolver and renderer. Choose authored effects or a preset, intensity, scene/viewport space, Android/Desktop/Compact view, darkness, combat, accessibility and state/milestone fixtures. Tap artwork to place up to eight emitters or two region corners; adjust the last source's angle. Pause/scrub or replay at a stable seed. Particle/jet/burst and live simulation CPU diagnostics are shown. Copy a JSON binding for review; the tool never saves room data automatically.

Example, in the existing room object:

```json
"environmental_effects": [
  {
    "id": "lamp_dust",
    "preset": "interior.dust_motes",
    "intensity": 0.1,
    "space": "scene",
    "requires_state": { "light_on": true }
  }
]
```

Coordinates are normalized against the original artwork. `angle_degrees` uses screen coordinates: -90 points up, 90 down, 0 right. Source-bound steam/sparks require emitters. `null`/absent effects use the original weather; `[]` explicitly disables it. Maximum four bindings; ambient priority precedes accents, authored order breaks ties. Missing required state keys do not match false. No effects are inferred from room environment names.

## Verification

- 21 focused runtime tests cover all 477 definitions, all 48 original steam frames, all legacy families in exploration/combat, compatibility, state/milestone matching, invalid input, quality/accessibility, darkness, geometry, stable seeds/slots, resize, fades, caps, overlapping scenes, and repeated navigation.
- Desktop preference persistence after store recreation passes.
- Two Android emulator instrumentation tests pass: preference persistence and 25 real taps through decorative effects, with native captures of the 12 legacy/shared families. An explicit pixel check confirms scene switches reach the renderer.
- Android debug app and desktop Kotlin builds compile. Portable packaging also verifies desktop assets against original Android source bytes.
- Desktop gameplay screenshots: Nova/Jed bunks, Lift Shaft, Night Market, Toxic Pocket, Campfire, Astra Simulation Deck; combat gas; authoring preview.
- Fixed-seed resonance captures at 1440?900, 1280?720, 900?650, 2560?1080 and Android-style 405?720. All 33 presets have three-intensity contact sheets and short sampled GIF sequences. Burst captures seek the first scheduled event so an inactive interval cannot hide the effect.

Artifacts live in `desktopApp/build/reports/environmental-effects/`; open `index.html` for the motion gallery. Native Android comparisons are under `android-latest/`. Campaign artwork contact sheets are under `tmp/environment-review/`.

### Measured desktop CPU probe

1440?900 offscreen Compose scene, 180 samples after warm-up. These numbers measure render submission/copy and simulation on this Windows host; they do not establish physical Android or native-window GPU frame times.

| Scene | Off p95 | Full p95 | Change |
|---|---:|---:|---:|
| Jed's Bunk | 14.095ms | 14.332ms | +1.7% |
| Night Market | 13.828ms | 14.571ms | +5.4% |
| Toxic Pocket | 13.894ms | 14.786ms | +6.4% |
| Campfire | 13.834ms | 13.864ms | +0.2% |
| Lift Shaft | 13.661ms | 14.205ms | +4.0% |

All sampled render p95 changes are below 10% after tuning rain density and resonance overdraw. Full simulation p95 is 0.0004?0.0031ms in these fixtures. The probe observes roughly 90?105 allocated bytes per simulation frame, including its measurement bookkeeping and iterator allocations; particle arrays/slots stay stable and navigation/layer counts stay bounded. The Off production path returns before composing the renderer; the benchmark's empty-engine probe itself still has harness overhead.

### Remaining verification and deliberate later scope

Physical Android devices and the lowest supported device were unavailable. Their GPU frame-time, battery and sustained-memory profiling remains a hardware acceptance task. Native game playtesting should also cover repeated room/hub/combat transitions, long sessions and high-density resizing; fixed captures cannot certify those experiences.

The repository's full Android instrumentation compilation currently has pre-existing references to removed UI APIs (`HubMapLayouts`, older inline-action helpers). The effects tests were compiled in isolation using `android-test.init.gradle`; this report does not claim the full old suite passes.

True image distortion, environmental audio, and hub/cinematic environmental authoring remain the plan's explicit later scope. The initial library is implemented; additional room layers should be selected through visual review rather than automatically filling quiet rooms.

## Reproduce

Use the existing Android Studio JBR for Gradle. From the repository root:

```powershell
.\gradlew.bat :gameRuntime:test --tests '*EnvironmentalEffectsTest'
.\gradlew.bat :desktopApp:test --tests '*DesktopEnvironmentalEffectsTest'
.\gradlew.bat -I tools/environmental-effects/render-review.init.gradle :desktopApp:renderEnvironmentReview :desktopApp:benchmarkEnvironmentReview
python tools/environmental-effects/make-gallery.py
.\gradlew.bat -I tools/environmental-effects/android-test.init.gradle :app:assembleDebug :app:assembleDebugAndroidTest
```

For native Android evidence, install both APKs on a disposable emulator and run:

```powershell
adb shell am instrument -w -e class com.example.starborn.EnvironmentalEffectsInstrumentationTest com.junewiregames.starborn.prealpha.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.junewiregames.starborn.prealpha/files/environmental-effects desktopApp/build/reports/environmental-effects/android-latest
```

The scoped test-init script is only for the new fixture and does not alter the normal test suite. The renderer review init adds diagnostic mains for that invocation; do not use it for packaging.

## Builds

Desktop regular folder: `desktopApp/build/distributions/Starborn-Windows-portable/Starborn/Starborn.exe`.
Android debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

No save migration is needed. This work is not automatically committed or pushed.
