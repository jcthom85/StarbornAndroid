# Content Volume Standards (The Golden Ratio)

**Status:** Canon
**Purpose:** To define the standard volume of Quests, Side Quests, and Enemies per World to ensure consistent pacing and scope control.

---

## 1. The Golden Ratio (Per World)
Every World in *Starborn* is divided into **2 Hubs**:
*   **Hub 1:** Town / Safe Zone / Setup (Social & Exploration focus).
*   **Hub 2:** Dungeon / Danger Zone / Climax (Combat & Puzzle focus).

To maintain scope and pacing, every World must adhere to these content limits:

| Content Type | Per World Total | Distribution Logic |
| :--- | :--- | :--- |
| **Main Quests** | **5** | **Hub 1 (Setup):** 2 Quests (Intro + Rising Action)<br>**Hub 2 (Climax):** 3 Quests (Dungeon + Event + Boss) |
| **Side Quests** | **5** | **Hub 1:** 3 Quests (World-building + Early Gear)<br>**Hub 2:** 2 Quests (High-risk exploration / Lore secrets) |
| **New Enemies** | **6–8** | **Common:** 3-4 (Melee, Ranged, Fast/Swarm)<br>**Elite:** 2 (Tank, Support/Caster)<br>**Boss:** 1 (Major Act Boss) |

---

## 2. The Logic (Why This Number?)

### A. Side Quests are for Loot, Not XP
In *Starborn*, Side Quests (SQs) aren't just filler; they are the primary source of **Mods** and **Unlockable Skills**.
*   **The Rule:** ~1 Side Quest = 1 Meaningful Upgrade (Skill or Weapon Mod).
*   **The Outcome:** 5 SQs per world ensures the player gets ~5 meaningful upgrades per biome without bloating the inventory or overwhelming the player with choices.

### B. Enemy Variety vs. Cognitive Load
With **6-8 new enemies** per world, the player has to learn a specific "combat language" for that biome (e.g., World 1 is "Robots + Bugs").
*   **< 6:** Combat feels repetitive.
*   **> 8:** Combat becomes random; player can't build a strategy.
*   **The Mix:** 6-8 allows for different *combinations* of enemy groups (e.g., 2 Guards + 1 Drone vs. 3 Rock-Borers) to create freshness without needing new assets.

#### World 1 Addendum: Tutorial Cognitive Load (Keep It Lean)
World 1 is the tutorial biome, so its "combat language" must be readable at a glance.
*   **Teach fewer status effects:** Prioritize reactive/brute-force statuses (Stun/Blind/Corrosion) and delay tactical layers (**Jammed**, **Marked**) until World 2 (Zeke joins too late in W1 to build lessons around them).
*   **Make Guard Break/Stagger the star:** World 1 introduces shielded/armored enemies early; players need a clear "this is the key" mechanic before the Warden.
*   **Elements are fine—don’t teach all of them at once:** World 1 can focus on Physical/Shock/Burn (+ Acid via an optional mod) and save deeper element interactions for later worlds.

### C. Main Quest Pacing
*   **Hub 1** is designed to be "Social/Exploration" heavy. Talk to NPCs, get access, learn the culture.
*   **Hub 2** is designed to be "Combat/Dungeon" heavy. Gauntlets, puzzles, boss fights.

---

## 4. Node Room Density Standard

To ensure locations feel like lived-in environments rather than single-purpose sets, every node must meet a minimum "Room Count" standard. This prevents nodes from feeling like "menu selections" and encourages exploration.

| Node Type | Room Count (W1) | Distribution |
| :--- | :--- | :--- |
| **Minor / Social** | **6–8** | 1 Entry, 1 Main, 2-3 Flavor/Lore, 2 Transition. |
| **Major / Home** | **8–12** | 1 Entry, 2-3 Main (Shops/Home), 4-5 Flavor/Loot, 2-3 Transition. |
| **Dungeon / Combat** | **10–15** | 1 Entry, 3-4 Combat Arenas, 1-2 Puzzle/Gating, 4-5 Lore/Loot, 1 Boss/Relic. |

**Progression Note:** This is the baseline for World 1. Later worlds (W3+) should increase these counts by ~50% (e.g., 15–20 rooms for a dungeon) as complexity and player traversal capabilities grow.

---

## 5. Actionable Word Standards

To support the "no typing" interaction model, room descriptions use a system of "Actionable Words" that are highlighted and tappable in the UI.

*   **Keyword Density:** Every room description must contain **2–4 actionable keywords**.
*   **Implementation:** Keywords are defined in the room's `actions` array. The `name` field of the action must match the exact string (including casing) used in the description.
*   **Casing Rule:** Use normal sentence casing. Keywords should not be artificially capitalized unless they start a sentence or are proper nouns.
*   **Interaction Requirement:** Every keyword found in a description **must** have a corresponding entry in the `actions` array.
*   **Feedback Loop:** If a keyword is purely for investigation (lore/flavor), use a `GenericAction` with a `condition_unmet_message` to provide the "Examine" text.
*   **Stateful Text:** Meaningful interactions should update room text through `description_variants` when the environment changes, a clue is revealed, a puzzle advances, or a quest changes the room's meaning.
*   **Persistent Truth:** Use popups for the immediate moment, but use the room description for what remains true after the action. If a vent clears, a console thaws, a light turns on, or an alarm changes the route, the room text should say so.
*   **Variant Keywords:** Each description variant should include the actionable words that remain relevant in that state. Do not keep dead keywords in variant prose just to satisfy density.
*   **World 3 Heist Text:** World 3 should use stateful room text as an infiltration readout. Intel, disguises, copied credentials, disabled scanners, Archive alarm states, and Lens/Scan revelations should persist in the room descriptions so the heist feels assembled and then disrupted in the actual play space.
*   **World 4 Foundry Text:** World 4 should use stateful room text as an industrial pressure gauge. Landing damage, heat routing, Phantom records, conveyor timing, matrix overload, Anvil access, Titan defeat, engine theft, and array theft should leave readable changes in the affected rooms so the Foundry feels like a system Nova is breaking, not a sequence of buttons.

---

## 6. World 1 Template (Applied)

*   **Hub 1 (Homestead):**
    *   **MQ:** 2 (Wake Up Call, Paperwork)
    *   **SQ:** 3 (Scavenger's Stash, System Flush, Heavy Lifting)
*   **Hub 2 (Logistics):**
    *   **MQ:** 3 (The Echo, Red Alert, The Launch)
    *   **SQ:** 2 (Protocol Override, The Lost Shift)
*   **Total:** 5 MQ, 5 SQ.
*   **Bestiary (Enemies):** 7 Unique Entries (4 Common, 2 Elite, 1 Boss).

**World 1 pacing note:** Treat **W1_SQ03 "Heavy Lifting"** as mandatory or strongly signposted, since it teaches **Guard Break/Stagger** and rewards **Hydraulic Kick** (a core tool for shielded enemies leading into the Warden).

This template should be replicated for Worlds 2–6.
