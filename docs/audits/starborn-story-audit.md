# Starborn — Six-World Story, Player-Facing Text & Cinematics Audit

> **Scope:** Comprehensive, read-only audit of *Starborn*’s narrative, dialogue, quest/journal guidance, interactions, inspect text, item descriptions, tutorials, and cinematic catalog across all six worlds (`world_1` through `world_6` plus `hub_astra`), verified directly against the current repository implementation (`HEAD` at commit `a8b12f4a`).
>
> **Companion Document:** See [starborn-checkpoints.md](starborn-checkpoints.md) for the complete current-state progression and checkpoint map (`W00-01` through `W06-04` and `W01-SQ01` through `W06-SQ30`).

---

## 0. Reconciliation With Previous Story & Dialogue Audits

Prior audit and revision documents in the repository (`docs/playtest/dialogue_quest_audit/01..10`, `docs/design/story_presentation/`, and `docs/playtest/npc_portrait_audit/`) were cross-checked against the live assets and Kotlin runtime:

### A. Confirmed Fixed Since the Nov 13–17 Audits
1. **World 1 Silent Taps & Unretired Actions (`01`–`03`):**
   - [ExplorationController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt#L6145-L6156) now checks `val worldOne = _uiState.value.currentWorld?.id == "world_1"` and `authoredUnavailable`, ensuring World 1 actions display a narration prompt instead of failing silently.
   - `pit_showers` (`drainage grate`), `workshop_dock` (`loader`, `cargo rail`), and `workshop_floor` (`mining cutter`, `Flux Liner`, `live cutter test`) now have accurate pre-prerequisite `unavailable_message` strings and `inspect_after_completion: true`.
   - `launch_bay` post-Warden `description_variants` now preserve inline `[action:cargo rails]` and `[action:escape pod]` references.
   - `w1_sq03` (*The Heavy Lifter*) moved its optional `start_loader` and `move_cargo` tasks into Stage 2 (`guard_break_training`) so they remain visible in the Journal rather than being auto-skipped on quest start.
2. **Duplicate Exit Choices & Linear Prompts (`01`–`03`):**
   - [DialogueService.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/domain/dialogue/DialogueService.kt#L85-L91) now supports `option.exit = true`. All 10 duplicate-exit nodes (`zeke_w2_sq01_intro_1`, `orion_w2_sq04_intro_1`, `w3_sq12_ghost_intro`, `w2_departure_entry`, `brutus_dialogue_*`, `w5_bulkheads_*`) and the 2 linear single-option nodes (`andrew_favor_acknowledgment`, `w3_mq12_jax_intro`) are fixed.
3. **Emotional Continuity, Antagonist Setup & Memory Stair (`07`–`10`):**
   - **Jed's wrench taps & lamp:** `jed_w1_mq01_workshop_brief_1` now shows Jed tapping the bench twice with his wrench and passing Nova the work lamp on the mandatory World 1 route, earning the World 6 Memory Stair (`w6_mq29_refuse_jed_revision_line_1`), Center (`nova_w6_final_answer`), and diner (`scene_w6_epilogue_credits[11]`) callbacks.
   - **Memory Stair cargo-pod correction:** `source_memory_stair` and `w6_mq29_refuse_astra_revision_line_1..4` now display **`cargo pod wreckage`** (preserving the internal `Astra hull` key for save compatibility), resolving the confusion between the World 1 cargo-pod crash and the World 2 Astra flight.
   - **Diner & Bridge continuity:** The premature "Colony's first real morning" was removed from `w6_mq28_build_bridge`; the diner photograph is explicitly framed as taken *after* the adventure; and Gh0st's diner callback (`"He was falling. I had a free hand"`) matches `scene_w2_astra_departure[5]`.
   - **Early Thorne presence & terms:** Foreman Boggs (`bogs_w1_mq03_review_2`) names Thorne's Sector 4 hazard-pay freeze; Jax (`w3_mq12_talk_jax`) names her private Executive Lift privilege; and in `deep_mainframe_nave` (`thorne_w5_mq22_intro_beat_2`, `nova_w5_thorne_terms`), Gh0st demands the occupied-seat list and Nova refuses to restore Thorne's command before accepting her firewall route.
   - **Elara's personal memory:** The mandatory Project Phantom / Cooling Springs sequence (`gh0st_w4_springs_elara`) establishes Elara asking from the doorway whether Gh0st wanted the hall light left on and waiting for his answer—which pays off directly at their reunion (`gh0st_w5_elara_lamp` → `elara_w5_lamp_answer`).
   - **Rest banter rotation:** [RestBanter.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/domain/narrative/RestBanter.kt) now rotates through all 27 party- and milestone-gated rest entries and authored location descriptions instead of locking onto the first party member.
4. **Unified Cast Portraits, OST Wiring & Cinematic Caption Alignment (`8965f879`..`a8b12f4a`):**
   - All 47 NPC identities have unified portraits; `w4_coup_display_v2.webp`, `w5_stasis_elara_v2.webp`, and `w5_tear_hands_v2.webp` match the revised character designs.
   - All 65 active Suno OST tracks (including character themes and streaming motif stingers) are wired, and cinematic captions/popups in [ExplorationScreen.kt](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L8373-L8390) now align consistently to `Alignment.BottomCenter` so they no longer block the center of the artwork.

### B. What Remains Unfixed or Newly Surfaced in Live Code & Data
Detailed in Section 1 below:
1. **World 2 (`sector9_`) Silent-Tap Logic Inversion** in `ExplorationController.kt:6141-6156` (suppresses authored 4-part puzzle hints on `ring console`, `acoustic lock`, `central relief`, and `resonator columns`).
2. **World 1 Item Spoiler on `ghost_signal_cell`** in `items.json` (names `"Orion's antique brass Chime"` in World 1 before Orion is introduced in World 2).
3. **World 5 Post-Scene NPC Presence Bugs** in `rooms.json` (`thorne` remains standing in `deep_mainframe_nave` even after Vale executes her in `deep_throne_room`; `elara` and `vale` remain in `deep_anchor_chamber` and `deep_throne_room` after crossing the Tear).
4. **Cold Open Portrait vs. Mystery Tension (`intro_prologue[3,6]`)** (`speaker: "????"` displays `orion_portrait.png`, visually giving away Orion's identity before `W02-02`).
5. **Vale's Ideological Gap Between World 3 (`W03-04`) and World 5 (`W05-04`)** (Vale's first override in World 3 and coup in World 4 show tactical seizure of guns/command, but his philosophical motive—eliminating variance and pain—is not heard until `W05-04`).
6. **Duplicate Exposition Between Event `show_message` Popups and Cinematics** (`evt_mine_generator_restore`, `w1_mq05_use_nav_console`, `w1_sq04_compile_spoof`).
7. **Orphaned / Bypassed Dialogue, Tasks & Cinematics** (`vale_w5_compliance_greeting`, `thorne_w5_mq23_core`, `w3_sq12_ghost_terminal`, `w3_mq12_workstation_dialogue`, task `w3_sq16:expose_vance`, and 6 unreferenced `CARD` cinematics including `scene_jed_tinkering_prompt` and `scene_jed_tinkering_wrapup` where `stinger_motif_jed` is attached).
8. **Postgame Narrative vs. Gameplay Contradiction (`W06-04`)** (`w6_mq30_tune_world` unlocks combat skill `source_art_tune_world` at the exact moment `scene_w6_final_note[7]` states Nova's Source connection permanently burned out).

---

## 1. Highest-Impact Findings First

| ID | Classification | Checkpoint | Summary | Action Tier |
|---|---|---|---|---|
| **F-01** | **Confirmed Issue** | `W02-02`, `W02-04`, `W02-SQ04` | **World 2 (`sector9_`) silent-tap inversion:** `ExplorationController.kt` suppresses `condition_unmet_message` feedback in `sector9_` rooms whenever `status != "Objective completed."`—silencing the authored 4-part puzzle hints on `ring console`, `acoustic lock`, `central relief`, and `resonator columns`. | **Fix Before Building** |
| **F-02** | **Confirmed Issue** | `W01-07` | **Premature identity spoiler in `ghost_signal_cell` item description:** Awarded in World 1 at the Cargo Lift, its description reads *"Orion's antique brass Chime. Launched from Sector 9 twenty years ago..."* before Nova or the player knows who Orion is (`W02-02`). | **Fix Before Building** |
| **F-03** | **Confirmed Issue** | `W05-02`, `W05-03`, `W05-04` | **Ghost NPCs after death/departure in World 5:** `deep_mainframe_nave` hardcodes `npcs: ["thorne"]`, `deep_anchor_chamber` hardcodes `npcs: ["elara"]`, and `deep_throne_room` hardcodes `npcs: ["vale"]` with no `forbidden_milestones`. Revisiting `deep_mainframe_nave` after Thorne's execution shows her still standing there. | **Fix Before Building** |
| **F-04** | **Editorial Judgment** | `W03-04` → `W04-04` | **Vale's motive arrives two worlds after his first override:** In `W03-04` (`vale_w3_mq15_override`), Vale usurps Thorne one line after his name is first mentioned, and in `W04-04` (`scene_w4_outer_shield`) he seizes the Ring, but neither scene hints *why* he wants the anomaly until `W05-04` (*"Variance contains pain. One voice"*). | **Fix Before Building** |
| **F-05** | **Confirmed Issue** | `W01-06`, `W01-08`, `W01-SQ04` | **Duplicate exposition between event `show_message` banners and cinematics:** `evt_mine_generator_restore` pops two text banners before `scene_mine_restore`; `w1_mq05_use_nav_console` pops a post-crash summary after `scene_launch_crash`; `w1_sq04_compile_spoof` repeats `scene_sq04_hack[2]`. | **Fix Before Building** |
| **F-06** | **Confirmed Issue** | `W05-01`, `W05-02` | **Orphaned & easily missed World 5 antagonist lines:** `vale_w5_compliance_greeting` is never triggered in `orbital_airlock_gallery`, and `thorne_w5_mq23_core` (which also carries `music_thorne_theme`) only plays if the player manually taps Thorne a second time before leaving `deep_mainframe_nave`. | **Fix Before Building** |
| **F-07** | **Editorial / Visual** | `W00-01` | **Cold open `"????"` speaker uses Orion's recognizable portrait:** `intro_prologue[3,6]` hides Orion's name as `"????"` while displaying `images/characters/orion_portrait.png`, undermining the mystery of who sent the Chime when Orion appears in `W02-02`. | **Refine During Production** |
| **F-08** | **Confirmed Issue** | `W01-03`, `W03-02`, `W03-SQ12`, `W03-SQ16` | **Orphaned cinematics, dialogue & quest task IDs:** `scene_jed_tinkering_prompt` and `scene_jed_tinkering_wrapup` (which currently holds `stinger_motif_jed` on step 0) have no triggers; `w3_sq12_ghost_terminal` and `w3_mq12_workstation_dialogue` are unreferenced; and `w3_sq16:expose_vance` is never marked done before `complete_quest:w3_sq16`. | **Fix Before Building** |
| **F-09** | **Editorial / Systems** | `W06-04` | **Postgame Source burnout vs. `source_art_tune_world` unlock:** `w6_mq30_tune_world` grants combat skill `source_art_tune_world` right as `scene_w6_final_note[7]` and `scene_w6_epilogue_credits[4]` declare Nova's Source connection permanently burned out. | **Fix Before Building** |
| **F-10** | **Playtest Question** | `W05-03`, `W05-04`, `W06-04` | **Bottom-aligned caption overlay vs. low-frame focal points (`w5_tear_hands_v2`, `w6_ending_diner_v1`) & ending credits cards:** Verify on-device whether the bottom-aligned dialogue card covers the reaching hands in `scene_w5_cross_the_tear[3]` or the rattling saucer in `scene_w6_epilogue_credits[10]`, and revamp the two no-art credits cards (`scene_w6_epilogue_credits[15..16]`). | **Verify in Playtest / Refine** |

---

### Detailed High-Impact Finding Cards

#### `F-01` — World 2 (`sector9_`) Silent-Tap Logic Inversion Suppresses Authored Puzzle Hints
- **Classification:** Confirmed Issue
- **Checkpoint:** `W02-02` (`w2_mq03`), `W02-04` (`w2_mq05`), `W02-SQ04` (`w2_sq04`)
- **Route / Trigger:** Tapping any `action_event` interaction in a `sector9_*` room when its `events.json` conditions are not yet met (or after completion on actions without `inspect_after_completion: true`).
- **Source Evidence:**
  - [ExplorationController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt#L6141-L6156):
    ```kotlin
    val needsUnavailableFeedback = roomId.startsWith("spire_") ||
        roomId.startsWith("foundry_") || roomId.startsWith("orbital_") ||
        roomId.startsWith("deep_") || roomId.startsWith("source_") ||
        eventId == "w2_sq02_trace_cliffs" || status == "Objective completed."
    ```
  - Because `sector9_` rooms are omitted from `needsUnavailableFeedback` and none of the 47 `sector9_` `action_event` entries in [rooms.json](../../app/src/main/assets/rooms.json) defines `unavailable_message`, the `if` block only runs when `status == "Objective completed."`
  - Whenever a `sector9_` action has a **real, helpful prerequisite hint** in `condition_unmet_message` (`status != "Objective completed."`), `needsUnavailableFeedback` evaluates to `false`, skipping `showInspection(message)` and falling through to `postStatus(status)` (which is not rendered on the Exploration Screen).
  - Affected puzzle gates in [rooms.json](../../app/src/main/assets/rooms.json):
    - `sector9_stasis_ring_array` → `ring console` (`w2_mq03_align_complete`): *"The rings refuse to move until all four alignment frequencies are in phase."* (Silent tap before aligning all 4 rings.)
    - `sector9_source_gate` → `acoustic lock` (`w2_mq05_bypass_gate`): *"Prepare all four feeds first: brace the horn, ground the cup, read root pressure, and isolate the breakers."* (Silent tap before all 4 feeds are complete.)
    - `sector9_hall_of_echoes` → `central relief` (`w2_sq04_complete`): *"The central relief hums out of tune. It needs the west, east, and north crystal voices seated before the mural chord can hold."* (Silent tap before collecting the 3 crystals.)
    - `sector9_archive_tuning_bay` → `resonator columns` (`w2_sq04_complete`): *"The columns answer with three missing tones. Seat the west, east, and north crystal voices before asking them to carry the mural chord."* (Silent tap.)
- **Player Impact:** In World 2's three largest multi-room puzzles (Stasis Ring Array, Source Gate, and Ancient Echoes), tapping the central lock before gathering its 3–4 prerequisites plays a sound effect and shows **nothing on screen**, hiding the exact clue that tells the player what to do.
- **Smallest Useful Fix:**
  1. Rewrite the 7 `sector9_` `condition_unmet_message` strings in [rooms.json](../../app/src/main/assets/rooms.json) that currently read as success/flavor text (`power conduits` in `sector9_landing_drop`, `tideglass growth` in `sector9_beach_pools`, `drifting spores` in `sector9_canopy`, `Face the Beast` in `sector9_canopy_ridge`, and the 3 resonance crystals in `sector9_temple_left_wing`, `sector9_temple_right_wing`, `sector9_hall_acoustics_lab`) so they describe the locked/pre-quest state.
  2. Add `roomId.startsWith("sector9_")` to `needsUnavailableFeedback` in [ExplorationController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt#L6141).

#### `F-02` — Premature Identity Spoiler in `ghost_signal_cell` Item Description
- **Classification:** Confirmed Issue
- **Checkpoint:** `W01-07` (`w1_mq04_complete`) through `W02-02` (`w2_mq03_talk_to_orion`)
- **Route / Trigger:** Mandatory main-quest reward at Cargo Lift (`w1_mq04_complete` grants `ghost_signal_cell`), viewable immediately in Menu → Bag → Key Items.
- **Source Evidence:** [items.json](../../app/src/main/assets/items.json) (`id: "ghost_signal_cell"`):
  > `"description": "Orion's antique brass Chime. Launched from Sector 9 twenty years ago, its etched acoustic pins authenticate compatible starship launch telemetry and navigation arrays."`
- **Player Impact:** Spoils that the anonymous `"????"` operator from the cold open is named **Orion** and that the Chime belongs to him a full chapter before Nova opens the stasis pod in `W02-02`.
- **Smallest Useful Fix:** Change `ghost_signal_cell`'s description in [items.json](../../app/src/main/assets/items.json) to omit Orion's name (e.g., *"An antique brass Chime kept by Jed. Launched from Sector 9 twenty years ago, its etched acoustic pins authenticate compatible starship launch telemetry and navigation arrays."*).

#### `F-03` — World 5 Story NPCs Remain in Rooms After Execution or Departure
- **Classification:** Confirmed Issue
- **Checkpoint:** `W05-02` (`deep_mainframe_nave`), `W05-03` (`deep_anchor_chamber`), `W05-04` (`deep_throne_room`)
- **Route / Trigger:** Backtracking or revisiting `deep_mainframe_nave`, `deep_anchor_chamber`, or `deep_throne_room` after completing their respective story scenes.
- **Source Evidence:** [rooms.json](../../app/src/main/assets/rooms.json):
  - `deep_mainframe_nave`: `"npcs": ["thorne"]`, `"npc_presence": null`
  - `deep_anchor_chamber`: `"npcs": ["elara"]`, `"npc_presence": null`
  - `deep_throne_room`: `"npcs": ["vale"]`, `"npc_presence": null`
- **Player Impact:** After Vale executes Thorne in the Throne Room (`w5_mq25_soloist_scene`), a player who walks back to `deep_mainframe_nave` finds **Mara Thorne still standing at the terminal** (falling through to *"Thorne has nothing more to say right now"*). Similarly, after Vale and Elara's cable are pulled into the Tear, both remain listed in their previous rooms.
- **Smallest Useful Fix:** Move `thorne` (`deep_mainframe_nave`), `elara` (`deep_anchor_chamber`), and `vale` (`deep_throne_room`) from static `npcs` into `npc_presence` in [rooms.json](../../app/src/main/assets/rooms.json) with `forbidden_milestones` (`["ms_w5_mq25_soloist"]` for Thorne and Vale; `["ms_w5_mq25_complete"]` for Elara).

#### `F-04` — Vale's Philosophical Motive Arrives Two Worlds After His First Override
- **Classification:** Editorial Judgment
- **Checkpoint:** `W03-04` (`w3_mq14_take_lens`) and `W04-04` (`w4_mq20_escape_launch`)
- **Route / Trigger:** Mandatory main-quest dialogue `thorne_w3_mq14_trap_intro` → `vale_w3_mq15_override` (`W03-04`) and `scene_w4_outer_shield` (`W04-04`).
- **Source Evidence:**
  - In `W03-04` ([dialogue.json](../../app/src/main/assets/dialogue.json)), Thorne names Vale for the first time in `thorne_w3_mq14_trap_intro` (*"I've opened an audit and given Vale recovery access"*). In the very next line (`vale_w3_mq15_override`), Vale says: *"Recovery access accepted. Seal the roof. Authorize lethal force. The anomaly won't leave this sector again."*
  - In `W04-04` ([cinematics.json](../../app/src/main/assets/cinematics.json), `scene_w4_outer_shield[3..6]`), Vale strips Thorne's credentials and locks down the Ring, still sounding like a standard military/security usurper.
  - Only in `W05-02` (`thorne_w5_mq22_intro_beat_1`) and `W05-04` (`vale_w5_mq25_soloist`: *"A choir still contains variance. Variance contains pain. One voice. A solo"*) does the player learn that Vale's goal is not military control, but erasing human variance and suffering by collapsing Harmony into a single mind.
- **Player Impact:** Vale's World 3 override and World 4 coup read initially as generic authoritarian escalation rather than the ideological split ("monopolizing Harmony for profit" vs. "rewriting reality to eliminate variance/pain") that drives Worlds 5 and 6.
- **Smallest Useful Fix:** Add one concise clause to `vale_w3_mq15_override` (and/or `vale_w4_compliance_bulletin` in `foundry_obsidian_overlook`) that exposes his "zero-variance / calibration" worldview early—for example, in `vale_w3_mq15_override`: *"Recovery access accepted. Seal the roof. Authorize lethal force. You leased Harmony like a product, Director; I am ending the variance."*

#### `F-05` — Duplicate Exposition Between Event `show_message` Popups and Cinematics
- **Classification:** Confirmed Issue
- **Checkpoint:** `W01-06` (`evt_mine_generator_restore`), `W01-08` (`w1_mq05_use_nav_console`), `W01-SQ04` (`w1_sq04_compile_spoof`)
- **Route / Trigger:** Triggering `mine_generator_restore`, `use_nav_console`, or `w1_sq04_compile_spoof`.
- **Source Evidence:** [events.json](../../app/src/main/assets/events.json):
  - `evt_mine_generator_restore` displays two `show_message` popups (*"The generator hums back to life, unlocking the northern gate."* and *"Some vents open deeper in the mines."*) and immediately plays `scene_mine_restore`, whose two steps narrate the exact same generator catching and locks releasing down the mine.
  - `w1_mq05_use_nav_console` plays the 7-step illustrated `scene_launch_crash` (ending at `sector9_crash_site` with Zeke bracing in the dark) and then fires an `on_complete` `show_message`: *"The pod is down in Sector 9. Zeke is injured, and the wreck still has live circuits"*—which duplicates both the cinematic and `sector9_crash_site`'s room description.
  - `w1_sq04_compile_spoof` plays `scene_sq04_hack` (whose step 2 states: *"ACCESS KEY WRITTEN TO REMOVABLE BOARD... The recovered key clears from the terminal"*) and then shows a popup: *"The access key copies to the cracked circuit board, then erases itself from the terminal."*
- **Player Impact:** Double-tapping through a prose banner that repeats word-for-word what a cinematic card just enacted flattens pacing right at dramatic transitions.
- **Smallest Useful Fix:** Remove the redundant `show_message` actions from `evt_mine_generator_restore`, `w1_mq05_use_nav_console` (`on_complete`), and `w1_sq04_compile_spoof` in [events.json](../../app/src/main/assets/events.json).

#### `F-06` — Orphaned & Easily Missed World 5 Antagonist Lines (`vale_w5_compliance_greeting`, `thorne_w5_mq23_core`)
- **Classification:** Confirmed Issue
- **Checkpoint:** `W05-01` (`orbital_airlock_gallery`) and `W05-02` (`deep_mainframe_nave`)
- **Route / Trigger:** Entering `orbital_airlock_gallery` during `w5_mq22`; completing `w5_mq22_find_thorne` in `deep_mainframe_nave`.
- **Source Evidence:**
  - `vale_w5_compliance_greeting` ([dialogue.json](../../app/src/main/assets/dialogue.json), condition `quest_active:w5_mq22`) has no trigger in `events.json` or `rooms.json`; the `compliance greeting` action in `orbital_airlock_gallery` only shows a static `condition_unmet_message`.
  - `thorne_w5_mq23_core` ([dialogue.json](../../app/src/main/assets/dialogue.json), condition `quest_active:w5_mq23,quest_task_not_done:w5_mq23:navigate_maze`, music `music_thorne_theme`) only plays if the player manually taps Thorne a second time in `deep_mainframe_nave` after `w5_mq22_find_thorne` ends and before walking into the maze.
- **Player Impact:** Players miss Vale's chilling welcome to the Ring and Thorne's explanation of how Vale rewrote the three firewall locks (along with one of the three placements of `music_thorne_theme`).
- **Smallest Useful Fix:** Wire `compliance greeting` in `orbital_airlock_gallery` to start dialogue `vale_w5_compliance_greeting` (or trigger it on room entry), and chain `thorne_w5_mq23_core` onto the end of `thorne_w5_route_terms` (or auto-trigger it when `w5_mq23` starts).

#### `F-08` — Orphaned Cinematics (`stinger_motif_jed` on `scene_jed_tinkering_wrapup`), Dialogue & Task IDs
- **Classification:** Confirmed Issue
- **Checkpoint:** `W01-03`, `W03-02`, `W03-SQ12`, `W03-SQ16`
- **Source Evidence:**
  - **6 Orphaned `CARD` Cinematics in [cinematics.json](../../app/src/main/assets/cinematics.json):** `scene_jed_tinkering_prompt`, `scene_jed_tinkering_wrapup`, `scene_complete_gather_broken_gear`, `scene_return_display`, `scene_return_grinder`, and `scene_return_projector` have zero references in `events.json`, `dialogue.json`, or Kotlin code. Notably, `stinger_motif_jed` is currently attached to `scene_jed_tinkering_wrapup[0]` (unreachable) as well as `scene_w6_jed_echo_lesson[4]` (reachable in optional quest `w6_sq26`).
  - **2 Orphaned World 3 Dialogue Nodes in [dialogue.json](../../app/src/main/assets/dialogue.json):** `w3_sq12_ghost_terminal` and `w3_mq12_workstation_dialogue` are superseded by event `show_message` actions in `w3_sq12_access_terminal` and `w3_mq12_hack_blueprints`.
  - **1 Uncredited Quest Task in [quests.json](../../app/src/main/assets/quests.json):** Task `expose_vance` in `w3_sq16` (*Murder in the Skyline Salon*, Stage 2 `confront_killer`) is never marked done via `set_quest_task_done:w3_sq16:expose_vance` when Director Sorrell confesses in `vance_confession_admission` (`complete_quest:w3_sq16` completes the quest without adding `expose_vance` to `questTasksCompleted["w3_sq16"]`).
- **Player Impact:** `stinger_motif_jed` is never heard on the mandatory main path (only in optional World 6 side quest `w6_sq26`); `w3_sq16` leaves its final stage task unchecked in raw task state.
- **Smallest Useful Fix:**
  - Move or mirror `stinger_motif_jed` onto a mandatory main-route beat (such as `scene_w1_lift_separation[0]` or `scene_cutter_surge[5]`).
  - Add `set_quest_task_done:w3_sq16:expose_vance` to `vance_confession_admission.trigger` in [dialogue.json](../../app/src/main/assets/dialogue.json).

#### `F-09` — Postgame Source Burnout Contradicts Unlocking `source_art_tune_world`
- **Classification:** Editorial / Systems Issue
- **Checkpoint:** `W06-04` (`w6_mq30_tune_world`)
- **Route / Trigger:** Solving `source_harmonic_focus` at the end of `w6_mq30`.
- **Source Evidence:**
  - [events.json](../../app/src/main/assets/events.json) (`w6_mq30_tune_world`) executes `{"type": "unlock_skill", "skill_id": "source_art_tune_world"}` immediately before playing `scene_w6_final_note`.
  - `scene_w6_final_note[7]` states: *"Her Source connection burns out as the network closes."*
  - `scene_w6_epilogue_credits[4]` states: *"I can't feel the Source anymore. My ears are still ringing."*
- **Player Impact:** When the player enters postgame exploration in `source_new_world` (*Morning Street*) and opens the Party/Abilities menu or enters a simulation/postgame fight, Nova has a brand-new pinnacle Source Art (`Tune World`) despite the story's central sacrifice being that her Source connection burned out forever.
- **Smallest Useful Fix:** Either frame `source_art_tune_world` in [skills.json](../../app/src/main/assets/skills.json) as a **Choir / Crew Relic Resonance** channeled through Orion, Gh0st, and Zeke's linked instruments rather than Nova's personal Source nerve, or remove the combat skill unlock if postgame combat is treated as pre-finale simulation.

---

## 2. Findings by World (In Experienced Order)

### World 1 — Mining Colony (`world_1`: `hub_1_homestead`, `hub_2_logistics`)

- **Character Voice & Knowledge:**
  - **Strong foundation:** Nova's mechanical fluency, Jed's practical care (feeding her, holding the housing, tapping the bench twice with his wrench and passing the lamp in `jed_w1_mq01_workshop_brief_1`), and Zeke's nervous procedural defiance at the booth (`zeke_w1_mq02_override_1..7`) are crisp, distinct, and well-earned.
  - **Supporting cast:** Matriarch Pam, Scrapper (`w1_sq01`), Doc (`w1_sq02`), and Foreman Boggs (`bogs_w1_mq03_review_1..4`) each speak from concrete local stakes.
- **Reveals & Spoilers:**
  - **Confirmed Spoiler (`F-02`):** `ghost_signal_cell` in [items.json](../../app/src/main/assets/items.json) names `"Orion's antique brass Chime"` in World 1 (`W01-07`) before the player learns Orion's identity in `W02-02`.
  - **Optional Early "Chorus" Mention (`W01-SQ04`):** In `scene_sq04_hack[1]`, the scrubbed rebel terminal displays `"one name: Chorus"`. This works well as an intriguing mystery breadcrumb as long as it remains unexplained until World 2/3.
- **Guidance & Discovery:**
  - World 1 action feedback and post-completion retirement (`inspect_after_completion: true`) are now clean across `pit_nova_bunk`, `pit_showers`, `workshop_yard`, `workshop_floor`, `workshop_dock`, `medbay_vents`, and `server_hub`.
  - **Mid-Scene Exit Nuance (`zeke_w1_mq02_override_choice`):** At Zeke's liability spoof prompt, `DialogueSession.choices()` still appends `"Let's leave it there for now."` If tapped, re-talking to Zeke replays the 6-line intro (`zeke_w1_mq02_override_1..6`). Setting a checkpoint milestone on line 5 or marking the choice node non-exiting would avoid repeating the panic buildup.
- **Lore & Continuity:**
  - The frequency progression is consistent and specific: factory cutter governor at **61 kHz**, Nova's bypass at **84 kHz**, and the Tuning Fork counter-tune at **87 kHz** (`scene_relic_sync[0]`).
  - The sacrificial ground strip on `nova_flux_liner` is established in `w1_mq01_patch_flux_liner` and pays off physically when it burns open during `scene_relic_sync[2]`.
- **Conflict & Emotional Pacing:**
  - Pacing builds naturally from quota pressure (`w1_mq01`) → bureaucratic disposal (`w1_mq02`) → deep-mine eerie quiet (`w1_mq03`) → lockdown urgency and Jed's sacrifice (`w1_mq04` / `scene_w1_lift_separation`) → Warden fight and pod crash (`w1_mq05` / `scene_launch_crash`).
  - Removing the duplicate `show_message` popups on `evt_mine_generator_restore`, `w1_sq04_compile_spoof`, and `w1_mq05_use_nav_console` (`F-05`) will tighten the transitions into and out of World 1's cinematics.

---

### World 2 — The Wilds & Sector 9 Facility (`world_2`: `hub_3_sector9`, `hub_4_facility`)

- **Character Voice & Knowledge:**
  - Orion's awakening (`orion_w2_awakening_1..9`) is grounded in sensory detail (the ceiling he painted, the roots through the masonry) before he explains the Bridge.
  - Gh0st's recruitment (`ghost_confrontation_1..6` → `scene_anchor_drill`) earns his turn by emphasizing that the disposal order is still in his head when he chooses to lower his rifle.
- **Reveals & Spoilers:**
  - If `F-02` (`ghost_signal_cell` description) and `F-07` (`intro_prologue` Orion portrait) are addressed, Orion's awakening in `W02-02` lands as a genuine reveal connecting the cold open's `"????"` operator to the present crew.
- **Guidance & Discovery:**
  - **Highest-Priority World 2 Bug (`F-01`):** Because `sector9_` is excluded from `needsUnavailableFeedback` in [ExplorationController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt#L6141-L6156), tapping `ring console` (`sector9_stasis_ring_array`), `acoustic lock` (`sector9_source_gate`), `central relief` (`sector9_hall_of_echoes`), or `resonator columns` (`sector9_archive_tuning_bay`) before completing their 3–4 sub-tasks produces a **silent tap** instead of showing their authored multi-part instructions.
  - **Unretired World 2 Actions:** Unlike World 1, most one-shot `sector9_` actions (`navigation console` in `sector9_crash_site`, `emergency medkit` in `sector9_pod_interior`, `stone lock` in `sector9_temple_gate`, `murals` in `sector9_hall_of_echoes`, `stasis pod` in `sector9_stasis_chamber`, `coolant garden` in `sector9_stasis_coolant_lines`, `dark console` in `sector9_hangar_bay`) lack `inspect_after_completion: true` in [rooms.json](../../app/src/main/assets/rooms.json), remaining highlighted as active interactions after use.
  - **Post-Quest NPC Fallback (`sector9_beach_cove`):** After talking to Scout Kaelen once (`kaelen_intro`, gated on `milestone_not_set:ms_kaelen_talked`), re-tapping him before or after `w2_sq02` falls through to *"Scout Kaelen has nothing more to say right now"* unless the player holds the exact turn-in items. Giving Kaelen a short default ambient line keeps him alive in the world.
- **Lore & Continuity:**
  - The distinction between the crashed **cargo pod** (`W01-08`) and the discovered starship **Astra** (`W02-04`) is clear throughout World 2 and now aligns with World 6's Memory Stair (`cargo pod wreckage`).
- **Conflict & Emotional Pacing:**
  - `scene_anchor_drill` (`W02-03`) is one of the best-written scenes in the game, turning a combat tutorial unlock (`nova_link`) into the physical and thematic blueprint for the finale.

---

### World 3 — The Spire (`world_3`: `hub_5_lower_city`, `hub_6_upper_city`)

- **Character Voice & Knowledge:**
  - The heist structure gives every crew member a distinct specialty: Zeke handles bureaucracy, maintenance logins, and cuff threads (`zeke_w3_mq13_cover`, `zeke_w3_pocket_the_scanner` with his `zeke_sheepish_v1` emote); Gh0st extracts patrol routes and blends into Upper City luxury by standing still; Orion reads acoustic prism timing; Nova drives the point work and listens when Zeke warns her at the Lens (`zeke_w3_mq14_lens_warning` → `nova_w3_lens_urgency`).
- **Reveals, Spoilers & Mystery Clue-Order Trace:**
  - **Trace 1 — Main-Quest Heist & Cover-Story Deduction (`w3_mq12` → `w3_mq13`):**
    - *Clue acquisition order:* In `w3_mq12`, the player completes 5 tasks in any order (`talk_jax`, `map_patrols`, `interrogate_guard`, `copy_badges`, `source_disguises`), then sequentially `hack_blueprints` and `assemble_table` (`w3_mq12_planning_table_dialogue`).
    - *Evidence given to the player:* `w3_mq12_copy_badges` states the copied freight badges require matching service uniforms; `w3_mq12_planning_table_dialogue` explicitly states: *"Wear the copied uniforms to Transit Plaza and use the freight badges at the service lift... Cover story: scheduled Skypark air-filter inspection."*
    - *Deduction test (`w3_cover_order` in `spire_skypark_dome`):* When the Museum Curator demands `"Maintenance, then? State your work order"`, the three options are:
      1. `"Scheduled air-filter inspection."` (Correct → `w3_cover_service_verified`)
      2. `"Emergency glass repair."` (Wrong → `w3_cover_emergency_rebuttal`: Zeke taps the folded service order authorizing *scheduled* work; loops back cleanly via `w3_cover_recover`)
      3. `"Private delivery for a guest."` (Wrong → `w3_cover_delivery_rebuttal`: Curator notes guest deliveries use the concierge while the crew is wearing maintenance uniforms; loops back cleanly).
    - *Verdict:* **100% fair and solvable.** The player is guaranteed to have read `w3_mq12_planning_table_dialogue` immediately before taking the elevator, and even if they forget, both wrong-answer rebuttals give in-world reminders without penalty.
  - **Trace 2 — Optional Mystery `w3_sq16` (*Murder in the Skyline Salon* in `spire_exec_lounge_scale_01`):**
    - *Structure:* 3 physical/digital clues (`inspect_glass`, `inspect_terminal`, `inspect_rig`) that can be inspected in **any of 6 permutation orders** (`3! = 6`), plus 3 witness conversations (Dr. Lyra, Bartender Dax, Singer Selene) and the suspect (Director Sorrell, internal speaker/ID `director_vance`).
    - *Gate check:* `director_vance_default_talk` is gated on `milestone_set:ms_w3_sq16_glass_inspected,milestone_set:ms_w3_sq16_terminal_inspected,milestone_set:ms_w3_sq16_rig_inspected`. If tapped *before* all 3 clues are collected, Sorrell delivers `vance_initial_alibi` (*"I've been at my corner table on an encrypted call since 21:00. My comms logs will confirm it"*). Thus, **no clue permutation can prematurely trigger the confrontation before the player has all three pieces of evidence**.
    - *Clue contents across all 6 orders:*
      1. `w3_sq16_inspect_glass`: Malick's drink contains **Cyano-Coolant 9**, a lethal refrigerant used in executive comms rigs.
      2. `w3_sq16_inspect_terminal`: Corner-table reader logged Sorrell's badge at **21:00**; **Syndicate Booth reader logged the same badge at 21:14**; equipment log shows his comms unit vented coolant at **21:12**.
      3. `w3_sq16_inspect_rig`: Sorrell's comms rig (bearing his procurement crest) has an **empty Cyano-Coolant 9 core bracket** with fresh frost around the catch.
    - *Two-stage deduction check:*
      - **Round 1 (`w3_case_alibi_question` — breaking the alibi that he never left his corner table):**
        - Option A (Correct): *"His badge entered the Syndicate Booth at 21:14."* → proves he left the corner table and entered Malick's booth (`vance_confession_1`).
        - Option B (Distractor): *"His comms unit vented coolant at 21:12."* → Sorrell rebuts: *"A unit can vent while I sit beside it. Show me a record of me leaving the table"* (`w3_case_remote_rebuttal` → loops back).
        - Option C (Distractor): *"The dispenser poured a drink at 21:15."* → Sorrell rebuts: *"Of course the dispenser logged it. Where does it say I went to the booth?"* (`w3_case_dispenser_rebuttal` → loops back).
      - **Round 2 (`w3_case_physical_question` — connecting Sorrell's equipment to the poison after he claims the 21:14 badge ping was a firmware timestamp glitch):**
        - Option A (Correct): *"Coolant in the glass matches the missing core in his comms rig."* → `vance_confession_2` (*"A logging fault doesn't empty a coolant bracket"*) → confession (`vance_confession_3` → `vance_confession_admission`).
        - Option B (Distractor): *"The badge timestamp repeats his name."* → *"You're reading the same log twice. I'm asking what else you have"* (`w3_case_same_record_rebuttal` → loops back).
        - Option C (Distractor): *"The dispenser poured the drink without additives."* → *"Then ask who touched the glass after it was poured. You still haven't shown me where the coolant came from"* (`w3_case_clean_rebuttal` → loops back).
    - *Verdict on `w3_sq16`:* **Clue logic is airtight across all 6 orders**—each clue states raw facts without doing the deduction for the player, and the two rounds test (1) spatial alibi contradiction and (2) physical poison provenance.
    - *Two minor technical blemishes in `w3_sq16`:*
      1. Task `expose_vance` (`Stage 2: confront_killer`) is never marked done via `set_quest_task_done:w3_sq16:expose_vance` before `complete_quest:w3_sq16` (`F-08`).
      2. In `w3_case_alibi_question` and `w3_case_physical_question`, the option texts use **third-person pronouns** (*"His badge entered the Syndicate Booth at 21:14"*, *"Coolant in the glass matches the missing core in his comms rig"*) even though Nova is speaking **directly to Sorrell** (`"You said you stayed right here?"`). Changing *"His"* to *"Your"* in those five option labels will make them read naturally as Nova's spoken replies.
- **Guidance & Discovery:**
  - In `spire_the_static`, Jax has four conditioned dialogue roots (`jax_w3_mq11_shelter`, `w3_mq12_jax_intro`, `w3_sq11_jax_intro`, `w3_sq11_jax_turnin`) and **no unconditional post-quest fallback**. Once `w3_mq12:talk_jax` and `w3_sq11` are complete, tapping Jax says *"Jax has nothing more to say right now."* Adding a 1-line barkeep fallback fixes this.
- **Conflict & Emotional Pacing:**
  - As noted in `F-04`, adding a single ideological phrase to `vale_w3_mq15_override` when Vale hijacks the rooftop guns will make his sudden override of Thorne connect to his larger campaign arc.

---

### World 4 — The Foundry (`world_4`: `hub_7_slag_pits`, `hub_8_assembly_line`)

- **Character Voice & Knowledge:**
  - World 4 is Gh0st's strongest chapter: asking to read the Project Phantom intake record first (`gh0st_w4_phantom_intro`), sharing the memory of Elara waiting at the hall light (`gh0st_w4_springs_elara`), catching Nova when her first Construct shatters (`gh0st_w4_mq19_catch_nova`), answering Rylos by his chosen name (`rylos_w4_mq20_intro`), and choosing to leave the Titan Walker hatch open (`scene_w4_rylos_spared`).
- **Reveals & Spoilers:**
  - The Project Phantom revelation (that synthetic units are stabilized by neural maps copied from living captives, with Elara as the template for the 700-series) is properly gated behind `w4_mq17_access_phantom_terminal` and never spoiled by earlier journal text.
- **Guidance & Discovery:**
  - The 4-part Forge conveyor puzzle (`foundry_forge_anvil` and surrounding rooms) is well-signposted: `anvil cradle` and `anvil relic` use state-aware `description_variants` and clear `condition_unmet_message` hints.
- **Lore & Continuity:**
  - The hardware stolen at Titan Dock (`deep_core_engine` and `phase_cutter_arrays`) has a clear functional purpose that is immediately enacted in `scene_w4_outer_shield[1..2]` when the Astra cuts through the outer Shield to reach the Orbital Ring.
- **Conflict & Emotional Pacing:**
  - `scene_w4_outer_shield` (`W04-04`) effectively stages Vale's coup during transit to the Ring (`w4_coup_display_v2.webp`), especially Gh0st's closing observation that the Ring's automated carrier has stopped saying Thorne's name.

---

### World 5 — The Orbital Ring (`world_5`: `hub_9_orbital_ring`, `hub_10_deep_ring`)

- **Character Voice & Knowledge:**
  - Analyst Charles (`analyst_charles_default_talk`), Mara Thorne (`thorne_w5_mq22_intro`..`route_terms`), and Elara (`elara_w5_mq24_reunion`..`release_cradle`) have distinct, memorable voices.
  - Thorne's final line in the Throne Room (`vale_w5_mq25_soloist_beat_1`: *"Let her finish the sentence. If we cut a live line now, we lose the whole sector's confidence"*) brilliantly blends a flash of human hesitation with her habitual corporate-confidence vocabulary.
- **Reveals & Spoilers:**
  - Check the top-level quest summary of `w5_mq23` (*The Core Approach* in [quests.json](../../app/src/main/assets/quests.json)): *"Follow Thorne's route through Vale's rewritten firewall and reach Elara."* Because `w5_mq23` starts immediately after `w5_mq22_find_thorne` (where Thorne hands over the occupied-seat list confirming Elara is at the core anchor), the player **already knows** Elara is behind the firewall when `w5_mq23` begins—so this summary is earned, not a premature spoiler.
- **Guidance & Discovery:**
  - **Fix `F-03` (Ghost NPCs in `deep_mainframe_nave`, `deep_anchor_chamber`, `deep_throne_room`)** and **`F-06` (Orphaned `vale_w5_compliance_greeting` & easily missed `thorne_w5_mq23_core`)**.
  - **Post-Quest Fallback (`orbital_solarium`):** After completing `w5_sq22` (*Solar Maintenance*), `Maintenance Bot` falls through to *"Maintenance Bot has nothing more to say right now."*
- **Conflict & Emotional Pacing:**
  - The sequence from `W05-02` (Thorne's bargain) → `W05-03` (Elara reunion & Nova's first Stasis freeze in `scene_w5_anchor_stasis`) → `W05-04` (Nova's Stasis failing against Vale, Thorne's execution, the Avatar fight, and Elara's hand slipping away in `scene_w5_cross_the_tear`) is the dramatic peak of the pre-finale campaign.

---

### World 6 — The Source & Epilogue (`world_6`: `hub_11_event_horizon`, `hub_12_singularity`)

- **Character Voice & Knowledge:**
  - The three nightmare releases (`zeke_w6_nightmare_release`, `gh0st_w6_nightmare_release`, `orion_w6_nightmare_release`), the Shared Memory Span (`w6_mq28_final_banter_line_1..5`), the three Memory Stair refusals (`w6_mq29_refuse_*`), and the final confrontation with Vale (`vale_w6_final_confrontation`..`nova_w6_final_resolve`) show every character acting from their lived journey.
- **Reveals & Spoilers:**
  - No premature spoilers remain in World 6; all callbacks on the mandatory route (Jed's two wrench taps and lamp, the cargo-pod crash into Sector 9, Gh0st lowering his rifle on Canopy Ridge, Orion's painted ceiling, Gh0st catching Orion's harness on the first Astra flight) now refer strictly to witnessed main-quest events.
- **Guidance & Discovery:**
  - In `source_center_node_scale_03` (*Silence Pulpit*), both `[action:blinding geometry]` and `[action:return to the fracture]` are present in the initial description, and `source_center` (*The Center*) preserves `[action:shared tuning focus]` across its post-Silence variants.
- **Lore & Continuity:**
  - **Fix `F-09` (`source_art_tune_world` vs. Nova's Source burnout)** so the postgame ability unlock does not contradict `scene_w6_final_note[7]` and `scene_w6_epilogue_credits[4]`.
- **Conflict & Emotional Pacing:**
  - The rhythm from the quiet campfire reunion (`scene_w6_song_fire_reunion`) → Echo Mines → Memory Bridge & campfire promise → Memory Stair temptations → two-phase Vale battle (`scene_w6_silence_rises`) → Harmonic Choir Lock (`scene_w6_final_note`) → quiet diner epilogue (`scene_w6_epilogue_credits`) gives the finale real emotional breathing room.

---

## 3. Cross-World Continuity & Systems Text Audit

### A. Antagonist Arc Across All Six Worlds (Mara Thorne vs. Arden Vale)
- **Current Arc State:**
  - **World 1 (`W01-05`):** Boggs names Director Thorne's Sector 4 hazard-pay freeze (`bogs_w1_mq03_review_2`).
  - **World 2:** No named Thorne/Vale dialogue (focus stays rightly on Orion, Gh0st, and the 20-year-old Sector 9 breach).
  - **World 3 (`W03-02`, `W03-04`):** Jax names Thorne's private freight lift (`w3_mq12_talk_jax`); Thorne claims commercial ownership of Harmony at the Archive Vault (`thorne_w3_lens_recovery_order`); Vale is named in `thorne_w3_mq14_trap_intro` and immediately overrides her in `vale_w3_mq15_override`.
  - **World 4 (`W04-01`, `W04-04`):** Optional audit bulletin (`vale_w4_compliance_bulletin`); Rylos cites Thorne's ownership (`rylos_w4_mq20_intro`); Vale strips Thorne's credentials in `scene_w4_outer_shield`.
  - **World 5 (`W05-02`, `W05-04`):** Thorne admits her commercial guilt and bargains for survival (`thorne_w5_mq22_intro`); Vale states his "One voice / zero variance" thesis and executes Thorne (`vale_w5_mq25_soloist`).
  - **World 6 (`W06-03`, `W06-04`):** Vale tempts the crew at Memory Stair and debates Nova at The Center (`vale_w6_final_confrontation`).
- **Remaining Gap (`F-04`, `F-06`):** Planting one ideological phrase in `vale_w3_mq15_override` ("ending the variance" vs. Thorne's commercial lease) and wiring `vale_w5_compliance_greeting` + `thorne_w5_mq23_core` will make Vale's philosophy legible from his first appearance.

### B. Item Descriptions ([items.json](../../app/src/main/assets/items.json)) & Tutorials ([tutorial_scripts.json](../../app/src/main/assets/tutorial_scripts.json))
- **Audited 260 Items in `items.json`:**
  - Almost all 260 item descriptions are atmospheric, specific, and grounded in world lore (e.g., arcade logic boards, regional fish and cooked dishes, weapons, mods, and the 10 *Great Frontier* VHS tapes `vhs_tape_01..10`, where `vhs_tape_02` accurately cites *"the discarded document locker in Zeke's Booth"*).
  - **Two Item Fixes Needed:**
    1. `ghost_signal_cell` (`F-02`): Remove `"Orion's"` from the description to avoid spoiling `W02-02` during World 1.
    2. `keystone` (`id: "keystone"`, category `key_item`): Currently has a generic 28-character placeholder description (`"The Keystone emits a low hum."`). Verify if `keystone` is a legacy unused item; if unused, leave or mark internal, or give it world-accurate copy if obtainable.
- **Audited 17 Tutorial Scripts in `tutorial_scripts.json` & 8 Tuning Puzzles in `tuning_puzzles.json`:**
  - All 17 tutorial scripts and 8 tuning puzzle prompts are concise, accurate to the current UI (`Tinker`, `Party`, `Journal`, `Momentum/Overcharge`, `Snacks`), and free of lore contradictions.

---

## 4. Cinematics Inventory & Recommendations (`keep`, `refine`, `move`, `cut`, `add`)

### A. Complete Inventory of All 32 Non-Demo Cinematics in [cinematics.json](../../app/src/main/assets/cinematics.json)

*(Note: 28 technical VFX/camera test scenes in `cinematics.json`—such as `camera_gallery_demo`, `fade_demo`, `shake_demo`, `master_showcase`—are debug showroom demos and are excluded from story recommendations.)*

| Scene ID | Mode | Steps / Dur | Distinct Images | Live Trigger | Verdict |
|---|---|---|---|---|---|
| `intro_prologue` | `illustrated` | 10 / 32.0s | 6 custom (`intro_*_v1.webp`) | New Game start (`AppServices.kt`) | **Refine** |
| `new_game_fade_in` | `illustrated` | 1 / 8.0s | 0 (black fade reveal) | `new_game_spawn_player_and_fade` (`W00-01`) | **Keep** |
| `scene_cutter_surge` | `CARD` | 6 / player | 0 (room backdrop) | `w1_mq01_cutter_surge` (`W01-03`) | **Keep** |
| `scene_mine_restore` | `CARD` | 2 / player | 0 (room backdrop) | `evt_mine_generator_restore` (`W01-06`) | **Refine** |
| `scene_relic_sync` | `CARD` | 5 / player | 0 (room backdrop) | `w1_mq03_touch_relic` (`W01-06`) | **Keep** |
| `scene_sq04_hack` | `CARD` | 3 / player | 0 (room backdrop) | `w1_sq04_compile_spoof` (`W01-SQ04`) | **Refine** |
| `scene_sq05_datapad` | `CARD` | 4 / player | 0 (room backdrop) | `w1_sq05_read_datapad` (`W01-SQ05`) | **Keep** |
| `scene_w1_lift_separation` | `illustrated` | 3 / 11.5s | 1 (`rooms/world_1/launch_lift.webp`) | `w1_mq04_complete` (`W01-07`) | **Refine** |
| `scene_launch_crash` | `illustrated` | 7 / 41.9s | 3 (`launch_bay`, `launch_pod`, `crash_site` room art) | `w1_mq05_use_nav_console` (`W01-08`) | **Refine** |
| `scene_anchor_drill` | `CARD` | 10 / player | 0 (room backdrop) | `w2_mq04_anchor_drill` (`W02-03`) | **Keep** |
| `scene_w2_astra_departure` | `illustrated` | 9 / 39.3s | 4 (`hangar_bay`, `astra/bridge`, `spire_vent_output`, `spire_sewers_landing` room art) | `w2_mq05_confirm_departure` (`W02-04`) | **Refine** |
| `scene_w3_foundry_flight` | `illustrated` | 5 / 19.3s | 3 (`spire_landing_pad_roof`, `astra/bridge`, `foundry_slag_landing` room art) | `w3_mq15_launch_astra` (`W03-05`) | **Refine** |
| `scene_w4_rylos_spared` | `illustrated` | 3 / 11.7s | 1 (`rooms/world_4/foundry_titan_dock.webp`) | `w4_mq20_defeat_titan_walker` (`W04-04`) | **Refine** |
| `scene_w4_outer_shield` | `illustrated` | 9 / 44.6s | 4 (3 room backdrops + `cinematics/w4_coup_display_v2.webp`) | `w4_mq20_escape_launch` (`W04-04`) | **Refine** |
| `scene_w5_anchor_stasis` | `illustrated` | 5 / 20.9s | 3 (1 room backdrop + `w5_stasis_lunge_v1`, `w5_stasis_elara_v2`) | `w5_mq24_take_anchor` (`W05-03`) | **Keep** |
| `scene_w5_cross_the_tear` | `illustrated` | 6 / 21.2s | 3 (2 room backdrops + `w5_tear_hands_v2.webp`) | `w5_mq25_enter_tear` (`W05-04`) | **Refine** |
| `scene_w6_song_fire_reunion` | `illustrated` | 4 / 16.1s | 1 (`rooms/world_6/source_campfire.webp`) | `w6_mq26_reassemble` (`W06-01`) | **Refine** |
| `scene_w6_jed_echo_lesson` | `CARD` | 5 / player | 0 (room backdrop) | `w6_sq26_hear_lesson` (`W06-SQ26`) | **Keep** |
| `scene_w6_silence_rises` | `illustrated` | 3 / 13.3s | 2 (`source_center`, `source_center_node_scale_03` room art) | `w6_mq30_defeat_vale` (`W06-04`) | **Refine** |
| `scene_w6_final_note` | `illustrated` | 17 / 93.7s | 2 custom (`w6_ending_circuit_v1`, `w6_ending_crossing_v1`) | `w6_mq30_tune_world` (`W06-04`) | **Refine** |
| `scene_w6_epilogue_credits` | `illustrated` | 17 / 93.1s | 2 custom (`w6_ending_morning_v1`, `w6_ending_diner_v1`) + 2 black cards | `scene_w6_final_note` `on_complete` & `w6_replay_epilogue` (`W06-04`) | **Refine** |
| `scene_encounter_defeat_tip`, `scene_blocked_lock`, `scene_blocked_enemy`, `ollie_intro_scene`, `scene_collect_display/grinder/projector`, `market_plaza_intro`, `town_square_intro` | `CARD` | 1–2 / player | 0 | System/tutorial helpers in `ExplorationController.kt` & legacy room triggers | **Keep** |
| `scene_jed_tinkering_prompt`, `scene_jed_tinkering_wrapup`, `scene_complete_gather_broken_gear`, `scene_return_display`, `scene_return_grinder`, `scene_return_projector` | `CARD` | 1 / player | 0 | **None (Orphaned — 0 references)** | **Cut** |

---

### B. Scene-by-Scene Cinematic Recommendations

> **Presentation Benchmark:** `intro_prologue` (10 steps, 32.0s, 6 dedicated 1088×1920 illustrations) is the visual standard: every cut changes the visual subject (doors buckling → Chime locked in tube → receiver needles answering → stasis pod sealing → Beast outside the glass → title card), camera motion (`slow_push`, `drift_left`, `drift_right`) adds depth over multi-step holds, and captions carry spoken or internal context that the art alone cannot show.
>
> **Guiding Principle:** Do **not** convert interactive dialogue confrontations or player-paced discoveries (`scene_cutter_surge`, `scene_relic_sync`, `scene_anchor_drill`, `scene_w6_jed_echo_lesson`, the Archive Vault Lens conversation, Project Phantom/Cooling Springs, Thorne's Mainframe bargain, the Throne Room Soloist confrontation, or the three Memory Stair refusals) into auto-advancing illustrated cutscenes. Those beats work *because* the player controls the pacing and choices. Reserve new illustrated art for **physical transitions, splits, and transformations** where existing scenes currently stretch an empty room background across an action beat.

---

#### 1. `intro_prologue` — Cold Open (`W00-01`)
- **Verdict:** **Refine** (1 minor portrait decision + deferred linear score when ready)
- **Checkpoint & Trigger:** `W00-01`; runs at New Game start immediately before `new_game_spawn_player_and_fade` → `pit_nova_bunk` (`w1_mq01`).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Fresh campaign start.
  - *One beat to land:* Someone trapped in Sector 9 twenty years ago sacrificed his freedom to fire a brass Chime into the dark before sealing himself in stasis as a monster breached the glass.
  - *Next playable assumption:* Nova wakes in a dark mining bunk twenty years later with no apparent connection yet to Sector 9.
- **Nearby Content Changes Required:**
  - **Portrait Spoiler Check (`F-07`):** Steps `[3]` and `[6]` set `"speaker": "????"` to conceal Orion's identity, but explicitly pass `"portrait": "images/characters/orion_portrait.png"`. Furthermore, step `[6]` plays over `intro_orion_stasis_v1.webp`, which already shows the operator inside the stasis pod. Removing the 64dp portrait thumbnail on steps `[3]` and `[6]` (or using a silhouetted/visor portrait) preserves the `"????"` mystery until Orion awakens in `W02-02` (note: update `IntroCinematicAssetIntegrityTest.kt` if `"portrait"` is changed).
- **Composition & On-Device Playback Check:**
  - With the commit `a8b12f4a` fix in [ExplorationScreen.kt](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L8373-L8390), step `[0]` (`LOCATION` plate `"SECTOR 9 // TWENTY YEARS EARLIER"`), step `[1]` (`SYSTEM` alert `"Lab PA"`), and narration steps `[2, 4, 5, 7]` now sit cleanly at `Alignment.BottomCenter` over the bottom gradient scrim instead of blocking the containment door, Chime tube, and stasis pod artwork.
  - **On-device check:** Verify step `[3]` (`2.3s`, `"Broadcast my identity. Get it away from them."`) and step `[4]` (`2.1s`, `"The tube fires, rocketing through the conduit."`) reading speed on phone hardware.

---

#### 2. `scene_mine_restore` & `scene_sq04_hack` — World 1 In-Room Card Scenes (`W01-06`, `W01-SQ04`)
- **Verdict:** **Refine** (keep as `CARD`, remove duplicate pre/post `show_message` popups)
- **Checkpoint & Trigger:**
  - `scene_mine_restore` (`W01-06`): triggered by `evt_mine_generator_restore` (`player_action:mine_generator_restore` in `mine_junction`).
  - `scene_sq04_hack` (`W01-SQ04`): triggered by `w1_sq04_compile_spoof` (`player_action:w1_sq04_compile_spoof` in `server_hub`).
- **Player State / One Beat to Land / Next Assumption:**
  - `scene_mine_restore` lands the physical contrast between the mine work lights snapping on and the deeper Architect hum continuing underneath.
  - `scene_sq04_hack` lands the discovery of the word **"Chorus"** inside an air-gapped rebel spoof.
- **Nearby Content Changes Required (`F-05`):**
  - In [events.json](../../app/src/main/assets/events.json), delete the two redundant `show_message` actions in `evt_mine_generator_restore` (lines before `play_cinematic: scene_mine_restore`) and the redundant `show_message` action at the end of `w1_sq04_compile_spoof` (after `play_cinematic: scene_sq04_hack`).

---

#### 3. `scene_cutter_surge` (`W01-03`), `scene_relic_sync` (`W01-06`), `scene_sq05_datapad` (`W01-SQ05`) & `scene_anchor_drill` (`W02-03`)
- **Verdict:** **Keep** (remain player-advanced `CARD` cinematics)
- **Checkpoint & Trigger:**
  - `scene_cutter_surge`: `w1_mq01_cutter_surge` in `workshop_floor` (preceded by `w1_mq01_patch_flux_liner` & `w1_mq01_confirm_governor`; followed by `jed_w1_mq02_handoff_1`).
  - `scene_relic_sync`: `w1_mq03_touch_relic` in `echo_heart` after solving `w1_echo_counter_tune` (followed by `source_art_unlock` tutorial and `w1_mq04_start_lockdown`).
  - `scene_sq05_datapad`: `w1_sq05_read_datapad` in `mine_shunt` (optional side quest `w1_sq05`).
  - `scene_anchor_drill`: `w2_mq04_anchor_drill` in `sector9_canopy_ridge` after defeating `the_beast` (followed by `link_unlock` tutorial and `w2_mq05`).
- **Why `CARD` Mode Is Right Here:**
  - All four sequences rely on close, tactile reading of diagnostic frequencies, datapad logs, or step-by-step circuit grounding (`87 kHz`, `68% cold loop`, four hands dividing the current). Player-paced `CARD` advancement lets the player read technical and emotional details at their own speed without a timer cutting them off.
- **Audio Verification Flag:**
  - `scene_cutter_surge[4]` now triggers `music_nova_theme`, and `scene_relic_sync[0..1]` triggers `music_orion_theme` + `stinger_motif_source`. Verify on device that `stinger_motif_source` (6.19s) blends smoothly over `music_orion_theme`.

---

#### 4. `scene_w1_lift_separation` — Jed's Sacrifice at the Cargo Lift (`W01-07`)
- **Verdict:** **Refine** (commission 1 dedicated illustration replacing the static empty room background)
- **Checkpoint & Trigger:** `W01-07`; triggered at the end of `w1_mq04_complete` (`player_action:jed_sacrifice`), immediately after `jed_w1_mq04_sacrifice_7` (*"Go. Both of you. Take the pod rail and don't look back"*), and followed immediately by `w1_mq05` (*The Launch*) in `launch_lift` (where Jed has vanished and the post-sacrifice room variant applies).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Nova has just been handed the brass Chime by Jed as the Iron Warden closes the blast shutters.
  - *One beat to land:* The physical separation—Nova on the rising car, Jed holding the manual bypass lever across the widening gap until the steel shutters slam shut and his radio goes dead.
  - *Next playable assumption:* Jed is gone behind sealed blast doors; Nova must follow Zeke's light north into `launch_bay`.
- **Nearby Content Changes Required:**
  - Attach `stinger_motif_jed` to `scene_w1_lift_separation[0]` (or move it from the orphaned `scene_jed_tinkering_wrapup[0]`) so Jed's motif stinger punctuates his mandatory farewell on the main route.
  - Fix `F-02` (`ghost_signal_cell` item description) so inspecting the Chime right after this scene does not spoil Orion's name.
- **Beat Outline & Visual/Text Plan (3 steps, 11.5s):**
  - *Current limitation:* Step `[0]` uses the generic empty room background `images/rooms/world_1/launch_lift.webp` with no characters visible.
  - *Shot 1 (`step[0]`, 4.5s — NEW ILLUSTRATION `w1_lift_separation_v1.webp`):* Low-angle POV from the rising cargo lift looking down across the widening shaft gap: Jed bracing both gloved hands on the sparking manual bypass lever as heavy horizontal blast shutters close from top and bottom. Caption: *"The car pulls away. Across the gap, Jed still has both hands on the lever."*
  - *Shot 2 (`step[1]`, 3.2s — BLACK frame + `sfx_door_airlock_close`):* Hard cut to black as the shutters slam shut. Caption: *"The shutters meet. Jed's end of the radio goes quiet."*
  - *Shot 3 (`step[2]`, 3.8s — BLACK frame, Zeke dialogue):* Zeke (`zeke_idle.png`): *"Nova. I'm still here. Follow my light."*

---

#### 5. `scene_launch_crash` — Planetary Impact (`W01-08`, World 1 → World 2 Handoff)
- **Verdict:** **Refine** (replace 3 empty room backgrounds with 3 dedicated story illustrations; delete duplicate post-crash `show_message`)
- **Checkpoint & Trigger:** `W01-08`; triggered by `w1_mq05_use_nav_console` (`player_action:use_nav_console` in `launch_pod` once `ms_w1_chime_spliced` is set); `on_complete` warps to `sector9_crash_site` and starts `w2_mq01` (*A Strange Coast*).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Nova and Zeke have defeated the Iron Warden, boarded the cargo pod, and spliced Jed's brass Chime into the navigation array.
  - *One beat to land:* The Chime successfully authenticates launch, **but the planetary Shield above the colony rejects the pod**, tearing a stabilizer loose and forcing Nova and Zeke to hold onto each other as they crash into Sector 9's jungle canopy.
  - *Next playable assumption:* The cargo pod is wrecked in Sector 9; Zeke is injured inside the cabin (`w2_mq01:check_on_zeke`).
- **Nearby Content Changes Required (`F-05`):**
  - In `w1_mq05_use_nav_console` ([events.json](../../app/src/main/assets/events.json)), delete the redundant trailing `on_complete` action `{"type": "show_message", "message": "The pod is down in Sector 9. Zeke is injured, and the wreck still has live circuits."}` because `sector9_crash_site` immediately presents that state.
- **Beat Outline & Visual/Text Plan (7 steps, 41.9s):**
  - *Current limitation:* Steps `[0..4]` and `[6]` reuse static, empty room backgrounds (`launch_bay.webp`, `launch_pod.webp`, `crash_site.webp`) where neither the planetary Shield nor Nova and Zeke bracing in the cockpit can be seen.
  - *Shot 1 (`step[0]`, 7.1s — `w1_pod_rail_launch_v1.webp` or `launch_bay.webp`):* Cargo pod kicking off the launch rail as the brass Chime glows in the dashboard socket.
  - *Shot 2 (`steps[1..2]`, 10.3s — NEW ILLUSTRATION `w1_pod_shield_impact_v1.webp`):* Over-the-shoulder cockpit view past Zeke at the controls as a blinding hexagonal **planetary Shield lattice** fills the viewport and rejects the pod in a flare of white light.
  - *Shot 3 (`steps[3..4]`, 11.0s — NEW ILLUSTRATION `w1_pod_cabin_brace_v1.webp`):* Tight interior shot of Nova grabbing Zeke's arm and guiding it through the crash frame (`Zeke: "It won't let me turn! Nova, I can't get us clear!"` → `Nova: "Look at me. Hook your arm through the frame. I'm here."`).
  - *Shot 4 (`step[5]`, 6.5s — BLACK frame):* Console arcs dark; storm rain and sputtering thrusters carry the fall in audio.
  - *Shot 5 (`step[6]`, 7.0s — `w2_pod_canopy_crash_v1.webp` or `crash_site.webp`):* Black jungle branches rushing past the cracked viewport before impact and blackout.

---

#### 6. `scene_w2_astra_departure` — Airlane to the Spire (`W02-04`, World 2 → World 3 Handoff)
- **Verdict:** **Refine** (execute the 3-illustration plan from [WORLD2_TO_WORLD3_STORYBOARD.md](../design/story_presentation/WORLD2_TO_WORLD3_STORYBOARD.md))
- **Checkpoint & Trigger:** `W02-04`; triggered by `w2_mq05_confirm_departure` (`player_action:w2_mq05_confirm_departure` at `nav console` in `astra_bridge`); `on_complete` warps to `spire_sewers_landing`, completes `w2_mq05`, and starts `w3_mq11` (*Under the Radar*).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* The full four-person crew (Nova, Zeke, Orion, Gh0st) is together for the first time aboard the restored Astra.
  - *One beat to land:* A brief moment of earned triumph as the Astra lifts free of Sector 9, interrupted when Spire guidance locks onto them—forcing Zeke onto the manual bus, **Gh0st to catch Orion by the harness** as the deck tilts (which directly sets up the epilogue diner callback!), and Nova to dive beneath the street lights into the sewers.
  - *Next playable assumption:* The Astra is hidden in `spire_sewers_landing` beneath the Lower City (`w3_mq11:clear_landing`).
- **Nearby Content Changes Required:**
  - None in `events.json` (`w2_mq05_confirm_departure` already has a clean `on_complete` with no duplicate `show_message`).
- **Beat Outline & Visual/Text Plan (9 steps, 39.3s):**
  - *Current limitation:* Steps `[1, 2, 4, 5, 6]` reuse the empty `images/rooms/astra/bridge.webp` room backdrop (no crew visible even when the caption says *"The deck tilts. Gh0st catches Orion by the harness"*), and step `[3]` uses street-level `spire_vent_output.webp` for an airborne approach.
  - *Commission 3 shared/reusable illustrations:*
    1. `w2_astra_liftoff_v1.webp` (`step[0]`, 4.5s): Wide exterior of the Astra rising above Sector 9's bioluminescent canopy and tideglass coast, staying below the distant planetary Shield.
    2. `astra_bridge_crewed_v1.webp` (`steps[1..2, 4..6]`): Crewed bridge interior showing Nova and Zeke at the forward consoles and Gh0st beside Orion's harness (framed so `camera_start_x/y` pans can isolate Zeke/Orion on steps 1–2 and Gh0st catching Orion's harness on step 5).
    3. `w3_spire_airlane_approach_v1.webp` (`step[3]`, 4.5s): High-to-low aerial view of the neon Spire towering above a dark under-city service airlane as a guidance beacon turns red.
  - Keep step `[7]` (black impact cut, 2.7s) and step `[8]` (`spire_sewers_landing.webp`, 5.0s).

---

#### 7. `scene_w3_foundry_flight` — Through the Curtain (`W03-05`, World 3 → World 4 Handoff)
- **Verdict:** **Refine** (reuse `astra_bridge_crewed_v1.webp` + add 1 exterior curtain-seam shot)
- **Checkpoint & Trigger:** `W03-05`; triggered by `w3_mq15_launch_astra` (`player_action:w3_mq15_launch_astra` on `spire_landing_pad_roof` after `w3_mq15_defeat_administrator` and `w3_scan_shield_gap`); `on_complete` warps to `foundry_slag_landing` and starts `w4_mq16` (*Into the Fire*).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Vale has seized the rooftop guns; the crew has defeated the Administrator and timed a 6-second gap in the low-altitude security curtain.
  - *One beat to land:* Threading the needle through the inner security curtain while the outer planetary Shield still blocks high orbit overhead—explaining visually why the crew must land in the volcanic Foundry (`world_4`) to steal a Deep-Core Engine and Phase-Cutter Arrays.
  - *Next playable assumption:* The Astra is down on the Obsidian Shelf (`foundry_slag_landing`) outside the Foundry Slag Pits.
- **Nearby Content Changes Required:** None; trigger and handoff are clean.
- **Beat Outline & Visual/Text Plan (5 steps, 19.3s):**
  - Keep `step[0]` (`spire_landing_pad_roof.webp`, 3.0s) and `step[4]` (`foundry_slag_landing.webp`, 3.8s).
  - Replace `steps[1, 3]` (`images/rooms/astra/bridge.webp`) with the crewed bridge asset (`astra_bridge_crewed_v1.webp`).
  - Replace `step[2]` (4.0s, *"The curtain flashes across the hull. The outer Shield still holds above them"*) with 1 dedicated exterior illustration (`w3_curtain_seam_flight_v1.webp`) showing the Astra slipping through a narrow gap in an amber security curtain while the massive blue planetary Shield lattice remains sealed across the upper atmosphere.

---

#### 8. `scene_w4_rylos_spared` — Mercy at Titan Dock (`W04-04`)
- **Verdict:** **Refine** (commission 1 close insert of Gh0st lowering his blade before the open Titan hatch)
- **Checkpoint & Trigger:** `W04-04`; triggered automatically by `w4_mq20_defeat_titan_walker` (`encounter_victory: titan_walker_boss` in `foundry_titan_dock`), immediately before the player collects `deep_core_engine` and `phase_cutter_arrays`.
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Commander Rylos tried to enslave Gh0st with the 700-series vocal override; the crew just defeated Rylos's Titan Walker mech.
  - *One beat to land:* Gh0st exercising his own free will—not by executing his creator, but by lowering his phase blade and leaving the hatch open for an injured Rylos to crawl away.
  - *Next playable assumption:* The dock is clear; the player can now salvage `deep_core_engine` and `phase_cutter_arrays` and launch.
- **Nearby Content Changes Required:** None.
- **Beat Outline & Visual/Text Plan (3 steps, 11.7s):**
  - *Current limitation:* All 3 steps hold on the wide, empty `foundry_titan_dock.webp` room background.
  - Keep `step[0]` (4.3s) on `foundry_titan_dock.webp`, then cut on `steps[1..2]` (7.4s) to 1 dedicated illustration (`w4_rylos_spared_v1.webp`): foreground close shot of Gh0st lowering his glowing phase blade beside a dropped sidearm while the smoking Titan Walker hatch hangs open behind an injured Rylos.

---

#### 9. `scene_w4_outer_shield` — Beyond the Shield & Vale's Coup (`W04-04`, World 4 → World 5 Handoff)
- **Verdict:** **Refine** (reuse `astra_bridge_crewed_v1.webp` on steps 1–2; keep `w4_coup_display_v2.webp` on steps 3–7)
- **Checkpoint & Trigger:** `W04-04`; triggered by `w4_mq20_escape_launch` (`player_action:w4_mq20_escape_launch` in `foundry_titan_dock` after stealing `deep_core_engine` and `phase_cutter_arrays`); `on_complete` warps to `orbital_executive_dock` and starts `w5_mq21` (*Docking Procedure*).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* The crew has installed the stolen Foundry engine and cutter arrays to breach high orbit.
  - *One beat to land:* Two linked turns: (1) the stolen arrays physically cutting an opening through the outer planetary Shield, and (2) Vale using his Spire recovery authorization over the command channel (`w4_coup_display_v2.webp`) to invalidate Thorne's ownership signature and lock down the Orbital Ring under his own name.
  - *Next playable assumption:* The Astra arrives at `orbital_executive_dock` under hostile Orbital Compliance fighter attack (`w5_mq21`).
- **Nearby Content Changes Required:**
  - This scene is already consolidated (the coup dialogue was moved inside `scene_w4_outer_shield[3..7]` and uses the unified `w4_coup_display_v2.webp` art + `stinger_motif_vale` on step 3 + `music_vale_theme` on step 6).
- **Composition & On-Device Playback Check:**
  - Replace the empty `images/rooms/astra/bridge.webp` on `steps[1..2]` with `astra_bridge_crewed_v1.webp` (or an exterior of the Phase-Cutter Arrays slicing the Shield on `step[2]`).
  - **On-device check:** Verify that the bottom-aligned dialogue card on `steps[3, 4, 6, 7]` leaves both Thorne's and Vale's faces clearly visible in the upper two-thirds of `w4_coup_display_v2.webp`, and verify the 0.75s silent beat on `step[5]` (`style=none`).

---

#### 10. `scene_w5_anchor_stasis` — One Breath (`W05-03`)
- **Verdict:** **Keep** (already uses dedicated custom art `w5_stasis_lunge_v1.webp` and `w5_stasis_elara_v2.webp`)
- **Checkpoint & Trigger:** `W05-03`; triggered by `w5_mq24_take_anchor` (`player_action:w5_mq24_take_anchor` in `deep_anchor_chamber` after completing the `elara_w5_mq24_reunion` dialogue chain); `on_complete` grants `anchor_relic`, unlocks `source_art_stasis`, completes `w5_mq24`, and starts `w5_mq25` (*Critical Mass*).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Gh0st and Elara have just reconnected across the tank glass via the hall-light memory, and Elara has opened the Anchor cradle from inside.
  - *One beat to land:* As Nova pulls the Anchor free, a Hunter-Killer lunges at Gh0st; Nova instinctively triggers **Stasis**, freezing the monster and suspended coolant mid-air (`w5_stasis_lunge_v1.webp`), before Gh0st touches the glass (`w5_stasis_elara_v2.webp`) and Elara tells them she is still physically tethered to the tank and to follow her cable to the Throne Room.
  - *Next playable assumption:* Nova now commands `source_art_stasis` (setting up her attempt to freeze Vale in the Throne Room moments later), and the crew must follow Elara's cable to `deep_throne_room`.
- **Composition & On-Device Playback Check:**
  - **On-device check:** Verify on phone hardware that the bottom-aligned dialogue overlay on `steps[3..4]` over `w5_stasis_elara_v2.webp` does not obscure Gh0st's and Elara's hands meeting against the tank glass, and that `stinger_motif_elara` (8.20s) on `step[4]` transitions cleanly back to exploration.

---

#### 11. `scene_w5_cross_the_tear` — Across the Tear (`W05-04`, World 5 → World 6 Handoff)
- **Verdict:** **Refine** (check hand-crop framing on `w5_tear_hands_v2.webp` against bottom caption; keep structure)
- **Checkpoint & Trigger:** `W05-04`; triggered by `w5_mq25_enter_tear` (`player_action:w5_mq25_enter_tear` in `deep_tear` after defeating `compliance_avatar`); `on_complete` warps to `source_campfire` (`world_6`) and starts `w6_mq26` (*Fractured Minds*).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Vale shrugged off Nova's Stasis, executed Thorne, and tore open the dimensional fracture into the Source, pulling Elara's tank cable in with him.
  - *One beat to land:* Gh0st's fingers slipping past Elara's reaching hand (`w5_tear_hands_v2.webp`), Nova grabbing Gh0st's arm to leap in together, and the current breaking the crew's four-way grip in the dark so Nova wakes alone beside the blue campfire.
  - *Next playable assumption:* Nova is separated from Zeke, Orion, and Gh0st in `source_campfire` (`w6_mq26:rescue_zeke/gh0st/orion`).
- **Composition & On-Device Playback Check (`F-10`):**
  - `step[2]` (`2.6s`, `style=none`) displays `w5_tear_hands_v2.webp` completely unobstructed with no caption box—which guarantees the player sees Elara's warm brown hand, charcoal sleeve, and cyan wrist interface slipping past Gh0st's glove.
  - `step[3]` (`4.2s`, `style=dialogue`, Nova: *"We're going after her. Take my arm."`) holds on `w5_tear_hands_v2.webp` while panning upward (`camera_start_y: 0.02` → `camera_end_y: -0.02`). Verify on device that the bottom dialogue card on `step[3]` feels natural after the unobstructed hold on `step[2]`.

---

#### 12. `scene_w6_song_fire_reunion` — Back at the Fire (`W06-01`)
- **Verdict:** **Refine** (optional 1 crew-tableau illustration; current pacing and audio are strong)
- **Checkpoint & Trigger:** `W06-01`; triggered by `w6_mq26_reassemble` (`player_action:w6_mq26_reassemble` at `song-fire` in `source_campfire` after rescuing Zeke, Gh0st, and Orion in any order); `on_complete` grants `key_relic`, completes `w6_mq26`, warps to `source_echo_mines`, and starts `w6_mq27`.
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Nova has pulled each companion out of Vale's painless nightmare revisions.
  - *One beat to land:* The relief of physical proximity—three sets of footsteps returning to the fire, Zeke offering an extra glove for Nova's shaking hand, and four shadows converging across **The Key**.
  - *Next playable assumption:* The full party is reunited with `key_relic` at the entrance to `source_echo_mines`.
- **Beat Outline & Visual/Text Plan (4 steps, 16.1s):**
  - Currently uses `images/rooms/world_6/source_campfire.webp` across all 4 steps. If art budget permits, 1 dedicated illustration (`w6_campfire_reunion_v1.webp`) showing Zeke holding out the spare glove to Nova beside the blue flame while Orion and Gh0st cast four converging shadows across the Key will elevate this reunion; otherwise the room art + portrait dialogue already reads cleanly.

---

#### 13. `scene_w6_silence_rises` — Vale Phase 1 → Phase 2 Transformation (`W06-04`)
- **Verdict:** **Refine** (commission 1 dedicated transformation illustration for `step[1]`)
- **Checkpoint & Trigger:** `W06-04`; triggered automatically by `w6_mq30_defeat_vale` (`encounter_victory: ascended_vale` in `source_center`), advancing `w6_mq30` to stage `singularity` and leaving the player in exploration mode for a save/recovery interval before challenging Phase 2 (`ascended_god`).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* The crew has just defeated Vale's human Ascended form (`ascended_vale`).
  - *One beat to land:* Vale's human outline cracking apart as two massive Architect anchors catch the fragments in mid-air and pull them into the monolithic geometric form of **The Singularity / Silence** (`ascended_god`).
  - *Next playable assumption:* The player has a brief breathing window in `source_center` / `source_center_node_scale_03` to heal, save, and adjust gear before initiating the final Phase 2 boss fight (`Nova: "Stay together. Check your gear before we go again"`).
- **Beat Outline & Visual/Text Plan (3 steps, 13.3s):**
  - Replace `step[1]` (4.8s, currently using the empty room backdrop `source_center_node_scale_03.webp`) with 1 dedicated transformation illustration (`w6_vale_transformation_v1.webp`) showing Vale's fractured human silhouette suspended between two glowing anchor pylons as geometric hard-light plates assemble around him.

---

#### 14. `scene_w6_final_note` & `scene_w6_epilogue_credits` — The Finale & Epilogue (`W06-04`)
- **Verdict:** **Refine** (add 2 close-up insert illustrations to `scene_w6_final_note`, 1 saucer insert + revamped credits cards to `scene_w6_epilogue_credits`, and resolve `F-09`)
- **Checkpoint & Trigger:** `W06-04`; triggered by `w6_mq30_tune_world` after solving `source_harmonic_focus` (*Harmonic Choir Lock*) in `source_center`. `scene_w6_final_note` (17 steps, 93.7s) chains directly via `on_complete` into `warp: source_new_world` and `scene_w6_epilogue_credits` (17 steps, 93.1s).
- **Player State / One Beat to Land / Next Assumption:**
  - *Going in:* Both forms of Vale are defeated, and the player has placed Nova's note into the open third beat of the Harmonic Choir Lock.
  - *One beat to land (`scene_w6_final_note`):* As trapped voices call for the door and the ground lead fails, each crewmate fulfills the role trained in `scene_anchor_drill` (Zeke holding the bare ground bus by hand, Gh0st bracing the force, Orion holding the counter-tone) while Nova opens her hand to release root access—burning out her own Source connection forever—and **Gh0st catches Elara's reaching hand** (`"I've got you, Elara"`) as they cross back together.
  - *One beat to land (`scene_w6_epilogue_credits`):* Ordinary life after survival—one regional corridor open, Nova living with ringing ears and a hand tremor without regret, Zeke steadying her rattling saucer with one finger, and Nova choosing breakfast with her crew (while Elara heads to meet them at the receiver) before the next repair job.
  - *Next playable assumption:* Postgame exploration in `source_new_world` (*Morning Street*).
- **Nearby Content Changes Required:**
  - Resolve `F-09` (`source_art_tune_world` skill description/unlock vs. Nova's burned-out Source connection).
- **Beat Outline & Visual/Text Refinements:**
  1. **`scene_w6_final_note` (`steps[0..16]`, 93.7s):**
     - Currently holds on `w6_ending_circuit_v1.webp` for 8 steps (50.8s) and `w6_ending_crossing_v1.webp` for 9 steps (42.9s).
     - Add 1 close insert on `steps[3..5]` (`w6_circuit_ground_hands_v1.webp`): Zeke's bare hand gripping the arcing ground bus while Nova opens her palm over the Key to release root access.
     - Add 1 close insert on `step[15]` (`w6_corridor_elara_caught_v1.webp`): visual payoff to `w5_tear_hands_v2.webp`—Gh0st's gloved hand firmly catching Elara's reaching hand (with her charcoal sleeve and cyan wrist interface) across the threshold.
  2. **`scene_w6_epilogue_credits` (`steps[0..16]`, 93.1s):**
     - Add 1 close insert on `step[10]` (`w6_diner_saucer_v1.webp`): Nova's trembling left hand lifting the coffee cup as Zeke's single finger steadies the saucer edge beside the Tideglass photograph.
     - **Credits Cards (`steps[15..16]`, `F-10`):** As noted earlier, `steps[15..16]` (`"STAR BORN"` and `"Created by the Starborn team. Thank you for playing."`) have `image: null` and currently render as bottom-aligned narration cards over black. When you revamp the credits section, give these a dedicated centered credits/title layout (or `caption_style: "location"` / title-card treatment) and pair them with `music_epilogue` / `music_credits_ending` once final cutscene timings are locked.

---

#### 15. Orphaned `CARD` Cinematics to Cut (`F-08`)
- **Verdict:** **Cut** (or archive) the 6 unreferenced 1-step `CARD` scenes in [cinematics.json](../../app/src/main/assets/cinematics.json):
  - `scene_jed_tinkering_prompt` (superseded by `jed_w1_mq01_workshop_brief_1` in `dialogue.json`)
  - `scene_jed_tinkering_wrapup` (superseded by `jed_w1_mq01_repair_done_1`; move its `stinger_motif_jed` cue to `scene_w1_lift_separation[0]` first)
  - `scene_complete_gather_broken_gear`, `scene_return_display`, `scene_return_grinder`, `scene_return_projector` (legacy prototype scenes with zero triggers).

---

## 5. Prioritized Action Plan

### Phase 1 — Fix Before Building Cinematics (Code & Data Polish)
1. **Fix World 2 (`sector9_`) Silent-Tap Inversion (`F-01`):**
   - Update the 7 success-phrased `condition_unmet_message` strings in `sector9_` rooms in [rooms.json](../../app/src/main/assets/rooms.json) and add `roomId.startsWith("sector9_")` to `needsUnavailableFeedback` in [ExplorationController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt#L6141).
   - Add `inspect_after_completion: true` to one-shot World 2 quest actions (`navigation console`, `emergency medkit`, `stone lock`, `murals`, `stasis pod`, `coolant garden`, `dark console`).
2. **Remove World 1 Item Spoiler (`F-02`):**
   - Edit `ghost_signal_cell` in [items.json](../../app/src/main/assets/items.json) so it does not name `"Orion"` before `W02-02`.
3. **Fix World 5 Ghost NPCs (`F-03`) & Orphaned/Bypassed Antagonist Lines (`F-06`):**
   - Move `thorne` (`deep_mainframe_nave`), `elara` (`deep_anchor_chamber`), and `vale` (`deep_throne_room`) into `npc_presence` with `forbidden_milestones` in [rooms.json](../../app/src/main/assets/rooms.json).
   - Wire `vale_w5_compliance_greeting` in `orbital_airlock_gallery` and chain `thorne_w5_mq23_core` so players always hear Thorne's firewall briefing and `music_thorne_theme`.
4. **Plant Vale's Ideological Motive in World 3 (`F-04`):**
   - Add one clause to `vale_w3_mq15_override` in [dialogue.json](../../app/src/main/assets/dialogue.json) contrasting Thorne's commercial lease of Harmony with Vale's goal of ending variance.
5. **Remove Duplicate Event `show_message` Popups (`F-05`) & Clean Up Orphaned IDs (`F-08`, `F-09`):**
   - Remove the redundant `show_message` actions on `evt_mine_generator_restore`, `w1_mq05_use_nav_console`, and `w1_sq04_compile_spoof` in [events.json](../../app/src/main/assets/events.json).
   - Add `set_quest_task_done:w3_sq16:expose_vance` to `vance_confession_admission` and change `"His"` → `"Your"` in the 5 spoken option labels of `w3_case_alibi_question` / `w3_case_physical_question` in [dialogue.json](../../app/src/main/assets/dialogue.json).
   - Move `stinger_motif_jed` from the orphaned `scene_jed_tinkering_wrapup[0]` to `scene_w1_lift_separation[0]` in [cinematics.json](../../app/src/main/assets/cinematics.json).
   - Reconcile `source_art_tune_world` in [skills.json](../../app/src/main/assets/skills.json) / [events.json](../../app/src/main/assets/events.json) with Nova's Source burnout (`F-09`).
   - Add 1-line post-quest fallback greetings for Jax (`spire_the_static`), Scout Kaelen (`sector9_beach_cove`), and Maintenance Bot (`orbital_solarium`).

### Phase 2 — Refine During Cinematic Production (Art, Framing & Score)
1. **Priority Batch A — Shared Bridge & World Handoffs:**
   - Produce `astra_bridge_crewed_v1.webp` (reusable across `scene_w2_astra_departure`, `scene_w3_foundry_flight`, and `scene_w4_outer_shield`), plus `w2_astra_liftoff_v1.webp`, `w3_spire_airlane_approach_v1.webp`, `w3_curtain_seam_flight_v1.webp`, and the 2–3 cockpit/Shield impact shots for `scene_launch_crash`.
2. **Priority Batch B — Character Climax & Finale Inserts:**
   - Produce `w1_lift_separation_v1.webp` (`scene_w1_lift_separation[0]`), `w4_rylos_spared_v1.webp` (`scene_w4_rylos_spared[1..2]`), `w6_vale_transformation_v1.webp` (`scene_w6_silence_rises[1]`), and the 3 finale close-ups (`w6_circuit_ground_hands_v1.webp`, `w6_corridor_elara_caught_v1.webp`, `w6_diner_saucer_v1.webp`).
3. **Priority Batch C — Cold Open Portrait & Credits Revamp:**
   - Decide whether to hide/silhouette `orion_portrait.png` on the two `"????"` lines in `intro_prologue[3,6]` (`F-07`).
   - Revamp the two no-art credits cards at `scene_w6_epilogue_credits[15..16]` (`F-10`).
   - Once cutscene timings are locked, generate and master the 6 deferred linear cinematic music tracks (`music_intro_breach`, `music_crash_flight`, `music_cinematic_prologue`, `music_cinematic_crash`, `music_epilogue`, `music_credits_ending`).

### Phase 3 — Verify in a Fresh-Save Device Playtest (On-Device Acceptance)
1. **Bottom-Aligned Caption vs. Artwork Focal-Point Check (`F-10`):**
   - Play `intro_prologue`, `scene_w4_outer_shield`, `scene_w5_anchor_stasis`, `scene_w5_cross_the_tear`, `scene_w6_final_note`, and `scene_w6_epilogue_credits` on the Pixel 8a to verify that bottom-aligned dialogue cards (`Alignment.BottomCenter` with `54.dp` bottom padding) never obscure hands, faces, or key props in the lower third of vertical 1088×1920 illustrations.
2. **Reading Speed & Hold Timing on Tall Phone Aspect Ratio:**
   - Check the shortest timed holds in `intro_prologue[3..4]` (2.3s / 2.1s), `scene_w5_anchor_stasis[1..2]` (2.8s / 2.4s), `scene_w6_final_note` (shortest post-reveal hold ~2.2s), and `scene_w6_epilogue_credits` (shortest post-reveal hold ~1.76s) at normal reading speed with dialogue murmurs active.
3. **Audio & Stinger Handoffs:**
   - Verify the transition from cinematic music/stingers (`stinger_motif_source`, `stinger_motif_vale`, `stinger_motif_elara`, `stinger_motif_nova`, `stinger_motif_jed`, `music_nova_theme`, `music_vale_theme`, `music_jed_theme`) into destination room music on both normal completion and `HOLD TO SKIP`.
