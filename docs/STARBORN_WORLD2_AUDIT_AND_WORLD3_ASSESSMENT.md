# STARBORN — World 2 Quality Audit & World 3 Development Assessment

**Repository Snapshot Audited:** `040e21d0` (`main`, Release `1.3.100`)  
**Scope:** Worlds 1 (`world_1`), 2 (`world_2`), and 3 (`world_3`) across `app/`, `gameRuntime/`, and `shared/`

---

## Phase 1 — Project Architecture & World 1 Benchmark

### 1.1 System & Content Architecture Map

Starborn is structured as a multi-module Kotlin/Jetpack Compose application ([settings.gradle.kts](file:///Users/jason/Developer/StarbornAndroid/settings.gradle.kts)) separating core runtime logic (`:gameRuntime`), shared UI/puzzle widgets (`:shared`), and the Android presentation layer plus JSON content catalogs (`:app`).

| Domain | Core Runtime & UI Files | Primary Content Catalogs (`app/src/main/assets/`) |
|---|---|---|
| **World, Hubs, Nodes & Rooms** | [`ExplorationController`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/session/ExplorationController.kt), [`HubViewModel`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/feature/hub/viewmodel/HubViewModel.kt), [`NodeProgression`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/node/NodeProgression.kt), [`AstraTravel`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/travel/AstraTravel.kt), [`RoomPresentation`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/room/RoomPresentation.kt) | [worlds.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/worlds.json), [hubs.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/hubs.json), [hub_nodes.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/hub_nodes.json), [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json), [node_transitions.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/node_transitions.json) |
| **Quests, Milestones & Events** | [`EventManager`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/event/EventManager.kt), [`QuestRuntimeManager`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/quest/QuestRuntimeManager.kt), [`MilestoneRuntimeManager`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/milestone/MilestoneRuntimeManager.kt), [`EventHooks`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/event/EventHooks.kt) | [quests.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/quests.json), [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json), [milestones.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/milestones.json) |
| **NPCs, Dialogue & Cinematics** | [`DialogueService`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueService.kt), [`DialogueTriggerParser`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueTriggerParser.kt), [`DialogueTriggerLogic`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueTriggerLogic.kt), [`CinematicCoordinator`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/cinematic/CinematicCoordinator.kt), [`AppServices.isDialogueConditionMet`](file:///Users/jason/Developer/StarbornAndroid/app/src/main/java/com/example/starborn/di/AppServices.kt#L2523-L2568) | [npcs.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/npcs.json), [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json), [cinematics.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/cinematics.json) |
| **Combat, Skills & Progression** | [`CombatController`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/feature/combat/viewmodel/CombatController.kt), [`CombatEngine`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/combat/CombatEngine.kt), [`CombatEnemyAI`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/combat/CombatEnemyAI.kt), [`CrewBuildPrototype`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/OriginBuilds.kt), [`LevelingManager`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/leveling/LevelingManager.kt) | [enemies.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json), [skills.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/skills.json), [statuses.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/statuses.json), [enemy_movement.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemy_movement.json), [leveling_data.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/leveling_data.json) |
| **Puzzles, Crafting, Shops & Minigames** | [`CounterTunePanel`](file:///Users/jason/Developer/StarbornAndroid/shared/src/main/java/com/example/starborn/ui/tuning/CounterTunePanel.kt), [`PrismOpticsPanel`](file:///Users/jason/Developer/StarbornAndroid/shared/src/main/java/com/example/starborn/ui/prism/PrismOpticsPanel.kt), [`CraftingService`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/crafting/CraftingService.kt), [`FishingService`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/fishing/FishingService.kt), [`ShopController`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/session/ShopController.kt), [`ArcadeService`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/arcade/ArcadeService.kt) | [tuning_puzzles.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/tuning_puzzles.json), [items.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/items.json), [shops.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/shops.json), [recipes_tinkering.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/recipes_tinkering.json), [recipes_cooking.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/recipes_cooking.json), [recipes_fishing.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/recipes_fishing.json) |
| **Atmosphere, Audio & Tutorials** | [`ExplorationScreen`](file:///Users/jason/Developer/StarbornAndroid/app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt), [`AudioRouter`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/audio/AudioRouter.kt), [`TutorialRuntimeManager`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/tutorial/TutorialRuntimeManager.kt), [`ThemeRepository`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/data/repository/ThemeRepository.kt) | [themes.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/themes.json), [theme_styles.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/theme_styles.json), [environmental_effects.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/environmental_effects.json), [audio_bindings.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/audio_bindings.json), [audio_catalog.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/audio_catalog.json) |

---

### 1.2 World 1 ("The Mines") — Established Quality Benchmark

World 1 ([worlds.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/worlds.json#L2-L7)) spans **2 hubs** (`hub_1_homestead`, `hub_2_logistics`), **10 hub nodes**, **82 bespoke rooms** (0 `_scale_` filler corridors), **5 main quests** (`w1_mq01`–`w1_mq05`), **5 side quests** (`w1_sq01`–`w1_sq05`), **9 story cinematics**, **105+ world-specific dialogue nodes** across 12 distinct speakers (Nova: 23 lines, Jed: 22, Zeke: 21, Boggs: 14, Doc: 5, Scrapper: 5, Warden: 2, Hank: 2, Mira: 1, etc.), and **8 enemies** culminating in `the_iron_warden`.

#### What Makes World 1 Successful
1. **Grounded Emotional Spine & Stakes:** World 1 anchors its cosmic premise (Nova's resonance mark awakening at `mine_collapse` in [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L728-L828)) in an immediate working-class crisis: Dominion Supervisor Mara Thorne's impossible ore quota, power Rationing Protocol 9, and the evacuation lock on Sector 4.
2. **earned Character Relationships:**
   - **Jed** evolves from a gruff mentor in `town_1` (`jed_w1_mq01_intro_1`–`3`, [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L222-L244)) into a father figure who stays behind at `launch_lift` to manually hold the hydraulic brake against Dominion lockdown (`jed_w1_mq05_lift_farewell_1`–`5`, [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L325-L359)).
   - **Zeke** earns his companion spot through active banter and competence: bluffing through Form 88-B at `gate_checkpoint` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L468-L554)), coaching Nova over comms in `echo_chamber` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L759-L785)), and hot-wiring the pod core (`zeke_w1_mq05_pod_core_1`–`3`, [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L808-L863)).
   - **Nova** is an active protagonist with 23 spoken dialogue nodes, plus distinct player dialogue choices (`zeke_gate_intro_3`, `w1_sq04_routing_choice`).
3. **Dynamic Environmental Reactivity:**
   - 21 of 82 rooms have persistent `state` definitions and 33 have `description_variants`.
   - Restoring the generator (`evt_mine_power_on`, [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L1772-L2148)) dynamically flips 6 dark mine rooms (`mine_entry` through `mine_foreman`) from `"dark": true` to `"dark": false`, swapping `description_dark` to lit descriptions and shifting room state to `"powered"`.
4. **Authentic Puzzle & Mechanic Variety:**
   - **Interactive Counter-Tune Minigame:** `w1_echo_counter_tune` ([tuning_puzzles.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/tuning_puzzles.json#L3-L43)) uses the custom Compose [`CounterTunePanel`](file:///Users/jason/Developer/StarbornAndroid/shared/src/main/java/com/example/starborn/ui/tuning/CounterTunePanel.kt) waveform UI.
   - **Spatial Multi-Room Puzzle:** `mine_junction` ore-cart track alignment (`w1_cart_track_toggle` + `w1_cart_brake_release`, [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L15389-L15648)) requires reading environmental clues before releasing the brake.
   - **Dialogue & Moral/Tactical Choices:** Form 88-B bureaucratic deflection (`zeke_gate_intro_3`) and the Rebel Terminal routing dilemma (`w1_sq04_routing_choice`, [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L3258-L3301)).
   - **Hands-On Crafting & Cooking Integration:** Tinkering the `broken_stabilizer` into `stabilizer_module` (`w1_mq01`) is bookended by `scene_jed_tinkering_prompt` and `scene_jed_tinkering_wrapup`.
5. **Paced Combat & Tutorial Onboarding:**
   - Combat ramps from a single `faulted_loader` (48 HP) to `acoustic_bulwark` (teaching Guard Break via mandatory `w1_sq03` before the gate opens), roaming `pressure_hauler` patrols ([enemy_movement.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemy_movement.json#L3-L25)), and `the_iron_warden` (240 HP, `the_warden_combat.png`, [enemies.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L329-L374)).

#### Verified Shortcomings in World 1 (Do Not Copy Into Subsequent Worlds)
1. **Broken Production Dialogue Condition Evaluator:** [`AppServices.isDialogueConditionMet`](file:///Users/jason/Developer/StarbornAndroid/app/src/main/java/com/example/starborn/di/AppServices.kt#L2523-L2568) and [`DialogueTriggerLogic.isDialogueConditionMet`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueTriggerLogic.kt#L9-L54) fall through to `else -> true` for `quest_task_done`, `quest_task_not_done`, `milestone_set`, and `quest_not_completed`. In World 1, this affects 5 dialogue nodes (`jed_w1_mq01_repair_done_1`, `warden_boss_intro_1`, `zeke_w1_mq05_defeat_warden_1`, `zeke_w1_mq05_pod_core_1`, `w1_sq04_routing_entry`).
2. **Unreachable Mechanic Shop & Orphaned Shops:** `mechanic_shop` ([shops.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/shops.json#L2-L77)) cannot be opened in-game because `mechanic_mira` ([npcs.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/npcs.json#L399-L422)) places `"shop_id": "mechanic_shop"` inside an unparsed `"interactions"` array rather than top-level `"shop_id"` + `"trade_after_dialogue": true`. Three other shops (`weapon_shop`, `armor_shop`, `accessory_shop` in [shops.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/shops.json#L78-L292)) have zero references anywhere.
3. **Backtrack-Locked Fishing Spot:** World 1's only fishing room (`mine_landing`, [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L2492-L2501)) requires `"condition": "ms_w2_pod_examined"`, locking players out of fishing during their entire World 1 playthrough.
4. **Late-Mine Lighting & Thin Room Density:** 8 rooms in `mine_depths` and `echo_chamber` (`mine_conveyor` through `echo_walkway`, [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L2721-L3066)) remain permanently `"dark": true` without `description_dark` or a power restoration event, and drop to 2 passive flavor clicks per room.
5. **Unreachable `launch_rail` & Duplicate Launch SFX:** `launch_rail` ([rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L3580-L3603)) has no inbound links (`launch_pad` exits directly north to `launch_hangar`), and `w1_mq05_use_nav_console` ([events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L3057-L3158)) fires the same launch/impact sound cues that `scene_launch_crash` ([cinematics.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/cinematics.json#L479-L520)) immediately replays.

---

### 1.3 Practical Quality Rubric (Applied to Worlds 2 & 3)

| Dimension | World 1 Benchmark Target |
|---|---|
| **1. Narrative & Cinematics** | Clear act structure, personal + macro stakes, 6–9 cinematics framing major transitions, active protagonist voice. |
| **2. Character Development** | ~80–100+ character dialogue lines per world; companions react to story beats, quests, and recruits; villains have presence. |
| **3. Exploration & Room Density** | 100% bespoke rooms (no filler corridors), 3–4 meaningful actions per room, reactive room states/variants (`description_variants`). |
| **4. Gameplay & Puzzle Variety** | Mix of bespoke UI puzzles (`CounterTunePanel` / `PrismOpticsPanel`), spatial/environmental puzzles, dialogue choices, and crafting. |
| **5. Encounter & Boss Design** | 12–16 encounters across distinct enemy archetypes, escalating mechanics/AI, unique enemy sprites, and a multi-phase/signature boss. |
| **6. Quest Design** | 5 main + 5 side quests with clear gating, zero premature turn-ins or RNG softlocks, and narrative payoff on completion. |
| **7. Pacing** | Smooth alternation of dialogue, exploration, puzzle-solving, and combat across 10 nodes (~2–3 hours total playtime). |
| **8. World Identity** | Registered theme/palette in `themes.json`, complete `audio_bindings.json` coverage, and cohesive sensory writing. |
| **9. Technical Completeness** | Connected node graphs, working dialogue/event conditions in production `AppServices`, valid item stats/drops, and reachable NPCs/shops. |

---

## Phase 2 — World 2 ("Sector 9") Quality Audit

### 2.1 Overview & Comparison Against World 1

World 2 ([worlds.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/worlds.json#L9-L13)) spans **2 hubs** (`hub_3_sector9` with 50 rooms, `hub_4_facility` with 43 rooms), **10 nodes**, **93 bespoke rooms** (0 `_scale_` filler corridors), **5 main quests** (`w2_mq01`–`w2_mq05`), **5 side quests** (`w2_sq01`–`w2_sq05`), **2 cinematics** (`scene_anchor_drill`, `scene_w2_astra_departure`), **62 world dialogue nodes**, and **7 enemies** culminating in `the_beast`.

#### Where World 2 Surpasses World 1
- **Sub-Branch Exploration & Puzzle Architecture:** Every node in `hub_3_sector9` and `hub_4_facility` features bespoke multi-room sub-branches (`sector9_crash_debris_trail` → `sector9_salvage_pit`, `sector9_canopy_ropewalk` → `sector9_signal_perch`, `sector9_bridge_cable_walk` → `sector9_winch_house`, `sector9_root_sump` → `sector9_spore_vent`, `sector9_vault_relays` → `sector9_vault_seal_lock`, `sector9_hangar_fuel_bay` / `sector9_hangar_gantry`). Unlike World 1's single-room toggles, World 2 turns bridge deployment, root-clearing, stasis thawing, and ship pre-flight into physical multi-room exploration loops.
- **Tinkering & Shop Integration:** Unlike World 1's inaccessible `mechanic_shop`, `sentinel_shop` ([shops.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/shops.json#L293-L353)) works at `sector9_gate_control` (`trade_sentinel_unit`), and Tinkering recipes (`marsh_tonic`, `shock_trap`, `rapid_capacitor`) are woven into side and main quests.
- **Interactive Tuning Puzzles:** World 2 includes two [`CounterTunePanel`](file:///Users/jason/Developer/StarbornAndroid/shared/src/main/java/com/example/starborn/ui/tuning/CounterTunePanel.kt) puzzles (`w2_beacon_harmonic_tune` and `w2_gate_anchor_tune` in [tuning_puzzles.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/tuning_puzzles.json#L45-L126)).

#### Where World 2 Equals World 1
- **Environmental Writing & Room State Reactivity:** All 93 rooms have rich descriptions, 3–4 interactive actions per room, and 17 rooms feature `description_variants` that evolve as machinery is restored (`sector9_crash_site`, `sector9_bridge`, `sector9_lab_entry`, `sector9_stasis_chamber`, `sector9_source_gate`, `sector9_hangar_bay`).
- **Audio Coverage:** All 93 World 2 rooms have explicit music and ambient bindings in [audio_bindings.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/audio_bindings.json#L101-L145) (`bgm_world_sector9`, `amb_swamp_drones`, `amb_facility_hum`).

#### Where World 2 Is Weaker Than World 1
1. **Severe Character Dialogue & Protagonist Voice Deficit:**
   - World 2 has **62 dialogue nodes total**, but **20 of those belong to inanimate consoles** (`Stasis Console`: 7, `Source Gate Console`: 5, `Mural Array`: 5, `Vent Scanner`: 3). Only **42 character dialogue lines** exist across the entire world (vs. ~100 in World 1).
   - **Nova speaks only 2 lines in all of World 2** (`orion_w2_awakening_2` at [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L9-L17) and `ghost_confrontation_2` at [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1080-L1089), the latter mislabeled `"speaker": "Player"`).
   - **Gh0st Recruitment Has Zero Post-Boss Dialogue:** In `w2_mq04`, after defeating `the_beast` (a corrupted security drone that was once Gh0st's partner), `w2_mq04_recruit_ghost` ([events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L3990-L4040)) recruits Gh0st into the permanent party via a single toast popup (`"Gh0st lowers their rifle and syncs their deck to your comms. 'Deal's a deal. Lead on.'"`) with **zero dialogue tree**.
   - **Finding the *Astra* Has Zero Crew Conversation:** Discovering the ancient starship *Astra* in `sector9_hangar_bay` (`w2_mq05_locate_ship`, [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L4185-L4233)) is handled via a 2-sentence popup (`"The tarp falls away to reveal a sleek, dormant starship..."`) with zero dialogue from Orion, Zeke, or Gh0st until the departure cinematic.
   - **Stabilizing Zeke at the Crash Site Is Silent:** `w2_mq01_stabilize_zeke` ([events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L3264-L3316)) heals Zeke via a text popup without a single spoken line from Zeke or Nova.
2. **Cinematic Scarcity:** World 2 has only **2 cinematics** (`scene_anchor_drill` and `scene_w2_astra_departure`) compared to World 1's 9 cinematics. Neither Orion's awakening from 1,000-year stasis nor the *Astra* reveal has a cinematic sequence.
3. **Antagonist Absence:** The Dominion has zero active presence, transmissions, or pursuit in World 2, creating an urgency vacuum between escaping the mines (`w1_mq05`) and infiltrating the Spire (`w3_mq11`).
4. **Dead-End "Sky Route" Node (`the_sky`):** The 10th node (`the_sky`, [hub_nodes.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/hub_nodes.json#L597-L626), 6 rooms from `sector9_sky_route` to `sector9_sky_threshold` in [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L12781-L12983)) sits *after* the hangar launch room and contains only passive "route preview" text plaques (`"Inspect the launch corridor..."`) with no quests, enemies, or rewards, while actual departure happens in `sector9_hangar_bay`.

---

### 2.2 Prioritized Findings for World 2

#### Verified Technical Defects

##### [W2-DEF-01] Production `isDialogueConditionMet` Ignores `quest_task_done`, `quest_task_not_done`, `milestone_set`, and `quest_not_completed`
- **Classification:** Verified Defect (Breaks Quest Progression & Dialogue Gating in Production APK)
- **Priority:** **Critical** | **Estimated Effort:** 1–2 hours
- **Exact Locations:**
  - [`AppServices.isDialogueConditionMet`](file:///Users/jason/Developer/StarbornAndroid/app/src/main/java/com/example/starborn/di/AppServices.kt#L2523-L2568)
  - [`DialogueTriggerLogic.isDialogueConditionMet`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueTriggerLogic.kt#L9-L54)
  - Masked in tests by custom lambdas in [`LegalRouteCampaignRunnerTest.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/test/java/com/example/starborn/domain/session/LegalRouteCampaignRunnerTest.kt#L2073-L2109) and [`World2RefinementTest.kt`](file:///Users/jason/Developer/StarbornAndroid/app/src/test/java/com/example/starborn/World2RefinementTest.kt#L455-L491)
- **Problem:** Both [`AppServices.kt`](file:///Users/jason/Developer/StarbornAndroid/app/src/main/java/com/example/starborn/di/AppServices.kt#L2563) and [`DialogueTriggerLogic.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueTriggerLogic.kt#L50) only handle `milestone:`, `quest_active:`, `quest_completed:`, `quest_stage:`, `item:`, and `level:`, returning `else -> true` for every other token. However, [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json) uses `quest_task_done:`, `quest_task_not_done:`, `milestone_set:`, and `quest_not_completed:` across 18 dialogue nodes.
- **Why It Matters to the Player (Concrete World 2 Failures):**
  1. **Instant `w2_sq01` ("Botanist") Exploit/Skip:** Talking to Zeke once starts `w2_sq01` (`zeke_w2_sq01_intro_1`–`2`). Talking to Zeke a second time *immediately* evaluates `zeke_w2_sq01_turnin_1` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L922-L929), gated by `quest_active:w2_sq01,quest_task_done:w2_sq01:scan_flora_5`) as `true` with **0 of 5 flora scanned**, firing `w2_sq01_complete` and granting 100 XP, 2 Painkillers, and 2 AP.
  2. **Infinite Crash-Site Intro Loop:** `zeke_w2_crash_1` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L865-L871), gated by `quest_task_done:w2_mq01:stabilize_zeke,quest_task_not_done:w2_mq01:talk_zeke`) evaluates `quest_task_not_done` as `true` forever, repeating the intro dialogue every time the player taps Zeke at the crash site and permanently hiding `zeke_w2_crash_repeat_1`–`3` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L891-L914)).
  3. **Shadowed Success Lines in `w2_mq03` and `w2_mq05`:** `zeke_w2_mq03_puzzle_hint_1` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L941)) and `orion_w2_mq05_gate_1` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L973)) precede their `_success_1` counterparts in [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json) and use `quest_task_not_done`, so they permanently shadow `zeke_w2_mq03_puzzle_success_1` and `orion_w2_mq05_gate_success_1` while those quests are active.
  4. **Orion Stasis Replay:** `orion_w2_awakening_1` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L3-L8)) replays from the beginning if the player talks to Orion again before finishing `w2_mq02`.
- **Recommended Fix:** Update [`DialogueTriggerLogic.isDialogueConditionMet`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueTriggerLogic.kt#L9-L54) and [`AppServices.isDialogueConditionMet`](file:///Users/jason/Developer/StarbornAndroid/app/src/main/java/com/example/starborn/di/AppServices.kt#L2523-L2568) to evaluate `milestone_set`, `milestone_not_set`, `quest_not_started`, `quest_not_completed`, `quest_task_done`, and `quest_task_not_done` against `sessionStore.state.value` (matching [`EventManager.checkSingleCondition`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/event/EventManager.kt#L1680-L1764)), and add a unit test that invokes `DialogueTriggerLogic.isDialogueConditionMet` directly.

##### [W2-DEF-02] Missing `"swamp"` Theme Entry in `themes.json` and `theme_styles.json`
- **Classification:** Verified Defect (Visual/Atmospheric Fallback Bug)
- **Priority:** **High** | **Estimated Effort:** 1 hour
- **Exact Locations:**
  - [worlds.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/worlds.json#L12) (`"theme": "swamp"`)
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L9000-L12983) (all 93 World 2 rooms specify `"env": "swamp"`)
  - [themes.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/themes.json#L1-L72) and [theme_styles.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/theme_styles.json#L1-L40)
  - [`ThemeRepository.getTheme`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/data/repository/ThemeRepository.kt#L20-L26)
- **Problem:** All 93 rooms in World 2 specify `"env": "swamp"`, and `environmental_effects.json` defines `"swamp"` (`"weather": "spore_drift"`), but `"swamp"` does not exist in `themes.json` or `theme_styles.json`. `ThemeRepository.getTheme("swamp")` returns `null`, causing the UI to fall back to default gray colors instead of a bioluminescent marsh palette.
- **Recommended Fix:** Add a `"swamp"` theme entry to [themes.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/themes.json) and [theme_styles.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/theme_styles.json) (and consider `"facility"` or `"ruins"` for `hub_4_facility` interior rooms).

##### [W2-DEF-03] Inverted `"condition_unmet_message": "Objective completed."` on 12 Main-Quest Actions
- **Classification:** Verified Defect (Misleading Player UX)
- **Priority:** **High** | **Estimated Effort:** 1 hour
- **Exact Locations:** [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json) at lines [L9031](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L9031), [L9037](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L9037), [L9106](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L9106), [L9112](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L9112), [L9333](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L9333), [L10862](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L10862), [L11256](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L11256), [L11436](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L11436), [L11735](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L11735), [L11947](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L11947), [L12614](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L12614), [L12627](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L12627)
- **Problem:** Twelve main-quest room actions (including `Search for salvage`, `Stabilize Zeke`, `Deploy Bridge`, `Clear Roots`, `Start Purge`, `Power Stasis`, `Awaken Orion`, `Attempt Gate`, `Locate Astra`, and `Board Astra`) set `"condition_unmet_message": "Objective completed."`. When a player clicks these buttons *before* meeting prerequisites (e.g., clicking `Deploy Bridge` before releasing the winch lock, or `Board Astra` before priming fuel and clamps), the game tells them `"Objective completed."` instead of explaining what prerequisite is still missing.
- **Recommended Fix:** Replace `"Objective completed."` on unmet conditions with specific prerequisite hints (e.g., `"The bridge cables are still locked inside the Winch House."`, `"Pre-flight fuel and clamp checks are not yet complete."`).

##### [W2-DEF-04] Disconnected `source_gate` Hub Node Subgraph
- **Classification:** Verified Defect (Broken Hub Node Topology / Minimap Navigation)
- **Priority:** **High** | **Estimated Effort:** 1 hour
- **Exact Locations:**
  - [hub_nodes.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/hub_nodes.json#L543-L571) (`source_gate` node rooms list)
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L12258-L12564) (`sector9_source_gate`, `sector9_power_turbine`, `sector9_power_manifold`, `sector9_power_breaker_deck`, `sector9_power_valve`)
- **Problem:** In `hub_4_facility`, `source_gate` contains 9 rooms (`sector9_source_gate` + 4 gate rooms, and `sector9_power_turbine` + 3 power rooms). However, `sector9_power_turbine` only connects west to `sector9_stasis_coolant_lines` (which belongs to the `stasis_chamber` node) and has no room connection to `sector9_source_gate` or its sub-rooms. Entering the `source_gate` node from the hub map drops the player at `sector9_source_gate`, from which the 4 power rooms in the same node cannot be reached without exiting into the `stasis_chamber` node.
- **Recommended Fix:** Either move `sector9_power_turbine`, `sector9_power_manifold`, `sector9_power_breaker_deck`, and `sector9_power_valve` into the `stasis_chamber` node in [hub_nodes.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/hub_nodes.json#L510-L571) (since they power the stasis chamber in `w2_mq03`), or add a physical door link between `sector9_power_breaker_deck` and `sector9_gate_cable_trench`.

##### [W2-DEF-05] `rapid_capacitor` Stat/Description Mismatch & Encoding Typo
- **Classification:** Verified Defect (Item Mechanics vs. Description)
- **Priority:** **Medium** | **Estimated Effort:** 0.5 hours
- **Exact Locations:**
  - [items.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/items.json#L1408-L1424)
  - [recipes_tinkering.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/recipes_tinkering.json#L312-L334)
- **Problem:** `rapid_capacitor` (crafted in `w2_sq05`) states `"Reduces skill cooldowns"`, but its equipment definition only grants `"stats": { "speed": 5, "agility": 3 }` with no cooldown reduction effect. Additionally, [recipes_tinkering.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/recipes_tinkering.json#L332) contains an encoding corruption typo: `"Gh0st?s overclocked capacitor mod."` (`?` instead of apostrophe).
- **Recommended Fix:** Update the description in `items.json` and `recipes_tinkering.json` to match its Speed/Agility boost (or wire cooldown reduction in `CombatEngine.kt`) and fix `"Gh0st?s"` to `"Gh0st's"`.

##### [W2-DEF-06] `ShopAction` Ignores `dialogue_id` (`sentinel_shop_intro` Never Fires)
- **Classification:** Verified Defect (Dead Content / Missing Shop Intro)
- **Priority:** **Medium** | **Estimated Effort:** 1–2 hours
- **Exact Locations:**
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L11141-L11147) (`trade_sentinel_unit` action specifies `"dialogue_id": "sentinel_shop_intro"`)
  - [`RoomAction.ShopAction`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/data/model/RoomAction.kt#L54-L66)
  - [`ExplorationController.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/session/ExplorationController.kt#L5338-L5361) and [`openShop`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/session/ExplorationController.kt#L6179-L6186)
- **Problem:** `RoomAction.ShopAction` does not deserialize `dialogue_id`, and `ExplorationController.openShop` opens the shop overlay immediately without triggering `sentinel_shop_intro` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L3672-L3680)) or calling `shopController.showShopGreeting(shop)`.
- **Recommended Fix:** Add `val dialogueId: String? = null` to `RoomAction.ShopAction` and trigger the intro dialogue (or `showShopGreeting`) when opening the shop.

##### [W2-DEF-07] Zero-Reward Enemies (`razor_vine`, `stalker_vine`), W1 Enemy Sprite Reuse, and Stale `siren_skimmer` Reference
- **Classification:** Verified Defect (Combat Rewards & Asset Reuse)
- **Priority:** **Medium** | **Estimated Effort:** 1 hour (data/code) + 4–8 hours (art)
- **Exact Locations:**
  - [enemies.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L500-L568) (`razor_vine`, `stalker_vine` have `"credits": 0` and no `drops`)
  - [enemies.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L578) (`bog_lurker` uses `siren_skimmer_combat.png`), [L614](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L614) (`corrupted_guardian` uses `acoustic_bulwark_combat.png`), [L650](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L650) (`security_drone_mkii` uses `siren_skimmer_combat.png`), [L1142](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L1142) (`harmonic_suppressor` uses `resonance_buoy_combat.png`)
  - [`CombatController.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/feature/combat/viewmodel/CombatController.kt#L3515) (checks `currentRoom.id == "sector9_canopy"` for `siren_skimmer`)
- **Problem:** Fighting `razor_vine` and `stalker_vine` yields 0 credits and 0 item drops (only XP). Furthermore, 4 of the 7 World 2 enemy types reuse World 1 combat sprites, and `CombatController.kt` still checks for `siren_skimmer` in `sector9_canopy` even though `razor_vine` replaced it.
- **Recommended Fix:** Add bioluminescent/plant crafting drops and credit rewards to `razor_vine` and `stalker_vine`, update `CombatController.kt#L3515` to include `razor_vine`, and commission/assign distinct sprites for the 4 reused enemy types.

##### [W2-DEF-08] Duplicate `Stabilize Zeke` Inside Pod Interior & `"speaker": "Player"` Label
- **Classification:** Verified Defect (Spatial Continuity & Speaker Tag)
- **Priority:** **Low** | **Estimated Effort:** 1 hour
- **Exact Locations:**
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L9102-L9114) (`sector9_pod_interior` has `Stabilize Zeke` even though Zeke is outside in `sector9_crash_site`)
  - [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1082) (`ghost_confrontation_2` uses `"speaker": "Player"` instead of `"speaker": "Nova"`)
- **Recommended Fix:** Remove the duplicate `Stabilize Zeke` action from `sector9_pod_interior` (keep `Search for salvage` inside the pod and `Stabilize Zeke` outside at `sector9_crash_site`), and change `"speaker": "Player"` to `"speaker": "Nova"` in `ghost_confrontation_2`.

---

#### Subjective Design Recommendations (Bringing World 2 to World 1 Narrative Standard)

##### [W2-DES-01] Expand World 2 Character Dialogue (Gh0st Recruitment, *Astra* Discovery, Zeke Crash Recovery, and Nova's Voice)
- **Classification:** Design Recommendation
- **Priority:** **High** | **Estimated Effort:** 3–4 hours
- **Exact Locations:**
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L3264-L3316) (`w2_mq01_stabilize_zeke`), [L3990-L4040](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L3990-L4040) (`w2_mq04_recruit_ghost`), [L4185-L4233](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L4185-L4233) (`w2_mq05_locate_ship`)
  - [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L865-L1101)
- **Why It Matters:** World 2's two biggest permanent milestones—recruiting the 4th party member (**Gh0st**) and discovering the crew's starship (***Astra***)—currently happen in 1-sentence toast popups, and **Nova** only speaks twice in the entire world.
- **Recommended Improvement:**
  1. Replace the toast popup in `w2_mq04_recruit_ghost` with a 6–8 node dialogue exchange (`ghost_post_beast_1`–`6`) between Gh0st, Nova, Orion, and Zeke mourning "Unit 7" (`the_beast`) and establishing why Gh0st joins the crew.
  2. Trigger a 5–6 node crew dialogue (`astra_discovery_1`–`5`) on `w2_mq05_locate_ship` where Orion recognizes the *Astra*, Zeke reacts to its Pre-Collapse drive, and Nova takes command of the pre-flight prep.
  3. Trigger a brief 3-node exchange when stabilizing Zeke in `w2_mq01_stabilize_zeke`.

##### [W2-DES-02] Give `the_sky` Node a Purpose (or Fold Into Launch Sequence) & Add a Dominion Signal Beat
- **Classification:** Design Recommendation
- **Priority:** **Medium** | **Estimated Effort:** 2–4 hours
- **Exact Locations:**
  - [hub_nodes.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/hub_nodes.json#L597-L626) (`the_sky`)
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L12781-L12983) (`sector9_sky_route` through `sector9_sky_threshold`)
- **Why It Matters:** `the_sky` is currently a 6-room cul-de-sac of passive plaques (`"Inspect the launch corridor..."`) that players have no quest reason to visit because `Board Astra` launches directly from `sector9_hangar_bay`. Meanwhile, the Dominion vanishes completely during World 2.
- **Recommended Improvement:** Either route the final launch clamp/telemetry override into `sector9_sky_route` where Gh0st intercepts a Dominion pursuit transmission from Lt. Arden Vale / Supervisor Thorne locking onto the Spire's relay network, or lock/hide `the_sky` node so players aren't confused by an empty post-hangar node.

---

## Phase 3 — World 3 ("The Spire") Development Readiness Assessment

### 3.1 Inventory of World 3 Implementation Status

World 3 ([worlds.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/worlds.json#L15-L20)) is **structurally completable from start to finish**, and transitions into `world_4` (`pebble_station_dock` in [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L6128-L6221)). However, compared to Worlds 1 and 2, World 3 is **a functional skeleton / first-pass vertical slice** that relies heavily on single-click popup completions and `*_scale_*` filler rooms.

| Asset / System | Implemented Inventory in World 3 (`world_3`) | Status vs. World 1 Benchmark |
|---|---|---|
| **Hubs & Nodes** | 2 hubs (`hub_5_lower_city`, `hub_6_upper_city`), 10 nodes (5 Lower, 5 Upper) | Complete node graph, but 2 Upper City nodes (`spire_exec_lounge`, `spire_landing_pad`) have only **1 core room each** padded by 5 `_scale_` corridors. |
| **Rooms** | **70 rooms total**: **37 core rooms** + **33 `*_scale_01..05` filler rooms** ([rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L13291-L14613)) | **47% of World 3's rooms are generic scale corridors** with 1 passive flavor click (`flavor_spire_*_scale_*`) and zero NPCs/quests. Only 5 of 70 rooms have `state` and only 1 room (`spire_landing_pad_roof`) has `description_variants`. |
| **Main Quests** | 5 main quests (`w3_mq11` *Entry Fee*, `w3_mq12` *Inside Help*, `w3_mq13` *Dress the Part*, `w3_mq14` *The Vault*, `w3_mq15` *Escape Velocity*) | All 5 quests are wired and completable, but **`w3_mq12` (6 tasks) and `w3_mq13` (3 tasks) are 100% single-click popup buttons** with zero combat, zero puzzles, and almost no dialogue. |
| **Side Quests** | 6 side quests (`w3_sq11` *Neon Fix*, `w3_sq12` *Data Heist*, `w3_sq13` *Lost Drone*, `w3_sq14` *High Roller*, `w3_sq15` *Lab Rat*, `w3_sq16` *The Whistleblower*) | **`w3_sq11` has a permanent RNG softlock** (50% drop from 1 enemy in Upper City) and a broken `inspect_sign` trigger. `w3_sq16` is broken in production by `AppServices.isDialogueConditionMet`. |
| **Dialogue & Characters** | **43 world dialogue nodes** across the entire world (excluding `pip_default_1`–`3` and `collector_intro_1`). Main cast lines: Zeke (10), Elara (4), Orion (3), Nova (2), Gh0st (2), Thorne (2), Vale (1), Administrator Boss (0 reachable). | **Critically under-written.** Cameo/Easter-egg NPCs (`easter_egg_twins`, `jax_teller`, `neon_drifter`, `mika_circuit`, `salon_stylist`) have more than 2x the dialogue of the main story cast. Nova has 2 lines total; Orion has 3. |
| **Cinematics** | **0 wired cinematics.** `scene_act2_interlude` ([cinematics.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/cinematics.json#L784-L809)) exists in `cinematics.json` but has **0 triggers** in `events.json`. | Missing all cinematic framing (arrival, heist briefing, Elara/Lens discovery, and Arden Vale departure interlude). |
| **Combat & Bosses** | 7 enemy definitions (`sewer_drone`, `riot_guard`, `corporate_assassin`, `sentinel_mki`, `laser_turret`, `phase_stalker`, `administrator_boss`), placed in only **7 rooms total** (only 2 sewer rooms in all of Lower City). | **`riot_guard` is unused (0 spawns)**, swaps sprites with `corporate_assassin`, and has the beast skill `"alpha_howl"`. **`administrator_boss` (420 HP, 400 XP) is weaker than World 2's boss `the_beast` (520 HP, 500 XP)** and reuses World 1's `the_warden_combat.png`. |
| **Puzzles** | 1 tuning puzzle (`w3_chrono_flux_tune` in `w3_sq15`, missing `"presentation": "counter_tune"`). Main quest `w3_mq14` "Light Puzzle" is just 4 single-click buttons. | [`PrismOpticsPanel`](file:///Users/jason/Developer/StarbornAndroid/shared/src/main/java/com/example/starborn/ui/prism/PrismOpticsPanel.kt) is already built in `:shared` and wired in [`ExplorationController.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/session/ExplorationController.kt#L2762-L2800), but **not used in World 3**! |

---

### 3.2 Assessment of Key Characters in World 3 (Orion, Lt. Arden Vale, Elara, Zeke, Gh0st)

- **Orion:** Speaks only **3 lines** in World 3 (`orion_w3_mq14_vault_1`, `orion_w3_mq14_vault_2`, `orion_w3_mq14_lens_1` in [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1211-L1222)), all inside `spire_archive_vault`. Despite `w3_mq12` taking place in Zeke's Apartment to plan the heist (`w3_mq12_plan_heist`), Orion is not even spawned in `spire_zekes_apartment` ([rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L13142-L13174)) and has zero reaction to the Spire's Pre-Collapse Aethelgard technology until the vault.
- **Lt. Arden Vale:** Introduced as the overarching military antagonist in `vale_w3_mq15_escalation` ([dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1318-L1329)) and in the orphaned cinematic `scene_act2_interlude` ([cinematics.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/cinematics.json#L784-L809)). However:
  1. `scene_act2_interlude` is **never triggered anywhere in the game**.
  2. `vale_w3_mq15_escalation` is chained off `zeke_w3_mq14_lens_warning` → `thorne_w3_mq15_lockdown_1`–`2`, which is **completely bypassed** if the player clicks `the Lens` room action in `spire_archive_vault` instead of talking to Zeke first!
- **Elara Vane:** Central emotional hook of World 3 (Nova's mentor/predecessor). Her hologram message in `spire_archive_vault` (`elara_w3_mq14_recording_1`–`4`, [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1272-L1294)) is well-conceived, but the earlier reveal that she was transferred to "Research Station 0" (`w3_mq12_interrogate_guard`, [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L5025)) happens via a 1-sentence popup when clicking `Interrogate Guard` in an empty room with no guard NPC.
- **The Administrator (`administrator_boss`):** Has 2 written dialogue nodes (`admin_w3_roof_1`–`2`, [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1224-L1235)), but **`the_administrator` NPC is never placed in `spire_landing_pad_roof`**, making those lines completely unreachable.

---

### 3.3 Classified Work Items to Bring World 3 to World 1 Standard

#### A. Technical Bugs & Integration Problems

##### [W3-BUG-01] `w3_sq11` ("Neon Fix") Permanent RNG Softlock & Unreachable `inspect_sign` Task
- **Priority:** **Critical** | **Dependencies:** None | **Estimated Effort:** 1.5–2 hours
- **Exact Locations:**
  - [quests.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/quests.json#L1588-L1635) (`w3_sq11` requires tasks `inspect_sign`, `get_parts`, `fix_sign`)
  - [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1147-L1158) (`jax_w3_sq11_intro_1`–`2` fires `w3_sq11_start_via_jax`)
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L5173-L5255) (`w3_sq11_inspect_sign` requires `quest_not_started:w3_sq11`; `w3_sq11_fix_sign` requires `item:neon_sign_core:1`)
  - [enemies.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L767-L768) (`sentinel_mki` has `"item_id": "neon_sign_core", "chance": 0.5`)
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L14320) (`sentinel_mki` only spawns once, in `spire_exec_lounge_scale_02`)
- **Problem:**
  1. If the player talks to Jax (`jax_teller`) before clicking `Flickering Sign`, `w3_sq11_start_via_jax` starts `w3_sq11` without completing `inspect_sign`. Because `w3_sq11_inspect_sign` has `"conditions": ["quest_not_started:w3_sq11"]`, `inspect_sign` can never be completed.
  2. `neon_sign_core` is not sold in any shop, not placed in any Lower City room, and has no crafting recipe. Its **only source in the entire repository** is a **50% RNG drop** (`"chance": 0.5`) from the single `sentinel_mki` enemy in `spire_exec_lounge_scale_02` (Upper City). If the 50% roll fails, `w3_sq11` is permanently uncompletable (masked in [`Hub3CriticalFlowTest.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/test/java/com/example/starborn/domain/session/Hub3CriticalFlowTest.kt#L133) and [`World3PlaythroughAuditTest.kt`](file:///Users/jason/Developer/StarbornAndroid/app/src/test/java/com/example/starborn/World3PlaythroughAuditTest.kt#L181) because both tests manually inject `neon_sign_core` into inventory!).
- **Recommended Action:** Allow `w3_sq11_inspect_sign` to fire when `quest_not_started:w3_sq11` OR `quest_task_not_done:w3_sq11:inspect_sign` (and mark `inspect_sign` done in `w3_sq11_start_via_jax`), and add a guaranteed Lower City source for `neon_sign_core` (e.g., salvaged in `spire_scrap_yard` or sold at `black_market_shop`).

##### [W3-BUG-02] `w3_sq16` ("The Whistleblower") Broken Gating & Unmarked `expose_vance` Task
- **Priority:** **Critical** | **Dependencies:** [W2-DEF-01] | **Estimated Effort:** 0.5 hours
- **Exact Locations:**
  - [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L2855-L2870) (`director_vance_default_talk` uses `milestone_set:ms_w3_sq16_desk_checked,milestone_set:ms_w3_sq16_logs_checked,milestone_set:ms_w3_sq16_manifest_checked`)
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L15149-L15173) (`w3_sq16_confront_vance` completes `w3_sq16` without marking `expose_vance` done)
- **Problem:** In production `AppServices.kt`, `milestone_set:` is ignored, so players can immediately accuse Director Vance in `spire_exec_lounge_bar` before inspecting any of the 3 clues (`check_desk`, `check_logs`, `check_manifest`), completing the quest with 0/4 tasks checked. Even when all 3 clues are found, `w3_sq16_confront_vance` forgets to call `"type": "complete_quest_task", "quest_id": "w3_sq16", "task_id": "expose_vance"` before completing the quest.
- **Recommended Action:** Fix `AppServices.isDialogueConditionMet` ([W2-DEF-01]) and add the `complete_quest_task` action for `expose_vance` in `w3_sq16_confront_vance`.

##### [W3-BUG-03] Duplicate Incompatible `phase_rounds` Definitions in `items.json`
- **Priority:** **High** | **Dependencies:** None | **Estimated Effort:** 1 hour
- **Exact Locations:**
  - [items.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/items.json#L1012-L1028) (`"id": "phase_rounds"`, `"type": "consumable"`)
  - [items.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/items.json#L2441-L2460) (`"id": "phase_rounds"`, `"type": "equipment"`, slot `"mod"`)
- **Problem:** `items.json` defines `"id": "phase_rounds"` twice with incompatible schemas (once as a combat consumable and once as a Tinkering equipment mod for `recipes_tinkering.json`). Catalog lookup collisions cause unpredictable behavior depending on map indexing order.
- **Recommended Action:** Rename the Tinkering mod or consumable so each has a unique ID (`phase_rounds` vs. `phase_rounds_mod`).

##### [W3-BUG-04] `riot_guard` Has Zero Spawns, Swapped Sprite with `corporate_assassin`, and Beast Skill `"alpha_howl"`
- **Priority:** **High** | **Dependencies:** None | **Estimated Effort:** 1.5 hours
- **Exact Locations:**
  - [enemies.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L695-L734) (`riot_guard` uses `corporate_assassin_combat.png` and `"skills": ["shield_bash", "alpha_howl"]`)
  - [enemies.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L878-L914) (`corporate_assassin` uses `riot_guard_combat.png`)
- **Problem:** `riot_guard` and `corporate_assassin` have each other's combat sprites assigned; `riot_guard` uses `"alpha_howl"` (a beast skill from World 2's `bog_lurker`); and `riot_guard` is never placed in any room or event in World 3.
- **Recommended Action:** Swap `combat_image` between `riot_guard` and `corporate_assassin`, replace `"alpha_howl"` on `riot_guard` with a baton/suppression skill, and place `riot_guard` in `spire_checkpoint_gate` / `spire_security_hub`.

##### [W3-BUG-05] `w3_mq14` Vault Lockout Bypass Skips Zeke, Thorne, and Arden Vale Escalation Dialogue
- **Priority:** **High** | **Dependencies:** None | **Estimated Effort:** 1 hour
- **Exact Locations:**
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L5721-L5808) (`w3_mq14_take_lens` vs. `w3_mq14_lens_lockdown`)
  - [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1296-L1329) (`zeke_w3_mq14_lens_warning` → `thorne_w3_mq15_lockdown_1`–`2` → `vale_w3_mq15_escalation`)
- **Problem:** In `spire_archive_vault`, clicking the primary room button `the Lens` triggers `w3_mq14_take_lens`, which directly completes `w3_mq14` and starts `w3_mq15` via popup—completely bypassing the 4-node dialogue sequence (`zeke_w3_mq14_lens_warning` → `thorne_w3_mq15_lockdown_1`–`2` → `vale_w3_mq15_escalation`) that only triggers if the player ignores the Lens button and talks to Zeke instead.
- **Recommended Action:** Have `w3_mq14_take_lens` trigger `zeke_w3_mq14_lens_warning` (which already triggers `w3_mq14_lens_lockdown` at the end of `vale_w3_mq15_escalation`) so every player experiences the lockdown and Lt. Arden Vale's introduction.

##### [W3-BUG-06] Orphaned Administrator Boss Dialogue (`admin_w3_roof_1`–`2`) & Orphaned Cinematic (`scene_act2_interlude`)
- **Priority:** **High** | **Dependencies:** None | **Estimated Effort:** 1.5 hours
- **Exact Locations:**
  - [dialogue.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/dialogue.json#L1224-L1235) (`admin_w3_roof_1`–`2`)
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L13250-L13290) (`spire_landing_pad_roof`)
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L6046-L6221) (`w3_mq15_roof_access`, `w3_mq15_launch_astra`)
  - [cinematics.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/cinematics.json#L784-L809) (`scene_act2_interlude`)
- **Problem:** `admin_w3_roof_1`–`2` is never triggered because `the_administrator` is not in `spire_landing_pad_roof` and `w3_mq15_roof_access` launches combat directly without pre-boss dialogue. Similarly, `scene_act2_interlude` (Arden Vale's command bunker scene bridging World 3 to World 4) has zero references in `events.json`.
- **Recommended Action:** Trigger `admin_w3_roof_1`–`2` before starting combat in `w3_mq15_roof_access`, and play `scene_act2_interlude` during `w3_mq15_launch_astra` before transitioning to `pebble_station_dock`.

##### [W3-BUG-07] 38 of 70 World 3 Rooms Missing from `audio_bindings.json` & `"env": "city"` Missing from `themes.json`
- **Priority:** **Medium** | **Dependencies:** None | **Estimated Effort:** 1–1.5 hours
- **Exact Locations:**
  - [audio_bindings.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/audio_bindings.json#L146-L185) (only 32 of 70 World 3 rooms bound)
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L20664) (`spire_hidden_studio` uses `"env": "city"`, which is absent from `themes.json`)
- **Problem:** All 33 `*_scale_*` rooms plus `spire_salon`, `spire_lower_elevator`, `spire_upper_elevator`, `spire_clone_chamber`, and `spire_hidden_studio` have no entries in `audio_bindings.json`, causing music/ambience dropouts when walking through scale corridors.
- **Recommended Action:** Bind the remaining 38 rooms in `audio_bindings.json` and change `spire_hidden_studio` `"env"` from `"city"` to `"cyberpunk"`.

---

#### B. Incomplete & Missing Content

##### [W3-INC-01] Wire Existing `PrismOpticsPanel` to `w3_mq14` Archive Light Puzzle & Enable `CounterTunePanel` on `w3_chrono_flux_tune`
- **Priority:** **High** | **Dependencies:** None | **Estimated Effort:** 3–5 hours
- **Exact Locations:**
  - [`PrismOpticsPanel.kt`](file:///Users/jason/Developer/StarbornAndroid/shared/src/main/java/com/example/starborn/ui/prism/PrismOpticsPanel.kt) and [`ExplorationController.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/session/ExplorationController.kt#L2762-L2800) (`startPrismOpticsChallenge`)
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L5566-L5719) (`w3_mq14_prism_align` through `w3_mq14_unlock_vault`)
  - [tuning_puzzles.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/tuning_puzzles.json#L128-L167) (`w3_chrono_flux_tune` omits `"presentation": "counter_tune"`)
- **Problem:** The codebase already has a complete, interactive beam-splitting prism minigame (`PrismOpticsPanel.kt` + `PrismOptics.kt`) wired into `ExplorationController.kt` via `"presentation": "prism_optics"`, yet `w3_mq14`'s Archive Light Puzzle in `spire_prism_gallery` / `spire_archive_vault` bypasses it in favor of 4 plain buttons! Additionally, `w3_chrono_flux_tune` (`w3_sq15`) omits `"presentation": "counter_tune"`, falling back to the legacy slider modal.
- **Recommended Action:** Add a `w3_vault_prism_optics` puzzle definition with `"presentation": "prism_optics"` in `tuning_puzzles.json` triggered by `w3_mq14_solve_light_puzzle`, and add `"presentation": "counter_tune"` to `w3_chrono_flux_tune`.

##### [W3-INC-02] Author 2 Missing Story Cinematics & Wire `scene_act2_interlude`
- **Priority:** **High** | **Dependencies:** [W3-BUG-06] | **Estimated Effort:** 4–6 hours
- **Exact Locations:** [cinematics.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/cinematics.json#L784-L910)
- **Recommended Action:**
  1. Wire `scene_act2_interlude` to the end of `w3_mq15_launch_astra`.
  2. Author `scene_w3_heist_briefing` for `w3_mq12_plan_heist` in `spire_zekes_apartment` (bringing Nova, Zeke, Orion, and Gh0st together around the holotable).
  3. Author `scene_w3_lens_lockdown` for `w3_mq14` when Elara's recording finishes and the Spire alarms trip.

##### [W3-INC-03] Replace Single-Click Popup Tasks in `w3_mq12` ("Inside Help") & `w3_mq13` ("Dress the Part") With Real Gameplay & Dialogue
- **Priority:** **High** | **Dependencies:** None | **Estimated Effort:** 6–8 hours
- **Exact Locations:**
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L4973-L5171) (`w3_mq12` 6 popup tasks) and [L5297-L5426](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L5297-L5426) (`w3_mq13` 3 popup tasks)
  - [rooms.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/rooms.json#L13142-L13248) (`spire_zekes_apartment`, `spire_security_hub`, `spire_exec_lounge_bar`, `spire_vip_room`, `spire_elevator_lobby`)
- **Problem:** All 9 tasks across `w3_mq12` and `w3_mq13` are currently completed by clicking room buttons that show a 1-sentence popup—including `Interrogate Guard` in an empty room (which reveals Elara's transfer to Research Station 0 via popup!) and `Charm VIPs` / `Spike Drink` / `Take Elevator`.
- **Recommended Action:**
  1. Place `orion` and `gh0st` in `spire_zekes_apartment` during `w3_mq12` and convert `Plan Heist` into a multi-character briefing dialogue.
  2. Place a Dominion Security Officer / `riot_guard` encounter in `spire_security_hub` so `Interrogate Guard` involves either a combat shakedown or a dialogue interrogation with Gh0st & Zeke before discovering Elara's Research Station 0 transfer log.
  3. Convert `w3_mq13` (`Charm VIPs` and `Spike Drink`) into an interactive social-engineering dialogue tree in `spire_exec_lounge_bar` / `spire_vip_room` where the player chooses how Nova, Orion, or Zeke distracts the VIPs.

##### [W3-INC-04] Rebalance World 3 Combat Density & Buff `administrator_boss`
- **Priority:** **High** | **Dependencies:** [W3-BUG-04] | **Estimated Effort:** 4–6 hours (data/balance) + 4–6 hours (boss sprite)
- **Exact Locations:**
  - [enemies.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/enemies.json#L916-L962) (`administrator_boss`: 420 HP, 34 STR, 24 DEF, 400 XP, `"combat_image": "enemies/the_warden_combat.png"`)
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L4939-L4971) (`w3_mq11_clear_landing` completes without combat)
- **Problem:** Only 7 of 70 rooms in World 3 have enemies (only 2 sewer rooms in all of Lower City). `w3_mq11` task `clear_landing` ("Clear the Landing Pad") completes via a non-combat button click. Finally, `administrator_boss` (420 HP, 400 XP) has *lower* HP and XP than World 2's boss `the_beast` (520 HP, 500 XP) despite facing a full 4-member higher-level party, and reuses World 1's Iron Warden sprite.
- **Recommended Action:**
  1. Trigger a `riot_guard` + `sewer_drone` security sweep encounter during `w3_mq11` (`clear_landing`) or `w3_mq12` (`spire_security_hub`).
  2. Buff `administrator_boss` to ~600–650 HP, 40 STR, 30 DEF, 650 XP, pair him with a summoned `sentinel_mki` or `laser_turret` shield drone, and give him a dedicated sprite (`administrator_boss_combat.png`).

##### [W3-INC-05] Spawn Missing Quest NPCs (`informant`, `orion`, `gh0st`) & Require NPC Turn-Ins for `w3_sq12` / `w3_sq13`
- **Priority:** **Medium** | **Dependencies:** None | **Estimated Effort:** 4–6 hours
- **Exact Locations:**
  - [npcs.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/npcs.json#L214-L223) (`informant` has 0 room spawns and 0 dialogue nodes)
  - [events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L5428-L5564) (`w3_sq12_decrypt_chip` and `w3_sq13_repair_drone` auto-complete via popup without talking to Gh0st or Mika)
- **Recommended Action:** Spawn `informant` in `spire_exec_lounge_bar` for `w3_sq14` ("High Roller") instead of using a disembodied room button; require returning the repaired drone to `mika_circuit` (`w3_sq13`) and the decrypted chip to `gh0st` (`w3_sq12`).

---

#### C. Existing Content Needing Improvement & Optional Polish

- **[W3-IMP-01] Consolidate or Enrich the 33 `*_scale_01..05` Rooms (Medium Priority, 4–6 hrs):** In `hub_5_lower_city` and `hub_6_upper_city`, walking through 5 identical `*_scale_01`–`05` rooms per node (`spire_exec_lounge` and `spire_landing_pad` have only 1 real room + 5 scale rooms) pads travel with empty clicks. Trim each chain from 5 scale rooms down to 1–2 meaningful transition rooms (e.g., a skybridge checkpoint, a maintenance crawlway, a VIP security scanner) or populate them with patrols, salvage, and reactive states.
- **[W3-IMP-02] Integrate Tinkering into World 3 Quests (Medium Priority, 2–3 hrs):** Require crafting a bypass module or repairing the `broken_neon_core` / `damaged_drone_chassis` at a tinkering bench during `w3_sq11` or `w3_sq13`.
- **[W3-IMP-03] Fix "Shield" vs. "Internal Curtain" Continuity in `w3_mq15_launch_astra` (Low Priority, 0.5 hrs):** `w3_mq15_launch_astra` ([events.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/events.json#L6138)) says the *Astra* `"slips the Shield"`, contradicting `scene_act2_interlude` ([cinematics.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/cinematics.json#L789)) and World 4 (`w4_mq_find_booster`), which establish that the *Astra* only cleared the Spire's internal curtain and still needs the **Helios Booster** at Pebble Station to punch through the outer Planetary Shield. Change `"slips the Shield"` to `"punches through the Spire's upper lockdown curtain"`.
- **[W3-OPT-01] Distinct NPC & Enemy Portrait/Sprite Pass (Low/Optional Priority, 6–12 hrs):** Currently 9 World 3 NPCs reuse `npcs/town_guard.png` or `npcs/scientist.png` ([npcs.json](file:///Users/jason/Developer/StarbornAndroid/app/src/main/assets/npcs.json#L160-L601)).

---

## Phase 4 — Three-World Comparative Scorecard & Playtime Estimates

### 4.1 Comparative Scorecard (1–10 Scale)

| Category | World 1 ("The Mines") | World 2 ("Sector 9") | World 3 ("The Spire") | Evidence-Based Reasoning |
|---|---|---|---|---|
| **Narrative** | **8.5** | **7.0** | **5.5** | **W1:** Tight personal/political stakes, 9 cinematics, earned sacrifice at `launch_lift`. **W2:** Strong ancient mystery & Orion reveal, but only 2 cinematics, zero Dominion presence, and major story beats (Gh0st join, *Astra* reveal) happen in popups. **W3:** Great cyberpunk heist & Elara premise, but 0 wired cinematics, `w3_mq12`–`13` told via popups, and the main lockdown/Vale reveal can be accidentally skipped. |
| **Characters** | **8.5** | **6.5** | **5.0** | **W1:** ~100 character dialogue nodes; Nova (23), Jed (22), Zeke (21), Boggs (14) feel alive. **W2:** Only 42 character lines (20 of 62 are consoles); Nova has 2 lines; Gh0st has 0 post-boss recruitment lines. **W3:** Only 43 world dialogue lines; cameo NPCs have >2x the lines of the main cast; Nova has 2 lines, Orion has 3, Administrator Boss has 0 reachable lines. |
| **Exploration** | **8.0** | **8.0** | **6.0** | **W1:** 82 bespoke rooms, dynamic power restoration across 6 dark rooms, minecart & vent secrets (minor dip in 8 late-mine dark rooms). **W2:** 93 bespoke rooms with multi-room sub-branches and 17 multi-state rooms (marred slightly by disconnected `source_gate` node and empty `the_sky` node). **W3:** 33 of 70 rooms (47%) are copy-pasted `*_scale_01..05` corridors with 1 flavor click; only 1 room has `description_variants`. |
| **Combat** | **8.0** | **7.0** | **5.0** | **W1:** 16 encounters, distinct sprites, Guard Break tutorial integration, roaming haulers, well-tuned Iron Warden boss. **W2:** 12 encounters and strong `the_beast` boss (520 HP), but 4/7 enemies reuse W1 sprites and 2 vine enemies drop 0 credits/items. **W3:** Only 7 rooms have enemies (2 in Lower City), `riot_guard` is unused & has swapped sprite + `"alpha_howl"`, and `administrator_boss` (420 HP) is weaker than W2's boss and reuses W1's Warden sprite. |
| **Quest design** | **8.5** | **7.5** | **5.5** | **W1:** Cohesive 5 MQ + 5 SQ structure with meaningful choices (`w1_sq04`). **W2:** Strong physical multi-room quest steps, but broken in production by `AppServices.isDialogueConditionMet` (`w2_sq01` instant turn-in). **W3:** `w3_sq11` has a 50% RNG permanent softlock, `w3_sq16` skips clue checks in production, and `w3_mq12`–`13` are 9 single-click popup tasks. |
| **Pacing** | **8.0** | **6.5** | **5.5** | **W1:** Balanced alternation of story, tinkering, puzzles, and combat. **W2:** Heavy stretch of silent console/machinery clicking in the back half with too little party banter. **W3:** Jarring oscillation between walking through 5 empty `_scale_` rooms and finishing entire heist quests in 30 seconds of popup clicks. |
| **World identity** | **8.5** | **7.5** | **7.0** | **W1:** Complete `"mine"` theme, audio bindings, dark/lit states, gritty frontier voice. **W2:** Vivid bioluminescent swamp & facility prose + complete audio bindings, but `"swamp"` is missing from `themes.json`. **W3:** Strong `"cyberpunk"` theme and neon noir prose in the 37 core rooms, but 38/70 rooms lack audio bindings and 33 rooms are generic corridors. |
| **Technical completeness** | **8.0** | **6.5** | **5.0** | **W1:** Solid flow (minor defects: unreachable `mechanic_shop`, W2-locked fishing, 5 lines hit by `isDialogueConditionMet`). **W2:** Hit hard by `isDialogueConditionMet`, missing `"swamp"` theme, 12 inverted `"Objective completed."` messages, and disconnected `source_gate` node. **W3:** `w3_sq11` RNG softlock, `w3_sq16` gating bug, duplicate `phase_rounds`, unused/swapped `riot_guard`, skippable `w3_mq14` lockdown dialogue, and unlinked `scene_act2_interlude`. |
| **Overall polish** | **8.0** | **6.5** | **5.0** | **W1:** Sets the studio standard for narrative/mechanical cohesion. **W2:** High environmental and structural polish held back by a thin dialogue layer and a handful of high-visibility data/condition bugs. **W3:** Reads clearly as an early structural draft awaiting its interactive puzzle, combat, cinematic, and dialogue pass. |

---

### 4.2 Estimated Playable Duration by World

*(Assumptions: Human player reading room descriptions and dialogue at ~180–200 WPM, ~20–30 seconds per room navigation/inspection, ~2.5–4 minutes per standard combat encounter, ~5–7 minutes per boss/puzzle sequence.)*

| World | Main Path Only | Completionist (All Side Quests & Sub-Branches) | Notes on Playtime Quality |
|---|---|---|---|
| **World 1 ("The Mines")** | **1.5 – 2.0 hours** | **2.5 – 3.5 hours** | Well-proportioned mix of dialogue (~30%), exploration/puzzles (~35%), and combat/tinkering (~35%). |
| **World 2 ("Sector 9")** | **1.75 – 2.25 hours** | **3.0 – 4.0 hours** | Slightly longer room-traversal footprint (93 rooms with deep sub-branches), though lower dialogue density makes the second half feel quieter. |
| **World 3 ("The Spire") — Current** | **1.0 – 1.5 hours** | **1.75 – 2.5 hours** | Despite 70 rooms, 33 are 5-second corridor transits and `w3_mq12`–`13` resolve in minutes via popups. **(Expected to reach 2.0 – 3.5 hours once Tier 2 completion work is added.)** |

---

## Phase 5 — Actionable Development Roadmaps

### 5.1 World 2 — Quality Improvement Plan

**Assessment:** World 2 does **not** require a structural rewrite or new rooms—its 93 bespoke rooms, sub-branch puzzles, and quest architecture are already strong. It requires **targeted technical fixes (4–6 hours)** before human playtesting, plus a **focused narrative/dialogue polish pass (6–10 hours)** to match World 1's emotional standard.

1. **Step 1 — Fix Production Dialogue Condition Evaluation ([W2-DEF-01], Critical, 1–2 hrs):**
   - Implement `quest_task_done`, `quest_task_not_done`, `milestone_set`, `milestone_not_set`, `quest_not_started`, and `quest_not_completed` in [`DialogueTriggerLogic.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueTriggerLogic.kt#L9-L54) and [`AppServices.kt`](file:///Users/jason/Developer/StarbornAndroid/app/src/main/java/com/example/starborn/di/AppServices.kt#L2523-L2568). (This simultaneously fixes defects in Worlds 1, 2, and 3.)
2. **Step 2 — Fix World 2 Topology, Theme, and Action Feedback ([W2-DEF-02], [W2-DEF-03], [W2-DEF-04], High, 3 hrs):**
   - Add `"swamp"` to `themes.json` and `theme_styles.json`.
   - Replace the 12 erroneous `"condition_unmet_message": "Objective completed."` strings in `rooms.json` with clear prerequisite messages.
   - Reassign the 4 power rooms (`sector9_power_turbine` etc.) to `stasis_chamber` in `hub_nodes.json` (or link `sector9_power_breaker_deck` to `sector9_gate_cable_trench`).
3. **Step 3 — Clean Up Items, Shop Greeting, and Combat Drops ([W2-DEF-05], [W2-DEF-06], [W2-DEF-07], [W2-DEF-08], Medium, 2–3 hrs):**
   - Fix `rapid_capacitor` description/apostrophe, wire `ShopAction.dialogueId` for `sentinel_shop_intro`, add credits/drops to `razor_vine` and `stalker_vine`, and remove the duplicate `Stabilize Zeke` button from `sector9_pod_interior`.
4. **Step 4 — Character & Story Polish Pass ([W2-DES-01], [W2-DES-02], High, 4–6 hrs):**
   - Add post-Beast recruitment dialogue for Gh0st (`w2_mq04_recruit_ghost`), crew discovery dialogue for finding the *Astra* (`w2_mq05_locate_ship`), brief recovery dialogue when stabilizing Zeke (`w2_mq01_stabilize_zeke`), and an intercepted Dominion pursuit comm in `the_sky` / `sector9_hangar_bay`.
5. **Step 5 — Optional Enemy Sprite Pass ([W2-DEF-07], Low/Optional, 4–8 hrs):**
   - Replace the 4 reused World 1 enemy sprites (`bog_lurker`, `corrupted_guardian`, `security_drone_mkii`, `harmonic_suppressor`).

---

### 5.2 World 3 — Completion Plan

Ordered by dependencies and implementation efficiency, leveraging existing systems (`PrismOpticsPanel`, `CounterTunePanel`, `CinematicCoordinator`, `DialogueTriggerLogic`):

#### Tier 1 — Minimum Work for a Bug-Free, Complete Playable World (6–10 Developer Hours)
1. **Fix `w3_sq11` ("Neon Fix") Softlock & Trigger ([W3-BUG-01], 1.5–2 hrs):** Fix `w3_sq11_inspect_sign` condition and place a deterministic `neon_sign_core` source in Lower City.
2. **Fix `w3_sq16` ("The Whistleblower") Task & Clue Gating ([W3-BUG-02], 0.5 hrs):** Relies on [W2-DEF-01]; add `complete_quest_task` for `expose_vance`.
3. **Resolve Duplicate `phase_rounds` ID & `riot_guard` Data Bugs ([W3-BUG-03], [W3-BUG-04], 2 hrs):** Deduplicate `phase_rounds` in `items.json`; swap `riot_guard`/`corporate_assassin` sprites and replace `"alpha_howl"`.
4. **Unify `w3_mq14` Vault Lockdown & Wire `admin_w3_roof` + `scene_act2_interlude` ([W3-BUG-05], [W3-BUG-06], 2–2.5 hrs):** Route `w3_mq14_take_lens` through `zeke_w3_mq14_lens_warning` → `vale_w3_mq15_escalation`; trigger `admin_w3_roof_1`–`2` before the rooftop boss fight; trigger `scene_act2_interlude` on `w3_mq15_launch_astra`; fix `"slips the Shield"` text ([W3-IMP-03]).
5. **Complete `audio_bindings.json` & `"city"` Theme Fix ([W3-BUG-07], 1–1.5 hrs):** Bind the 38 missing World 3 rooms in `audio_bindings.json` and change `spire_hidden_studio` to `"env": "cyberpunk"`.

#### Tier 2 — Additional Work to Reach World 1 Quality Standard (18–28 Developer Hours)
1. **Activate Existing Puzzle UIs ([W3-INC-01], 3–5 hrs):** Wire `PrismOpticsPanel` (`"presentation": "prism_optics"`) into `w3_mq14` (`spire_prism_gallery` / `spire_archive_vault`) and add `"presentation": "counter_tune"` to `w3_chrono_flux_tune` (`w3_sq15`).
2. **Convert `w3_mq12` & `w3_mq13` Popup Tasks into Interactive Gameplay ([W3-INC-03], 6–8 hrs):** Spawn Orion and Gh0st in `spire_zekes_apartment` for a full heist briefing dialogue; turn `Interrogate Guard` in `spire_security_hub` into a guard encounter + interrogation dialogue; turn `w3_mq13` VIP infiltration into an interactive social-engineering dialogue choice.
3. **Combat Density & Boss Escalation Pass ([W3-INC-04], 4–6 hrs):** Add `riot_guard` / `sentinel_mki` encounters to `w3_mq11` (`clear_landing`), `spire_checkpoint_gate`, and Upper City security hubs; buff `administrator_boss` to ~600–650 HP with a turret/drone add.
4. **Author 2 New Cinematics & Quest NPC Interactions ([W3-INC-02], [W3-INC-05], 5–9 hrs):** Add `scene_w3_heist_briefing` and `scene_w3_lens_lockdown`; spawn `informant` in `spire_exec_lounge_bar` for `w3_sq14`; require NPC turn-ins for `w3_sq12` and `w3_sq13`; expand Nova and Orion's dialogue across Lower and Upper City.

#### Tier 3 — Optional Polish Beyond World 1 Baseline (10–18 Developer Hours)
1. **Trim or Enrich the 33 `*_scale_01..05` Corridor Rooms ([W3-IMP-01], 4–6 hrs):** Condense 5-room scale chains into 1–2 atmospheric rooms with patrols, environmental hazards, or lore terminals, and add Tinkering integration ([W3-IMP-02]).
2. **Custom Boss & NPC Art Pass ([W3-OPT-01], 6–12 hrs):** Create a dedicated `administrator_boss_combat.png` sprite and distinct portraits for key Spire NPCs (`elara_vane`, `director_vance`, `jax_teller`).

---

## Phase 6 — Final Verdict

### 1. Is World 2 currently up to World 1 standards?
**Structurally and environmentally, yes; narratively and technically, not quite yet.** World 2's 93 bespoke rooms, multi-room mechanical sub-branches, and `the_beast` boss encounter equal or surpass World 1, but it falls short of World 1 in character dialogue density (only 42 character lines vs. ~100 in World 1, with Nova speaking only twice and Gh0st's recruitment happening in a popup) and suffers from a critical production dialogue-condition bug (`AppServices.isDialogueConditionMet`).

### 2. What are the five most important improvements World 2 needs?
1. **Fix `AppServices.isDialogueConditionMet` & `DialogueTriggerLogic.isDialogueConditionMet` ([W2-DEF-01])** so `quest_task_done`, `quest_task_not_done`, `milestone_set`, and `quest_not_completed` are evaluated in production (fixing the `w2_sq01` 0-scan turn-in exploit and Zeke's infinite crash-site loop).
2. **Author character dialogue for Gh0st's recruitment (`w2_mq04`) and the *Astra* discovery (`w2_mq05`) ([W2-DES-01])** so the world's two biggest story milestones aren't resolved in 1-sentence toast popups, and give Nova an active voice.
3. **Add `"swamp"` to `themes.json` and `theme_styles.json` ([W2-DEF-02])** so all 93 World 2 rooms render with their intended visual palette instead of a null fallback.
4. **Replace the 12 inverted `"condition_unmet_message": "Objective completed."` strings ([W2-DEF-03])** on main-quest buttons with clear prerequisite hints.
5. **Fix the disconnected `source_gate` hub node subgraph ([W2-DEF-04])** so the 4 power rooms are either grouped with `stasis_chamber` or physically connected to `sector9_source_gate`.

### 3. Is World 2 ready for human playtesting?
**It will be ready after 4–6 hours of targeted bug fixes (Steps 1–3 of the World 2 plan).** Do **not** put human playtesters into World 2 before fixing [W2-DEF-01] and [W2-DEF-03], because playtesters will immediately trigger `w2_sq01`'s turn-in with 0 flora scanned, get stuck in Zeke's repeating crash dialogue, and see `"Objective completed."` when clicking locked main-quest objectives.

### 4. How complete is World 3?
**Approximately 55%–60% complete.** The hub/node skeleton, 37 core rooms, quest state machine (`w3_mq11`–`w3_mq15`, `w3_sq11`–`w3_sq16`), and transition into World 4 are in place, but nearly half the rooms (33/70) are 1-click `*_scale_*` filler corridors, `w3_mq12` and `w3_mq13` are 100% single-click popups, 0 cinematics are wired, `PrismOpticsPanel` isn't hooked up to the Vault puzzle, and several critical bugs (`w3_sq11` 50% RNG softlock, skippable `w3_mq14` lockdown dialogue, under-leveled boss reusing W1 art) remain.

### 5. What are the biggest obstacles to completing World 3?
1. **Popup-Driven Mid-Act Quests (`w3_mq12` & `w3_mq13`):** All 9 tasks across the middle two main quests are currently placeholder single-click popups that need interactive dialogue, guard encounters, and heist-planning scenes.
2. **Unwired Existing Systems:** The codebase already has `PrismOpticsPanel` (`:shared`), `CounterTunePanel`, and `scene_act2_interlude` (`cinematics.json`), plus `admin_w3_roof_1`–`2` and `vale_w3_mq15_escalation` (`dialogue.json`), which simply haven't been connected to World 3's events and puzzles.
3. **Low Combat & Character Dialogue Density:** Lower City has only 2 combat rooms, `riot_guard` isn't spawned anywhere, `administrator_boss` is weaker than World 2's boss, and the main cast (Nova, Orion, Gh0st, Vale) has fewer lines combined than Lower City cameo NPCs.

### 6. How many developer hours would you budget for each world?
- **World 2 ("Sector 9"):**
  - **4–6 hours** for mandatory pre-playtest bug fixes ([W2-DEF-01] through [W2-DEF-08])
  - **6–10 hours** for narrative/dialogue polish ([W2-DES-01], [W2-DES-02])
  - **Total World 2 Budget:** **10–16 hours** *(+ 4–8 hours optional art pass for reused enemy sprites)*
- **World 3 ("The Spire"):**
  - **6–10 hours** for Tier 1 (minimum bug-free playable world)
  - **18–28 hours** for Tier 2 (reaching World 1 quality standard)
  - **Total World 3 Budget:** **24–38 hours** *(+ 10–18 hours optional Tier 3 corridor consolidation & custom art)*

### 7. What should I work on next to achieve the greatest improvement in player experience?
**Start immediately with the 4–6 hour World 2 Technical Fix Sprint ([W2-DEF-01] through [W2-DEF-06]), beginning with `DialogueTriggerLogic.isDialogueConditionMet` and `AppServices.isDialogueConditionMet`.**
- That single 1-hour Kotlin fix in [`DialogueTriggerLogic.kt`](file:///Users/jason/Developer/StarbornAndroid/gameRuntime/src/main/java/com/example/starborn/domain/dialogue/DialogueTriggerLogic.kt#L9-L54) and [`AppServices.kt`](file:///Users/jason/Developer/StarbornAndroid/app/src/main/java/com/example/starborn/di/AppServices.kt#L2523-L2568) repairs **18 broken dialogue gates across Worlds 1, 2, and 3 simultaneously**.
- Following that with the `"swamp"` theme entry, the 12 `"condition_unmet_message"` fixes, and the 3–4 hour World 2 dialogue pass (Gh0st recruitment + *Astra* discovery) will bring **World 2 to playtest-ready, World 1 parity within ~10 hours of work**, giving you a solid two-world campaign while you tackle World 3's Tier 1 and Tier 2 roadmap.
