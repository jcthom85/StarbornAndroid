# Starborn Kotlin Port Roadmap

## Phase 0 – Project Setup (Week 0)
- Split packages by layer (`core`, `domain`, `data`, `ui`, `feature.*`) within `app` module; enable Hilt, Moshi, Kotlin serialization.
- Migrate JSON assets/fonts/audio into `app/src/main/assets` & `res` with normalized naming; add validation script.
- Establish shared `GameSessionStore` skeleton and baseline unit-test harness.

## Phase 1 – World Exploration Core (Weeks 1-3)
- Implement `WorldRepository` to load worlds/hubs/nodes/rooms with blocked exit logic.
- Port inventory (`Item`, `Bag`) and room state tracking (actions, state flags, enemy placement).
- Build Compose navigation shell with layered rendering: background, room hotspots, HUD, dialogue stub.
- Implement `DialogueService` and Compose `DialogueOverlay` using existing JSON; support triggers → EventEngine stub.
- Deliver walking flow: load save → hub → node map → room transitions with dialogue popups and inventory pickup.

## Phase 2 – Encounter Loop & Exploration Shell (Weeks 3-6)
- **2A Combat polish**: add turn timeline/FX overlays, multi-target selection, ally skills, status badges, and scripted defeat/retreat flows. Extend tests to cover DOT/buffs/loot. 
- **2B Exploration overlays**: recreate direction markers, hotspot cards, dialogue choices, quest log panel, tutorial/milestone popups, and narration banners. Ensure room state toggles unlock exits and spawn encounters. 
- **2C Progression kickoff**: design level-up ribbon/skill unlock overlays, storyboard hub/shop UX, and scope cooking/first-aid minigames alongside save-slot UI entry points.
- **Data validation**: lock JSON schema for enemies/skills/dialogue choices; add regression tests for encounter + event chains.

## Phase 3 – Systems & Overlays (Weeks 6-9)
- Quest/Milestone/Tutorial managers with Compose popups; connect to Dialogue/Event triggers.
- Crafting, Cooking, Tinkering, Fishing minigames: implement logic services and Compose screens.
- Hub/Shop interfaces, radial menu, inventory management, and system tutorials.
- AudioRouter implementation (music layers, ambient loops, combat stingers).

## Phase 4 – Visual & Audio Fidelity (Weeks 9-11)
- Recreate shaders/VFX (vignette, weather, damage numbers, timeline) using AGSL and custom Canvas composables.
- Theme & environment transitions (palettes, overlays), responsive layout polish, accessibility checks.
- Voice/SFX mixing, crossfades, and runtime audio settings menu.

## Phase 5 – Persistence, QA, Packaging (Weeks 11-12+)
- Save/Load parity with autosave slots, migration path from legacy JSON. Add regression tests (for example `GameSessionPersistenceTest`) covering tutorials/unlocked exits and run nightly.
- Asset/data validation: JVM checks ensure every `play_cinematic` and `unlock_exit` reference has a matching asset before shipping.
- Device instrumentation smoke tests: `app/src/androidTest/java/com/example/starborn/NarrativeSystemsInstrumentedTest.kt` drives the tutorial → milestone → cinematic loop off real JSON; extend coverage to dialogue-triggered rewards and combat transitions.
- End-to-end UI tests, load testing for large JSON, and performance profiling (FrameTimeline, CPU/memory, audio underruns) captured in a standing checklist before beta.
- Performance gameplan: capture FrameTimeline + GPU counters on Pixel 6/7, log heap usage after 30-minute exploration/combat soak, and validate audio mixer headroom (battle ducking, narration VO) using systrace captures.
- Release readiness: document QA hand-off steps (instrumentation pass, JVM suite, lint), prep signing keystore + Play Console track scripts, and add `./gradlew bundleRelease` smoke invocation to CI once secrets are wired.
- Profiling/release checklist: see `docs/phase5_perf_release_checklist.md` for step-by-step capture + automation requirements that must pass before beta.
- CI: `.github/workflows/android-ci.yml` runs lint/unit tests and boots an API 33 emulator for `connectedDebugAndroidTest` so device smoke coverage gates every PR.
- Release automation: document signing keys, wire `./gradlew bundleRelease`/`publishBundle` tasks, and capture QA gating in a Play Console handoff note.
- Google Play signing setup, beta build generation, analytics/debug tooling.

## Backlog & Risks
- Shader parity: verify AGSL coverage for Kivy fragment shaders; may require fallback effect pipeline.
- Asset memory footprint: confirm Compose + large PNG pipeline performance; consider texture atlas generation.
- Minigame physics/audio sync: ensure frame timing under 60fps; might need bespoke render loops.
- Tooling integration: decide whether to port Python content generators or embed them as external steps.

## Immediate Next Sprint Backlog
1. Ship quest/tutorial/milestone managers with queued overlays and dialogue triggers.
2. Implement fishing minigame UI + logic using timing preset infrastructure; author failure tutorials for existing crafting stations.
3. Build radial service menu and hub hotspot art, wiring shop smalltalk branches and rotating stock definitions.
4. Extend `AudioRouter` with ambience/music layering, fade helpers, and author cue metadata.
5. Expand JVM test suite (quest/milestone migration, dialogue branching) and stand up CI task for `lint` + `test`.
