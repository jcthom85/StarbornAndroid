# World 2 Quest List (Canonical)

This file is the single source of truth for World 2 quest IDs, titles, objectives, beats, and rewards.

## Main Quests (World 2)

| ID | Title | Primary Objective | Key Beats (canonical) | Rewards / Unlocks |
| :--- | :--- | :--- | :--- | :--- |
| **w2_mq01** | **A Strange Coast** | Survive the crash site. | Wake in wreckage -> Check injured Zeke -> Recover Chime and emergency kit -> Stabilize Zeke -> Follow glow-water stream. | Chime recovered; opens path to the Wilds |
| **w2_mq02** | **The Signal** | Track the distress beacon. | Track the pulse through thickets -> Arrive at Temple Gate -> Slot Chime into lock -> Enter Sanctuary. | Opens Sanctuary Facility; unlocks Event EVT_W2_01 |
| **w2_mq03** | **Sleeping Giant** | Awake the Ancient and recover the Bridge. | Authenticate Temple Gate with Chime -> power Sanctuary from reserve cells -> align stasis rings -> Orion wakes -> recover separate Bridge Echo from sealed cradle. | Orion joins; `bridge_relic`; `ms_w2_bridge_recovered` |
| **w2_mq04** | **The Hunter** | Deal with the stalker and complete Link training. | Confront Gh0st -> Source Lock pauses one action -> Beast ambush -> Gh0st chooses to protect -> Beast victory recruits Gh0st but does not finish quest -> complete Anchor Drill. | Link unlocks and MQ05 starts only after Anchor Drill callback |
| **w2_mq05** | **Liftoff** | Repair *The Astra*. | Calibrate coolant lines -> Overload breakers in Power Grid -> Overcome hangar security -> Launch bridge. | Repaired *Astra* accessible; world transition; hit Planetary Shield warning |

## Side Quests (World 2)

| ID | Title | Hub | Quest Giver | Objective | Reward (canonical) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **w2_sq01** | **Botanist** | Hub 3 | Zeke | Scan 5 unique plant variants in Sector 9. | Passive: **Ecological Insight** (Beast crit chance +10%) |
| **w2_sq02** | **Lost Patrol** | Hub 3 | Dead Scout | Trace three patrol signals, recover the transceiver and damaged cutter, then repair it. | Weapon Tool: **Thermal Cutter** |
| **w2_sq03** | **Tideglass Day** | Hub 3 | Orion (at Camp) | Gather tideglass herbs and clean meat, then craft them into stabilized resin. | Weapon Mod: **Source Resin** |
| **w2_sq04** | **Ancient Echoes** | Hub 4 | Orion | Align standing waves in Archive tuning bay to C#m7. | Orion Skill: **Chorus Echo** (AoE heal/cleanse) |
| **w2_sq05** | **Stolen Tech** | Hub 4 | Gh0st | Recover the Dominion transmitter core, then assemble the timing crystal into a capacitor. | Weapon Mod: **Rapid Capacitor** (cooldown speed +15%) |

## World 2 Mechanical & Tutorial Goals:
1. **Erosion Management:** Tutorialize the transition from hunger to Erosion (mental strain) and usage of Neural Stabilizers (Painkillers) in the Snack Slot.
2. **The Anchor Drill:** Combat tutorial showing how Nova links with party members to split/ground Source static.
3. **Snack Slot Management:** Show that consumables have a 5-turn cooldown in combat and require self-targeting.
4. **Tinkering:** W2 should make Nova build the useful thing from field salvage: repair the **Thermal Cutter**, craft **Source Resin**, and assemble the **Rapid Capacitor** instead of handing out finished upgrades.
