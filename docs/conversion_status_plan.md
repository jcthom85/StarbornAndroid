# Starborn Port – Status & Forward Plan (Session Hand-off)

Context: Evaluation performed after completing Phase 1 (exploration parity, quest pipeline, tinkering screen). This document compares the Android/Kotlin project against the original Python/Kivy game (`~/AndroidStudioProjects/Starborn/Starborn_Python`) and outlines the next execution plan.

---

## 1. Conversion Status Overview
| Feature / System | Kotlin Status | Python Reference | Notes |
|------------------|---------------|------------------|-------|
| **Asset loading & repositories** | ✅ | `data/*.json`, `item_repository.py` | Moshi models cover rooms, items, events, characters; more schema verification needed. |
| **Session management** | ✅ Base | `game_session.py`, `game.py` | `GameSessionStore` tracks room/world/player, quests, milestones, learned schematics. No persistence yet. |
| **Event engine** | ⚠️ Partial | `event_manager.py` | Core trigger dispatch present; milestone/quest hooks implemented, but cinematics/tutorial system still basic. |
| **Dialogue** | ⚠️ Partial | `dialogue_manager.py` | Dialogue service parses JSON and advances lines; lacks branching choices, portraits, audio cues. |
| **Exploration loop** | ✅ Phase-1 parity | `game.py` (`ExplorationScreen`, `Bag`) | Room navigation, quest progression, loot drops, milestone/narration overlays, inventory/tinkering routing implemented. Hotspots/minimap still to do. |
| **Inventory UI/logic** | ✅ First pass | `ui/bag_screen.py`, `game_objects.Bag` | List/detail/use flows working; equipment management and sorting TBD. |
| **Crafting services/UI** | ⚠️ Partial | `crafting_manager.py`, `ui/tinkering_screen.py` | Recipe logic ready; tinkering Compose screen added. Cooking/first aid minigames still missing. |
| **Combat** | 🚧 Not started | `combat_manager.py`, `ui/combat_screen.py` | Only navigation hook exists. Need domain model + Compose battle UI. |
| **Quests/Milestones/Tutorials** | ⚠️ Partial | `quest_manager.py`, `tutorial_manager.py` | Quest metadata loaded, objectives tracked, milestone banners in place. Tutorial system still basic; quest journal detail view TBD. |
| **Shops, radial menu, minigames** | 🚧 Not started | `ui/shop_screen.py`, `ui/radial_menu.py`, etc. | No Kotlin equivalents yet. |
| **Audio/theme/shaders** | 🚧 Not started | `sound_manager.py`, `theme_manager.py`, `vfx.py` | Android project currently silent with static backgrounds. |
| **Persistence (Save/Load)** | 🚧 Not started | `save_system.py` | No save slots or serialization in Kotlin. |
| **Tooling/tests** | ⚠️ Minimal | Python tools, pytest scripts | JVM tests cover inventory/crafting/event logic; combat/event integration tests still needed. |

---

## 2. Key Deltas vs Python Implementation
1. **Room actions & state mutations**  
   - Python `ExplorationScreen` flips breakers, lights, etc., updating room state and unlocking exits. Kotlin now tracks state but lacks logic to re-evaluate blocked directions, spawn events, or update visuals. |
2. **Inventory Integration**  
   - Python handles pickups, loot tables, and contextual prompts; Kotlin needs event-driven item acquisition (currently mostly status messages). |
3. **Combat Pipeline**  
   - None of the Python combat stack (initiative, skills, FX) has been ported yet; blockers for mid-game parity. |
4. **Quest Flow & Tutorials**  
   - Python uses event hooks to start quests, pop tutorials, and manage cinematics. Kotlin stubs exist but require UI + logic to match original pacing. |
5. **UI Fidelity**  
   - Compose layouts are minimal; Python uses layered canvases, particle effects, and theming. Visual parity will demand significant Compose/AGSL work. |
6. **Persistence & Progression**  
   - Save slots, load on boot, and long-term progression (skills, XP curves) remain unimplemented. |

---

## 3. Immediate Next Steps (Phase 2 Kickoff)
1. **Combat foundations**
   - Port combat domain models (stats, actions, turn queue, status effects) from Python.
   - Build a `CombatViewModel` and Compose battle UI (timeline, action buttons).
   - Wire `ExplorationEvent.EnterCombat` to launch the new screen and return results.
2. **Crafting lineup expansion**
   - Extend crafting UI to include ingredient availability, filtering, and feedback.
   - Begin cooking/first-aid flow stubs that reuse crafting data.
3. **Dialogue & cinematic polish**
   - Add branching dialogue options and richer tutorial/cutscene handling using the new narration pipeline.
4. **Testing/QA**
   - Unit tests for combat calculations and quest progress.
   - Integration tests that simulate event chains (breaker toggles, quest pickups).

Deliverable: combat prototype battle + tuning so Phase 2 can iterate on combat/progression systems.

---

## 4. Near-Term Roadmap (Phase 2 Prep)
| Sprint Goal | Tasks | Dependencies |
|-------------|-------|--------------|
| **Combat foundations** | - Port combat domain models (stats, actions, turn queue) from `combat_manager.py`.<br>- Build `CombatViewModel` + basic Compose UI mirroring initiative order.<br>- Wire exploration `EnterCombat` to start simulated battles. | Requires finalized item/skill data schema; ensure inventory consumables integrate. |
| **Quest/Tutorial surfaces** | - Enhance quest journal (detail panel, objective markers).<br>- Port tutorial manager to display overlays (text + triggers). | Event hooks already firing; need richer UI. |
| **Crafting UI expansion** | - Polish tinkering screen with available/locked recipes, resource checks.<br>- Prototype cooking/first-aid minigame stubs. | Requires expanded crafting data and UX. |

---

## 5. Longer-Term Considerations
- **Persistence**: choose data layer (Room/DataStore) to persist `GameSessionState`, inventory, and learned schematics. Adopt migration path for future updates.
- **Visual Identity**: plan Compose theme/shader workstream—likely separate from core gameplay tasks to avoid blocking functionality. |
- **Audio Architecture**: evaluate using ExoPlayer + SoundPool; map Python audio router to Android channels. |
- **Tooling**: determine whether to port Python editors or create scripts to convert assets/build data. |
- **QA & CI**: set up Git repository, CI workflows (Gradle lint/tests), and manual QA checklists early to catch regressions as systems grow. |

---

## 6. References for Future Work
- `Starborn_Python/game.py`: master state machine for exploration/combat transitions; use for feature parity validation. |
- `Starborn_Python/event_manager.py`: canonical event handling (toggle_memory, sinks, quest triggers). |
- `Starborn_Python/ui/*`: UI layouts to mirror in Compose. |
- Existing docs in `docs/`: `remaining_parity.md`, `inventory_crafting_plan.md`, `item_effect_handling.md`, and `conversion_reference.md` all updated alongside this status note. |

Maintain this file as the “current strategy” log—update after major milestones or planning shifts so future sessions can onboard quickly.
