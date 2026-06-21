# Starborn — World 2 Completion Progress

This document tracks implementation status of the tasks defined in the [World 2 Completion Plan](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/docs/world_2_completion_plan.md).

---

## Progress Overview

| Phase | Task | Status | Details |
| :--- | :--- | :--- | :--- |
| **Phase 1: Config** | 🛠️ Minimap Connectivity Fix | **Completed** | Fixed in `ExplorationViewModel.kt#updateMinimap` |
| **Phase 1: Config** | 🧪 Tinkering Recipes Update | **Completed** | Added `mod_source_resin` and `mod_rapid_capacitor` in `recipes_tinkering.json` |
| **Phase 1: Config** | 🤖 Sentinel-3 NPC & Dialogues | **Completed** | Added Sentinel-3 NPC config, dialogue, shop, and spawned in Temple Lock |
| **Phase 2: Code** | 🎒 Snack Slot Limit | **Completed** | Enforced 5-turn cooldown in processor and limited menu to Snack Slot |
| **Phase 2: Code** | ⚔️ Combat Cooldown System | **Completed** | Confirmed skills function natively on turn-based cooldown system |
| **Phase 3: Assets** | World 2 production art | **Completed** | Added and wired 91 room backgrounds, 2 hub maps, 10 node icons, 3 enemy sprites, and Sentinel-3 portrait |
| **Phase 4: Device QA** | All-node hub navigation | **Completed** | Pixel 8a Maestro coverage passes for all 10 World 2 hub nodes |
| **Phase 4: Device QA** | Main quest end-to-end flow | **Next** | Cover crash, Gh0st/Source Beast, Orion, Astra launch, and World 3 handoff |

---

## Log of Changes

### 2026-06-21
* **Completed World 2 Art Production:** Added and wired production art for all 91 rooms, both hubs, all 10 nodes, the Shard-Hound, Spore-Spitter, Source Beast, and Sentinel-3.
* **Fixed Tideglass Hub Routing:** Corrected the invalid `sector9_stream` entry room to `sector9_stream_pools`; added a regression test requiring every hub entry room to exist and belong to its node.
* **Added World 2 Hub Device Coverage:** Expanded `debug_world2_hub.yaml` to enter every Jungle Ruins node and added `debug_world2_facility_hub.yaml` for all Sector 9 Ruins nodes. Both flows pass on Pixel 8a.
* **Added Minimap Connectivity Fix:** Modified `updateMinimap` inside `ExplorationViewModel.kt` to automatically discover neighboring rooms when entering a room. Tests passed successfully.
* **Added World 2 Tinkering Recipes:** Registered recipes for `mod_source_resin` and `mod_rapid_capacitor` in `recipes_tinkering.json`.
* **Added Sentinel-3 NPC:** Registered Tuner Drone NPC in `npcs.json`, added `sentinel_scraps` shop in `shops.json`, added dialogue lines in `dialogue.json`, and spawned `sentinel_3` in `sector9_temple_lock_chamber` in `rooms.json`.
* **Implemented Snack Slot Limit:** Removed the generic "Items" button from the combat command menu ([CommandPalette.kt](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/java/com/example/starborn/feature/combat/ui/components/CommandPalette.kt)) to restrict players to using their single equipped Snack Slot in combat, and enforced a strict 5-turn cooldown for snacks inside the action processor ([CombatActionProcessor.kt](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/java/com/example/starborn/domain/combat/CombatActionProcessor.kt)).
* **Verified Combat Cooldown System:** Confirmed that the combat engine and skills function natively on the turn-based cooldown system.
* **Implemented Room Background Gradient Fallback:** Modified `rememberRoomBackgroundPainter` in [BackgroundPainters.kt](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/java/com/example/starborn/ui/background/BackgroundPainters.kt#L68) to automatically render a beautiful, themed green-bioluminescent vertical gradient for any missing World 2 background assets, preventing flat black screens.
* **Initialized Progress Document:** Created `world_2_completion_progress.md` and `world_2_completion_plan.md`.
