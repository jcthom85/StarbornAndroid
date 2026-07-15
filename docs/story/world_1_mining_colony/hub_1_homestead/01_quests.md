# Quests - World 1: The Mines (Hub 1: Homestead)

## Main Quests (World 1)

See `docs/story/world_1_mining_colony/00_quest_list.md` for the canonical World 1 quest list.

| ID | Title | Objective | Key Steps | Rewards | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **W1_MQ01** | **Wake Up Call** | Prepare and test the cutter. | 1. Wake after four hours to the quota buzzer.<br>2. Inspect the cutter and confirm the governor bypass needed to make quota.<br>3. Talk to Jed; he sets down food and repairs the Cryo-Inductor.<br>4. Patch the suit-linked Flux Liner and attach its sacrificial ground strip.<br>5. Rebuild the cold loop; crafting sets `ms_w1_mq01_cryo_repaired` but does not complete the quest.<br>6. Confirm the bypass, ground the liner, set the loop to 68%, and run the live test.<br>7. Finish `scene_cutter_surge`; the conduit surge, breaker failure, and operator/cutter fault log complete MQ01. | Opens Hub 1 exploration only after the cutter-surge callback | Jed's care is repairs, food, and warnings. Their conflict is preservation versus Nova's dangerous overextension. |
| **W1_MQ02** | **Shift Clearance** | Survive mine clearance. | 1. Hank/scanner rejects Nova's clearance.<br>2. **Paperwork Hacking:** Zeke spoofs the form with the safest bureaucratic lie.<br>3. Zeke finds her file flagged for **Mandatory Retirement** because the system has calculated her as a liability.<br>4. Zeke saves her by moving the target: Nova is alive, but usable labor again. | Mine Access Badge | **Zeke Introduced.** He saves a troublemaker from corporate math without truly freeing her from it. |
| **W1_MQ03** | **The Echo** | Counter-tune the signal Nova woke. | 1. Follow the surviving operator/cutter damage report to Sector 4.<br>2. Reach the Ancient Chamber as the Fork repeats the prohibited sweep.<br>3. Use cutter diagnostics to set **87 kHz phase sweep**, **68% cold loop**, and **180-degree ground phase**.<br>4. Survive the cutter-suit-operator handshake.<br>5. Wait for `scene_relic_sync` to finish before rewards and completion. | Tuning Fork and Blast; Echo mark; Cryo-Inductor fuses; Flux Liner stays equipped with only its sacrificial ground strip spent | Learned mechanical reasoning, not ancestry or recognition. |
| **W1_MQ04** | **Red Alert** | Escape the lockdown. | 1. The relic exposes Nova as an anomaly.<br>2. Zeke guides Nova via comms while the sector seals behind her.<br>3. Chase/gauntlet through Maintenance Tunnels.<br>4. Reach the Cargo Lift.<br>5. **Jed's Sacrifice.** Jed hands her the **Chime** (Ghost Signal Cell), which can spoof the launch frequency long enough to wake a pod. He closes the doors from the outside. | **Chime (Ghost Signal Cell)**<br>Launch Bay path opens. | The colony stops being a workplace and becomes a trap. |
| **W1_MQ05** | **The Launch** | Hijack a pod and reject Dominion's asset lock. | 1. Fight through Logistics to the **Pod Bay**.<br>2. **Boss: The Warden.**<br>3. Zeke splices the **Chime** into the pod core to override the launch frequency.<br>4. Launch sequence -> shield/nav lock -> crash. | **Zeke joins party**<br>**Source Art: Signal Jammer (Zeke)** | Transition to World 2 (crash interlude). |

## Side Quests (Hub 1)

These quests reward combat tools and skills, encouraging players to explore before the point of no return.

| ID | Title | Quest Giver | Objective | Rewards | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **W1_SQ01** | **The Scavenger's Stash** | Scrapper (Fence) | Track down a hidden cache of old mining explosives. | **Nova Skill: Pulse Grenade** | Teaches usage of consumables/AoE. |
| **W1_SQ02** | **System Flush** | Doc (Medic) | Clear contaminated vents / chemical hazard. | **Weapon Mod: Corrosive Rounds** (Applies Corrosion / Acid DoT for Nova's guns) | Explains Corrosion (anti-armor) mechanics. |
| **W1_SQ03** | **Heavy Lifting** | Foreman Boggs | Use a loader/mech to move cargo / clear path. | **Nova Skill: Hydraulic Kick** | Make this mandatory or heavily signposted: it teaches Guard Break/Stagger, needed for shielded enemies leading into the Warden. |
