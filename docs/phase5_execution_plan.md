# Phase 5 Execution Plan

Tracking sheet for the remaining Kotlin port work. Tasks are grouped by the “next steps” backlog and reference the files/assets they touch so ownership hand-offs are clear. Keep this doc updated as features land.

## 1. Narrative Data Parity
- [ ] **Tutorial script import sweep** – Diff `Starborn_Python/tutorial_manager.py` + related JSON against `app/src/main/assets/tutorial_scripts.json`; migrate missing late-game scripts (ally tutorials, hub hints). Update `DataIntegrityTest` to assert every `system_tutorial`/`tutorial_id` reference exists.
  - _2025-02-21_: Added a `DataIntegrityTest` check so quest `tutorial_id` values must correspond to entries in `tutorial_scripts.json`.
  - _2025-02-21_: Added a placeholder `bag_basics` script entry so inventory tutorials exist in the Kotlin asset catalog.
  - _2025-02-21_: Inventory menu overlay now queues the `bag_basics` tutorial script when the player opens or selects the Inventory tab.
  - _2025-02-21_: Unit test `openingInventoryTabQueuesBagTutorial` guards this behavior inside `ExplorationViewModelTest`.
  - _2025-02-21_: Instrumentation now verifies the Inventory overlay emits the `bag_basics` tutorial via `NarrativeSystemsInstrumentedTest`.
  - _2025-02-21_: `TutorialRuntimeManagerTest.playScriptQueuesStepsAndInvokesCompletion` stubs a script to ensure multi-step tutorials queue correctly and invoke completion callbacks.
  - _2025-02-22_: Imported the Nova's House blackout tutorials (`scene_lights_out_*`) and wired them through quest `tutorial_id`s plus the intro event.
  - _2025-02-22_: Extended `DataIntegrityTest` to assert every `system_tutorial` action references a valid script (covers new `scene_scrap_run_*` + `scene_fixers_favor_*` entries).
  - _2025-02-22_: Added ally/hub scripts (`scene_ollie_recruitment`, `scene_market_*`) tied to `talk_to_jed`/`gather_broken_gear` events so recruitment + journal hints are data-driven.
  - _2025-02-24_: Awaiting the remaining ally/hub script definitions from the Python data set (late-hub journal cards, additional recruitment beats) before continuing the import; blocked items tracked in `docs/event_quest_tutorial_roadmap.md`.
- [ ] **Quest/milestone backlog** – Re-import ally/journal quests from `Starborn_Python/quests.json` and ensure `milestones.json` unlock metadata matches (`unlock_exits`, `on_event` effects). Extend `QuestRuntimeManager` instrumentation paths once data lands.
  - _2025-02-22_: Added the `lights_out` quest, backup fuse item, and associated milestones/events so Nova's blackout intro mirrors the Python flow.
  - _2025-02-22_: Added the `scrap_run` quest (Tyson's ration errand), related tutorials, milestones, and room interactions so the early hub errands are data-driven.
  - _2025-02-22_: Added the `fixers_favor` quest plus tinkering tutorials/milestones, and wired Jed's bench events so the repair tutorial is tracked end-to-end.
- [ ] **Hotspot metadata & art hooks** – Audit Python room definitions (`Starborn_Python/rooms.json`, `game.py` hotspots) and backfill art-authored hotspot descriptors into Kotlin assets so blocked exits + service trays don’t rely on Compose placeholders.

## 2. Event / Dialogue Polish
- [ ] **Cinematic staging parity** – Bring over portrait alignment + VO sequencing cues referenced in Python cinematics (`Starborn_Python/cinematics.py`, `cinematics.json`) so `CinematicCoordinator` scenes animate correctly. Requires updating `CinematicScene` assets and Compose overlay timings.
- [ ] **Advanced trigger coverage** – Port the remaining `EventManager` trigger types (`item_given`, ally recruitment, chained boss encounters). Wire Kotlin `EventPayload`/`EventHooks` so late-game flows fire without bespoke Compose glue.
  - _2025-02-21_: Added a unit test (`EventManagerTest.audioLayerActionDelegatesToHook`) verifying `audio_layer` actions fire the router hook with the expected payload.
  - _2025-02-21_: Added `EventManagerTest.systemTutorialActionInvokesHookWithContext` to ensure `system_tutorial` actions call the hook with scene/context and completion callbacks.
- [ ] **Quest breadcrumbs & hub journal cards** – Implement the UI surfaces called out in `docs/remaining_parity.md` so quests surface per-node breadcrumbs, ally summaries, and tutorial callouts inside Compose overlays.

## 3. Audio / Visual Fidelity
- [ ] **AudioRouter Layer API** – Finish the plan in `docs/audio_mixing_plan.md`: add layer fade helpers, hook runtime sliders to `AudioRouter`, and validate `audio_layer` event actions via tests.
- [ ] **Shader/FX fallback audit** – Document and implement AGSL fallbacks for damage numbers, blocked-exit cinematics, and weather overlays. Capture screenshots + frame metrics for accessibility sizing.

## 4. Outstanding Feature Polish
- [ ] **Radial vendor overlays** – Recreate the art-authored service menus and equipment compare UI from Python (`Starborn_Python/ui/radial_menu.py`, `ui/shop_screen.py`), including rotating stock flavor lines.
- [ ] **Equipment comparison + inspect** – Expand `InventoryScreen` detail panel with stat deltas and contextual inspect entries before release.
- [ ] **Skill tree ribbons + unlock FX** – Align skill tree overlays with Python’s `SkillTreeManager`, adding unlock cinematics and resonance summaries.

## 5. QA, Instrumentation & Tooling
- [ ] **Instrumentation expansion** – Add combat intro tutorial, ally recruitment, and hub journal tests to `NarrativeSystemsInstrumentedTest` (or new suites) so `connectedDebugAndroidTest` covers late-game flows.
  - _2025-02-21_: Added coverage for the `tinkering_screen_entered` system tutorial to ensure scripted prompts queue correctly via instrumentation.
  - _2025-02-21_: Added a fishing basics tutorial test to validate `fishing_success` actions trigger prompts and milestones.
  - _2025-02-21_: Added defeat/retreat combat tutorial tests that verify `encounter_defeat` and `encounter_retreat` triggers set their respective milestones.
  - _2025-02-22_: Added instrumentation coverage for the `lights_out`, `scrap_run`, and `fixers_favor` quests so the blackout, ration, and tinkering flows are verified via events.
  - _2025-02-22_: Added intro/journal instrumentation (`ollieRecruitmentTutorial_showsOnIntro`, `marketJournalTutorial_showsWhenTalkingToJed`) to ensure ally onboarding + hub journal prompts fire through JSON.
- [ ] **Static analysis & detekt** – Introduce detekt (or ktlint) into CI alongside existing `lintDebug testDebugUnitTest assembleDebugAndroidTest`.
- [ ] **runAssetIntegrity task** – Wire a dedicated Gradle task that executes `DataIntegrityTest` without the full JVM suite; update CI + docs to reference it.
  - _2025-02-21_: Added `runAssetIntegrity` to the CI unit test job so asset validation runs on every pull request (`.github/workflows/android-ci.yml`).

## 6. Perf & Release Readiness
- [ ] **Perfetto captures** – Execute the FrameTimeline, GPU counters, memory soak, and audio headroom runs described in `docs/perf_capture_playbook.md`. Log trace paths and findings in `docs/profiling_reports.md`.
- [ ] **Autosave fingerprint QA** – Add regression tests confirming autosave throttling and fingerprint comparisons skip redundant writes (see `GameSessionStore.fingerprint()` helpers once added).
  - _2025-02-21_: Added `GameSessionStateTest` to verify the fingerprint stays stable for reordered quest/inventory entries and changes when session state differs.
- [ ] **Export / cloud prep** – Implement the “Export Save” tooling from `docs/save_backup_plan.md` and document expected payloads for future cloud sync.
- [ ] **Release automation** – Script signing key retrieval, `bundleRelease` CI gating, and Play Console upload flow per `docs/phase5_perf_release_checklist.md`.

Update the checkbox status and add notes/dates as tasks complete. This sheet should mirror Jira/issue tracker tickets to keep CODEX hand-offs synchronized.
