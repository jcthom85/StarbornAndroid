# Locations — World 1: The Mines (Hub 1: Homestead Quarter)

## Hub Layout
The Homestead is a vertical slice of the colony, built into the canyon walls. It is cramped, neon-lit, and industrial.

**Progression Model:** Linear Start → Open Mid-Game → Linear Exit.
Nodes unlock in Stages. Nodes in the same Stage are available simultaneously.

## Stage 1
### Node 1: The Pit (Residential)
*   **Icon:** 🛏️
*   **Type:** Start / Tutorial
*   **Description:** A dense cluster of sleeping pods and communal mess halls. The air smells of recycled ozone and unwashed bodies.
*   **Rooms (10):**
    1.  **Pit Landing (pit_L1_landing):** Entry/exit point used by the Hub 1 map.
    2.  **Mess Hall (pit_mess):** Large social space; community flavor and possible Zeke volunteer beat.
    3.  **Ration Sink (pit_kitchen):** Small flavor room off the mess hall.
    4.  **Lift Shaft (pit_shaft):** A vertical room connecting L1 and L2.
    5.  **Pod Row (pit_L2_corridor):** Hallway lined with pods.
    6.  **Nova's Bunk (pit_nova_bunk):** The new-game story start (Player's pod), not a hub-map entry point.
    7.  **Jed's Bunk (pit_jed_bunk):** Jed's personal pod (Lore/Flavor).
    8.  **Shared Showers (pit_showers):** Grimy, industrial; hide a small loot item here.
    9.  **Supply Closet (pit_storage):** Small room for tools/flavor.
    10. **Vent Crawl (pit_vents):** Leads to a secret crawlspace overlooking the Mess.
*   **Purpose:** Intro to movement and basic interaction.
*   **Unlocks:** Stage 2 (Jed's Workshop).

## Stage 2
### Node 2: Jed's Workshop (Home Base)
*   **Icon:** 🔧
*   **Type:** Story / Main Quest
*   **Description:** Cluttered but cozy. Workbench covered in scavenged droid parts. The only safe place.
*   **Rooms (8):**
    1.  **Scrap Yard (workshop_yard):** Cluttered exterior with scrap piles.
    2.  **Main Bench (workshop_floor):** The heart of the shop; Workbench location.
    3.  **Jed's Office (workshop_office):** Cramped back room for private talk.
    4.  **Parts Loft (workshop_loft):** Storage area overlooking the floor.
    5.  **Tool Shed (workshop_shed):** Small side room with basic materials.
    6.  **Flooded Basement (workshop_basement):** Flooded storage; hides a secret mod.
    7.  **Back Alley (workshop_back):** Leads to an alley transition.
    8.  **Loading Dock (workshop_dock):** Where Bogs starts SQ_03 and Nova learns Guard Break/Stagger.
*   **Purpose:** Get Main Quest. Tutorial for Tinkering.
*   **Unlocks:** Stage 3 (Med-Bay AND Trade Row).

## Stage 3 (Choice)
### Node 3A: Med-Bay
*   **Icon:** 💊
*   **Type:** Choice A / Side Quest
*   **Description:** Sterile white walls stained with grime. The hum of bio-scanners covers the sound of coughing miners.
*   **Rooms (7):**
    1.  **Triage Hall (medbay_hall):** Waiting area with grimy benches.
    2.  **Doc's Exam Room (medbay_exam1):** Where Doc treats patients.
    3.  **Med Supply Closet (medbay_storage):** Hides medical supplies.
    4.  **Ventilation Hub (medbay_vents):** The combat/quest area for SQ_02.
    5.  **Exhaust Mouth (medbay_exhaust):** Exterior transition room.
    6.  **The Morgue (medbay_morgue):** Quiet lore room for "Retired Assets."
    7.  **Decon Lock (medbay_decon):** Small transition airlock.
*   **Purpose:** Side Quest SQ_02.
*   **Unlocks:** Stage 4 (Admin Gate) - *Unlocks alongside Trade Row progress.*

### Node 3B: Trade Row
*   **Icon:** 🛒
*   **Type:** Choice B / Shop
*   **Description:** A narrow corridor of stalls. Miners barter scrip for contraband.
*   **Rooms (9):**
    1.  **Trade Row Gate (trade_entrance):** Entry point with bright flickering signs.
    2.  **Market Strip (trade_strip):** The central corridor of stalls.
    3.  **Scrapper's Stall (trade_scrapper):** Main shop location.
    4.  **The Hidden Stash (trade_stash):** Quest location for SQ_01.
    5.  **Recyc Bar (trade_bar):** Small social room with 1-2 NPCs.
    6.  **Cold Locker (trade_locker):** Locked room with high-tier loot.
    7.  **Upper Gantry (trade_gantry):** Overlook room for scouting.
    8.  **Pump Passage (trade_maint):** Transition to the sewers.
    9.  **Card Den (trade_den):** Lore room with a card game.
*   **Purpose:** Shop access / Side Quest SQ_01.
*   **Unlocks:** Stage 4 (Admin Gate) - *Unlocks alongside Med-Bay progress.*

## Stage 4
### Node 4: Transit Checkpoint
*   **Icon:** 🚧
*   **Type:** Gate
*   **Description:** The heavily guarded checkpoint leading to the Admin Concourse. Massive blast doors feel like the mouth of a beast.
*   **Rooms (6):**
    1.  **Shift Queue (checkpoint_queue):** A narrow corridor with biometric scanners.
    2.  **Search Bay (checkpoint_bay):** Where guards check miners for contraband.
    3.  **Zeke's Booth (checkpoint_booth):** Zeke's window (Meeting location).
    4.  **Holding Cell (checkpoint_cell):** Lore room with "Asset" graffiti.
    5.  **Blast Door A (checkpoint_door):** The massive physical barrier.
    6.  **Transit Tunnel (checkpoint_tunnel):** The transition to Hub 2.
*   **Requirement:** None to enter Admin Concourse (Public Border Zone).
*   **Restriction:** Deep Elevator (Hub 2) requires **Mine Access Badge**.
*   **Purpose:** Transition to Hub 2 (Logistics Sector).
