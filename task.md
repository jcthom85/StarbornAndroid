# Starborn: World 3 Hub 2 Progress Tracker

> Current takeover context, asset inventory, validation commands, and recommended execution order are documented in `antigravity_handoff.md`.

This file tracks the step-by-step implementation of World 3 Hub 2 ("Upper City / Penthouse Heist").

## Progress Checklist

- `[x]` Phase 1: Setup & Topology
    - `[x]` Register `hub_6_upper_city` in `hubs.json`
    - `[x]` Register Hub 6 nodes in `hub_nodes.json`
    - `[x]` Add room data for Upper City rooms in `rooms.json`
- `[x]` Phase 2: Logic & Content
    - `[x]` Add Main and Side Quests in `quests.json`
    - `[x]` Add transition and progress events in `events.json`
    - `[x]` Implement dialogue trees in `dialogue.json`
    - `[x]` Define new items and accessories in `items.json`
    - `[x]` Define new enemies in `enemies.json`
- `[x]` Phase 3: Scripting & Verification
    - `[x]` Add new milestones to `$storyMilestoneOrder` in `validate_room_presence.ps1`
    - `[x]` Author `Hub4CriticalFlowTest.kt` unit test
    - `[x]` Run static asset integrity check (`.\gradlew.bat :app:runAssetIntegrity`)
    - `[x]` Run unit tests (`.\gradlew.bat :app:testDebugUnitTest`)

## World 4 Hub 1: Slag Pits

- `[x]` Register `world_4`, `hub_7_slag_pits`, and Slag Pits hub nodes
- `[x]` Expand Foundry rooms: Obsidian Shelf, Slag River, Cooling Springs, Waste Intake, Service Airlock
- `[x]` Wire W3 finale launch into `w4_mq16`
- `[x]` Add main quests `w4_mq16` and `w4_mq17`
- `[x]` Add side quests `w4_sq16`, `w4_sq17`, and `w4_sq18`
- `[x]` Add Foundry events, dialogue, milestones, items, skills, enemies, and theme
- `[x]` Add `Hub5CriticalFlowTest.kt`
- `[x]` Run static asset integrity check (`.\gradlew.bat :app:runAssetIntegrity`)
- `[x]` Run unit tests (`.\gradlew.bat :app:testDebugUnitTest`)

## World 4 Hub 2: Assembly Line

- `[x]` Register `hub_8_assembly_line` and Assembly Line hub nodes
- `[x]` Add rooms: Conveyor Belt, Conditioning Chamber, The Forge, Power Core, Titan Dock
- `[x]` Wire Slag Pits completion into `w4_mq18`
- `[x]` Add main quests `w4_mq18`, `w4_mq19`, and `w4_mq20`
- `[x]` Add side quests `w4_sq19` and `w4_sq20`
- `[x]` Add Assembly Line events, dialogue, milestones, items, skills, and enemies
- `[x]` Add `Hub6CriticalFlowTest.kt`
- `[x]` Run static asset integrity check (`.\gradlew.bat :app:runAssetIntegrity`)
- `[x]` Run unit tests (`.\gradlew.bat :app:testDebugUnitTest`)

## World 5 Hub 1: Orbital Ring

- `[x]` Register `world_5`, `hub_9_orbital_ring`, and Orbital Ring hub nodes
- `[x]` Add rooms from Executive Dock through the Server Farm
- `[x]` Wire the Foundry escape into `w5_mq21`
- `[x]` Add main quests `w5_mq21` and `w5_mq22`
- `[x]` Add side quests `w5_sq21`, `w5_sq22`, and `w5_sq23`
- `[x]` Add Orbital Ring events, dialogue, milestones, items, skills, enemies, NPCs, and theme
- `[x]` Add `Hub7CriticalFlowTest.kt`
- `[x]` Run static asset integrity check (`.\gradlew.bat :app:runAssetIntegrity`)
- `[x]` Run unit tests (`.\gradlew.bat :app:testDebugUnitTest`)

## World 5 Hub 2: Deep Ring

- `[x]` Register `hub_10_deep_ring` and Deep Ring nodes
- `[x]` Add Anchor Chamber, Throne Room, The Tear, and World 6 Campfire handoff
- `[x]` Wire `w5_mq22` completion into `w5_mq23`
- `[x]` Add main quests `w5_mq23`, `w5_mq24`, and `w5_mq25`
- `[x]` Add side quests `w5_sq24` and `w5_sq25`
- `[x]` Add finale events, dialogue, milestones, items, skills, enemies, NPCs, and Source theme
- `[x]` Register the minimal World 6 Event Horizon entry
- `[x]` Add `Hub8CriticalFlowTest.kt`
- `[x]` Run static asset integrity check (`.\gradlew.bat :app:runAssetIntegrity`)
- `[x]` Run unit tests (`.\gradlew.bat :app:testDebugUnitTest`)

## World 6 Hub 1: Event Horizon

- `[x]` Expand `hub_11_event_horizon` and register Event Horizon nodes
- `[x]` Add the three nightmare rooms, Echo Mines, elevator, and memory bridge
- `[x]` Wire the Tear transition into `w6_mq26`
- `[x]` Add main quests `w6_mq26`, `w6_mq27`, and `w6_mq28`
- `[x]` Add side quests `w6_sq26`, `w6_sq27`, and `w6_sq28`
- `[x]` Add nightmare events, dialogue, milestones, Key relic, passives, and enemies
- `[x]` Register the minimal Singularity Memory Stair handoff
- `[x]` Add `Hub9CriticalFlowTest.kt`
- `[x]` Run static asset integrity check (`.\gradlew.bat :app:runAssetIntegrity`)
- `[x]` Run unit tests (`.\gradlew.bat :app:testDebugUnitTest`)

## World 6 Hub 2: The Singularity

- `[x]` Complete `hub_12_singularity` with Spire of Thought, Center, and New World nodes
- `[x]` Add the final ascent, boss-rush rooms, final arena, and epilogue room
- `[x]` Wire `w6_mq28` completion into `w6_mq29`
- `[x]` Add main quests `w6_mq29` and `w6_mq30`
- `[x]` Add side quests `w6_sq29` and `w6_sq30`
- `[x]` Add final enemies, two Ascended boss phases, skills, rewards, and milestones
- `[x]` Add final-note and epilogue/credits cinematics
- `[x]` Persist `ms_game_complete` and `ms_credits_seen`
- `[x]` Add `Hub10CriticalFlowTest.kt`
- `[x]` Run static asset integrity check (`.\gradlew.bat :app:runAssetIntegrity`)
- `[x]` Run unit tests (`.\gradlew.bat :app:testDebugUnitTest`)

## Art Production

- `[x]` Read and apply the canonical Starborn art-production guides
- `[x]` Establish and approve the late-game room-background style gate
- `[x]` Generate, inspect, install, and wire all World 6 room backgrounds
- `[x]` Generate, inspect, install, and wire all World 5 room backgrounds
- `[x]` Generate and wire World 3 and World 4 room backgrounds
- `[x]` Replace placeholder hub maps and node icons
- `[ ]` Generate missing NPC portraits and enemy combat sprites
    - `[ ]` Replace borrowed NPC portraits for `the_warden`, `jax`, `mika`, `curator`, `lab_terminal`, `thorne`, `maintenance_bot`, `elara`, and `vale`
    - `[ ]` Create missing enemy files: `core_drill_behemoth_combat.png`, `mutated_crawler_combat.png`, `sentinel_droid_combat.png`, `aero_drone_combat.png`, `sentinel_orb_combat.png`, `ruin_guardian_combat.png`, and `phantom_assassin_combat.png`
    - `[ ]` Wire identity-specific NPC portraits and run `validate_dialogue_emotes.py`
- `[ ]` Complete audio production and assignment
- `[ ]` Run full campaign playtest and balance pass
