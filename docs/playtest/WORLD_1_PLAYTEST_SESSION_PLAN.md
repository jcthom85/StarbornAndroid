# Starborn — World 1 Playtesting Session Plan

**Target Device:** Pixel 8a  
**Schedule:** Monday – Wednesday (~1 hour per session, 3 sessions total)  
**Reference Walkthrough:** `docs/playtest/walkthroughs/WORLD_1_COMPLETIONIST_WALKTHROUGH.md`  
**Scope:** Full World 1 completionist route (81 rooms, 5 main quests, 5 side quests, environmental puzzles, all caches/lore)

---

## Executive Summary & Pacing Reality

A thorough manual playtest of World 1 on a mobile device—reading all dialogue, inspecting room keywords, managing inventory/tinkering menus, testing combat abilities, and completing all optional branches—requires **approximately 3 hours to 3 hours 30 minutes** of active gameplay.

To respect natural story progression and safe save points, World 1 is partitioned into three balanced sessions of ~55–70 minutes each:
1. **Monday (Session 1):** *The Worker's Cage & Civilian Hubs* — Onboarding in the Pit, repairing the cutter in the Workshop, sweeping Trade Row and Med-Bay, and confronting the corporate wall at Transit Checkpoint.
2. **Tuesday (Session 2):** *Corporate Infiltration, The Deep Mine & Ancient Secrets* — Heavy lifting shield certification, Server Room terminal extraction, elevator descent, restoring power, mine puzzles, and reaching the alien Architect threshold.
3. **Wednesday (Session 3):** *Relic Sync, The Red Alert Gauntlet & Escape* — Counter-tuning the Fork, colony lockdown escape, Jed's sacrifice at the Cargo Lift, defeating the Iron Warden, splicing the Chime, and launch to World 2.

---

## Monday — Session 1: The Worker's Cage & Civilian Hubs

- **Starting Point:** Nova's Bunk (`pit_nova_bunk`) — Fresh New Game / Prologue.
- **Target Stopping Point:** Concourse Lobby (`admin_lobby`) in the Logistics Sector (safe hub room with Foreman Boggs, immediately past Blast Door A).
- **Approximate Gameplay Duration:** ~55–65 minutes.

### Story Events & Gameplay Milestones Covered
1. **The Pit Onboarding & Lore Sweep:**
   - Watch the opening cinematic, wake in Nova's bunk, turn on the bunk light, and isolate the electrical conduit to unlock the exit door.
   - Sweep the sleeping corridor: loot Supply Closet for **VHS Tape 1 (Film 01: Unpayable Debt)**.
   - Meet Jed in his bunk, talk through his shift warning, and complete the starter equipment tutorial (equip Mining Pistol and Nova's Flux Liner).
   - Explore Mess Hall (inspect the broken Hyperion arcade cabinet, the ceramic rooster, and the vintage mystery monitor), loot the kitchen ingredient hopper, and harvest the shower steam valves cache.
2. **Jed's Workshop & The Cutter Surge (`w1_mq01` Wake Up Call):**
   - Inspect the loader diagnostic strip in Scrap Yard, then defeat the Faulted Loader (combat tutorial).
   - Enter Jed's Shutter and access **Menu > Tinkering** to craft the **Functional Cryo-Inductor** (unlocks *Cryo Vent*).
   - Patch the Flux Liner ground strip, bypass the cutter governor, and trigger the live cutter test surge.
   - Sweep the workshop: collect **VHS Tape 2 (Film 02)** from Jed's Office, loot the Parts Loft mod bench (Precision Sight), Tool Shed toolbox, and Flooded Basement chest.
   - *Milestone:* `w1_mq01` (Wake Up Call) completes; `w1_mq02` (Shift Clearance) begins.
3. **Trade Row Sweep (`w1_sq01` The Scavenger's Stash):**
   - Enter Trade Row and talk to Scrapper at Scrapper's Stall to initiate `w1_sq01`.
   - Loot Cold Locker (`trade_locker`) and open the rebel cache in The Hidden Stash (`trade_stash`).
   - Return to Scrapper for turn-in (80 XP + 2 Pulse Grenades).
   - Explore Recyc Bar (talk to Miner Bill, inspect jukebox), Card Den, and tune the Pump Passage valve.
4. **Med-Bay Sweep (`w1_sq02` System Flush):**
   - Enter Med-Bay and talk to Doc in Exam Room 1 to start `w1_sq02`.
   - Loot the Med Supply Closet cryogenic locker.
   - Defeat the 2 Siren Skimmers in Ventilation Hub.
   - Execute the 3-step repair sequence: **Inspect Vent → Reroute Fans → Clear Toxic Blockage**.
   - Return to Doc for turn-in (90 XP + Corrosive Rounds mod).
   - Sweep Decon Lock and The Morgue (bio-containment canister, recycling terminal).
5. **Transit Checkpoint & The Bureaucratic Wall (`w1_mq02` Shift Clearance):**
   - Head through Shift Queue to Search Bay; talk to Guard Hank and trigger the clearance rejection.
   - Talk to Zeke in Zeke's Booth; select the **"Grid Instability"** paperwork category to bypass Nova's liability flag.
   - Receive the temporary **Mine Access Badge**.
   - Pass through Blast Door A and Transit Tunnel into Concourse Lobby (`admin_lobby`).

### Walkthrough Checkpoint & Safe Save
- **Walkthrough Section:** End of **Section 5: Get Zeke's temporary clearance**.
- **Journal Checkpoint:** `Shift Clearance (w1_mq02)` -> Stage: `override_retirement` -> Completed:
  - `[x] Take the temporary Mine Access Badge before the system notices the override. (receive_mine_access_badge)`
- **Next Active Objective:** `The Echo (w1_mq03)` -> Stage: `sector_four_assignment` -> `[ ] Report to Foreman Boggs in the Concourse Lobby...`
- **Save Location:** Open Field Menu (`MENU > Save`) in Concourse Lobby (`admin_lobby`).

---

## Tuesday — Session 2: Corporate Infiltration, The Deep Mine & Ancient Secrets

- **Starting Point:** Concourse Lobby (`admin_lobby`).
- **Target Stopping Point:** The Heart (`echo_heart`) in the Echo Chamber, standing directly before the ancient Tuning Fork.
- **Approximate Gameplay Duration:** ~60–70 minutes.

### Story Events & Gameplay Milestones Covered
1. **Admin Concourse & Guard Break Certification (`w1_sq03` Heavy Lifting):**
   - Talk to Foreman Boggs in Concourse Lobby to get the Sector 4 assignment and start `w1_sq03`.
   - Proceed east through Customer Service to Security Post (`admin_security`).
   - Engage the Acoustic Bulwark in the shield drill combat; learn and execute *Hydraulic Kick* (Guard Break).
   - Victory grants 100 XP, Heavy Gear, Hydraulic Fluid, and 2 AP.
   - Return to Boggs to complete certification and officially authorize the Deep Mine elevator descent.
   - Sweep Concourse: Foreman's Office, Observation Deck, and Staff Lounge dispenser/datapad.
2. **Server Room Infiltration (`w1_sq04` Protocol Override):**
   - Enter Server Room (`server_airlock`), defeat 2 Resonance Buoys in Aisle 01, and inspect Cooling Unit B.
   - In The Core Hub (`server_hub`), inspect terminal to start `w1_sq04`.
   - Interact with console to thaw controls, read output ports warning, then re-engage terminal and select: **"Write to the removable circuit board"** (extracts key without leaving audit logs; grants Circuit Board + 110 XP).
   - Sweep Backup Storage (breaker box, archive terminal) and Admin Office (executive terminal).
3. **Deep Mine Descent & Power Restoration:**
   - Head west to Elevator Lobby (`admin_elevator`), choose "Plan mine descent", and enter Deep Mine.
   - Arrive at Elevator Landing (`mine_landing`). Note: drain pool fishing is locked until World 2.
   - Advance north to Main Tunnel Alpha (`mine_alpha`) and defeat 2 Echo Borers.
   - Proceed north to Cavern Junction (`mine_junction`) and use the breaker box: **"Restore power"**.
4. **The Lost Shift (`w1_sq05`):**
   - With power restored, backtrack south to Main Tunnel Alpha, then east to Side Shunt 4 (`mine_shunt`) to trigger `w1_sq05`.
   - Inspect the crew datapad, read the miner's poignant final letter (grants Recoil Dampener + 120 XP), and mine the resonant vein.
5. **Mine Puzzles & Detours:**
   - At Cavern Junction, inspect switch rails, throw track diverter lever to **"Divert Track West"**, then release wheel brake. The cart crashes into the bulkhead.
   - Enter Sealed Munitions Stash (`mine_sealed_munitions_vault`) and loot Dominion munitions cache for **Thermite Core Mod**, Scrap Metal, and Pure Iron.
   - Complete optional northern detour to Riot Control Post (`mine_checkpoint`): defeat Acoustic Bulwark + Dominion Dampener and inspect Toxic Pocket.
   - Follow main route east/north: Conveyor Belt, Ore Sifter, Deep Shoring, Ancient Threshold, The Long Slope, Echo Antechamber.
6. **Entering the Architect Domain:**
   - Advance through Geometric Gap and Silent Walkway into the impossible alien architecture.
   - Enter The Heart (`echo_heart`) and stand before the pulsating ancient tuning fork relic.
   - Perform party maintenance: spend AP in skill trees, equip mods, check healing supplies.

### Walkthrough Checkpoint & Safe Save
- **Walkthrough Section:** End of **Section 9: Breach the ore-cart vault and finish the mine sweep** / **Pre-Fork completion checklist**.
- **Journal Checkpoint:** `The Echo (w1_mq03)` -> Stage: `relic_sync` -> Completed:
  - `[x] Step through the threshold into the impossible chamber. (reach_echo_chamber)`
- **Next Active Objective:** `The Echo (w1_mq03)` -> `[ ] Match the pulse spacing, balance the cold loop, then oppose the Fork's wave to quiet the combined signal. (touch_tuning_fork)`
- **Save Location:** Open Field Menu in The Heart (`echo_heart`) and save to a **DEDICATED SLOT** (keep this pre-Fork save file untouched).

---

## Wednesday — Session 3: Relic Sync, The Red Alert Gauntlet & Escape

- **Starting Point:** The Heart (`echo_heart`) — Dedicated Pre-Fork Save.
- **World Completion Milestone:** Crash Site (`sector9_crash_site`) in World 2 — Pod examined, Zeke in party, `w1_mq05` complete, and `w2_mq01` (A Strange Coast) active.
- **Approximate Gameplay Duration:** ~55–65 minutes.

### Story Events & Gameplay Milestones Covered
1. **Counter-Tuning the Fork & Relic Sync:**
   - Interact with the tuning fork in The Heart to launch the 3-stage waveform calibration:
     - *Pulse:* Adjust Phase Sweep to align peaks (Target: **87 kHz**, ±2 kHz) → Hold sweep.
     - *Cold Loop:* Adjust wave height to reference (Target: **68%**, ±3%) → Hold cold loop.
     - *Handshake:* Adjust Ground Phase to oppose traces (Target: **180°**, ±6°) → Engage handshake.
   - Watch the sync cinematic: the Functional Cryo-Inductor fuses/disappears, *Blast Wave* unlocks, and Nova recovers the **Tuning Fork** (+150 XP). *(Completes `w1_mq03` The Echo).*
   - Sweep the newly lit chambers: The Harmonic Well (east) and Signal Alcove (west).
2. **Red Alert Lockdown & Escape Gauntlet (`w1_mq04`):**
   - Alarms blare, PA declares Nova a containment threat; `w1_mq04` (Red Alert) begins.
   - Escape north through Emergency Exit (`echo_exit`) into Zeke's stolen comms route.
   - Enter Maintenance Access (`launch_access`) and defeat Siren Skimmer + Echo Borer; sweep Gantry Level 1 and Refueling Bay.
3. **Cargo Lift & Jed's Sacrifice:**
   - Enter Cargo Lift (`launch_lift`) and defeat Acoustic Bulwark + Resonance Buoy.
   - Talk to Jed to trigger his sacrifice sequence: Jed holds off security and gives Nova **Jed's Chime (Ghost Signal Cell)**, Medkit, rations, and 150 XP before sealing the bulkhead behind her.
   - Rest at the cargo lift and save in your primary progress slot.
   - *Milestone:* `w1_mq04` (Red Alert) completes; `w1_mq05` (The Launch) begins.
4. **Security Checkpoint B & The Iron Warden Boss Fight:**
   - Advance to Security Checkpoint B (`launch_checkpoint`) and defeat Dominion Dampener + Resonance Buoy.
   - Enter Pod Bay (`launch_bay`); talk to Zeke for his combat warning.
   - **Boss Encounter — The Iron Warden:** Multi-phase boss battle. Utilize *Cryo Vent* (Brittle) combos, *Hydraulic Kick* (Guard break), and *Blast Wave* while managing drone summons and healing.
5. **Chime Splice & Colony Launch:**
   - Talk to Zeke in Pod Bay after victory; follow him north into The Pod Core (`launch_pod`).
   - Talk to Zeke inside The Pod Core to splice the Chime into launch authorization/navigation. Zeke officially joins the active party.
   - Interact with the **navigation console** to initiate emergency launch.
6. **Planetary Shield Impact & World 2 Transition:**
   - Watch the launch cinematic: escape the colony shaft, collide with the Planetary Shield, suffer pod failure, and crash into World 2's swamp.
   - Land at Crash Site (`sector9_crash_site`).

### Walkthrough Checkpoint Confirming World 1 Completion
- **Walkthrough Section:** End of **Section 11: Defeat the Warden, splice the Chime, and launch**.
- **Journal Checkpoints Completed:**
  - `[x] Talk to Zeke in the Pod Bay, then follow him north into The Pod Core and talk again to splice the Chime into authorization/navigation. (splice_chime)`
  - `[x] After splicing the Chime, use the navigation console in The Pod Core to launch. (launch_from_colony)`
  - `[x] Survive the launch. (hit_planetary_shield)`
- **End State Verification:**
  - Quest `The Launch (w1_mq05)` marked **Completed**.
  - Quest `A Strange Coast (w2_mq01)` marked **Active**.
  - Party contains **Nova + Zeke**.
  - Inventory contains **Tuning Fork**, **Mine Access Badge**, and **Jed's Chime**.

---

## Discrepancies & Practical Mobile Playtest Notes

Keep these specific implementation rules and quirks in mind while playing on the Pixel 8a:

1. **Pre-Fork Point of No Return:**
   - When tuning the Fork at the end of Session 2, the world state permanently switches to Red Alert lockdown. Certain upper-colony rooms alter descriptions, and free exploration of civilian areas is cut off. Always preserve your dedicated Pre-Fork save slot.
2. **Locked Drain Pool Fishing:**
   - In Elevator Landing (`mine_landing`), the drain pool mentions fishing. This mechanic is strictly locked behind World 2's fishing gear. Do not spend time looking for a rod in World 1.
3. **Legacy "Launch Rail" Definition:**
   - In game data, `launch_rail` exists as a background room entry, but you never manually walk into it. Interacting with the navigation console in `launch_pod` triggers the cutscene and directly transitions to `sector9_crash_site`.
4. **Hub Map Return Restrictions:**
   - The "Return to Map" button only activates in designated entrance rooms (e.g., Pit Landing, Scrap Yard, Trade Row Gate, Triage Hall, Shift Queue, Concourse Lobby). If the button is greyed out, walk back to that sector's entrance room.
5. **Two-Step Zeke Chime Dialogue:**
   - In Session 3, after defeating the Iron Warden, you must talk to Zeke in Pod Bay *first*, then follow him north into The Pod Core and talk to him *a second time* to splice the Chime. Touching the navigation console before this second dialogue will display a locked error message.
6. **Strict Action Sequences:**
   - *Med-Bay Vents:* Must inspect cracked vent → reroute airflow via fans → clear toxic blockage in that exact order.
   - *Server Room Terminal:* Must inspect terminal → thaw console → read output ports → re-open terminal → choose "Write to removable circuit board".

---

## Quick Reference Summary Table

| Day | Focus / Narrative Arc | Starting Room | Ending / Save Room | Walkthrough Milestone |
| :--- | :--- | :--- | :--- | :--- |
| **Mon (S1)** | Homestead, Cutter Repair & Transit (`w1_mq01`, `sq01`, `sq02`, `mq02`) | Nova's Bunk (`pit_nova_bunk`) | Concourse Lobby (`admin_lobby`) | Section 5 (Shift Clearance complete, badge acquired) |
| **Tue (S2)** | Shield Training, Server Hack & Deep Mine (`sq03`, `sq04`, `sq05`, `mq03` pt 1) | Concourse Lobby (`admin_lobby`) | The Heart (`echo_heart`) | Section 9 / Pre-Fork Checklist (Before tuning Fork) |
| **Wed (S3)** | Relic Calibration, Red Alert, Boss & Launch (`mq03` finish, `mq04`, `mq05`) | The Heart (`echo_heart`) | Crash Site (`sector9_crash_site`) | Section 11 (Launch complete, World 2 arrival) |
