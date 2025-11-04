# Starborn Port – Status & Forward Plan (Session Hand-off)

Context: Evaluation captured after wrapping Phase 2C (progression systems, cooking/first-aid polish, shop greetings, autosave tooling). This document compares the Android/Kotlin project against the original Python/Kivy game (`~/AndroidStudioProjects/Starborn/Starborn_Python`) and outlines the next execution plan.

---

## 1. Conversion Status Overview
| Feature / System | Kotlin Status | Python Reference | Notes |
|------------------|---------------|------------------|-------|
| **Asset loading & repositories** | ✅ | `data/*.json`, `item_repository.py` | Moshi models cover rooms, items, events, characters; more schema verification needed. |
| **Session management** | ✅ | `game_session.py`, `game.py` | `GameSessionStore` now persists via DataStore with autosave cadence, slot management, and legacy import hook. |
| **Event engine** | ⚠️ Partial | `event_manager.py` | Core trigger dispatch present; milestone/quest hooks implemented, but cinematic queueing and scripted tutorials still need parity passes. |
| **Dialogue** | ⚠️ Partial | `dialogue_manager.py` | Dialogue service parses JSON, portraits/VO surface in Compose, and shop greetings leverage choice tags; cinematic sequencing + portrait alignment remain. |
| **Exploration loop** | ✅ Phase-2C slice | `game.py` (`ExplorationScreen`, `Bag`) | Room navigation, quest progression, loot drops, minimap service glyphs, crafting/shop routing, milestone/narration overlays in place. Outstanding polish: art-driven hotspots and direction ring flourish. |
| **Inventory UI/logic** | ✅ First pass | `ui/bag_screen.py`, `game_objects.Bag` | List/detail/use flows working; equipment management and advanced sorting TBD. |
| **Crafting services/UI** | ⚠️ Partial | `crafting_manager.py`, `ui/tinkering_screen.py` | Tinkering plus tuned cooking/first-aid timing bars (with FX/audio hooks) implemented; authored failure tutorials and fishing loop still pending. |
| **Combat** | ✅ Phase-2A parity | `combat_manager.py`, `ui/combat_screen.py` | Multi-party support, reward pipeline, FX overlays, support cues, and defeat/retreat cinematics all implemented. |
| **Quests/Milestones/Tutorials** | ⚠️ Partial | `quest_manager.py`, `tutorial_manager.py` | Quest metadata loaded, objectives tracked, milestone banners in place. Tutorial system still basic; quest journal detail view TBD. |
| **Shops, radial menu, minigames** | ⚠️ Partial | `ui/shop_screen.py`, `ui/radial_menu.py`, etc. | Shop flow includes greetings, dialogue choices, VO, and FX routing; fishing minigame now mirrors timing loop but radial menu/vendor scripting still upcoming. |
| **Audio/theme/shaders** | ⚠️ Partial | `sound_manager.py`, `theme_manager.py`, `vfx.py` | AudioRouter now applies catalogue-driven fades/looping; ambience/music layering in place, shader-driven theming still pending. |
| **Persistence (Save/Load)** | ⚠️ Partial | `save_system.py` | Autosave cadence, slot timestamps, legacy quest/milestone migration implemented; cloud backup story outstanding. |
| **Tooling/tests** | ⚠️ Minimal | Python tools, pytest scripts | Added persistence regression tests; still need combat/event integration coverage and CI wiring. |

---

## 2. Key Deltas vs Python Implementation
1. **Room actions & state mutations**  
   - Direction ring and hotspot overlays are live, but event-driven room art, blocked-exit cinematics, and automatic encounter spawns still trail the Python flow. |
2. **Inventory integration**  
   - Loot cards and ground item pickup flows are working; shop greetings and vendor VO now live, but contextual prompts for hub services still trail Python. |
3. **Combat pipeline**  
   - Turn order, targeting, and rewards are ported; cinematic FX, ally support scripting, and defeat/retreat story beats remain. |
4. **Quest Flow & Tutorials**  
   - Python uses event hooks to start quests, pop tutorials, and manage cinematics. Kotlin stubs exist but require UI + logic to match original pacing. |
5. **UI fidelity**  
   - Exploration HUD mirrors core layout, but minimap, particle FX, and shader-based theming still need Android counterparts. |
6. **Persistence & Progression**  
   - Save slots, load on boot, and long-term progression (skills, XP curves) remain unimplemented. |

---

> Phase 2C milestone shipped: progression overlays, tuned cooking/first-aid loops, shop greetings with dialogue choices, minimap service glyphs, and autosave metadata/timestamps are live. Focus now shifts to Phase 3 systems and overlays.

### Current Session Highlights
- Exploration HUD now mirrors the Python layout: service tray, animated quick menu, quest journal overlay, and milestone timeline landed in Compose.
- Audio layering reads per-room weather/env data and blends hub music + ambience via the `AudioRouter`.
- Hub navigation now features hotspot art, per-node subtitles, and a pulse animation that highlights the selected destination.
- Minimap adds breadcrumb lines, path hints, and current-room rings to match the zoomed-map UX from Python.
- Exploration now renders an AGSL-driven vignette overlay with a radial-gradient fallback, giving rooms the same edge shading as the Python build.
- Settings menu exposes music/SFX volume sliders and a vignette toggle, piping straight into the runtime audio mixer and room shading.
- Weather-aware overlays and theme bands now render in both exploration and combat, adapting palettes per environment and weather tags while keeping pre-Tiramisu devices on gradient fallbacks.
- `GameSessionStore` honors pre-selected world/hub/room when returning from the hub; tests/build are green again.

## 3. Immediate Next Steps (Phase 4 – Visual & Audio Fidelity)
1. **Shader & VFX pass**
   - Port vignette, weather layers, and combat FX using AGSL/RenderEffect so ambience matches the Python build.
2. **UI theming & accessibility**
   - Finalise theme bands, high-contrast mode, and touchscreen affordances for combat/exploration overlays.
3. **Audio polish & mixing**
   - Integrate voiceover routing, refine combat stingers/palette swaps, and surface haptics once VO stems arrive.

Deliverable: Phase 4 milestone — visual identity lock-in, accessibility polish, and full audio mixing controls ready for content finalisation.

---

## 4. Near-Term Roadmap
| Sprint Goal | Tasks | Dependencies |
|-------------|-------|--------------|
| **Phase 3 – Systems & overlays** *(complete)* | Complete: event parity, hub hotspots, minimap breadcrumbs, and runtime services shipped. | — |
| **Phase 4 – Visual & audio fidelity** | - Compose shader/VFX pass (vignette, weather, damage numbers).<br>- Scene theming & accessibility polish.<br>- Voiceover mixing, runtime audio settings, haptics. | Dependent on stabilized visual style guide and audio stems. |

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
