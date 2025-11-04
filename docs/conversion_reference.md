# Starborn Kotlin Port Reference

Authoring context: final turn of the prior CODEX session (context budget nearly exhausted). This document is meant to brief the next agent on the current Android/Kotlin port of **Starborn**, originally built with Python + Kivy in `~/AndroidStudioProjects/Starborn/Starborn_Python`.

---

## 1. Goal & Snapshot
- **Objective**: Rebuild the full Python Starborn experience inside an Android Studio project using Kotlin (Jetpack Compose for UI).
- **Current Kotlin repo**: `~/AndroidStudioProjects/Starborn` (this project).
- **Original reference**: `~/AndroidStudioProjects/Starborn/Starborn_Python` (retain for parity checks; ignore `_old` / `.old` artifacts per user).
- **Porting status**: Core data loading, session scaffolding with autosave, exploration/combat loops, inventory, and crafting scaffolds exist. Remaining gaps focus on advanced cinematics, authored minigames, shop narrative polish, and shader/audio fidelity.

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

Key entry point: `NavigationHost` (Compose) -> `MainMenuScreen` -> `ExplorationScreen`. Inventory flows are feature-complete; Combat route runs the prototype encounter loop.

---

## 3. Systems Completed / In Progress
| Area | Kotlin Status | Notes |
|------|---------------|-------|
| **JSON asset loading** | ✅ | Using Moshi with reflection factory; assets mirrored from Python (`rooms`, `events`, `items`, `recipes_*`, etc.). |
| **Item repository & inventory service** | ✅ | Inventory state is flow-backed; supports add/remove/use; consumables & schematics produce `ItemUseResult`. |
| **Crafting service** | ⚠️ Partial | Recipe data loads; cooking/first-aid minigames resolve correctly, and fishing catches now apply rod/lure tuning plus name-aware messaging. Advanced effect handling still pending. |
| **Session store** | ✅ | Tracks world/hub/room, quests, milestones, learned schematics, inventory; autosave + slot system live. |
| **Event system** | ⚠️ Partial | Core dispatch + milestone hooks wired; still tightening cinematic/tutorial sequencing and complex quest scripting. |
| **Dialogue service** | ⚠️ Partial | Supports branching choices and trigger hooks; portraits/audio/cinematic sequencing still pending. |
| **Exploration UI** | ⚠️ Partial | Layered HUD with direction ring, hotspot cards, minimap service glyphs, shop greetings, and FX bursts; cinematic playback and radial menu still pending. |
| **Inventory UI** | ✅ | Filter chips, detail pane, "Use" flow; integrates with shared services. |
| **Combat** | ⚠️ | Prototype battle loop now includes turn timeline, target selection, loot/XP routing, and reward tests; still missing cinematic FX, ally support scripting, and defeat/retreat cinematics. |
| **Crafting/Tinkering UI** | ⚠️ Partial | Tinkering, cooking, first-aid, and fishing Compose screens are live with timing meters; needs bespoke art/layout polish. |
| **Minigames, Shops, Quests UI** | ⚠️ Partial | Direction ring overlay and services radial menu exist; still author hotspot art, vendor scripting, and quest journal detail view. |
| **Persistence UI** | ⚠️ Partial | Main menu surfaces autosave + manual slots with timestamps; cloud sync story outstanding. |
| **Testing** | ⚠️ | JVM suite covers inventory/crafting/persistence; need combat/event integration tests and instrumentation smoke runs. |

---

## 4. Latest Changes (this session)
1. **Exploration overlay polish**  
   - Added Compose direction-ring overlay tied to live travel checks and refreshed hotspot handling while keeping minimap/service glyphs.
2. **Fishing tuning pass**  
   - Weighted catches now respect lure rarity and rod power, emit item-aware messaging, and quantity floors align with minigame grades.
3. **Event follow-up fix**  
   - `play_cinematic` actions once again execute chained `on_complete` actions; covered by a new unit test in `domain/event`.
4. **Minigame coverage**  
   - Fishing service tests expanded to assert rod scaling and messaging; failure flows retain zero-quantity outcomes when appropriate.

---

## 5. High-Priority TODOs
1. **Quest/tutorial parity & tests**  
   - Audit quest stage/task data against Python, flesh out scripted tutorial triggers, and add JVM coverage for quest journaling.
2. **Hotspot & cinematic polish**  
   - Bring in art-driven hotspot overlays, blocked-exit cinematics, and richer radial menu styling to match Kivy behaviour.
3. **Minigame authoring**  
   - Finish cooking/first-aid failure tutorials, add remaining side minigames, and surface contextual dialogues/audio cues.
4. **Audio layering**  
   - Extend `AudioRouter` for ambience/music fades, cue metadata, and VO routing.
5. **Combat polish**  
   - Add ally support scripting, defeat/retreat cinematics, and expanded FX overlays.
6. **Testing & CI**  
   - Add integration coverage for quest/tutorial flows and automate `lint`/`test` in CI.

---

## 6. Known TODO Markers & Gaps
- `feature/exploration/ui/ExplorationScreen.kt`: direction ring overlay is functional; cinematic overlay and art-directed hotspot styling remain TODO.
- `feature/exploration/viewmodel/ExplorationViewModel.kt`: quest/tutorial timing scripts and cinematic queuing still need parity verification; review TODO markers.
- `docs/item_effect_handling.md`: outlines next steps for applying effects and crafting progress UI; still accurate.
- `domain/crafting/CraftingService.kt`: integrate authored failure tutorials and add fishing recipes.
- `feature/crafting/ui/*`: fishing minigame overlays pending; cooking/first-aid still using placeholder art.

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
1. Stand up quest/tutorial runtime managers and integrate them with dialogue triggers.  
2. Implement fishing minigame UI/logic plus authored crafting failure tutorials.  
3. Deliver radial service menu + vendor smalltalk/rotation scripting.  
4. Extend AudioRouter with ambience/music layering and catalogue FX usage.  
5. Expand integration/CI coverage and keep docs in sync as new systems land.

---

## 10. Quick File Reference
- `app/src/main/java/com/example/starborn/feature/exploration/viewmodel/ExplorationViewModel.kt` (event handling, navigation, item use).  
- `app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt` (Compose UI & new snackbar logic).  
- `app/src/main/java/com/example/starborn/feature/inventory/ui/InventoryScreen.kt` (filters/detail/use).  
- `app/src/main/java/com/example/starborn/di/AppServices.kt` (shared services instantiation).  
- `app/src/main/java/com/example/starborn/domain/crafting/CraftingService.kt` (schematic persistence).  
- `app/src/main/java/com/example/starborn/domain/inventory/Inventory.kt` (item handling).  
- `app/src/main/java/com/example/starborn/domain/leveling/` (LevelingData + LevelingManager handling XP → level curves).  
- `app/src/test/java/com/example/starborn/domain/`... (new JVM tests).  
- `docs/item_effect_handling.md`, `docs/inventory_crafting_plan.md`, `docs/remaining_parity.md` (design notes).  
- `Starborn_Python/game.py` (source-of-truth mechanics).  

Keep this document updated as major systems land so future sessions inherit accurate progress context.
