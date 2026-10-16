# World 1 ("The Mines") — Deep-Dive Evaluation & Polish Plan

**Date:** October 16, 2026
**Scope:** `world_1` (`hub_1_homestead`, `hub_2_logistics`, 82 playable rooms, `w1_mq01`–`w1_mq05`, `w1_sq01`–`w1_sq05`)
**Related Docs:** [Full Game Evaluation & Roadmap](EVALUATION_AND_ROADMAP_2026-10-16.md) | [World 1 Round 1 Prose Checkpoint](story/WORLD1_ROUND1_PROSE_CHECKPOINT_2026-07-15.md)

---

## 1. Executive Assessment: How Good Is World 1 Right Now?

**Current World 1 Score: 8.8 / 10 (Potential: 9.6 / 10 with targeted fixes)**

World 1 is **the strongest, most cohesive vertical slice in Starborn**. Unlike Worlds 3–6 (which contain `_scale_` corridor padding and pending prose passes), **100% of World 1's 82 playable rooms are bespoke**, shifting across 7 distinct visual/particle/audio themes (`mine_dark` $\rightarrow$ `workshop_warm` $\rightarrow$ `medbay_sterile` $\rightarrow$ `neon_alley` $\rightarrow$ `admin_cold` $\rightarrow$ `server_glow` $\rightarrow$ `echo_void`).

### Why World 1 Works So Well
1. **Immediate "Blue-Collar Cosmic" Identity:** Within 90 seconds of waking in `pit_nova_bunk`, the player learns who Nova is and how Dominion operates—not through a lore codex, but by flipping a bunk switch and seeing a sparking door relay stamped `TICKET CLOSED: WITHIN QUOTA`.
2. **Tactile "Inspect $\rightarrow$ Prepare $\rightarrow$ Exploit" Onboarding:**
   - **Combat #1 (`faulted_loader` in `workshop_yard`):** Inspecting the `loader diagnostic strip` teaches elemental weakness (`Shock`) and the `-1 turn` cooldown refund before the player swings once.
   - **Tinkering & Causality (`workshop_floor`):** Repairing the `functional_cryo_inductor` (68% cold loop), grounding the `Flux Liner`, and confirming the `mining cutter` governor bypass directly causes `scene_cutter_surge` (`CUTTER FAULT 4C-117`), which drives the entire plot of `w1_mq02` and `w1_mq03`.
   - **Guard Break Gate (`w1_sq03` in `admin_security`):** Requiring Nova to learn `nova_hydraulic_kick` against the Shield Trainer (`acoustic_bulwark`) before Foreman Boggs unlocks the Deep Mine guarantees the player has the exact tool needed for Sector 4 and `the_iron_warden`.
   - **Diegetic Climax Puzzle (`w1_echo_counter_tune` in `echo_heart`):** The 3-dial oscilloscope handshake (`87 kHz / 68% / 180°`) pays off the exact numbers established in Jed's workshop.
3. **Earned Emotional Payoff:** Jed's gruff care in `pit_jed_bunk`, Zeke's progression from awkward compliance clipboard-holder (`pit_mess` / `checkpoint_booth`) to co-conspirator (`"Grid Instability"`), and the tragic letter in `w1_sq05` (*"They aren't paying us anymore, Mar. They're just keeping us."*) hit with real emotional weight.

---

## 2. The 15 Concrete Issues Holding World 1 Back From Perfection

Despite its strong foundation, our room-by-room and line-by-line audit uncovered **15 specific bugs, pacing stalls, narrative disconnects, and design friction points** across World 1:

### A. Opening 5-Minute Hook & First-Impression Friction

1. **10.2 Seconds of Silent Black Screen After the Prologue (`cinematics.json:266-278`, `ExplorationController.kt:1153-1230`):**
   - **What happens:** Right after `intro_prologue` finishes (or is skipped), `new_game_spawn_player_and_fade` launches `new_game_fade_in`, which is an **8.0-second empty step** (`"text": "", "duration_seconds": 8.0`) followed by a `2200ms` completion delay in `ExplorationController.kt:1230`.
   - **Why it hurts:** `ExplorationController.kt:1165` expects `fadeStep?.audioCue` to play the shift buzzer over the black screen (`// The shift buzzer is what wakes Nova`), **but `new_game_fade_in` in `cinematics.json:266-278` has no `"audio_cue"` field!** And because `pit_nova_bunk` starts as a dark room (`dark: true`), fading from black into a dark room over 10.2 seconds in dead silence makes the game look frozen right when gameplay starts.
   - **Fix:** Cut `new_game_fade_in` duration from `8.0s` to `1.8s`, add the shift buzzer `audio_cue`, and reduce the post-fade callback delay in `ExplorationController.kt:1230`.

2. **Unhooked Interactive Props in the Very First Room (`pit_nova_bunk`, `rooms.json:311-407` vs. `events.json:620-650`):**
   - **What happens:** In `pit_nova_bunk`, the room prose (`lines 326, 341`) calls out `cargo netting` and a `rusty locker holding your tools, a spare ration, and Jed's small metal carving`. Furthermore, `events.json:620-650` has a fully written event (`w1_mq01_check_netting`) where Nova inspects the netting and touches the bent-metal bird Jed carved for her. **However, `pit_nova_bunk`'s `actions` array (`lines 380-406`) only includes `bunk light`, `door control panel`, and `sleeping shelf`!**
   - **Why it hurts:** In a game about tapping nouns in prose, the very first room mentions `cargo netting` and `rusty locker` without making them interactive, and orphans a warm character beat between Nova and Jed.
   - **Fix:** Wire `cargo netting` (triggering `w1_mq01_check_netting`) and `rusty locker` into `pit_nova_bunk`'s `actions` array once `light_on == true`.

3. **Prologue Portrait Spoils `"???"` Mystery (`intro_prologue`, `cinematics.json:610-613, 653-656`):**
   - **What happens:** Steps 4 and 7 of `intro_prologue` use `"speaker": "???"` and `"An operator locks the brass Chime into the tube"` so Orion's identity is a mystery until World 2—**but they attach `"portrait": "images/characters/emotes/orion_angry.png"` and `"orion_worried.png"`**, showing Orion's face on top of the illustrated cinematic plate.
   - **Fix:** Remove the `"portrait"` field from steps 4 and 7 of `intro_prologue` so the full-bleed keyframe art (`intro_chime_launch_v1.webp` and `intro_orion_stasis_v1.webp`) carries the scene cleanly.

4. **Out-of-Order Tasks & Premature Spoilers in `w1_mq01` Stage `report_to_jed` (`quests.json:31-76`):**
   - **What happens:** As soon as Nova steps out of the sleeping corridor into `pit_shaft`, `w1_mq01` advances to stage `report_to_jed`. Its description (`line 33`) immediately spoils the next 3 rooms (*"Find Jed in his bunk and reach the workshop. After clearing the loader, collect the starter gear and craft the Functional Cryo-Inductor"*) and lists `equip_starter_gear` (#6) *after* `reach_workshop` (#3) and `clear_faulted_loader` (#5)—even though Jed gives the starter gear in `pit_jed_bunk` (#2) *before* Nova ever leaves the Pit!
   - **Fix:** Reorder `equip_starter_gear` right after `talk_to_jed` (or split `report_to_jed` into `report_to_jed` in the Pit and `prep_the_cutter` at the Workshop) and update the stage summary.

5. **`faulted_loader` Can Be Killed Before Inspecting `[loader diagnostic strip]` (`workshop_yard`, `rooms.json:563-615`):**
   - **What happens:** In `workshop_yard`, the `faulted_loader` enemy standee and `[loader diagnostic strip]` are clickable at the same time. If a player taps the enemy standee first and wins the fight, North unlocks, leaving `inspect_loader_relay` unchecked in `w1_mq01` until they walk back out to the yard and click the diagnostic strip on the already-defeated loader.
   - **Fix:** Either auto-complete `inspect_loader_relay` when `w1_mq01_clear_faulted_loader` triggers, or require `inspect_loader_relay` (or auto-trigger its inspection beat) when engaging `faulted_loader` for the first time.

6. **Duplicate `nova_flux_liner` Grant & Misleading `[live cutter test]` Fallback (`workshop_floor`, `events.json:970-973`, `rooms.json:701-705`):**
   - **What happens:**
     1. `w1_mq01_receive_starter_kit` (`events.json:12542`) gives Nova `nova_flux_liner` (qty 1). Then tapping `[Flux Liner]` in `workshop_floor` (`w1_mq01_patch_flux_liner`, `events.json:970-973`) gives her a *second* `nova_flux_liner`!
     2. If the player taps `[live cutter test]` *before* tapping `[Flux Liner]` and `[mining cutter]`, the `condition_unmet_message` (`rooms.json:704`) says: *"Cold loop 68%. Liner grounded. Governor bypass confirmed. Fire one sweep."*—claiming both steps are done when they aren't!
   - **Fix:** Remove the duplicate `give_item: nova_flux_liner` from `w1_mq01_patch_flux_liner` and change `[live cutter test]`'s `condition_unmet_message` to: *"Ground the Flux Liner and confirm the mining cutter bypass before firing a live test sweep."*

---

### B. Mid-World Pacing, Navigation & Narrative Continuity Issues

7. **Zeke Recognizes Nova in `checkpoint_booth` Even If She Never Entered `pit_mess` (`dialogue.json:363` vs. `events.json:1456`):**
   - **What happens:** Zeke's optional introduction (`w1_evt_zeke_volunteer` in `pit_mess`) only fires if the player detours East from `pit_L1_landing` into `pit_mess`. If the player exits straight from `pit_L1_landing` to the Hub Map, they never meet Zeke in the mess hall—yet in `checkpoint_booth`, `zeke_w1_mq02_1` (`dialogue.json:363`) unconditionally says: *"Wait—you're the miner from the bulletin board! Nova, right?"*
   - **Fix:** Add a conditional greeting line if `ms_w1_met_zeke_volunteer` is not set (or nudge the player into `pit_mess` / place Zeke's flyer beat on the critical path in `pit_L1_landing`).

8. **Confusing Admin Booth Direction After Guard Hank Denies Passage (`checkpoint_bay`, `rooms.json:1601-1673`):**
   - **What happens:** Guard Hank is in `checkpoint_bay` (`pos: [0, 1]`), while `checkpoint_booth` (`pos: [1, 0]`) is East of `checkpoint_queue` (`pos: [0, 0]`). When Hank says *"Go talk to the clerk in the booth"*, players in `checkpoint_bay` look for an East exit and only see West (`checkpoint_cell`) and South (`checkpoint_queue`).
   - **Fix:** Have Hank's line and the `visit_admin_window` task text specify *"Step back to the queue line and head east to the Admin Booth."*

9. **Dual-Location Residue in `w1_sq03` ("Heavy Lifting: Required Training", `quests.json:500-565`):**
   - **What happens:** `w1_sq03` contains both Stage 1 (`clear_the_dock`: `start_loader`, `clear_cargo` in `workshop_dock`) and Stage 2 (`guard_break_training` in `admin_security`). When Boggs assigns `w1_sq03` in `admin_lobby`, his trigger skips Stage 1 (`set_quest_stage:w1_sq03:guard_break_training`), leaving Stage 1's tasks unchecked in the quest structure.
   - **Fix:** Remove the obsolete `clear_the_dock` stage from `w1_sq03` in `quests.json` (and turn `workshop_dock`'s cargo loader into a standalone minor salvage interaction).

10. **Jarring Hub-Map Bounce When Riding `admin_elevator` (`ExplorationController.kt:2569-2579`):**
    - **What happens:** When the player swipes North from `admin_elevator` to descend into `mine_landing`, `ExplorationController.kt:2569-2579` intercepts the move and forces `ReturnToHub` so the player watches `deep_mine` unlock on the Hub 2 map, forcing them to tap `deep_mine` to actually enter `mine_landing`.
    - **Fix:** Either transition directly into `mine_landing` while unlocking `deep_mine` in the background, or make `admin_elevator`'s `[elevator]` action explicitly open the Hub descent transition.

---

### C. Climax (`w1_mq03`–`w1_mq05`) Logic & Urgency Bugs

11. **Premature Jed Sacrifice in `launch_lift` (`rooms.json:3348-3419`, `events.json:1951-2024`):**
    - **What happens:** Stepping into `launch_lift` immediately triggers `w1_mq04_jed_sacrifice` $\rightarrow$ `jed_w1_mq04_sacrifice_1..6` $\rightarrow$ `w1_mq04_complete` (*"Jed slams the manual override and charges the Enforcers with his wrench as the lift rockets upward."*).
    - **The Disconnect:** When that dialogue/popup finishes, **Nova is still standing inside `launch_lift`** with `acoustic_bulwark` + `resonance_buoy` blocking the North exit! Even worse, if the player clicks `[cargo lift]` to rest before or after that fight, its message (`rooms.json:3399`) says *"Nova takes cover behind the cargo lift hydraulics while Jed braces the manual override"*—even though the game just said Jed charged the Enforcers and the lift rocketed upward!
    - **Fix:** Trigger `w1_mq04_jed_sacrifice` **after** defeating the `launch_lift` enemies (or on transitioning North from `launch_lift` to `launch_checkpoint`), so Nova clears the platform, Jed holds the manual lever to send the car up, and Nova arrives in `launch_checkpoint`!

12. **Players Can Walk North Out of `launch_pod` Into `launch_rail` Before Launching (`rooms.json:3558-3631`):**
    - **What happens:** `launch_pod` has an unblocked `"north": "launch_rail"` connection (`rooms.json:3560`). A player entering `launch_pod` can ignore Zeke and the `[navigation console]`, swipe North into `launch_rail` (*"The magnetic rail pins Nova against the seat as the colony drops from view..."*), and then swipe South back into the unlaunched pod! Meanwhile, actually clicking `[navigation console]` (`use_nav_console`) plays `scene_launch_crash` and warps directly to `sector9_crash_site`, bypassing `launch_rail` completely.
    - **Fix:** Remove the walkable `"north": "launch_rail"` connection from `launch_pod` (or lock it / integrate `launch_rail`'s backdrop into the launch sequence) so players cannot casually stroll onto the active magnetic rail mid-flight and walk back!

13. **Zero Lockdown Gating During `w1_mq04` (Red Alert) & `w1_mq05` (The Launch):**
    - **What happens:** After `scene_relic_sync` triggers a colony-wide Red Alert (`ms_w1_lockdown_active`) and even after Jed sacrifices himself (`ms_w1_jed_sacrifice`), the player can freely exit to the Hub Map, travel back to `hub_1_homestead`, casually shop at Scrapper's stall, or chat with miners in `pit_mess` who act as if nothing has happened.
    - **Fix:** Once `ms_w1_lockdown_active` (`ms_w1_mq03_complete`) is set, lock backward travel above `echo_chamber` / `launch_bay` (with a clear lockdown message: *"Colony lockdown is active—the upper sector bulkheads are sealed. Get to the Launch Bay!"*).

14. **Redundant `show_message` Popups Immediately After Cinematics (`events.json:1114, 1888, 13627`):**
    - **What happens:** `w1_mq01_cutter_surge_aftermath`, `w1_restore_mine_generator`, and `w1_mq03_complete` play a multi-step illustrated/room cinematic and then immediately pop up a `show_message` card that repeats the exact same text the cinematic just showed.
    - **Fix:** Remove the redundant post-cinematic `show_message` step from those three events so the cinematic lands with punch.

---

### D. Broken Side Hooks, Empty Dead-Ends & Combat Spike

15. **Orphaned Cooking Event (`pit_kitchen`), Overwritten Storage Loot (`pit_storage`), Fishing Catch-22 (`mine_landing`), & Roaming Hauler Spike:**
    - **`pit_kitchen` (`rooms.json:118-121`):** `thermal cooker` references the wrong NPC name (*"Cook Rourke"* instead of `cook_vance`) and is missing `"action_event": "w1_cook_ration_soup"`, orphaning `events.json:13089-13128`.
    - **`pit_storage` (`rooms.json:437-442`):** `loose floor panel` triggers `find_vhs_tape_01` and sets `ms_w1_pit_storage_looted`, completely shadowing `w1_loot_pit_storage_panel` (`events.json:12885`, which grants 50 Credits, `painkillers`, and `2x scrap_metal`).
    - **`mine_landing` Fishing (`rooms.json:2471-2477`):** If a player unlocks fishing or inspects `sump drain` (`colony_pit_drain`) without a rod/lure in World 1, there is no World 1 source for `wooden_rod` + `basic_lure`.
    - **3 Empty Dead-End Rooms (`medbay_exhaust`, `echo_well`, `echo_memory`):** Players exploring off the main path into these dead ends get zero XP, zero items, and zero events.
    - **Roaming `pressure_hauler` (`enemy_movement.json:19-52`):** Has `220 HP, 110 Stability, 9 STR, 8 VIT, +50% Physical Resist`—making a random corridor patrol tankier and harder-hitting than `the_iron_warden` boss (`250 HP, 65 Stability, 6 STR, 6 VIT`) against a solo Level 3 Nova.

---

## 3. Recommended Execution Order for World 1 Polish

We can execute all of these World 1 improvements cleanly in **3 focused passes**:

### Pass 1: The Opening 5-Minute Hook & `w1_mq01` Perfection
1. Shorten `new_game_fade_in` (`cinematics.json:266-278`) from `8.0s` to `1.8s`, wire the shift buzzer `audio_cue`, and trim the extra `2200ms` delay in `ExplorationController.kt:1230`.
2. Remove the spoiler `"portrait"` fields (`orion_angry.png` / `orion_worried.png`) from the `"???"` lines in `intro_prologue` (`cinematics.json:613, 656`).
3. Add `cargo netting` (`action_event: "w1_mq01_check_netting"`) and `rusty locker` to `pit_nova_bunk` (`rooms.json:380-406`).
4. Fix `w1_mq01` stage `report_to_jed` task order (`quests.json:31-76`), auto-complete `inspect_loader_relay` on `faulted_loader` defeat if skipped, remove the duplicate `nova_flux_liner` grant (`events.json:970-973`), and fix `[live cutter test]`'s `condition_unmet_message` (`rooms.json:704`).

### Pass 2: Mid-World hooks, Side Quests & Dead-End Rewards
1. Hook `"action_event": "w1_cook_ration_soup"` onto `thermal cooker` in `pit_kitchen` (`rooms.json:118-121`) and fix `"Cook Rourke"` $\rightarrow$ `"Cook Vance"`.
2. Merge `w1_loot_pit_storage_panel`'s loot (`50 Cr`, `painkillers`, `2x scrap_metal`) into `find_vhs_tape_01` (or separate the actions in `pit_storage`, `rooms.json:436-441`).
3. Clean up `w1_sq03` in `quests.json:500-565` (remove the skipped `clear_the_dock` stage), add directional clarity from `checkpoint_bay` to `checkpoint_booth`, and add a conditional Zeke greeting in `checkpoint_booth` if `ms_w1_met_zeke_volunteer` is not set.
4. Add minor salvage/lore rewards (`+20 XP` / components) to the 3 empty dead-end rooms (`medbay_exhaust`, `echo_well`, `echo_memory`) and tune `pressure_hauler` (`enemies.json:278-327`) from `220 HP / 110 Stab / 9 STR` to `150 HP / 55 Stab / 7 STR` so it fits solo Level 3 Nova.

### Pass 3: Climax Sequencing (`w1_mq03`–`w1_mq05`) & Lockdown Integrity
1. Sequence `w1_mq04_jed_sacrifice` in `launch_lift` so Jed's sacrifice fires **after** clearing the `launch_lift` enemies (when riding the lift North to `launch_checkpoint`), and update `[cargo lift]`'s rest message after `ms_w1_mq04_complete`.
2. Remove the premature `"north": "launch_rail"` walkable exit from `launch_pod` (`rooms.json:3560`).
3. Lock backward travel to upper hubs during Red Alert (`ms_w1_lockdown_active`) and remove redundant post-cinematic `show_message` popups in `w1_mq01_cutter_surge_aftermath`, `w1_restore_mine_generator`, and `w1_mq03_complete`.
