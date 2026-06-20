# Starborn: World 3 Hub 1 (Lower City) Walkthrough

> **Historical walkthrough:** This records the World 3 Hub 1 implementation only. Use `antigravity_handoff.md` for the current through-ending project state.

This document outlines the complete implementation, integration, and verification of World 3 Hub 1 ("Lower City / The Return") for the *Starborn* Android port.

## Implementation Details

### 1. Topology & Setup (Phase 1)
- **World Registration:** Registered `world_3` ("The Spire") in [worlds.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/worlds.json) with custom neon theme style configuration.
- **Hub Registration:** Added `hub_5_lower_city` ("Lower City") in [hubs.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/hubs.json).
- **Node Setup:** Configured 5 navigation nodes (`spire_vent_output`, `spire_the_static`, `spire_night_market`, `spire_sewers`, `spire_transit_plaza`) in [hub_nodes.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/hub_nodes.json).
- **Room Setup:** Defined 7 fully bidirectional playable rooms in [rooms.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/rooms.json), complete with inspect/repair interactions and NPC presence configurations.

### 2. Logic, Dialogue & Content (Phase 2)
- **Quests:** Registered Main Quests `w3_mq11` ("Homecoming"), `w3_mq12` ("The Plan"), and Side Quests `w3_sq11`–`w3_sq13` in [quests.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/quests.json).
- **Transition Events:** Connected the transition event from the end of World 2 (`w2_mq05_launch`) to warp players into the landing pad (`spire_sewers_landing`) and trigger `w3_mq11`.
- **Dialogue Trees:** Configured rich interactive dialogues in [dialogue.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/dialogue.json) for Jax, Mika, Zeke, and the party planning table heist brainstorm.
- **Combat & Skills:** Added `gh0st_intercept` skill in [skills.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/skills.json), and registered three new enemy units (`sewer_crawler`, `riot_guard`, `sentinel_mki`) in [enemies.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/enemies.json).
- **Items & Rewards:** Registered `neon_band`, `neon_sign_core`, and `hacked_blueprints` in [items.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/items.json).

### 3. Verification & Scripting (Phase 3)
- **Milestone Verification:** Added milestones (`ms_w3_mq11_complete`, `ms_w3_mq12_complete`) to `$storyMilestoneOrder` in [validate_room_presence.ps1](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/scripts/validate_room_presence.ps1) to ensure asset integrity checks validate cleanly.
- **End-to-End Test Suite:** Authored [Hub3CriticalFlowTest.kt](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/test/java/com/example/starborn/domain/dialogue/Hub3CriticalFlowTest.kt) to simulate the entire progression flow (landing, clearing enemies, safehouse setup, information gathering, side quests, skill/item rewards).

---

## Verification Results

### 1. Static Asset Integrity Checks
Successfully executed gradle static analysis task:
```powershell
.\gradlew.bat :app:runAssetIntegrity
```
**Results:**
- Checked **105 rooms** and **13 NPCs**.
- Verified all audio references, progression triggers, and dialogue emote mappings.
- **0 validation errors**, **0 warnings**, **0 duplicate availability issues**.

### 2. Unit Testing
Successfully executed test suite:
```powershell
.\gradlew.bat :app:testDebugUnitTest
```
**Results:**
- All tests, including the end-to-end flow checks in `Hub3CriticalFlowTest.kt` and `Hub2CriticalFlowTest.kt`, passed cleanly with **zero failures**.
