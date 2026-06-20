# Starborn: World 3 Hub 2 (Upper City) Implementation Plan

> **Completed historical plan:** World 3 Hub 2 and all subsequent campaign hubs through the ending are implemented. Use `antigravity_handoff.md` for current priorities and `task.md` for the live checklist.

This plan details the implementation strategy for introducing World 3 Hub 2 ("Upper City / Penthouse Heist") into the game. All changes will follow the data-driven model using the game's JSON configuration files.

## Goal Description
Implement the environments, quests, events, dialogues, items, and combat challenges for World 3 Hub 2: Upper City, detailing the heist setup, the split party infiltration mechanics, retrieving the Lens relic, the boss fight with the Administrator, and escaping the Spire to transition to World 4.

---

## User Review Required

> [!IMPORTANT]
> **Heist Mechanics & Party Split**
> - **Infiltration Strategy:** The team splits up during `w3_mq13`. Zeke and Orion disable security from the laundry rooms, while Nova and Gh0st infiltrate the main lobby.
> - **Source Art: Scan (True Sight):** Acquired upon picking up the Lens Relic in `w3_mq14`. It will allow scanning for hidden paths and analyzing corporate assassin weaknesses.
> - **Transition to World 4:** Slipping through the sub-orbital shield at the end of the Administrator boss fight transitions the campaign to World 4 ("The Foundry").

---

## Open Questions

1. **Light Puzzle implementation:** Orion needs to solve a Light Puzzle in the Archive to acquire the Lens. We propose wiring this as a choice dialogue/interaction in the room where Orion must tune the Source to correct acoustic frequencies.
2. **Shop/Lounge Integration:** The Exec Lounge node features a high-end shop. We should define a new shop in `shops.json` with higher-tier items/modifiers. Do you have specific premium accessories or mods you'd like added there?

---

## Proposed Changes

We will register and build out all World 3 Hub 2 content across the following files:

### World Configs & Topology
#### [MODIFY] [hubs.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/hubs.json)
- Register `hub_6_upper_city` ("Upper City") mapping to `world_3`.

#### [MODIFY] [hub_nodes.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/hub_nodes.json)
- Register node coordinates and connections for Hub 6:
  - `spire_laundry` (Laundry Service - Entry)
  - `spire_skypark` (Skypark - Social/Breather)
  - `spire_exec_lounge` (Exec Lounge - Shop)
  - `spire_archive` (The Archive - Vault Climax)
  - `spire_landing_pad` (Landing Pad - Boss Gate)

#### [MODIFY] [rooms.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/rooms.json)
- Append room data for Upper City rooms:
  - `spire_laundry_service` (concrete, pipes, steam, laundry chutes)
  - `spire_skypark_dome` (pristine fake sun garden, velvet ropes)
  - `spire_exec_lounge_bar` (floating bar, VIP booths)
  - `spire_archive_vault` (glass floor, shard displays)
  - `spire_landing_pad_roof` (windy rooftop launch pad)
- Wire actions: `Disable sensors`, `Solve light puzzle`, `Initiate override`, `Launch Astra`.

### Quests & Logic
#### [MODIFY] [quests.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/quests.json)
- Add World 3 Hub 2 quests:
  - `w3_mq13` / **Social Engineering** (Blend in, disable sensors, infiltrate lobby)
  - `w3_mq14` / **The Lens** (Archive infiltration, light puzzle, steal relic)
  - `w3_mq15` / **Burn Notice** (Escape alarm, defeat Administrator, fly to Foundry)
  - `w3_sq14` / **Corporate Espionage** (Ledger theft side quest, reward: Passive Blackmail)
  - `w3_sq15` / **Prototype Testing** (Weapon trial side quest, reward: Phase Rounds mod)

#### [MODIFY] [events.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/events.json)
- Add transition event from MQ_12 to MQ_13.
- Define Archive alarm trigger and Thorne's trap event (`EVT_W3_06`) venting the room.
- Setup final launch of *The Astra* to warp player to World 4.

#### [MODIFY] [dialogue.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/dialogue.json)
- Implement dialogues for the Curator in the Skypark, Zeke's console exorcism, Vale's compliance override broadcast, and Thorne's trap.

### Combat, Skills, and Items
#### [MODIFY] [enemies.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/enemies.json)
- Add new hostiles:
  - `aero_drone` (flying stun drone)
  - `corporate_assassin` (stealth critical striker)
  - `heavy_mech` (flamethrower tank)
  - `administrator_boss` (boss: summons drones and fires lasers)

#### [MODIFY] [items.json](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/items.json)
- Add `the_lens` (relic item).
- Add `encrypted_ledger` (quest item).
- Add `phase_rounds` (weapon mod accessory).

### Scripts
#### [MODIFY] [validate_room_presence.ps1](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/scripts/validate_room_presence.ps1)
- Append the new milestones (`ms_w3_mq13_complete`, `ms_w3_mq14_complete`, `ms_w3_mq15_complete`) to `$storyMilestoneOrder`.

---

## Verification Plan

### Automated Tests
- Run Gradle checks to verify all world structures, progression paths, and dialogue configurations are valid:
  ```powershell
  .\gradlew.bat :app:runAssetIntegrity
  ```
- Author a new unit test suite [Hub4CriticalFlowTest.kt](file:///C:/Users/jctho/StudioProjects/StarbornAndroid/app/src/test/java/com/example/starborn/domain/dialogue/Hub4CriticalFlowTest.kt) (covering Hub 2 / Penthouse Heist flow):
  - Simulates MQ_13 social engineering, sensor bypass, and lobby infiltration.
  - Simulates MQ_14 Archive entry, puzzle resolution, and Lens acquisition.
  - Simulates MQ_15 alarm breakout, Administrator boss encounter, and Astra launch warp to World 4.
  - Simulates SQ_14 and SQ_15 completions and checks rewards.

### Manual Verification
- Deploy build to a connected emulator.
- Trigger transition warp to `spire_laundry_service`.
- Test the dialogue triggers in Skypark and the boss battle on the Landing Pad.
