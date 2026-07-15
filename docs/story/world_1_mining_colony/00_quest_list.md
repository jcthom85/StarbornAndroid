# World 1 Quest List (Canonical)

This file is the single source of truth for World 1 quest IDs, titles, objectives, beats, and rewards.
Canonical key item name: Mine Access Badge (UI can label it "Mining Pass" if needed, but use one label consistently).

## Main Quests (World 1)

| ID | Title | Primary Objective | Key Beats (canonical) | Rewards / Unlocks |
| :--- | :--- | :--- | :--- | :--- |
| **W1_MQ01** | **Wake Up Call** | Prepare and test the cutter | Quota buzzer -> inspect cutter -> repair Cryo-Inductor -> patch/ground Flux Liner -> deliberately confirm governor bypass -> live test -> buried-conduit surge -> fault/operator record logged | Completes only after `scene_cutter_surge`; opens Hub 1 exploration and orders MQ02 |
| **W1_MQ02** | **Shift Clearance** | Survive mine clearance | Hank/scanner rejection -> Zeke finds the Mandatory Retirement flag -> paperwork spoof saves Nova by moving the liability target -> Mine Access Badge granted as temporary usable-labor status | Mine Access Badge; unlocks Transit Checkpoint / Admin path |
| **W1_MQ03** | **The Echo** | Counter-tune the signal Nova woke | Travel to Logistics -> Boggs follows the surviving cutter/operator damage record -> Deep Elevator -> restore power -> Ancient Chamber -> tune 87 kHz / 68% / 180 degrees -> survive cutter-suit-operator handshake | Tuning Fork and Blast; Echo mark; Cryo-Inductor consumed; Flux Liner retained with sacrificial ground strip spent |
| **W1_MQ04** | **Red Alert** | Escape the lockdown | Relic exposes Nova as an anomaly -> sector lockdown -> Zeke guides via comms -> chase/gauntlet through tunnels -> Cargo Lift -> Jed's Chime handoff | Chime (Ghost Signal Cell); opens Launch Bay path |
| **W1_MQ05** | **The Launch** | Hijack a pod and reject Dominion's asset lock | Fight through Logistics -> Pod Bay -> Boss: The Warden -> Chime spoofs the launch frequency -> launch sequence -> shield/nav lock -> crash | Zeke joins party; Zeke syncs -> Source Art: Signal Jammer (tutorialized in World 2); transition to World 2 (crash interlude) |

## Side Quests (World 1)

| ID | Title | Hub | Quest Giver | Objective | Reward (canonical) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **W1_SQ01** | **The Scavenger's Stash** | Hub 1 | Scrapper (Fence) | Track down a hidden cache | Nova Skill: Pulse Grenade |
| **W1_SQ02** | **System Flush** | Hub 1 | Doc (Medic) | Clear contaminated vents / chemical hazard | Weapon Mod: Corrosive Rounds (Applies Corrosion / Acid DoT for Nova's guns) |
| **W1_SQ03** | **Heavy Lifting** | Hub 1 | Foreman Bogs | Mandatory Logistics certification: use the loader, clear cargo, rescue workers, and prove Guard Break before Boggs authorizes Sector 4 | Nova Skill: Hydraulic Kick (Guard Break/Stagger) |
| **W1_SQ04** | **Protocol Override** | Hub 2 | Hacked Terminal (Server Room) | Restore / spoof access protocols | Passive: Admin Access (Hacking speed/efficiency up) |
| **W1_SQ05** | **The Lost Shift** | Hub 2 | Datapad (Deep Mine) | Find out what happened to the missing crew | Weapon Mod: Recoil Dampener |

**World 1 teaching note:** W1 should not build tutorials/puzzles around **Jammed** or **Marked** (Zeke joins too late). Keep World 1 focused on reactive/brute-force status learning. **Guard Break/Stagger** is mandatory through **W1_SQ03 "Heavy Lifting"** before Nova receives Boggs' Sector 4 authorization.
