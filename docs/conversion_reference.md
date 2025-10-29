# Starborn Kotlin Port Reference

Authoring context: final turn of the prior CODEX session (context budget nearly exhausted). This document is meant to brief the next agent on the current Android/Kotlin port of **Starborn**, originally built with Python + Kivy in `~/AndroidStudioProjects/Starborn/Starborn_Python`.

---

## 1. Goal & Snapshot
- **Objective**: Rebuild the full Python Starborn experience inside an Android Studio project using Kotlin (Jetpack Compose for UI).
- **Current Kotlin repo**: `~/AndroidStudioProjects/Starborn` (this project).
- **Original reference**: `~/AndroidStudioProjects/Starborn/Starborn_Python` (retain for parity checks; ignore `_old` / `.old` artifacts per user).
- **Porting status**: Core data loading, session scaffolding, basic exploration loop, and inventory/crafting scaffolding exist. Large portions of gameplay logic (combat, dialogue depth, quests, cinematics, tutorials, minigames, shops, cutscenes) remain unimplemented or stubbed.

---

## 2. Project Layout (Kotlin)
```
app/
  src/main/java/com/example/starborn/
    core/            → Moshi, dispatchers, etc.
    data/assets/     → JSON asset readers (rooms, items, events, recipes...)
    data/repository/ → ItemRepository currently implements ItemCatalog
    di/              → AppServices container (shared singletons)
    domain/          → Session, inventory, crafting, dialogue, events, models
    feature/
      exploration/   → Exploration screen + viewmodel (room navigation)
      inventory/     → Inventory screen + viewmodel
      mainmenu/      → Main menu stub
      combat/        → Placeholder Compose screen (no real combat yet)
    navigation/      → NavHost wiring between MainMenu, Exploration, Inventory, Combat.
  src/main/assets/    → JSON data converted from Python assets
  src/test/java/      → JVM unit tests (Inventory & Crafting coverage recently added)
docs/                 → Planning notes (inventory_crafting_plan, item_effect_handling, remaining_parity, etc.)
Starborn_Python/      → Original Kivy implementation
```

Key entry point: `NavigationHost` (Compose) -> `MainMenuScreen` -> `ExplorationScreen`. Inventory and Combat routes exist, but only inventory is functional.

---

## 3. Systems Completed / In Progress
| Area | Kotlin Status | Notes |
|------|---------------|-------|
| **JSON asset loading** | ✅ | Using Moshi with reflection factory; assets mirrored from Python (`rooms`, `events`, `items`, `recipes_*`, etc.). |
| **Item repository & inventory service** | ✅ | Inventory state is flow-backed; supports add/remove/use; consumables & schematics produce `ItemUseResult`. |
| **Crafting service** | ⚠️ Partial | Recipe data loads; `canCraft` & `craft*` functions implemented. Learned schematics now persist in `GameSessionStore` but no UI. |
| **Session store** | ✅ Base | Tracks world/hub/room, quests, milestones, learned schematics. |
| **Event system** | ⚠️ Partial | `EventManager` skeleton ported; many hooks log/snackbar but do not mutate gameplay state fully. |
| **Dialogue service** | ⚠️ Partial | Basic dialogue progression; triggers mutate quests/milestones. No UI beyond exploration overlay. |
| **Exploration UI** | ⚠️ Partial | Room display, NPC list, POIs, exits, status text; new snackbars for events. Most action handlers still "coming soon". |
| **Inventory UI** | ✅ | Filter chips, detail pane, "Use" flow; integrates with shared services. |
| **Combat** | 🚧 | Placeholder screen only; exploration emits `EnterCombat` event. |
| **Crafting/Tinkering UI** | 🚧 | Not started. |
| **Minigames, Shops, Quests UI** | 🚧 | Not started. |
| **Testing** | ⚠️ | New JVM tests cover inventory item effects & schematic persistence. Remaining systems untested. |

---

## 4. Latest Changes (end of previous session)
1. **Learned schematic persistence**  
   - Added `learnedSchematics` to `GameSessionState` (`app/.../GameSessionState.kt:11`).  
   - `GameSessionStore` now stores/forgets schematics (`GameSessionStore.kt:50`).  
   - `CraftingService.learnSchematic` writes to session store and avoids duplicates (`CraftingService.kt:33`).  
   - Inventory/Exploration messaging differentiates already-known schematics (`InventoryViewModel.kt:44`, `ExplorationViewModel.kt:307`).

2. **Inventory service refactor**  
   - Introduced `ItemCatalog` interface so `InventoryService` doesn't require Android assets in tests (`domain/inventory/Inventory.kt`).  
   - `ItemRepository` now implements `ItemCatalog` (`data/repository/ItemRepository.kt`).  
   - Tests can provide simple fakes.

3. **Exploration event surface**  
   - Snackbar feedback for rewards, quests, item use, tutorials, etc. in `ExplorationScreen` (lines ~44-110).  
   - `ExplorationEvent.ItemUsed` now carries the formatted message to reduce duplication.

4. **Unit tests**  
   - `InventoryServiceTest` validates consumable, buff, schematic flows with fake items.  
   - `CraftingServiceTest` ensures schematic persistence semantics (no duplicate unlock).  
   - Tests use pure Kotlin fakes; `./gradlew testDebugUnitTest` passes.

5. **Navigation injection**  
   - Inventory route now receives both `InventoryService` & `CraftingService` via `InventoryViewModelFactory` (`AppNavigation.kt:51`).  
   - `AppServices` constructs services once and shares them across screens (`di/AppServices.kt:34-37`).

6. **Room state tracking, loot, & travel gating**  
   - `ExplorationViewModel` now retains per-room state flags, reacts to event-driven `set/toggle_room_state`, and mirrors updates in UI state (`ExplorationViewModel.kt`).  
   - Travel honours `blocked_directions`: locks check requirements, consume keys, and unlock paths with feedback; enemy blocks now defer until encounters resolved.  
   - Container actions and event-driven drops place items on the ground, surface status/snackbar cues, and let players collect loot via the exploration UI (`ExplorationScreen.kt`).

7. **Event branching & tutorials hooks**  
   - `EventManager` supports `else` branches for `if_*` actions plus new hooks for ground loot spawns, search unlocks, and chained cinematics/tutorial completions.  
   - Player-facing feedback surfaces via new `ExplorationEvent` cases (loot/tutor/room search) and an in-game tutorial banner + quest log overlay, mirroring Python narrative beats.

8. **Quest data & log UI**  
   - Added `quests.json` asset and repository; quest log overlay shows quest titles, summaries, stage info, and auto-checking objectives as event hooks fire. Milestone changes trigger both toasts and a dismissible banner.

9. **Tinkering screen**  
   - New `TinkeringRoute` lists crafting recipes, crafts via `CraftingService`, and unlocks once `ms_tinkering_prompt_active` is set in exploration.

---

## 5. High-Priority TODOs
1. **Complete Exploration event handling**  
   - Replace remaining TODO placeholders (combat entry, cinematic playback, quest updates UI, etc.) in `ExplorationViewModel` & `ExplorationScreen`.  
   - Align with Python event triggers in `Starborn_Python/game.py` (search for matching event names).

2. **Combat system port**  
   - Implement combat models, turn logic, UI; integrate with item effects (restorative/buff/damage) and inventory service.  
   - Reference Python `combat_manager`, `battle_screen` etc. in original repo.

3. **Crafting/Tinkering UI**  
   - Build Compose screens for cooking/first aid/tinkering, hooking into `CraftingService`.  
   - Reflect Python `ui/tinkering_screen.py`, `ui/cooking_screen.py` flows.  
   - Ensure learned schematics gate recipe visibility (reads from `GameSessionStore.state`).

4. **Persistent storage / saves**  
   - Currently session data is in-memory only; decide on storage (DataStore/Room) aligning with Python's behavior.

5. **Dialogue & quest parity**  
   - Many quest hooks do not update UI or enforce conditions (see `ExplorationViewModel.onActionSelected`).  
   - Need quest log screen, milestone tracking visuals, dialogue choices, etc.

6. **World navigation & interactions**  
   - Implement toggles, tinkering machines, room states, timed events per Python logic.  
   - See TODO comment `ExplorationViewModel.kt` lines around 200-260 and `item_effect_handling.md` for guidance.

7. **Testing expansion**  
   - Add coverage for `InventoryService.consumeItems`, `CraftingService.craft*`, `EventManager` flows, navigation transitions.  
   - Consider integration tests around `AppServices` to ensure shared instances behave.

---

## 6. Known TODO Markers & Gaps
- `feature/exploration/ui/ExplorationScreen.kt`: event handling now uses snackbars but still placeholders for cinematics/tutorial content.
- `feature/exploration/viewmodel/ExplorationViewModel.kt`:  
  - `onActionSelected` still returns "feature coming soon" (line ~300).  
  - `ExplorationEvent` cases (combat resolution, spawn encounter) require implementations.  
  - `useInventoryItem` damage/buff outcomes not wired into player/combat stats.
- `docs/item_effect_handling.md`: outlines next steps for applying effects and crafting progress UI; still accurate.
- `domain/crafting/CraftingService.kt`: learned schematics persisted but `tinkering/cooking/first aid` craft flows untested; future UI should leverage `CraftingOutcome` messaging.

---

## 7. Python Reference Tips
- Key gameplay logic resides in `Starborn_Python/game.py` (~5k lines). Look at class `ExplorationScreen`, `Bag`, `CraftingManager`, etc.  
- JSON assets under `Starborn_Python/` mirror Android assets; use to verify parity.  
- For graphics references, inspect `Starborn_Python/data/` (images/maps) and plan Android equivalents (drawables, Lottie, etc.).

---

## 8. Building & Testing
- **Run app**: standard Android Studio (Compose). No specific instructions yet due to missing gameplay flows.  
- **Unit tests**: `./gradlew testDebugUnitTest` (requires Gradle cache access; in CODEX, may need escalated permissions).  
- **Future**: consider adding instrumentation tests once UI screens mature.

---

## 9. Suggested Next Steps for New Session
1. Confirm Kotlin project still builds and launches to Exploration screen. Capture logs for TODO events.  
2. Decide on immediate milestone (e.g., finish inventory integration with combat or start crafting UI).  
3. Reference this document alongside `docs/remaining_parity.md` & `docs/inventory_crafting_plan.md` for deeper backlog context.  
4. Keep `AppServices` singleton model consistent so learned schematics persist across screens.  
5. Maintain separation between data (assets) and game logic so tests stay fast (use `ItemCatalog`/`CraftingRecipeSource` abstractions).

---

## 10. Quick File Reference
- `app/src/main/java/com/example/starborn/feature/exploration/viewmodel/ExplorationViewModel.kt` (event handling, navigation, item use).  
- `app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt` (Compose UI & new snackbar logic).  
- `app/src/main/java/com/example/starborn/feature/inventory/ui/InventoryScreen.kt` (filters/detail/use).  
- `app/src/main/java/com/example/starborn/di/AppServices.kt` (shared services instantiation).  
- `app/src/main/java/com/example/starborn/domain/crafting/CraftingService.kt` (schematic persistence).  
- `app/src/main/java/com/example/starborn/domain/inventory/Inventory.kt` (item handling).  
- `app/src/test/java/com/example/starborn/domain/`... (new JVM tests).  
- `docs/item_effect_handling.md`, `docs/inventory_crafting_plan.md`, `docs/remaining_parity.md` (design notes).  
- `Starborn_Python/game.py` (source-of-truth mechanics).  

Keep this document updated as major systems land so future sessions inherit accurate progress context.
