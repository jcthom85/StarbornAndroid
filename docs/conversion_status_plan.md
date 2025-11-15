# Starborn Port – Status & Forward Plan (Session Hand-off)

Context: Evaluation captured while spinning up **Phase 5 (Perf, QA, Packaging)** after shipping exploration overlays, narrative tooling, and instrumentation coverage. This document compares the Android/Kotlin project against the original Python/Kivy game (`~/AndroidStudioProjects/Starborn/Starborn_Python`) and outlines the refreshed execution plan.

---

## 1. Conversion Status Overview
| Feature / System | Kotlin Status | Python Reference | Notes |
|------------------|---------------|------------------|-------|
| **Asset loading & repositories** | ✅ | `data/*.json`, `item_repository.py` | Moshi models cover rooms, items, events, characters; integrity tests enforce cinematic/tutorial/exit references. |
| **Session management** | ✅ | `game_session.py`, `game.py` | `GameSessionStore` persists via Proto DataStore, slots expose timestamps, and legacy JSON import keeps milestone/quest parity. |
| **Event engine** | ⚠️ Partial | `event_manager.py` | Trigger dispatch, milestone hooks, quest/task propagation, and tutorial/cinematic callbacks are live; late-game encounter choreography + audio-layer verbs still pending. |
| **Dialogue** | ⚠️ Partial | `dialogue_manager.py` | Dialogue service parses JSON, portraits/VO surface in Compose, and shop greetings leverage choice tags; cinematic sequencing + portrait alignment remain for end-game scenes. |
| **Exploration loop** | ✅ Phase-4 polish | `game.py` (`ExplorationScreen`, `Bag`) | Room navigation, quest progression, loot drops, minimap/service glyphs, crafting/shop routing, skill tree overlays, vignette/weather FX, and milestone/narration overlays match Python beats. Remaining polish: art-authored hotspots + final accessibility sizing. |
| **Inventory UI/logic** | ✅ | `ui/bag_screen.py`, `game_objects.Bag` | List/detail/use flows plus filters, contextual actions, and data-driven rewards are in place; future work is equipment comparison + contextual inspect surfaces. |
| **Crafting services/UI** | ⚠️ Partial | `crafting_manager.py`, `ui/tinkering_screen.py` | Tinkering, cooking, first-aid, and fishing Compose screens mirror timing loops with FX/audio cues; authored failure tutorials + recipe storytelling still TODO. |
| **Combat** | ✅ Phase-3 parity | `combat_manager.py`, `ui/combat_screen.py` | Multi-party support, reward pipeline, FX overlays, support cues, weather/theme overlays, and defeat/retreat cinematics implemented; final telegraph polish still upcoming. |
| **Quests/Milestones/Tutorials** | ⚠️ Partial | `quest_manager.py`, `tutorial_manager.py` | Quest runtime state persists, milestone gallery + tutorial queue work, and instrumentation drives core loops; late-game tutorials, hub journal cards, and quest breadcrumbs outstanding. |
| **Shops, radial menu, minigames** | ⚠️ Partial | `ui/shop_screen.py`, `ui/radial_menu.py`, etc. | Shop loop includes greetings, dialogue choices, VO, rotating stock, and FX routing; radial vendor scripting + equipment compare overlays still pending. |
| **Audio/theme/shaders** | ⚠️ Partial | `sound_manager.py`, `theme_manager.py`, `vfx.py` | AudioRouter drives room/hub music, ambience, weather cues, and cinematic ducking with VO layering; audio-layer actions, runtime mixer sliders-to-router bridge, and shader fallback audits remain. |
| **Persistence (Save/Load)** | ⚠️ Partial | `save_system.py` | Autosave cadence, quicksave button, backup rotation, manual save/load overlay, and legacy migration exist; cloud/export workflow remains to be designed. |
| **Tooling/tests** | ⚠️ In progress | Python tools, pytest scripts | JVM + instrumentation suites run via CI (`lintDebug`, `testDebugUnitTest`, `connectedDebugAndroidTest`); need dedicated asset-only Gradle task + broader combat/event coverage. |

---

## 2. Key Deltas vs Python Implementation
1. **Author-driven encounters & hotspots**  
   - Runtime unlocks, milestone overlays, and minimap breadcrumbs match Python, but the art-authored hotspot sprites/blocked-exit cinematics still lag for late hubs. |
2. **Tutorial cadence**  
   - Light-switch/swipe/equipment tutorials now schedule through `TutorialRuntimeManager`, yet late-game scripted tutorials aren’t imported and hub journal cards are absent. |
3. **Audio mixing + shaders**  
   - AGSL vignette/weather overlays and accessibility toggles landed, but AudioRouter still lacks layer-specific fades from events/dialogue and shader fallbacks need perf validation. |
4. **Persistence & backups**  
   - Slots/autosave/legacy import exist, but quicksave/backups/cloud sync aren’t spec’d. |
5. **Tooling & perf**  
   - CI runs lint/unit/instrumentation, though dedicated asset-only Gradle tasks + profiling captures (FrameTimeline, GPU, memory, audio) still need to be executed and logged. |

---

> Current milestone: exploration loop, quest runtime, accessibility toggles, instrumentation smoke, and save-slot UX are live. Focus now shifts to **Phase 5** – profiling, QA automation, release packaging, and audio/persistence parity polish.

### Current Session Highlights
- Exploration HUD now mirrors the Python layout: service tray, animated quick menu, quest journal overlay, and milestone timeline landed in Compose.
- Audio layering reads per-room weather/env data and blends hub music + ambience via the `AudioRouter`.
- Voiceover routing is now centralized through a reusable controller: exploration dialogue, shop greetings, and smalltalk clips queue through the same duck/restore logic, with catalog metadata providing gain/fade/duration hints.
- Hub navigation now features hotspot art, per-node subtitles, and a pulse animation that highlights the selected destination.
- Minimap adds breadcrumb lines, path hints, and current-room rings to match the zoomed-map UX from Python.
- Exploration now renders an AGSL-driven vignette overlay with a radial-gradient fallback, giving rooms the same edge shading as the Python build.
- Settings menu exposes music/SFX volume sliders and a vignette toggle, piping straight into the runtime audio mixer and room shading.
- Settings now persist via a new DataStore-backed `UserSettingsStore`, and the disable flashes/screenshake toggles drive the exploration/combat FX layers (lightning, crafting bursts, and damage shakes respect accessibility).
- Accessibility toggles (high-contrast + large touch targets) propagate through inventory, shop, cooking, first-aid, tinkering, and fishing screens so secondary workflows share the same contrast palettes and ≥52 dp hit targets.
- Quest journal overlay has been rebuilt to match the Python MenuOverlay: filter tabs (Active/Completed/Failed), detailed pane with objectives/logs, and inline track/untrack controls.
- Map tab now offers both the legend panel and a full pan/zoom map overlay rendered from minimap data.
- Save/Load buttons open a slot selector overlay wired to `GameSessionPersistence`, so manual saves and loads are functional again.
- Weather-aware overlays and theme bands now render in both exploration and combat, adapting palettes per environment and weather tags while keeping pre-Tiramisu devices on gradient fallbacks.
- `GameSessionStore` honors pre-selected world/hub/room when returning from the hub; tests/build are green again.
- Victory flow now matches Python: the in-combat two-step dialog (spoils then level-ups) is the only reward surface, and the exploration layer no longer spams post-combat banners/snackbars.
- Generator-style toggle actions execute immediately when tapped (no confirmation dialog) and apply their events/state changes on the spot.
- Combat log flavor text is back to single-line animated callouts timed with attack FX, and the inline action menu replaces the old bottom buttons when a player becomes ready.
- Target selection relies on a glow/pulse overlay instead of brackets, ATB/HP bars only show blue/red pairs, and the resonance bar respects min/max from the session store.

## 3. Immediate Next Steps (Phase 5 – Perf, QA, Packaging)
1. **Profiling campaign**
   - Capture FrameTimeline, GPU counters, memory soak, and audio headroom on target hardware; log results in `docs/profiling_reports.md` and file follow-ups for shader/audio issues.
2. **Asset validation tasking**
   - Ship the `runAssetIntegrity` Gradle task, expand `DataIntegrityTest` coverage (quests/tasks/milestones), and wire the task into local/CI workflows per the perf playbook.
3. **Persistence & backup polish**
   - Design quicksave/backups (mirroring Python’s rotation + fingerprint throttling) and scope cloud/export needs; ensure docs + QA scripts reference the plan.
4. **Audio mixing plan delivery**
   - Implement `audio_layer` event actions, router fade helpers, and settings-store → router hooks so runtime sliders and cinematics share the same gain control.
5. **Release automation**
   - Document signing assets, add `bundleRelease` smoke to CI, and prep Play Console hand-off (track scripts, QA checklist, analytics toggles).

---

## 4. Near-Term Roadmap
| Sprint Goal | Tasks | Dependencies |
|-------------|-------|--------------|
| **Phase 3 – Systems & overlays** *(complete)* | Event parity, hub hotspots, minimap breadcrumbs, crafting/cooking/first-aid/fishing screens, AudioRouter MVP. | — |
| **Phase 4 – Visual & audio fidelity** *(functionally complete)* | AGSL vignette/weather overlays, accessibility toggles, hub navigation polish, VO routing stubs. | Final polish feeds into Phase 5. |
| **Phase 5 – Perf, QA, Packaging** *(in progress)* | - Capture perf traces + log findings.<br>- Ship `runAssetIntegrity`, broaden tests, and enforce CI gates.<br>- Implement quicksave/backups + release automation (signing, bundle builds, Play tracks). | Needs audio mixing plan + persistence polish to close. |

---

## 5. Longer-Term Considerations
- **Persistence**: add cloud/backup strategy once local slots stabilize; evaluate schema migration testing in CI.
- **Visual Identity**: schedule shader/theming workstream (likely parallel to Phase 4 once art direction is locked). |
- **Audio Architecture**: weigh ExoPlayer + SoundPool hybrid, integrate ambience cue definitions. |
- **Tooling**: decide on Python → Kotlin asset converters vs. maintaining legacy editors. |
- **QA & CI**: automate `./gradlew lint test` in CI; expand instrumentation smoke tests after mobile navigation solidifies. |

---

## 6. References for Future Work
- `Starborn_Python/game.py`: master state machine for exploration/combat transitions; use for feature parity validation. |
- `Starborn_Python/event_manager.py`: canonical event handling (toggle_memory, sinks, quest triggers). |
- `Starborn_Python/ui/*`: UI layouts to mirror in Compose. |
- Existing docs in `docs/`: `remaining_parity.md`, `inventory_crafting_plan.md`, `item_effect_handling.md`, and `conversion_reference.md` all updated alongside this status note. |

Maintain this file as the “current strategy” log—update after major milestones or planning shifts so future sessions can onboard quickly.
