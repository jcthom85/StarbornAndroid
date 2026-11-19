# Starborn — Story Progression & Checkpoint Map (As Currently Implemented)

> **Purpose:** Authoritative reconstruction of *Starborn*’s live narrative and gameplay progression across all six worlds (`world_1` through `world_6` plus `hub_astra`), traced directly from implemented quest stages/tasks ([quests.json](../../app/src/main/assets/quests.json)), event triggers and conditions ([events.json](../../app/src/main/assets/events.json)), dialogue chains ([dialogue.json](../../app/src/main/assets/dialogue.json)), cinematic definitions ([cinematics.json](../../app/src/main/assets/cinematics.json)), room states/actions ([rooms.json](../../app/src/main/assets/rooms.json)), and runtime controllers ([ExplorationController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt), [DialogueService.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/domain/dialogue/DialogueService.kt), [RestBanter.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/domain/narrative/RestBanter.kt)).
>
> **Scope Note:** Sections 1–7 document the **current implemented state only**. Section 8 isolates proposed progression and trigger changes so this document can serve as a stable baseline for the story and cinematic audit ([starborn-story-audit.md](starborn-story-audit.md)).

---

## 1. Cold Open & World 1 — Mining Colony (`world_1`: `hub_1_homestead`, `hub_2_logistics`)

### `W00-01` — Cold Open: Sector 9 Containment Breach & Chime Launch
- **Location:** Pre-spawn cinematic sequence → spawns into `pit_nova_bunk` (*Nova's Bunk*, `hub_1_homestead`).
- **Actual Trigger / Prerequisites:** New Game launch queues `intro_prologue` (10 steps, `illustrated`, 32.0s) followed by bootstrap action `new_game_spawn_player_and_fade` (`events.json`), which warps to `pit_nova_bunk`, starts `w1_mq01` (*Wake Up Call*), and plays `new_game_fade_in` (1 step, `illustrated`, 8.0s).
- **Guaranteed Player Knowledge:**
  - Twenty years earlier in Sector 9, a "Source Beast" containment breach occurred (`intro_prologue[0..1]`).
  - An unnamed operator (`speaker: "????"`, currently displaying `images/characters/orion_portrait.png`) locked a brass **Chime** into a launch tube (`"Broadcast my identity. Get it away from them"`), heard an answering chord across the receiver needles, and sealed himself into a dark stasis pod (`intro_prologue[2..7]`).
- **Might Have Encountered / Missed:**
  - The player may long-press `HOLD TO SKIP` (`skippable: true` on `intro_prologue`), skipping the visual cues of the brass Chime, launch tube, stasis pod, and Orion's face.
- **Unlocks Next:** `pit_nova_bunk` in dark state (`light_on: false`, `conduit_isolated: false`); `w1_mq01` stage `wake_in_the_pit` (`turn_on_bunk_light`, `inspect_safety_fault`, `leave_sleeping_level`).
- **Citations:** [cinematics.json](../../app/src/main/assets/cinematics.json) (`intro_prologue`, `new_game_fade_in`), [events.json](../../app/src/main/assets/events.json) (`new_game_spawn_player_and_fade`, `w1_mq01_start_on_bunk_entry`), [AppServices.kt](../../app/src/main/java/com/example/starborn/di/AppServices.kt).

---

### `W01-01` — Waking in the Pit & Isolating the Safety Fault
- **Location:** `pit_nova_bunk` (*Nova's Bunk*) → `pit_main` (*The Pit*, `hub_1_homestead`).
- **Actual Trigger / Prerequisites:**
  1. Tap `bunk light` (`player_action:w1_mq01_turn_on_bunk_light`) → sets `room_state.light_on = true`, `ms_w1_mq01_fault_visible`, completes `w1_mq01:turn_on_bunk_light`.
  2. Tap `door control panel` (`player_action:w1_mq01_inspect_safety_fault`, gated on `ms_w1_mq01_fault_visible`) → sets `room_state.conduit_isolated = true`, `ms_w1_mq01_safety_fault_inspected`, completes `w1_mq01:inspect_safety_fault`, unlocks `north` exit to `pit_main`.
  3. Move north into `pit_main` (`enter_room:pit_main` → `w1_mq01_leave_sleeping_level`) → advances `w1_mq01` to stage `report_to_jed`.
- **Guaranteed Player Knowledge:**
  - Nova Vance is a miner under severe quota pressure; a live electrical fault on her bunk door was falsified in colony records as "repaired."
  - Her mining cutter's Cryo-Inductor is cracked and running hot.
- **Might Have Encountered / Missed:**
  - Optional room inspections in `pit_nova_bunk` once lit (`cargo netting`, `rusty locker`), shower valve puzzle in `pit_showers` (`steam valves` → `drainage grate`), VHS Tape 1 (`find_vhs_tape_01` in `pit_bunks`), and talking to Miner Orlo / Miner Bill / Matriarch Pam (`pam_biscuit_gift`).
- **Unlocks Next:** `w1_mq01` stage `report_to_jed`; full traversal of `pit_main` and `market_plaza`.
- **Citations:** [rooms.json](../../app/src/main/assets/rooms.json) (`pit_nova_bunk`, `pit_showers`, `pit_bunks`), [events.json](../../app/src/main/assets/events.json) (`w1_mq01_turn_on_bunk_light`, `w1_mq01_inspect_safety_fault`, `w1_mq01_leave_sleeping_level`).

---

### `W01-02` — Meeting Jed, Starter Gear & Scrap Yard Loader
- **Location:** `market_2` (*Market South*) → `workshop_yard` (*Scrap Yard*) → `workshop_floor` (*Jed's Shutter*, `hub_1_homestead`).
- **Actual Trigger / Prerequisites:**
  1. Enter `market_2` (`w1_mq01_find_jed`) → completes `find_jed`.
  2. Talk to Jed (`jed_w1_mq01_intro_1`..`6`, music `music_jed_theme`) → grants `mining_pistol`, `nova_flux_liner`, `broken_cryo_inductor`, `scrap_metal`, completes `talk_to_jed`; auto-equips or prompts `gear_equip_basics` (`equip_starter_gear`).
  3. In `workshop_yard`, tap `loader diagnostic strip` (`w1_mq01_inspect_loader_relay` → sets `ms_w1_mq01_loader_relay_read`, completes `inspect_loader_relay`) and defeat `faulted_loader` (`w1_mq01_faulted_loader_victory` → completes `clear_faulted_loader`).
  4. Enter `workshop_floor` (`w1_mq01_reach_workshop`) → auto-starts `jed_w1_mq01_workshop_brief_1`..`3`.
- **Guaranteed Player Knowledge:**
  - Jed acts as Nova's surrogate mentor/father figure: he feeds her, kept a spare inductor housing for her, and taps his bench twice with his wrench before passing her the work lamp (`jed_w1_mq01_workshop_brief_1`).
  - Nova's cutter tripped on an anomalous deep frequency during her previous shift.
- **Might Have Encountered / Missed:**
  - Optional shops in `market_plaza`, talking to Cook Rourke (`cook_vance_default_talk`), Scrapper (`w1_sq01`: *The Scavenger's Stash*), or Doc (`w1_sq02`: *System Flush*).
- **Unlocks Next:** Tinkering tutorial (`scene_tinkering_tutorial`) and workbench access in `workshop_floor`.
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`jed_w1_mq01_intro_1`..`6`, `jed_w1_mq01_workshop_brief_1`..`3`), [events.json](../../app/src/main/assets/events.json) (`w1_mq01_inspect_loader_relay`, `w1_mq01_faulted_loader_victory`, `w1_mq01_reach_workshop`).

---

### `W01-03` — Cutter Repair, Governor Bypass & The Cutter Surge (`scene_cutter_surge`)
- **Location:** `workshop_floor` (*Jed's Shutter*, `hub_1_homestead`).
- **Actual Trigger / Prerequisites:**
  1. Craft `functional_cryo_inductor` (` broken_cryo_inductor` + `scrap_metal`) on the Tinkering table → sets `ms_w1_mq01_cryo_repaired`, completes `use_tinkering_table`, auto-starts `jed_w1_mq01_repair_done_1`..`5` (where Nova insists on bypassing the factory governor from 61 kHz to 84 kHz to make quota).
  2. Tap `Flux Liner` (`w1_mq01_patch_flux_liner`) and `mining cutter` (`w1_mq01_confirm_governor`) in either order → sets `ms_w1_mq01_liner_patched` and `ms_w1_mq01_governor_confirmed`.
  3. Tap `live cutter test` (`w1_mq01_cutter_surge`, requires both milestones above) → plays `scene_cutter_surge` (6 steps, `CARD`, `music_nova_theme` on step 4).
- **Guaranteed Player Knowledge:**
  - When Nova fires the bypassed cutter, its phase sweep catches a buried conduit under Jed's bench and continues answering even after she kills the power (`"I have. Jed... it's still answering"`).
  - The terminal logs `CUTTER FAULT 4C-117` against `NOVA VANCE`, flagging her badge for administrative review, while an old chamber seal cracks deep below the mine.
- **Might Have Encountered / Missed:**
  - Players can tap `Flux Liner` or `mining cutter` in either order before `live cutter test`.
  - Inspecting `workshop_dock` (`loader`, `cargo rail`) early shows unavailable messages referring to Foreman Boggs's certification assignment.
- **Unlocks Next:** Completes `w1_mq01`; starts `w1_mq02` (*Shift Clearance*); unlocks hub node `admin_gate`; auto-starts `jed_w1_mq02_handoff_1`..`3`.
- **Citations:** [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_cutter_surge`), [events.json](../../app/src/main/assets/events.json) (`w1_mq01_patch_flux_liner`, `w1_mq01_confirm_governor`, `w1_mq01_cutter_surge`), [dialogue.json](../../app/src/main/assets/dialogue.json) (`jed_w1_mq01_repair_done_1`..`5`, `jed_w1_mq02_handoff_1`..`3`).

---

### `W01-04` — Guard Hank's Denial & Zeke's Liability Spoof
- **Location:** `checkpoint_gate` (*Transit Gate*) → `checkpoint_booth` (*Zeke's Booth*, `hub_1_homestead`).
- **Actual Trigger / Prerequisites:**
  1. Enter `checkpoint_gate` (`w1_mq02_approach_checkpoint`) and talk to Guard Hank (`hank_w1_mq02_denial_1`..`3`) → badge rejected; advances `w1_mq02` to stage `override_retirement` (`ms_w1_mq02_hank_denied`).
  2. Enter `checkpoint_booth` and talk to Zeke (`zeke_w1_mq02_override_1`..`7`, music `music_zeke_theme` on line 4).
  3. At `zeke_w1_mq02_override_choice`, select one of the three liability spoof causes (`Grid Instability`, `Operator Error`, or `Unauthorized Modification`).
- **Guaranteed Player Knowledge:**
  - Dominion's automated HR logic has flagged Nova for `"Mandatory retirement. Asset liability exceeds projected value. Disposition: recycle"` because of the cutter fault.
  - Zeke risks his own position to spoof the liability form and issue a temporary `mine_access_badge`, buying Nova time by routing her to Foreman Boggs in the Logistics Sector.
- **Might Have Encountered / Missed:**
  - All three spoof choices converge on `zeke_w1_mq02_override_7` and set `ms_w1_mq02_complete`.
  - VHS Tape 2 (`find_vhs_tape_02` on `discarded document locker`) is in `checkpoint_booth` and can be missed if not tapped.
- **Unlocks Next:** Completes `w1_mq02`; grants `mine_access_badge`; unlocks travel to `hub_2_logistics` (`admin_lobby`); starts `w1_mq03` (*The Echo*).
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`hank_w1_mq02_denial_1`..`3`, `zeke_w1_mq02_override_1`..`7`), [rooms.json](../../app/src/main/assets/rooms.json) (`checkpoint_gate`, `checkpoint_booth`).

---

### `W01-05` — Logistics Review with Foreman Boggs & Guard Break Certification (`w1_sq03`)
- **Location:** `admin_lobby` (*Concourse Lobby*) → `admin_security` (*Security Post*) / `workshop_dock` (*Loading Dock*).
- **Actual Trigger / Prerequisites:**
  1. Enter `admin_lobby` and talk to Foreman Boggs (`bogs_w1_mq03_review_1`..`4`).
  2. Boggs requires shield-break certification (`w1_sq03`: *The Heavy Lifter*) before unlocking the Deep Mine elevator (`ms_w1_guardbreak_trained` or `ms_w1_guard_drill_won`).
  3. `bogs_w1_mq03_review_4` starts `w1_sq03`, unlocks `nova_hydraulic_kick`, completes `w1_sq03:talk_to_bogs` and `learn_hydraulic_kick`, and advances `w1_sq03` to stage `guard_break_training`.
  4. Defeat the training target in `admin_security` (`w1_sq03_training_victory`) → sets `ms_w1_guard_drill_won` and `ms_w1_guardbreak_trained`.
  5. Return to Boggs (`bogs_w1_sq03_complete` / `w1_sq03_main_path_return_to_boggs`) → completes `w1_sq03` and `w1_mq03:talk_to_bogs`, sets `ms_w1_mine_authorized`, unlocks `deep_mine` node.
- **Guaranteed Player Knowledge:**
  - **First mention of Director Mara Thorne:** Boggs states in `bogs_w1_mq03_review_2` that *"Director Thorne froze hazard pay across Sector 4 until the Harmony survey hits quota."*
  - Nova is assigned to clear the Sector 4 Deep Mine disturbance as punishment duty.
  - Nova learns how to break enemy Guard using `nova_hydraulic_kick`.
- **Might Have Encountered / Missed:**
  - **Alternate Order:** If the player defeated the training target or used the `loader`/`cargo rail` in `workshop_dock` *before* talking to Boggs, `bogs_w1_sq03_prior_drill` / `w1_sq03_register_prior_drill` recognizes prior completion.
  - **Optional Tasks within `w1_sq03`:** `start_loader` and `move_cargo` in `workshop_dock` are marked `optional: true` in stage `guard_break_training` and can be skipped entirely while still completing `w1_sq03` via Boggs.
  - **Optional Side Quest `w1_sq04` (*Protocol Override*):** In `server_hub`, players can thaw the console and compile the rebel spoof (`scene_sq04_hack`, 3 steps, `CARD`), seeing the word **"Chorus"** for the first time and choosing whether to route power to the rebel network or Nova's account (`w1_sq04_routing_choice`).
- **Unlocks Next:** `w1_mq03` stage `deep_mine_descent`; access to `mine_landing` (*Deep Mine*).
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`bogs_w1_mq03_review_1`..`4`, `bogs_w1_sq03_complete`), [quests.json](../../app/src/main/assets/quests.json) (`w1_mq03`, `w1_sq03`, `w1_sq04`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_sq04_hack`).

---

### `W01-06` — Deep Mine Descent, Generator Restore & The Tuning Fork (`scene_relic_sync`)
- **Location:** `mine_landing` → `mine_junction` → `mine_threshold` → `echo_heart` (*The Heart*, `hub_2_logistics`).
- **Actual Trigger / Prerequisites:**
  1. Restore mine power at the generator (`evt_mine_generator_restore` → sets `ms_mine_power_on`, plays `scene_mine_restore` [2 steps, `CARD`], completes `w1_mq03:restore_deep_mine_power`).
  2. Clear `w1_mq03_clear_rock_borer` (`clear_first_mine_encounter`) and optional `w1_mq03_clear_riot_guard` (`break_riot_guard`).
  3. Reach `mine_threshold` (`w1_mq03_reach_threshold`) and `echo_heart` (`w1_mq03_reach_echo_chamber`) → advances `w1_mq03` to stage `relic_sync`.
  4. In `echo_heart`, solve tuning puzzle `w1_echo_counter_tune` (*Counter-Tune the Fork* at 87 kHz) and trigger `w1_mq03_touch_relic` → plays `scene_relic_sync` (5 steps, `CARD`, `music_orion_theme` on step 0, `stinger_motif_source` on step 1).
- **Guaranteed Player Knowledge:**
  - Nova counter-tunes **The Tuning Fork** (Architect Echo #1) at 87 kHz.
  - The cradle closes a live circuit through her cutter and suit; her `functional_cryo_inductor` fuses and her `Flux Liner`'s sacrificial ground strip burns open to save her life, leaving a persistent tremor in her left hand and a skipped half-second on her suit clock.
  - Above the mine, a **Harmony** sensor registers the Architect handshake and triggers a red alert.
- **Might Have Encountered / Missed:**
  - Optional side quest `w1_sq05` (*The Lost Shift*) in `mine_shunt`: recovering `crew_datapad` plays `scene_sq05_datapad` (4 steps, `CARD`), revealing that Dominion sealed a prior mining shift alive inside a collapse after they broke into a pocket of the same hum, calling it a "biohazard."
  - Deep Mine arcade cabinet discovery (`hyperion_logic_board`).
- **Unlocks Next:** Consumes `functional_cryo_inductor`; grants `tuning_fork`; unlocks Source Art `nova_blast_wave` (`source_art_unlock` tutorial); completes `w1_mq03`; reveals `launch_bay`; triggers `w1_mq04` (*Red Alert*) on leaving the chamber.
- **Citations:** [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_mine_restore`, `scene_relic_sync`, `scene_sq05_datapad`), [events.json](../../app/src/main/assets/events.json) (`evt_mine_generator_restore`, `w1_mq03_touch_relic`, `w1_sq05_read_datapad`).

---

### `W01-07` — Lockdown Gauntlet & Jed's Sacrifice at the Cargo Lift (`scene_w1_lift_separation`)
- **Location:** `echo_well` / `mine_antechamber` → `launch_access` → `launch_lift` (*Cargo Lift*, `hub_2_logistics`).
- **Actual Trigger / Prerequisites:**
  1. `w1_mq04_start_lockdown` starts `w1_mq04` (*Red Alert*); Zeke guides Nova by radio through the escape gauntlet (`w1_mq04_gauntlet_victory` → completes `clear_escape_gauntlet`, advances to stage `cargo_lift_sacrifice`).
  2. Enter `launch_lift` (`w1_mq04_reach_lift`) and talk to Jed (`jed_w1_mq04_sacrifice_1`..`7`, music `music_theme_emotional`), whose last line triggers `player_action:jed_sacrifice` (`w1_mq04_complete`).
  3. `w1_mq04_complete` awards `ghost_signal_cell` (the brass Chime), completes `w1_mq04`, starts `w1_mq05` (*The Launch*), and plays `scene_w1_lift_separation` (3 steps, `illustrated`, 11.5s).
- **Guaranteed Player Knowledge:**
  - The Iron Warden is closing the blast shutters. The lift motor's brake is stripped; Jed already bent his wrench trying to jam it and must hold the manual bypass lever by hand so Nova's car can ascend.
  - When Nova asks if he has a spare, Jed answers *"Not this time"* (`jed_w1_mq04_sacrifice_3`), presses the brass **Chime** (`ghost_signal_cell`) into her palm, tells her to meet Zeke at the pod bay, and pulls the lever.
  - The shutters close between them and Jed's radio channel goes dead; Zeke calls Nova forward toward his light (`scene_w1_lift_separation[0..2]`).
- **Might Have Encountered / Missed:**
  - **Spoiler Risk (Verified Defect):** Inspecting `ghost_signal_cell` in the Key Items inventory immediately after this scene reveals `"Orion's antique brass Chime. Launched from Sector 9 twenty years ago..."` before the player knows Orion's name.
- **Unlocks Next:** `w1_mq05` (*The Launch*) stage `reach_pod_bay`; `jed` disappears from `launch_lift` (`forbidden_milestones: ["ms_w1_mq04_complete"]`); access to `launch_bay`.
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`jed_w1_mq04_sacrifice_1`..`7`), [events.json](../../app/src/main/assets/events.json) (`w1_mq04_complete`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w1_lift_separation`).

---

### `W01-08` — The Iron Warden, Chime Splice & Planetary Impact (`scene_launch_crash`)
- **Location:** `launch_bay` (*Pod Bay*) → `launch_pod` (*The Pod Core*) → warps to `sector9_crash_site` (*Crash Site*, `world_2`).
- **Actual Trigger / Prerequisites:**
  1. Enter `launch_bay` (`w1_mq05_warden_trigger`) → dialogue `warden_boss_intro_1`..`3` → boss battle against `the_iron_warden` (`music_w1_boss_warden`).
  2. Victory (`w1_mq05_warden_victory`) sets `ms_w1_warden_defeated`, plays `music_victory_boss`, and advances `w1_mq05` to stage `launch_pod`.
  3. Talk to Zeke (`zeke_w1_mq05_pod_prep_1`..`3`) and splice the Chime in `launch_pod` (`ms_w1_chime_spliced`).
  4. Tap `navigation console` (`player_action:use_nav_console` → `w1_mq05_use_nav_console`) → plays `scene_launch_crash` (7 steps, `illustrated`, 41.9s).
- **Guaranteed Player Knowledge:**
  - The Iron Warden explains Dominion's containment logic: *"One open shaft tells the whole pit the doors can be beaten"* (`warden_boss_intro_2`), and Nova tells him Jed held the lift so she could reach the pod.
  - Zeke splices Jed's Chime into the cargo pod's navigation array, warning Nova he cannot guarantee the landing; Nova refuses to leave without him.
  - Upon launch, the Chime unlocks the interlock and locks the pod onto its paired beacon in Sector 9, but the planetary **Shield** rejects the pod, tearing a stabilizer loose and throwing them down through a storm into Sector 9's jungle canopy (`scene_launch_crash[0..6]`).
- **Might Have Encountered / Missed:**
  - Any uncompleted World 1 side quests (`w1_sq01`, `w1_sq02`, `w1_sq04`, `w1_sq05`) or uncollected World 1 VHS tapes (`vhs_tape_01`, `vhs_tape_02`) are left behind until post-Astra travel opens.
- **Unlocks Next:** Completes `w1_mq05` (`ms_w1_mq05_complete`); warps to `sector9_crash_site`; starts `w2_mq01` (*A Strange Coast*) with Zeke injured.
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`warden_boss_intro_1`..`3`, `zeke_w1_mq05_pod_prep_1`..`3`), [events.json](../../app/src/main/assets/events.json) (`w1_mq05_warden_victory`, `w1_mq05_use_nav_console`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_launch_crash`).

---

## 2. World 2 — The Wilds & Sector 9 Facility (`world_2`: `hub_3_sector9`, `hub_4_facility`)

### `W02-01` — Crash Aftermath & The Sanctuary Gate
- **Location:** `sector9_crash_site` (*Crash Site*) → `sector9_pod_interior` → `sector9_landing_stream` → `sector9_canopy` → `sector9_temple_gate` (*Temple Gate*, `hub_3_sector9`).
- **Actual Trigger / Prerequisites:**
  1. Talk to Zeke at the wreck (`zeke_w2_crash_1`..`4` → completes `w2_mq01:check_on_zeke`).
  2. Tap `navigation console` (`w2_mq01_examine_pod`) and `emergency medkit` in `sector9_pod_interior` (`w2_mq01_stabilize_zeke`) → stabilizes Zeke and recruits him to the active party.
  3. Follow the stream (`w2_mq01_explore_stream` → completes `w2_mq01`, starts `w2_mq02`: *The Signal*).
  4. Clear the canopy path (`w2_mq02_clear_canopy`), reach `sector9_temple_gate` (`w2_mq02_locate_gate`), and tap `stone lock` (`w2_mq02_use_chime` → `w2_mq02_unlock_temple`, plays `sfx_w2_chime_gate`).
- **Guaranteed Player Knowledge:**
  - Zeke's ribs/suit are patched at the wreck; the brass Chime is pulling toward a clean harmonic signal inside an overgrown Architect facility.
  - Seating the Chime in the temple gate's stone lock opens the Sector 9 sanctuary (`hub_4_facility`).
- **Might Have Encountered / Missed:**
  - VHS Tape 3 (`find_vhs_tape_03` in `sector9_landing_stream`), Canopy Hopper arcade optic board (`sector9_optic_board`), starting `w2_sq01` (*Botanist*) with Zeke or `w2_sq02` (*Lost Patrol*) at `distress signature alpha` (`sector9_wilds_thickets`).
- **Unlocks Next:** Completes `w2_mq02`; unlocks `hub_4_facility` (`sector9_foyer_grand_hall`); starts `w2_mq03` (*Sleeping Giant*).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w2_mq01_examine_pod`, `w2_mq01_stabilize_zeke`, `w2_mq01_explore_stream`, `w2_mq02_unlock_temple`).

---

### `W02-02` — Stasis Ring Alignment, Orion's Awakening & The Bridge Relic
- **Location:** `sector9_hall_of_echoes` → `sector9_stasis_chamber` → `sector9_stasis_ring_array` / `sector9_stasis_coolant_lines` / `sector9_stasis_observation` (`hub_4_facility`).
- **Actual Trigger / Prerequisites:**
  1. Inspect `murals` in `sector9_hall_of_echoes` (`w2_mq03_inspect_murals`) and enter `sector9_stasis_chamber` (`w2_mq03_find_stasis`) → advances `w2_mq03` to stage `override_stasis`.
  2. Inspect `stasis pod` (`w2_mq03_inspect_pod`), `stasis mural overview` in `sector9_stasis_observation` (`w2_mq03_read_mural_overview`), and `coolant garden` in `sector9_stasis_coolant_lines` (`w2_mq03_stabilize_coolant`).
  3. Solve the 4-ring alignment at `ring console` in `sector9_stasis_ring_array` (`w2_mq03_align_complete` → sets `ms_w2_stasis_awake`, completes `align_stasis_rings`).
  4. Talk to Orion in `sector9_stasis_chamber` (`orion_w2_awakening_1`..`9` → `w2_mq03_talk_to_orion`, recruits Orion).
  5. Recover **The Bridge** (`bridge_relic`) from `sector9_archive_secret_stash` (`w2_mq03_recover_bridge`).
- **Guaranteed Player Knowledge:**
  - The stasis occupant from the cold open is **Orion**, an Aethel Tuner who sealed himself in twenty years ago during the Sector 9 breach after launching his Chime.
  - Orion looks up at the ceiling he painted before the lockdown, notices how thick the roots have grown, and accepts the seat Zeke clears for him before explaining **The Bridge** (Architect Echo #2, which links multiple willing carriers across separate frequency bands).
- **Might Have Encountered / Missed:**
  - **Silent Tap Defect (Verified):** Tapping `ring console` in `sector9_stasis_ring_array` *before* stabilizing coolant/reading the mural currently produces a **silent tap** on Android because `sector9_` is excluded from `needsUnavailableFeedback` and `ring console` lacks `unavailable_message`.
  - Optional side quests `w2_sq03` (*Tideglass Day* at `sector9_beach_pools` with Scout Kaelen in `sector9_beach_cove`) and `w2_sq04` (*Ancient Echoes* — gathering the west, east, and north resonance crystals to tune the mural chord).
- **Unlocks Next:** Grants `bridge_relic`; completes `w2_mq03` (`ms_w2_mq03_complete`); starts `w2_mq04` (*The Hunter*).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w2_mq03_align_complete`, `w2_mq03_talk_to_orion`, `w2_mq03_recover_bridge`), [dialogue.json](../../app/src/main/assets/dialogue.json) (`orion_w2_awakening_1`..`9`).

---

### `W02-03` — Gh0st Confrontation, The Beast & The Anchor Drill (`scene_anchor_drill`)
- **Location:** `sector9_canopy_ridge` (*Canopy Ridge*, `hub_3_sector9`).
- **Actual Trigger / Prerequisites:**
  1. Enter `sector9_canopy_ridge` (`w2_mq04_reach_canopy`).
  2. Tap `Confront stalker` (`w2_mq04_confront`) → dialogue `ghost_confrontation_1`..`6` (music `music_gh0st_theme`), setting `room_state.hunter_confronted = true` and `ms_w2_hunter_confronted`.
  3. Tap `Face the Beast` (`w2_mq04_beast_ambush`) → boss fight against `the_beast` (`music_w2_boss_guardian`).
  4. Victory (`w2_mq04_victory`) sets `ms_w2_beast_defeated` and `room_state.beast_defeated = true`.
  5. Tap `Anchor Drill` (`w2_mq04_anchor_drill`) → plays `scene_anchor_drill` (10 steps, `CARD`).
- **Guaranteed Player Knowledge:**
  - **Gh0st** (Dominion Synthetic Unit 734) was tracking the Chime under an automated disposal command, but when his command channel fails to answer his query, he chooses to lower his rifle even though *"The order's still in my head"* (`scene_anchor_drill[0]`).
  - After defeating the Source Beast together, Nova tries to open the Bridge alone and is overwhelmed by current; Zeke clips a ground lead to her bracer, Gh0st offers his armored hand, and Orion holds the counter-tone—establishing the **four-person shared circuit** that foreshadows the World 6 finale.
- **Might Have Encountered / Missed:**
  - VHS Tape 4 (`find_vhs_tape_04` in `sector9_ridge_plateau`).
  - Optional side quest `w2_sq05` (*Stolen Tech* in `sector9_vents_gantry`..`tech_alcove`) unlocked once Gh0st joins.
- **Unlocks Next:** Recruits `gh0st` to the party; unlocks Source Art `nova_link` (`link_unlock` tutorial); completes `w2_mq04` (`ms_w2_mq04_complete`); starts `w2_mq05` (*Liftoff*).
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`ghost_confrontation_1`..`6`), [events.json](../../app/src/main/assets/events.json) (`w2_mq04_confront`, `w2_mq04_beast_ambush`, `w2_mq04_victory`, `w2_mq04_anchor_drill`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_anchor_drill`).

---

### `W02-04` — Bypassing the Source Gate, Restoring the Astra & Airlane to the Spire (`scene_w2_astra_departure`)
- **Location:** `sector9_source_gate` (and surrounding emitter/power rooms) → `sector9_hangar_bay` (*Hangar Bay*) → `astra_bridge` (*Astra Bridge*) → warps to `spire_sewers_landing` (`world_3`).
- **Actual Trigger / Prerequisites:**
  1. Complete all four Source Gate feeds: `w2_mq05_stabilize_horn` (`sector9_gate_emitter_left`), `w2_mq05_ground_cup` (`sector9_gate_emitter_right`), `w2_mq05_read_pressure_gauge` (`sector9_power_valve`), and `w2_mq05_overload_breakers` (`sector9_power_breaker_deck`).
  2. Tap `acoustic lock` in `sector9_source_gate` (`w2_mq05_bypass_gate`) → unlocks `sector9_hangar_bay`.
  3. In `sector9_hangar_bay`, tap `dark console` (`w2_mq05_inspect_astra`), use `salvage return route` to collect `power conduits` at `sector9_landing_drop` (`w2_mq05_collect_conduits`), and tap `bridge relic` (`w2_mq05_reboot`, plays `sfx_w2_astra_reboot`).
  4. Board the Astra via `launch cradle` (`w2_mq05_board_astra` → `astra_common_room`), walk north to `astra_bridge`, and confirm departure at `nav console` (`w2_departure_entry` → `w2_mq05_confirm_departure`) → plays `scene_w2_astra_departure` (9 steps, `illustrated`, 39.3s).
- **Guaranteed Player Knowledge:**
  - The crew installs **The Bridge**, salvaged pod conduits, and Orion's **Chime** into the dormant starship **Astra**.
  - On their first flight (`scene_w2_astra_departure`), Orion keeps them low under the planetary Shield toward the Spire (`world_3`); Spire guidance interference drags them off the airlane, the deck tilts, **Gh0st catches Orion by the harness**, and Nova brings the ship down hard into the rain-flooded sewers beneath the Lower City.
- **Might Have Encountered / Missed:**
  - **Silent Tap Defect (Verified):** Tapping `acoustic lock` in `sector9_source_gate` before completing all four feeds produces a **silent tap** on Android (`unavailable_message` missing).
  - Completing `w2_sq02` (*Lost Patrol* — repairing Leto's thermal cutter and returning the patrol news to Scout Kaelen) or `w2_sq03` (*Tideglass Day*).
- **Unlocks Next:** Hub ship `hub_astra` (`astra_common_room`, `astra_quarters`, `astra_cargo_bay`, `astra_simulation_deck`, `astra_bridge`); completes `w2_mq05` (`ms_w2_mq05_complete`); warps to `spire_sewers_landing`; starts `w3_mq11` (*Under the Radar*).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w2_mq05_bypass_gate`, `w2_mq05_reboot`, `w2_mq05_confirm_departure`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w2_astra_departure`).

---

## 3. World 3 — The Spire (`world_3`: `hub_5_lower_city`, `hub_6_upper_city`)

### `W03-01` — Sewer Landing, Jax's Bar & The Safehouse Shield
- **Location:** `spire_sewers_landing` → `spire_vent_output` → `spire_the_static` (*The Static*) → `spire_zekes_apartment` (*Safehouse Apartment*, `hub_5_lower_city`).
- **Actual Trigger / Prerequisites:**
  1. Clear the sewer landing encounter (`w3_mq11_clear_landing`) and climb out at `spire_vent_output` (`w3_mq11_reach_vent`).
  2. Enter `spire_the_static` and talk to Jax (`jax_w3_mq11_shelter` / `w3_mq11_jax_shelter`).
  3. Enter `spire_zekes_apartment` (`w3_mq11_find_apartment`) and tap the signal shield (`w3_mq11_establish_shield`) → completes `w3_mq11`, starts `w3_mq12` (*The Plan*).
- **Guaranteed Player Knowledge:**
  - Bartender Jax lends the crew an old safehouse apartment above the Underrail on the condition that they keep patrols off his stairs and his name out of their reports.
  - Zeke and Orion wire a localized signal shield so the crew can plan a heist into the Upper City Archive where **The Lens** (Architect Echo #3) is held.
- **Might Have Encountered / Missed:**
  - Optional side quests `w3_sq11` (*Neon Fix* for Jax) and `w3_sq13` (*Night Market Run* for Mika in `spire_night_market`, plus VHS Tape 5 `find_vhs_tape_05`).
- **Unlocks Next:** `w3_mq12` (*The Plan*) stage `gather_intel`.
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w3_mq11_clear_landing`, `w3_mq11_reach_vent`, `w3_mq11_jax_shelter`, `w3_mq11_establish_shield`), [dialogue.json](../../app/src/main/assets/dialogue.json) (`jax_w3_mq11_shelter`, `zeke_w3_mq11_safehouse_intro`..`end`).

---

### `W03-02` — Lower City Heist Prep & First Elara Discovery
- **Location:** `spire_the_static`, `spire_underrail_platform`, `spire_transit_plaza`, `spire_checkpoint_queue`, `spire_safehouse_roof`, `spire_zekes_apartment` (`hub_5_lower_city`).
- **Actual Trigger / Prerequisites:**
  1. Complete the five intelligence tasks in any order after `w3_mq12_talk_jax`:
     - `w3_mq12_talk_jax` (`spire_the_static`): Jax provides curfew schedules and notes that **Director Mara Thorne** suspended public freight while keeping her private Executive Lift open.
     - `w3_mq12_map_patrols` (`spire_underrail_platform`): patrol timing gaps.
     - `w3_mq12_interrogate_guard` (`spire_transit_plaza` security kiosk): extraction codes, staff routes, and **a deleted transfer chain stamped with the name "Elara"** (*"and his [Gh0st's] voice goes flat enough to hurt"*).
     - `w3_mq12_copy_badges` (`spire_checkpoint_queue`): valid freight service badge authentication pattern.
     - `w3_mq12_source_disguises` (`spire_safehouse_roof`): maintenance uniforms from the laundry lines.
  2. Once all five are done, tap the workstation in `spire_zekes_apartment` (`w3_mq12_hack_blueprints`) → Zeke compiles the elevator route.
  3. Tap the planning table (`w3_mq12_review_plan` → `w3_mq12_planning_table_dialogue` → confirm `w3_mq12_assemble_planning`) → completes `w3_mq12`, starts `w3_mq13` (*Social Engineering*).
- **Guaranteed Player Knowledge:**
  - **First mandatory mention of Elara:** Gh0st discovers Elara's name in a deleted Spire transfer record at the Transit Plaza kiosk (`w3_mq12_interrogate_guard`).
  - **Cover Story Established:** The planning table explicitly assigns the crew's Upper City cover story: **maintenance uniforms + copied freight badges + `"scheduled Skypark air-filter inspection"`** (`w3_mq12_planning_table_dialogue`).
- **Might Have Encountered / Missed:**
  - **Optional Side Quest `w3_sq12` (*Cold Case*):** Talking to Gh0st (`w3_sq12_ghost_intro`) and breaching the security database (`w3_sq12_find_case_number` → `w3_sq12_access_terminal` → `w3_sq12_expose_transfer`) explicitly reveals that **Elara is Gh0st's human sister**, whose transfer to Sector 7 (The Foundry) under Project Phantom was scrubbed after he entered service. If the player skips `w3_sq12`, they know the name "Elara" matters to Gh0st from `w3_mq12_interrogate_guard`, and learn her full sibling bond in World 4 (`w4_mq17`).
  - **Optional Side Quest `sq_chloe_01` (*The Wayfarer's Signal*):** Helping musicians Chris and Andrew restore their broadcast in the Lower City studio (`andrew_favor_acknowledgment` → `nova_personal_thanks` → `wayfarer_quest_conclude`).
- **Unlocks Next:** `w3_mq13` (*Social Engineering*); service elevator transition to `hub_6_upper_city` (`spire_laundry_service` / `spire_skypark_dome`).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w3_mq12_talk_jax`..`w3_mq12_assemble_planning`, `w3_sq12_expose_transfer`), [dialogue.json](../../app/src/main/assets/dialogue.json) (`w3_mq12_planning_table_dialogue`, `w3_sq12_ghost_intro`).

---

### `W03-03` — Upper City Infiltration & The Curator Cover-Story Deduction
- **Location:** `spire_skypark_dome` (*Skypark Dome*) → `spire_laundry_service` (*Laundry Service*) → `spire_prism_gallery` (`hub_6_upper_city`).
- **Actual Trigger / Prerequisites:**
  1. In `spire_skypark_dome`, talk to the Museum Curator (`curator_w3_mq13_intro` → `curator_w3_mq13_insult` → `w3_cover_order`).
  2. Select `"Scheduled air-filter inspection."` (`opt:correct` → `w3_cover_service_verified` → `zeke_w3_mq13_cover` → triggers `player_action:w3_mq13_blend_in`).
  3. In `spire_laundry_service`, talk to Zeke / use the sensor console (`zeke_w3_mq13_console_intro`..`end` → `w3_mq13_disable_sensors`).
  4. Enter the Archive approach (`w3_mq13_enter_lobby`) → completes `w3_mq13`, starts `w3_mq14` (*The Lens*).
- **Guaranteed Player Knowledge:**
  - The Curator treats Orion as "salvage" and demands the crew's work order; presenting the `"Scheduled air-filter inspection"` work order matches the copied freight badges and clears the guest registry.
  - Zeke uses a maintenance login left in the laundry to blind the penthouse scanner grid while Orion dampens Source noise.
- **Might Have Encountered / Missed:**
  - Choosing the wrong cover story (`"Emergency glass repair"` or `"Private delivery for a guest"`) triggers in-character rebuttals (`w3_cover_emergency_rebuttal`, `w3_cover_delivery_rebuttal`) where Zeke/narration reminds Nova of the folded service order in her hand and loops cleanly back to `w3_cover_recover`.
  - **Optional Upper City Content:**
    - `w3_sq14` (*Corporate Espionage* in `spire_exec_lounge_bar` — copying the concierge credential mirror, stealing the encrypted ledger, and choosing between leaking it publicly `w3_sq14_leak_ledger` or selling/weaponizing it privately `w3_sq14_weaponize_ledger`, plus discovering the `spire_infiltrator` VIP arcade cabinet).
    - `w3_sq15` (*Prototype Testing* with `lab_terminal`).
    - `w3_sq16` (*Murder in the Skyline Salon* in `spire_exec_lounge_scale_01` — audited in detail in [starborn-story-audit.md](starborn-story-audit.md)).
    - Botanist Evelyn and Entrepreneur Robert conversations/gifts; VHS Tape 6 (`find_vhs_tape_06`).
- **Unlocks Next:** `w3_mq14` (*The Lens*) stage `archive_infiltration`.
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`curator_w3_mq13_intro`, `w3_cover_order`), [events.json](../../app/src/main/assets/events.json) (`w3_mq13_blend_in`, `w3_mq13_disable_sensors`, `w3_mq13_enter_lobby`).

---

### `W03-04` — The Archive Vault, Thorne's Order & Vale's First Override
- **Location:** `spire_prism_gallery` → `spire_archive_vault` (*Archive Vault*, `hub_6_upper_city`).
- **Actual Trigger / Prerequisites:**
  1. Read the three prism clues (`w3_mq14_read_containment_field`, `w3_mq14_read_prism_shutters`, `w3_mq14_trace_command_tethers`) and solve tuning puzzle `w3_chrono_flux_tune` (`w3_mq14_solve_light_puzzle` → advances `w3_mq14` to stage `claim_lens`).
  2. Tap `lens relic` (`w3_mq14_review_lens`) → runs dialogue chain:
     - `zeke_w3_mq14_lens_warning` → `nova_w3_lens_urgency` → `zeke_w3_lens_isolate` (Nova waits for Zeke to isolate the return feed).
     - `thorne_w3_lens_recovery_order`: **Director Mara Thorne** speaks over the vault channel (*"I sold access to Harmony for the next ten years. I intend to deliver"*).
     - `nova_w3_mq14_lens_take` → triggers `player_action:w3_mq14_take_lens`.
  3. `w3_mq14_take_lens` grants `the_lens`, unlocks Source Art `source_art_pierce`, completes `w3_mq14`, starts `w3_mq15` (*Burn Notice*), and immediately triggers `thorne_w3_mq14_trap_intro` (music `music_thorne_theme`) → `vale_w3_mq15_override` (music `music_vale_theme`) → `thorne_w3_mq14_trap_vale`.
- **Guaranteed Player Knowledge:**
  - **The Lens** (Architect Echo #3) matches Nova's Fork protocol scar and allows the carrier to see hidden phase fractures and weak points (`source_art_pierce`).
  - **Antagonist Split Introduced:** Director Thorne orders the Administrator to contain the crew and recover the Lens intact, mentioning she has *"opened an audit and given Vale recovery access."*
  - **Arden Vale** immediately uses that recovery access (`vale_w3_mq15_override`) to seal the roof, authorize **lethal force**, and lock out Thorne's countermand (`DENIED`), stating: *"The anomaly won't leave this sector again."*
  - Realizing Vale has seized the rooftop guns and wants the Lens destroyed or captured for himself, Thorne tells Nova out of self-interest: *"He's taken the guns. Get the Lens off his roof. The launch route still answers to me"* (`thorne_w3_mq14_trap_vale`).
- **Might Have Encountered / Missed:**
  - None on the critical path; the entire exchange is mandatory upon claiming the Lens.
- **Unlocks Next:** `w3_mq15` (*Burn Notice*) stage `escape_lockdown`; access to `spire_landing_pad_roof`.
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w3_mq14_solve_light_puzzle`, `w3_mq14_review_lens`, `w3_mq14_take_lens`), [dialogue.json](../../app/src/main/assets/dialogue.json) (`zeke_w3_mq14_lens_warning`, `thorne_w3_lens_recovery_order`, `thorne_w3_mq14_trap_intro`, `vale_w3_mq15_override`, `thorne_w3_mq14_trap_vale`).

---

### `W03-05` — Rooftop Boss Battle & Escape Through the Curtain (`scene_w3_foundry_flight`)
- **Location:** `spire_landing_pad_roof` (*Rooftop Landing Pad*, `hub_6_upper_city`) → warps to `foundry_slag_landing` (`world_4`).
- **Actual Trigger / Prerequisites:**
  1. Defeat `administrator_boss` (`music_w3_boss_administrator`, victory `music_victory_boss`) on `spire_landing_pad_roof` (`w3_mq15_defeat_administrator`).
  2. Tap `security-curtain timing` (`w3_scan_shield_gap`) → completes `scan_shield_gap`, advances `w3_mq15` to stage `launch_to_foundry`.
  3. Tap `launch window display` (`w3_mq15_launch_astra`) → plays `scene_w3_foundry_flight` (5 steps, `illustrated`, 19.3s).
- **Guaranteed Player Knowledge:**
  - The crew defeats the Spire Administrator, times a narrow seam in the low-altitude security curtain while the outer planetary Shield remains sealed overhead, and flies the damaged Astra to Sector 7 (**The Foundry**) because only a Foundry Deep-Core Engine and Phase-Cutter Arrays can punch through to the Orbital Ring.
- **Might Have Encountered / Missed:**
  - Tapping `launch window display` before defeating the Administrator or scanning the shield gap triggers dedicated state-aware error feedback in `ExplorationController.kt:6125-6139`.
- **Unlocks Next:** Completes `w3_mq15` (`ms_w3_mq15_complete`, `ms_w4_access_unlocked`); warps to `foundry_slag_landing`; starts `w4_mq16` (*Into the Fire*).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w3_mq15_defeat_administrator`, `w3_scan_shield_gap`, `w3_mq15_launch_astra`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w3_foundry_flight`).

---

## 4. World 4 — The Foundry (`world_4`: `hub_7_slag_pits`, `hub_8_assembly_line`)

### `W04-01` — Obsidian Shelf, Slag River & Cooling the Airlock
- **Location:** `foundry_slag_landing` → `foundry_obsidian_overlook` → `foundry_slag_river` → `foundry_service_airlock` (`hub_7_slag_pits`).
- **Actual Trigger / Prerequisites:**
  1. Land at `foundry_slag_landing` (`w4_mq16_land_shelf` → `gh0st_w4_landing_intro`).
  2. Solidify and cross the slag bridge (`w4_mq16_cross_slag_river` via `w4_magma_conduit_tune`).
  3. Hack the cooling vents (`w4_mq16_hack_cooling_vents`) and enter `foundry_service_airlock` (`w4_mq16_enter_airlock`) → completes `w4_mq16`, starts `w4_mq17` (*Ghost in the Machine*).
- **Guaranteed Player Knowledge:**
  - Gh0st recognizes the Foundry's heat and industrial cadence from his own manufacture.
- **Might Have Encountered / Missed:**
  - In `foundry_obsidian_overlook`, tapping `shield window telemetry` (`w4_compliance_bulletin` → `vale_w4_compliance_bulletin`) plays Vale's sector-wide audit lockdown broadcast (*"Sector 7 is under audit. Unregistered craft detected. No departures until the anomaly is resolved"*).
  - Optional side quests `w4_sq16` (*Sabotage*), `w4_sq17` (*Lost Worker* with Forge Hand Rylan), `w4_sq18` (*The Scrap Heap*), and `w4_sq_crucible` (*The Scrapper's Crucible* gladiator waves with Pitmaster Brutus).
- **Unlocks Next:** `w4_mq17` (*Ghost in the Machine*) stage `phantom_records`.
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w4_mq16_land_shelf`, `w4_compliance_bulletin`, `w4_mq16_cross_slag_river`, `w4_mq16_hack_cooling_vents`, `w4_mq16_enter_airlock`).

---

### `W04-02` — Project Phantom Records & Cooling Springs Override Key
- **Location:** `foundry_waste_intake` (*Waste Intake*) → `foundry_cooling_springs` (*Cooling Springs*, `hub_7_slag_pits`).
- **Actual Trigger / Prerequisites:**
  1. In `foundry_waste_intake`, tap `project phantom terminal` (`w4_mq17_access_phantom_terminal_scene` → dialogue `gh0st_w4_phantom_intro` → `nova_w4_phantom_wait` → `w4_mq17_access_phantom_terminal`).
  2. Move to `foundry_cooling_springs` and tap `steam pool` (`w4_mq17_regroup_springs_scene` → dialogue `gh0st_w4_springs_intro` → `gh0st_w4_springs_elara` → `gh0st_w4_springs_key` → `w4_mq17_regroup_springs`).
- **Guaranteed Player Knowledge:**
  - Gh0st asks to read the Project Phantom intake record first; Nova respects his boundary and waits.
  - The file reveals that **Project Phantom** built synthetic enforcers (including Unit 734 / Gh0st) around neural compliance maps harvested from live human subjects—and the baseline template for Unit 734's series was **his sister Elara**, who left a handwritten margin correction on his calibration sheet.
  - At Cooling Springs, Gh0st recalls a specific memory of Elara before they were taken: she used to ask from the doorway whether he wanted the hall light left on, and **waited until he answered**.
  - Because Commander Rylos holds the master vocal override for the 700-series at the Assembly Line, Gh0st hands Nova his `gh0st_override_key` (*"It can pause his kill suite long enough for him to choose; it cannot control him"*), asking her to use it only if Rylos forces his command channel.
- **Might Have Encountered / Missed:**
  - Tapping `steam pool` in `foundry_cooling_springs` before reading the Phantom terminal triggers `w4_mq17_steam_pool_before_records`.
  - VHS Tape 7 (`find_vhs_tape_07`) and `thermite_logic_gate` arcade discovery.
- **Unlocks Next:** Grants `gh0st_override_key`; completes `w4_mq17`; unlocks `hub_8_assembly_line`; starts `w4_mq18` (*The Assembly*).
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`gh0st_w4_phantom_intro`, `nova_w4_phantom_wait`, `gh0st_w4_springs_intro`, `gh0st_w4_springs_elara`, `gh0st_w4_springs_key`), [events.json](../../app/src/main/assets/events.json) (`w4_mq17_access_phantom_terminal`, `w4_mq17_regroup_springs`).

---

### `W04-03` — The Assembly Floor, The Anvil & Nova's Failed Construct
- **Location:** `foundry_conveyor_belt` → `foundry_conditioning_chamber` → `foundry_forge_anvil` (*The Forge*, `hub_8_assembly_line`).
- **Actual Trigger / Prerequisites:**
  1. Navigate conveyors (`w4_mq18_navigate_conveyors`), defeat the Phantom prototypes (`w4_mq18_defeat_prototypes`), and tap `fabrication matrix` in `foundry_forge_anvil` (`w4_mq18_overload_matrix`) → shuts down the Phantom shell line, completes `w4_mq18`, starts `w4_mq19` (*The Anvil*).
  2. Complete the four Forge prerequisites across the conveyor/alcove rooms: `w4_mq19_read_pulse_board` (`ms_w4_pulse_board_read`), `w4_mq19_read_grease_marks` (`ms_w4_grease_route_read`), `w4_mq19_throw_override_paddle` (`ms_w4_paddle_thrown` via `w4_conveyor_override`), and `w4_mq19_starve_breath_pistons` (`ms_w4_pistons_starved`).
  3. Tap `anvil cradle` in `foundry_forge_anvil` (`w4_mq19_open_anvil_cradle` → sets `ms_w4_conveyor_puzzle_solved`, advances `w4_mq19` to `claim_anvil`).
  4. Tap `Anvil relic` (`w4_mq19_take_anvil_scene` → dialogue `nova_w4_mq19_anvil_intro` → `gh0st_w4_mq19_catch_nova` → `w4_mq19_take_anvil`).
- **Guaranteed Player Knowledge:**
  - Nova claims **The Anvil** (Architect Echo #4, which gives physical mass/form to Source projections via `source_art_construct`).
  - Overconfident, Nova tries to force a hard-light Construct alone right out of the damaged cradle; the projection shatters violently and **Gh0st catches her before she hits the floor**, reminding her not to carry the load alone.
- **Might Have Encountered / Missed:**
  - Optional side quests `w4_sq19` (*Quality Control* — choosing how to resolve sentient rejected units) and `w4_sq20` (*Overclocked* — `w4_hazard_calibration` at `test chamber terminal` in `foundry_forge_anvil`).
- **Unlocks Next:** Grants `the_anvil`; unlocks Source Art `source_art_construct`; completes `w4_mq19`; starts `w4_mq20` (*Meltdown*).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w4_mq18_overload_matrix`, `w4_mq19_open_anvil_cradle`, `w4_mq19_take_anvil`), [dialogue.json](../../app/src/main/assets/dialogue.json) (`nova_w4_mq19_anvil_intro`, `gh0st_w4_mq19_catch_nova`).

---

### `W04-04` — Rylos Showdown, Mercy at Titan Dock (`scene_w4_rylos_spared`) & Vale's Coup (`scene_w4_outer_shield`)
- **Location:** `foundry_titan_dock` (*Titan Dock*, `hub_8_assembly_line`) → warps to `orbital_executive_dock` (`world_5`).
- **Actual Trigger / Prerequisites:**
  1. Confront Commander Rylos (`w4_mq20_confront_rylos_scene` → `rylos_w4_mq20_intro` → `w4_mq20_confront_rylos`).
  2. Defeat `titan_walker_boss` (`music_w4_boss_titan`, victory `music_victory_boss`) → triggers `w4_mq20_defeat_titan_walker`, which plays `scene_w4_rylos_spared` (3 steps, `illustrated`, 11.7s) and advances `w4_mq20` to `steal_and_escape`.
  3. Collect `deep_core_engine` (`w4_mq20_steal_engine`) and `phase_cutter_arrays` (`w4_mq20_steal_arrays`).
  4. Tap `escape launch` (`w4_mq20_escape_launch`) → plays `scene_w4_outer_shield` (9 steps, `illustrated`, 44.6s, `stinger_motif_vale` on step 3, `music_vale_theme` on step 6).
- **Guaranteed Player Knowledge:**
  - Rylos attempts to command `"Unit 734"`, emphasizing that Director Thorne paid for every circuit in the Foundry; Gh0st answers as **Gh0st**, breaks the override, and after defeating the Titan Walker **lowers his blade and leaves the hatch open for an injured Rylos to escape** (`scene_w4_rylos_spared`).
  - As the Astra uses the stolen Deep-Core Engine and Phase-Cutter Arrays to slice through the outer planetary Shield (`scene_w4_outer_shield[0..2]`), **Vale executes his coup over the command channel** (`scene_w4_outer_shield[3..7]`): he invalidates Director Thorne's signature on the ownership register, extends the emergency authorization she gave him at the Spire across the entire Orbital Ring, and locks down all sectors.
  - Gh0st notes the chilling shift in the Ring's automated carrier wave: *"The Ring used to announce whose orders I was following. Listen. It's stopped saying her name"* (`scene_w4_outer_shield[7]`).
- **Might Have Encountered / Missed:**
  - VHS Tape 8 (`find_vhs_tape_08` at Titan Dock gantry).
- **Unlocks Next:** Completes `w4_mq20` (`ms_w4_mq20_complete`, `ms_w5_access_unlocked`); warps to `orbital_executive_dock`; starts `w5_mq21` (*Docking Procedure*).
- **Citations:** [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w4_rylos_spared`, `scene_w4_outer_shield`), [events.json](../../app/src/main/assets/events.json) (`w4_mq20_defeat_titan_walker`, `w4_mq20_escape_launch`).

---

## 5. World 5 — The Orbital Ring (`world_5`: `hub_9_orbital_ring`, `hub_10_deep_ring`)

### `W05-01` — Forced Docking at Executive Dock & Crossing the Solarium
- **Location:** `orbital_executive_dock` (*Executive Dock*) → `orbital_airlock_gallery` → `orbital_grand_concourse` → `orbital_solarium` → `orbital_security_hub` (`hub_9_orbital_ring`).
- **Actual Trigger / Prerequisites:**
  1. At `orbital_executive_dock`, trigger `w5_mq21_arrive_halo`, defeat the orbital fighter interceptors (`w5_mq21_fighters_down`), force the docking clamps (`w5_mq21_force_dock`), and hack the airlock (`w5_mq21_hack_airlock` → `gh0st_w5_mq21_airlock`) → completes `w5_mq21`, starts `w5_mq22` (*Zero G*).
  2. Cross `orbital_solarium` (`w5_mq22_cross_solarium`), clear `orbital_security_hub` (`w5_mq22_clear_security`), and traverse `orbital_service_shaft` (`w5_mq22_traverse_shaft` via `w5_graviton_matrix_tune`).
- **Guaranteed Player Knowledge:**
  - Vale's Compliance forces are actively trying to shoot the Astra off the Ring collar; the crew forces a hard dock and breaches the executive habitat, which is eerily quiet under Vale's lockdown.
- **Might Have Encountered / Missed:**
  - **Orphaned Dialogue (Verified Defect):** `vale_w5_compliance_greeting` in `dialogue.json` (*"Welcome to the Ring. Director Thorne is unavailable. Orbital Compliance is acting command..."*) is never triggered by `orbital_airlock_gallery` (whose `compliance greeting` action is a static inspection).
  - Optional side quests `w5_sq21` (*Employee of the Month* — Thorne's private logs), `w5_sq22` (*Solar Maintenance* with Maintenance Bot), `w5_sq23` (*Vacuum Seal* — sealing pressure breaches from the forced dock), Analyst Charles's favor in the Customs Lounge (`analyst_charles_default_talk` + `zenith_telemetry_array` arcade discovery), and VHS Tape 9 (`find_vhs_tape_09` in `orbital_solarium`).
- **Unlocks Next:** Access to `deep_mainframe_nave` (`hub_10_deep_ring`).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w5_mq21_arrive_halo`..`w5_mq21_hack_airlock`, `w5_mq22_cross_solarium`..`w5_mq22_traverse_shaft`).

---

### `W05-02` — The Great Silence Mainframe & Thorne's Bargain
- **Location:** `deep_mainframe_nave` (*Mainframe Nave*, `hub_10_deep_ring`).
- **Actual Trigger / Prerequisites:**
  1. Tap `mainframe terminal` (`w5_mq22_access_mainframe`) → completes `access_mainframe`.
  2. Tap `Thorne trace` / talk to Mara Thorne (`w5_mq22_find_thorne_scene` → dialogue `thorne_w5_mq22_intro` [music `music_thorne_theme`] → `thorne_w5_mq22_intro_beat_1` → `thorne_w5_mq22_intro_beat_2` → `nova_w5_thorne_terms` [music `music_nova_theme`] → `thorne_w5_route_terms` → `w5_mq22_find_thorne`).
- **Guaranteed Player Knowledge:**
  - **The Great Silence & Harmony Truth:** The mainframe log reveals that **Harmony** is not a synthetic neural cloud—it is a parasitic broadcast network routed through living human brains (led by **Elara**, wired into the Anchor Chamber as its central wetware stabilization node), and the **"Great Silence"** was the catastrophic mental collapse of the first test colony when their individual frequencies were forcibly synchronized.
  - **Thorne vs. Vale Contrast:** Cornered and locked out of her own station, Mara Thorne admits she built the cage and sold Harmony subscriptions for profit (*"I sold Harmony by the seat. The Foundry supplied the hardware, and my signature put those people in the chairs"*), while warning that Vale copied her command cipher not to sell the network, but to collapse every connected mind into a single conflict-free voice.
  - **The Crew's Terms:** Gh0st demands the unredacted occupied-seat list (`thorne_w5_mq22_intro_beat_2`), and Nova makes clear they are taking Thorne's maintenance route to free the captives, **not** to restore Thorne's access (`nova_w5_thorne_terms`).
- **Might Have Encountered / Missed:**
  - **Easily Missed Follow-Up (`thorne_w5_mq23_core`):** Once `w5_mq22_find_thorne` completes and starts `w5_mq23`, Thorne has a dedicated line explaining the three firewall nodes (`thorne_w5_mq23_core`, music `music_thorne_theme`), **but** it only plays if the player manually taps `thorne` a second time before walking out of `deep_mainframe_nave`.
  - **NPC Persistence Bug (Verified):** `thorne` is hardcoded in `deep_mainframe_nave` `npcs: ["thorne"]` with `npc_presence: null`, so she remains standing in `deep_mainframe_nave` even after moving to `deep_throne_room` and being executed by Vale in `w5_mq25_soloist_scene`!
- **Unlocks Next:** Completes `w5_mq22`; starts `w5_mq23` (*The Core Approach*).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w5_mq22_access_mainframe`, `w5_mq22_find_thorne`), [dialogue.json](../../app/src/main/assets/dialogue.json) (`thorne_w5_mq22_intro`..`thorne_w5_route_terms`, `thorne_w5_mq23_core`), [rooms.json](../../app/src/main/assets/rooms.json) (`deep_mainframe_nave`).

---

### `W05-03` — The Firewall Maze, Elara's Reunion & One Breath (`scene_w5_anchor_stasis`)
- **Location:** `orbital_server_farm` / firewall nodes → `deep_anchor_chamber` (*Anchor Chamber*, `hub_10_deep_ring`).
- **Actual Trigger / Prerequisites:**
  1. Clear the three firewall nodes (`w5_mq23_firewall_alpha`, `w5_mq23_firewall_beta`, `w5_mq23_firewall_gamma`) and defeat the Hard-Light Guardian (`w5_mq23_defeat_construct`) → completes `w5_mq23`, starts `w5_mq24` (*The Anchor*).
  2. Enter `deep_anchor_chamber` (`w5_mq24_enter_chamber`) and tap `elara` (`w5_mq24_find_elara_scene` → dialogue `elara_w5_mq24_reunion` [music `music_elaras_song`] → `gh0st_w5_elara_lamp` → `elara_w5_lamp_answer` → `gh0st_w5_answer_elara` → `elara_w5_release_cradle` → `w5_mq24_find_elara`).
  3. Tap `anchor relic` (`w5_mq24_take_anchor`) → plays `scene_w5_anchor_stasis` (5 steps, `illustrated`, 20.9s, `stinger_motif_elara` on step 4).
- **Guaranteed Player Knowledge:**
  - Through the firewall static, Elara's voice guides the crew in. Inside `deep_anchor_chamber`, she is suspended inside a wetware life-support tank wired directly into **The Anchor** (Architect Echo #5).
  - **Payoff of the Cooling Springs Memory:** Gh0st asks her across the glass if she still leaves the hall lamp on (`gh0st_w5_elara_lamp`); Elara answers *"Only when you're late. I kept the door clear for you"* (`elara_w5_lamp_answer`), calls him *"brother,"* and holds the firewall open from inside so Nova can pull the Anchor free.
  - When Nova pulls **The Anchor**, a cloaked Hunter-Killer construct lunges at Gh0st; Nova instinctively fires **Stasis** (`source_art_stasis`), freezing the Hunter-Killer and suspended coolant mid-air (`scene_w5_anchor_stasis[0..2]`).
  - Elara remains trapped inside the tank glass (`"I can feel the glass. The tank still has me. Follow the cable to the Throne Room. I'll keep the door open"` — `scene_w5_anchor_stasis[4]`).
- **Might Have Encountered / Missed:**
  - Optional side quests `w5_sq24` (*Ghost in the Shell*) and `w5_sq25` (*Admin Privileges*).
- **Unlocks Next:** Grants `anchor_relic`; unlocks Source Art `source_art_stasis`; completes `w5_mq24`; starts `w5_mq25` (*Critical Mass*).
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`elara_w5_mq24_reunion`..`elara_w5_release_cradle`), [events.json](../../app/src/main/assets/events.json) (`w5_mq24_find_elara`, `w5_mq24_take_anchor`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w5_anchor_stasis`).

---

### `W05-04` — The Soloist in the Throne Room, Compliance Avatar & Across the Tear (`scene_w5_cross_the_tear`)
- **Location:** `deep_throne_room` (*Throne Room*) → `deep_tear` (*The Tear*, `hub_10_deep_ring`) → warps to `source_campfire` (`world_6`).
- **Actual Trigger / Prerequisites:**
  1. Enter `deep_throne_room` (`w5_mq25_reach_throne`) and tap `containment field` / `vale` (`w5_mq25_soloist_scene` → dialogue `vale_w5_mq25_soloist` [music `music_vale_theme`] → `vale_w5_mq25_soloist_beat_1`..`6` → `w5_mq25_soloist`).
  2. Move to `deep_tear` and defeat `compliance_avatar` (`music_w5_boss_compliance`, victory `music_victory_boss` → `w5_mq25_defeat_avatar`).
  3. Tap `dimensional fracture` (`w5_mq25_enter_tear`) → plays `scene_w5_cross_the_tear` (6 steps, `illustrated`, 21.2s, `stinger_motif_source` on step 5).
- **Guaranteed Player Knowledge:**
  - In the Throne Room, a trapped worker's voice begs through the dais for her sister in Sector 4 (`vale_w5_mq25_soloist`). Even Mara Thorne shows a human limit, telling Vale: *"Let her finish the sentence. If we cut a live line now, we lose the whole sector's confidence"* (`vale_w5_mq25_soloist_beat_1`).
  - Vale flattens the woman's voice into a single pitch (*"A choir still contains variance. Variance contains pain. One voice. A solo"*).
  - Nova fires **Stasis** at Vale (`vale_w5_mq25_soloist_beat_4`), **but Vale's Anchor-level resonance shrugs it off**; he drops Thorne's containment lock—**executing Mara Thorne**—and channels the Ring's full power to tear reality open into **The Source**, dragging Elara's tank cable into the fracture (`vale_w5_mq25_soloist_beat_5..6`).
  - After defeating the Compliance Avatar at the edge of the Tear, Gh0st reaches for Elara as her cable peels into the rift (`scene_w5_cross_the_tear[0..2]`); their fingers slip apart, Nova grabs Gh0st's arm (`"We're going after her. Take my arm"`), the four crewmates leap in holding hands, and the current tears their grip apart—leaving Nova waking alone beside a blue campfire (`scene_w5_cross_the_tear[3..5]`).
- **Might Have Encountered / Missed:**
  - **NPC Persistence Bug (Verified):** `deep_anchor_chamber` still lists `npcs: ["elara"]` and `deep_throne_room` still lists `npcs: ["vale"]` without `forbidden_milestones`.
- **Unlocks Next:** Completes `w5_mq25` (`ms_w5_mq25_complete`, `ms_w6_access_unlocked`); warps to `source_campfire`; removes Zeke, Orion, and Gh0st from the active party until rescued; starts `w6_mq26` (*Fractured Minds*).
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`vale_w5_mq25_soloist`..`beat_6`), [events.json](../../app/src/main/assets/events.json) (`w5_mq25_soloist`, `w5_mq25_defeat_avatar`, `w5_mq25_enter_tear`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w5_cross_the_tear`).

---

## 6. World 6 — The Source & Epilogue (`world_6`: `hub_11_event_horizon`, `hub_12_singularity`)

### `W06-01` — Three Nightmares & Back at the Fire (`scene_w6_song_fire_reunion`)
- **Location:** `source_campfire` (*The Campfire*) → `source_zeke_nightmare` / `source_gh0st_nightmare` / `source_orion_nightmare` → `source_campfire` (`hub_11_event_horizon`).
- **Actual Trigger / Prerequisites:**
  1. Rescue the three separated companions in **any order**:
     - **Zeke (`source_zeke_nightmare`):** `w6_mq26_rescue_zeke` (`music_w6_nightmare_zeke`) → win nightmare battle → `zeke_w6_nightmare_release` (Zeke rejects the endless safe booth where he stamps retirement forms and nobody gets hurt if he never looks up).
     - **Gh0st (`source_gh0st_nightmare`):** `w6_mq26_rescue_gh0st` (`music_w6_nightmare_gh0st`) → win nightmare battle → `gh0st_w6_nightmare_release` (Gh0st rejects the silent firing range where an empty command channel relieves him of ever choosing again).
     - **Orion (`source_orion_nightmare`):** `w6_mq26_rescue_orion` (`music_w6_nightmare_orion`) → win nightmare battle → `orion_w6_nightmare_release` (Orion rejects the painless stasis sleep where everyone he outlived is still waiting for him).
  2. Return to `source_campfire` and tap `song-fire` (`w6_mq26_reassemble`) → plays `scene_w6_song_fire_reunion` (4 steps, `illustrated`, 16.1s).
- **Guaranteed Player Knowledge:**
  - Vale's realm tempts each companion with a tailored, painless revision of their deepest guilt (complicity for Zeke, agency/responsibility for Gh0st, survivor's grief for Orion).
  - At the song-fire, all four crewmates reunite; Zeke offers Nova a spare glove for her shaking hand, Orion hears all four frequencies together, and their four shadows converge to form **The Key** (`key_relic`, Architect Echo #6).
- **Might Have Encountered / Missed:**
  - All 6 permutation orders of rescuing Zeke, Gh0st, and Orion are valid; party-aware rest banter in `RestBanter.kt` adapts to whichever companions are currently rescued.
  - VHS Tape 10 (`find_vhs_tape_10`) and `prismatic_tuning_fork` arcade discovery (`harmonic_pulse`).
- **Unlocks Next:** Grants `key_relic`; completes `w6_mq26`; warps to `source_echo_mines`; starts `w6_mq27` (*The Echo of the Mines*).
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w6_mq26_rescue_zeke`, `w6_mq26_rescue_gh0st`, `w6_mq26_rescue_orion`, `w6_mq26_reassemble`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w6_song_fire_reunion`).

---

### `W06-02` — Echo Mines, Memory Bridge & The Campfire Promise
- **Location:** `source_echo_mines` → `source_echo_elevator` → `source_memory_bridge` → `source_memory_bridge_span` → `source_memory_threshold` (`hub_11_event_horizon`).
- **Actual Trigger / Prerequisites:**
  1. Cross `source_echo_mines` (`w6_mq27_enter_mines`), evade/defeat the distorted Manager (`w6_mq27_evade_manager`), and ride `source_echo_elevator` (`w6_mq27_reach_elevator`) → completes `w6_mq27`, starts `w6_mq28` (*The Crossing*).
  2. In `source_memory_bridge`, activate all four crew memory anchors in any order (`w6_mq28_anchor_zeke`, `w6_mq28_anchor_ghost`, `w6_mq28_anchor_orion`, `w6_mq28_anchor_nova`) and tap `build bridge` (`w6_mq28_build_bridge`).
  3. In `source_memory_bridge_span` (*Shared Memory Span*), tap `campfire promise` (`w6_mq28_final_banter_scene` → dialogue `w6_mq28_final_banter_line_1`..`5` → `w6_mq28_final_banter`).
  4. In `source_memory_threshold`, tap `singularity threshold` (`w6_mq28_reach_singularity`) → completes `w6_mq28`, warps into `hub_12_singularity`, starts `w6_mq29` (*The Spire of Thought*).
- **Guaranteed Player Knowledge:**
  - The crew builds a span out of four witnessed memories from their actual journey: Zeke gripping the cargo pod frame beside Nova, Gh0st lowering his rifle on Canopy Ridge while the order was still in his head, Orion waking to his painted ceiling in Sector 9, and Jed's workbench lamp.
  - At the Shared Memory Span, the crew pauses for a quiet promise grounded in Zeke's squeaking left boot and four sets of footsteps crossing together (`w6_mq28_final_banter_line_1`..`5`).
- **Might Have Encountered / Missed:**
  - **Optional Hub 11 Side Quests:**
    - `w6_sq26` (*Jed's Echo* at `source_echo_workbench`): finding Jed's crooked chisel marks (`w6_sq26_jed_echo`) and tapping `jed echo` (`w6_sq26_hear_lesson`) plays `scene_w6_jed_echo_lesson` (5 steps, `CARD`, `music_jed_theme` on step 0, `stinger_motif_jed` on step 4), where Nova remembers Jed testing a solder joint (`"Power off. Then pull the lead"`) and turns to find the bench empty (`"It holds"`), followed by `w6_sq26_carry_legacy`.
    - `w6_sq27` (*The HR Record*): Zeke chooses whether to simply delete his own employee file or broadcast a mass revocation freeing colony workers (`w6_sq27_free_workers`).
    - `w6_sq28` (*Elara's Song*): isolating a clean recording of Elara's voice (`music_elaras_song`) from the command layer.
- **Unlocks Next:** `w6_mq29` (*The Spire of Thought*) stage `memory_ascent` in `source_memory_stair`.
- **Citations:** [events.json](../../app/src/main/assets/events.json) (`w6_mq28_anchor_*`, `w6_mq28_build_bridge`, `w6_mq28_final_banter`, `w6_mq28_reach_singularity`, `w6_sq26_hear_lesson`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w6_jed_echo_lesson`).

---

### `W06-03` — The Three Memory Stair Refusals & Spire Ascent
- **Location:** `source_memory_stair` (*Memory Stair*) → `source_spire_thought` (*Spire of Thought*, `hub_12_singularity`).
- **Actual Trigger / Prerequisites:**
  1. In `source_memory_stair`, complete all three interactive refusal dialogues in **any order** (each can be backed out of and reopened without granting premature credit):
     - **`Jed's door`** (`w6_mq29_refuse_jed_revision_scene` → `w6_mq29_refuse_jed_revision_line_1`..`4` → `w6_mq29_refuse_jed_revision` → sets `ms_w6_jed_revision_refused`): Two wrench taps sound behind the door; Jed's voice asks Nova to hold the light, and Vale offers a mine where the Cargo Lift brake never stripped. Nova admits *"I want to open it"* (`line_2`), then refuses: *"He told me to go. I miss him. Leave him as he was"* (`line_4`).
     - **`cargo pod wreckage`** (internal action key `Astra hull`, display alias `cargo pod wreckage` → `w6_mq29_refuse_astra_revision_scene` → `w6_mq29_refuse_astra_revision_line_1`..`4` → `w6_mq29_refuse_astra_revision` → sets `ms_w6_astra_revision_refused`): Vale offers a safe pod departure from World 1 with no crash into Sector 9. Orion realizes *"You never reach Sector 9. My pod stays closed,"* and Nova refuses to erase the crash that brought them to Orion.
     - **`Foundry catwalk`** (`w6_mq29_refuse_foundry_revision_scene` → `w6_mq29_refuse_foundry_revision_line_1`..`4` → `w6_mq29_refuse_foundry_revision` → sets `ms_w6_foundry_revision_refused`): Vale offers a Foundry with empty glass and no memory of Project Phantom. Gh0st refuses: *"If the glass is empty, nobody goes back for Elara. Keep the record"* (`line_4`).
  2. Climb the stair (`w6_mq29_climb_stair`), defeat the shadow wave (`w6_mq29_shadow_wave`) and Memory Guard (`w6_mq29_memory_guard`), and enter `source_center` (`w6_mq29_reach_center`) → completes `w6_mq29`, starts `w6_mq30` (*The Final Note*).
- **Guaranteed Player Knowledge:**
  - The crew rejects Vale's revision of their history not because suffering is good, but because erasing their losses would erase Jed's choice, Orion's rescue, and their responsibility to save Elara.
- **Might Have Encountered / Missed:**
  - All 6 permutation orders of the three refusals are supported.
  - Optional Hub 12 side quests `w6_sq29` (*The Aethel Grave* — Orion tuning the burial current of Aethel victims in `w6_sq29_tune_grave_line_1`..`3`) and `w6_sq30` (*The Final Scavenge* — recovering future Astra hull plating).
- **Unlocks Next:** `w6_mq30` (*The Final Note*) stage `soloist` in `source_center` (*The Center*).
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`w6_mq29_refuse_jed_revision_line_1`..`4`, `w6_mq29_refuse_astra_revision_line_1`..`4`, `w6_mq29_refuse_foundry_revision_line_1`..`4`), [rooms.json](../../app/src/main/assets/rooms.json) (`source_memory_stair`).

---

### `W06-04` — Final Confrontation with Vale, The Silence Rises (`scene_w6_silence_rises`) & The Choir Lock (`scene_w6_final_note` / `scene_w6_epilogue_credits`)
- **Location:** `source_center` (*The Center*) / `source_center_node_scale_03` (*Silence Pulpit*) → warps to `source_new_world` (*Morning Street*, `hub_12_singularity`).
- **Actual Trigger / Prerequisites:**
  1. In `source_center`, tap `Vale` (`w6_mq30_confront_vale_scene` → dialogue `vale_w6_final_confrontation` [music `music_vale_theme`] → `nova_w6_final_answer` → `vale_w6_final_reply` → `nova_w6_final_resolve` → `w6_mq30_confront_vale`).
  2. Defeat Phase 1 boss `ascended_vale` (`music_w6_boss_final`) → triggers `w6_mq30_defeat_vale`, which advances `w6_mq30` to stage `singularity` and plays `scene_w6_silence_rises` (3 steps, `illustrated`, 13.3s, `music_nova_theme` on step 2).
  3. After a playable save/recovery interval, confront Phase 2 boss `ascended_god` (`music_w6_boss_vale_p2`, victory `music_victory_boss` → `w6_mq30_defeat_god`, advances `w6_mq30` to stage `final_note`).
  4. Tap `shared tuning focus` in `source_center` → solve tuning puzzle `source_harmonic_focus` (*Harmonic Choir Lock*) → triggers `w6_mq30_tune_world`.
  5. `w6_mq30_tune_world` plays `scene_w6_final_note` (17 steps, `illustrated`, 93.7s, `music_w6_boss_final` → `stinger_motif_nova` on step 3 → `music_nova_theme` on step 12 → `stinger_motif_elara` on step 15), and its `on_complete` warps to `source_new_world` and immediately queues `scene_w6_epilogue_credits` (17 steps, `illustrated`, 93.1s, `music_epilogue` → `music_credits_ending`).
- **Guaranteed Player Knowledge:**
  - Vale makes his clearest argument (`vale_w6_final_confrontation`): *"Thorne charged for every voice she let into Harmony. I can reach every shift at once. A mine where no child goes hungry, no lift fails, and no one stays behind a closing door."*
  - Nova challenges him with the two wrench taps from Jed's bench (`nova_w6_final_answer`): *"At the stair, Jed tapped the bench twice before he spoke. What happens the first time he taps three times, or tells you your weld is crooked?"* Vale replies that in a calibrated world *"A weld does not fail here. No one needs to object"* (`vale_w6_final_reply`), proving his paradise erases human agency.
  - After Vale's human form cracks (`scene_w6_silence_rises`) and his second geometric form (`ascended_god`) falls, voices still call from the collapsing network asking for the door.
  - At the **Harmonic Choir Lock** (`scene_w6_final_note`), the ground lead fails; mirroring the World 2 Anchor Drill, **Zeke holds the bare ground bus by hand, Gh0st braces the force, Orion holds the counter-tone, and Nova opens her hand to release root access**—opening a regional corridor, dropping Vale's forced links, and **burning out her own Source connection forever**.
  - As the crew crosses back, **a hand reaches through the corridor and Gh0st catches it: `"I've got you, Elara"`** (`scene_w6_final_note[15]`).
  - In the epilogue (`scene_w6_epilogue_credits`), one regional corridor stands open while Dominion traffic continues beyond the sector. At Zeke's diner, Nova looks at a post-adventure photograph from their first trip back to Tideglass, admits she can no longer feel the Source and her ears still ring, has her rattling saucer steadied by Zeke's finger, recalls Jed handing her the lamp before asking if she was ready, and chooses to eat breakfast with her crew (while Elara is on her way to meet them at the receiver) before taking on the next repair.
- **Might Have Encountered / Missed:**
  - In postgame exploration at `source_new_world` (*Morning Street*), players can replay the epilogue (`w6_replay_epilogue`) or take the shuttle back to the Astra.
- **Unlocks Next:** Sets `ms_w6_mq30_complete`, `ms_game_complete`, `ms_credits_seen`; unlocks postgame exploration in `source_new_world` and `source_art_tune_world`.
- **Citations:** [dialogue.json](../../app/src/main/assets/dialogue.json) (`vale_w6_final_confrontation`..`nova_w6_final_resolve`), [events.json](../../app/src/main/assets/events.json) (`w6_mq30_confront_vale`, `w6_mq30_defeat_vale`, `w6_mq30_defeat_god`, `w6_mq30_tune_world`), [cinematics.json](../../app/src/main/assets/cinematics.json) (`scene_w6_silence_rises`, `scene_w6_final_note`, `scene_w6_epilogue_credits`).

---

## 7. Complete Side-Quest & Optional Branch Matrix (As Implemented)

| Checkpoint ID | Quest ID & Title | Hub / Primary Room(s) | Start Trigger / Prerequisites | Key Narrative Payoff & Unlocks |
|---|---|---|---|---|
| `W01-SQ01` | `w1_sq01` — *The Scavenger's Stash* | `hub_1_homestead` / `trade_scrapper`, `trade_stash` | Talk to Scrapper (`scrapper_w1_sq01_intro_1`) | Nova recovers dead miners' tags from a rebel cache; Scrapper refuses to profit off the dead's names. |
| `W01-SQ02` | `w1_sq02` — *System Flush* | `hub_1_homestead` / `medbay_main`, `medbay_vents` | Talk to Doc (`doc_w1_sq02_intro_1`) | Exposes Dominion's "acceptable losses" ventilation neglect poisoning miners; teaches ordered 3-step vent/fan/blockage puzzle. |
| `W01-SQ03` | `w1_sq03` — *The Heavy Lifter* | `hub_2_logistics` / `admin_lobby`, `admin_security`, `workshop_dock` | **Mandatory gate** on Boggs (`bogs_w1_mq03_review_4`) or early drill | Unlocks `nova_hydraulic_kick` Guard Break certification; includes 2 optional loader/rail tasks in `workshop_dock`. |
| `W01-SQ04` | `w1_sq04` — *Protocol Override* | `hub_2_logistics` / `server_hub` | Tap `terminal` (`w1_sq04_locate_terminal`) | Plays `scene_sq04_hack`; first optional mention of **"Chorus"**; player chooses between funding rebel network or personal credits. |
| `W01-SQ05` | `w1_sq05` — *The Lost Shift* | `hub_2_logistics` / `mine_shunt` | Enter `mine_shunt` (`w1_sq05_start_shunt`) | Plays `scene_sq05_datapad`; reveals Dominion sealed a prior shift alive after they broke into a pocket of the Architect hum. |
| `W02-SQ01` | `w2_sq01` — *Botanist* | `hub_3_sector9` / 5 flora rooms | Talk to Zeke (`zeke_w2_sq01_intro_1`) | Ecological scan of Sector 9's Source-mutated flora (`moss`, `weeds`, `ferns`, `spores`, `roots`). |
| `W02-SQ02` | `w2_sq02` — *Lost Patrol* | `hub_3_sector9` / wilds & ridge, `sector9_beach_cove` | Tap `distress signature alpha` (`w2_sq02_start_trace_alpha`) | Traces 3 fallen patrol beacons, repairs Leto's `thermal_cutter` (unlocking razor-vine shortcuts), and gives Scout Kaelen closure. |
| `W02-SQ03` | `w2_sq03` — *Tideglass Day* | `hub_3_sector9` / `sector9_beach_pools`, `beach_grotto` | Talk to Orion (`orion_w2_sq03_intro_1`) or visit beach | Quiet breather gathering tideglass and crafting `source_resin` with Orion. |
| `W02-SQ04` | `w2_sq04` — *Ancient Echoes* | `hub_4_facility` / temple wings, `hall_of_echoes`, `archive_tuning_bay` | Talk to Orion (`orion_w2_sq04_intro_1`) | Recovers west, east, and north resonance crystals to restore the Aethel mural chorus; Orion asks to listen to the voices a moment longer. |
| `W02-SQ05` | `w2_sq05` — *Stolen Tech* | `hub_4_facility` / `sector9_vents_gantry`..`tech_alcove` | Talk to Gh0st (`ghost_w2_sq05_intro_1`) | Early competence & trust beat with Gh0st bypassing Dominion security grids in the vents. |
| `W03-SQ11` | `w3_sq11` — *Neon Fix* | `hub_5_lower_city` / `spire_the_static` | Talk to Jax (`w3_sq11_jax_intro`) | Repairs The Static's flickering neon sign (`w2_biolum_matrix_tune`) to keep patrol pretext away from the bar. |
| `W03-SQ12` | `w3_sq12` — *Cold Case* | `hub_5_lower_city` / `spire_zekes_apartment`, security terminal | Talk to Gh0st (`w3_sq12_ghost_intro`) | Explicitly establishes before World 4 that **Elara is Gh0st's sister** and traces her scrubbed transfer to Sector 7 Project Phantom. |
| `W03-SQ13` | `w3_sq13` — *Night Market Run* | `hub_5_lower_city` / `spire_night_market` | Talk to Mika (`w3_sq13_mika_intro`) | Traces Upper City power theft and restores the Night Market lanterns. |
| `W03-SQ-CHLOE` | `sq_chloe_01` — *The Wayfarer's Signal* | `hub_5_lower_city` / hidden studio | Talk to Chris & Andrew | Restores an independent musical broadcast across the Lower City. |
| `W03-SQ14` | `w3_sq14` — *Corporate Espionage* | `hub_6_upper_city` / `spire_exec_lounge_bar` | Copy concierge key (`w3_sq14_copy_concierge_key`) | Steals encrypted executive ledger; choice between leaking publicly (`w3_sq14_leak_ledger`) or weaponizing privately (`w3_sq14_weaponize_ledger`). |
| `W03-SQ15` | `w3_sq15` — *Prototype Testing* | `hub_6_upper_city` / `spire_drone_test_alcove` | Talk to `lab_terminal` (`lab_terminal_w3_sq15_intro`) | Calibrates a recovered weapon prototype and scrubs telemetry. |
| `W03-SQ16` | `w3_sq16` — *Murder in the Skyline Salon* | `hub_6_upper_city` / `spire_exec_lounge_scale_01` | Inspect crime scene (`w3_sq16_start`) | 3-clue whodunit (`inspect_glass`, `inspect_terminal`, `inspect_rig`) + 2-stage deduction exposing **Director Sorrell** for poisoning Auditor Malick with Cyano-Coolant 9; Dr. Lyra files the audit under her own name. |
| `W04-SQ16` | `w4_sq16` — *Sabotage* | `hub_7_slag_pits` / 3 ammo crate rooms | Tap crate alpha (`w4_sq16_start`) | Destroys 3 staged Dominion munitions crates across the Slag Pits. |
| `W04-SQ17` | `w4_sq17` — *Lost Worker* | `hub_7_slag_pits` / lava tube maintenance | Trace ping (`w4_sq17_trace_ping`) | Rescues trapped engineer Talia by venting a safe route; acknowledged by Forge Hand Rylan. |
| `W04-SQ18` | `w4_sq18` — *The Scrap Heap* | `hub_7_slag_pits` / `foundry_waste_intake` | Stop intake (`w4_sq18_stop_intake`) | Salvages destroyed mech parts from the smelter intake. |
| `W04-SQ-CRUC` | `w4_sq_crucible` — *The Scrapper's Crucible* | `hub_7_slag_pits` / `foundry_crucible_arena` | Talk to Pitmaster Brutus (`brutus_dialogue_initial`) | 3-wave combat arena trial culminating in `crucible_champion`. |
| `W04-SQ19` | `w4_sq19` — *Quality Control* | `hub_8_assembly_line` / reject bay | Read rejection codes (`w4_sq19_read_rejection_codes`) | Moral choice resolving sentient defective synthetic units (`w4_sq19_release_units` vs `w4_sq19_defend_workers`). |
| `W04-SQ20` | `w4_sq20` — *Overclocked* | `hub_8_assembly_line` / `foundry_forge_control_alcove`, `foundry_forge_anvil` | Map hazards (`w4_sq20_map_hazards`) | Solves `w4_hazard_calibration` tuning puzzle and survives hazard waves. |
| `W05-SQ21` | `w5_sq21` — *Employee of the Month* | `hub_9_orbital_ring` / director archive | Find redactions (`w5_sq21_find_redactions`) | Exposes and publishes Mara Thorne's private Harmony monetization logs. |
| `W05-SQ22` | `w5_sq22` — *Solar Maintenance* | `hub_9_orbital_ring` / `orbital_solarium` | Talk to Maintenance Bot (`maintenance_bot_w5_sq22_intro`) | Realigns the Solarium mirror array to restore light to the habitat gardens. |
| `W05-SQ23` | `w5_sq23` — *Vacuum Seal* | `hub_9_orbital_ring` / `orbital_executive_dock`, bulkhead controls | Map pressure loss (`w5_sq23_map_pressure_loss`) | Interactive bulkhead-routing dialogue (`w5_bulkheads_first`) sealing the hull breaches caused by the Astra's forced docking. |
| `W05-SQ24` | `w5_sq24` — *Ghost in the Shell* | `hub_10_deep_ring` / `orbital_server_farm` | Trace purge (`w5_sq24_trace_purge`) | Recovers defensive guardian code Vale ordered purged. |
| `W05-SQ25` | `w5_sq25` — *Admin Privileges* | `hub_10_deep_ring` / server & armory | Find `sysadmin_keycard` (`w5_sq25_find_keycard`) | Uses a dead administrator's keycard to unlock the executive armory. |
| `W06-SQ26` | `w6_sq26` — *Jed's Echo* | `hub_11_event_horizon` / `source_echo_workbench` | Find tool marks (`w6_sq26_jed_echo`) | Plays `scene_w6_jed_echo_lesson`; Nova completes a remembered solder-check lesson at Jed's bench and carries his legacy forward. |
| `W06-SQ27` | `w6_sq27` — *The HR Record* | `hub_11_event_horizon` / memory records | Find backups (`w6_sq27_delete_record`) | Zeke deletes his personnel file or broadcasts a sector-wide worker revocation (`w6_sq27_free_workers`). |
| `W06-SQ28` | `w6_sq28` — *Elara's Song* | `hub_11_event_horizon` / signal archive | Isolate voice (`w6_sq28_elara_song`) | Strips Vale's command layer off a pure recording of Elara's voice (`music_elaras_song`). |
| `W06-SQ29` | `w6_sq29` — *The Aethel Grave* | `hub_12_singularity` / `source_memory_threshold`, grave | Trace names (`w6_sq29_aethel_grave`) | Orion speaks a quiet remembrance over the burial current of Aethel victims (`w6_sq29_tune_grave_line_1`..`3`). |
| `W06-SQ30` | `w6_sq30` — *The Final Scavenge* | `hub_12_singularity` / fracture wreckage | Identify timeline (`w6_sq30_final_scavenge`) | Recovers and fits a piece of Astra hull plating from an alternate unchosen timeline. |

---

## 8. Proposed Changes to Progression & Triggers (Kept Separate from Current-State Map)

The following targeted progression and trigger adjustments are recommended based on the audit in [starborn-story-audit.md](starborn-story-audit.md):

1. **Enable Unavailable-Action Feedback in World 2 (`W02-01`..`W02-04`):**
   - In [ExplorationController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt#L6141-L6156), include `sector9_` rooms (or `_uiState.value.currentWorld?.id == "world_2"`) in `needsUnavailableFeedback` after auditing the 7 foraging/gathering `condition_unmet_message` strings that read as success text (`power conduits`, `tideglass growth`, `drifting spores`, `western/eastern/northern resonance crystal`), so that tapping `ring console` (`sector9_stasis_ring_array`), `acoustic lock` (`sector9_source_gate`), `central relief` (`sector9_hall_of_echoes`), and `resonator columns` (`sector9_archive_tuning_bay`) before prerequisites displays their authored 4-part puzzle hints instead of producing a silent tap.
2. **Add Post-Scene `npc_presence` Guards in World 5 (`W05-02`..`W05-04`):**
   - In [rooms.json](../../app/src/main/assets/rooms.json), move `thorne` (`deep_mainframe_nave`), `elara` (`deep_anchor_chamber`), and `vale` (`deep_throne_room`) from static `npcs` arrays into `npc_presence` with `forbidden_milestones` (`["ms_w5_mq25_soloist"]` / `["ms_w5_mq25_complete"]`) so Thorne does not remain standing in the Mainframe Nave after her execution in the Throne Room, and Elara/Vale do not remain in empty rooms after crossing into the Tear.
3. **Wire or Retire Orphaned Dialogue & Task Triggers (`W03-02`, `W03-SQ16`, `W05-01`, `W05-02`):**
   - In `vance_confession_admission` ([dialogue.json](../../app/src/main/assets/dialogue.json)), add `set_quest_task_done:w3_sq16:expose_vance` before `complete_quest:w3_sq16` so the final stage task of *Murder in the Skyline Salon* is explicitly marked completed in `questTasksCompleted`.
   - In `orbital_airlock_gallery` ([rooms.json](../../app/src/main/assets/rooms.json)), wire `vale_w5_compliance_greeting` on entry or on the `compliance greeting` action during `w5_mq22`, or retire the unused dialogue entry.
   - Surface `thorne_w5_mq23_core` automatically at the start of `w5_mq23` (or as the post-`w5_mq22_find_thorne` greeting before leaving `deep_mainframe_nave`) so players do not miss Thorne's firewall briefing.
4. **Remove World 1 Spoiler from `ghost_signal_cell` Item Description (`W01-07`):**
   - Update `ghost_signal_cell` in [items.json](../../app/src/main/assets/items.json) so it describes the antique brass Chime and its Sector 9 launch-telemetry pins without naming **"Orion"** before the player meets him in `W02-02`.
