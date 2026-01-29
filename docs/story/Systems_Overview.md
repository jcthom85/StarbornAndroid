Starborn — Systems Overview as We Know It

Gameplay, UI, and data architecture snapshot for handoff to a separate LLM.

| Document purpose | Describe Starborn’s gameplay systems and content architecture in clear, implementation-friendly terms. |
| --- | --- |
| Platform | Mobile-first (Android initially), portrait orientation; later ports possible. |
| Engine | Custom made in Kotlin |
| Design philosophy | Modern text-adventure exploration + JRPG-inspired ATB-based combat, heavily streamlined for mobile UX. |

# 1) High-Level Game Loop

- Navigate room-to-room using swipe gestures that correspond to NSEW
- Interact via room descriptions (no typing). Tap highlighted words to interact.
- Discover exits and move between nodes/rooms within a hub/world.
- Talk to NPCs, accept quests, shop, craft (tinker), and manage inventory.
- Enter battles (mostly player-initiated). Choose actions/abilities strategically.
- Gain rewards (items, skills, story milestones), unlocking new areas and systems.

# 2) Content Architecture (Data-Driven)

Starborn is designed to be driven from JSON data: rooms, events, dialogue, items, enemies, quests, battles, and cinematics are intended to be authored via editors that write JSON. The game runtime loads this data to render UI, choices, and encounters.

Known tooling direction: a growing suite of editors and an AI assistant panel to generate content via OpenAI API.

# 3) Exploration System

## 3.1 Main Exploration UI

- Screen is portrait. Main text window occupies ~75% of the top portion with the room title above it (stylized pixel font).
- Room description includes highlighted/interactive words (bold/glow).
- Tapping a highlighted word opens an action menu (e.g., examine, use, talk, take, toggle), or triggers the action directly if only one logical option exists (accompanied by a narrative popup).
- Actions can change the room description (stateful text).
- Discovered exits persist for navigation clarity.
- No typing: all movement and actions are UI-driven.
## 3.2 World/Hub/Node Structure

The overworld structure is hierarchical: World → Hub → Node → Room. Generally each world will have two hubs. In World 1 there are currently two hubs: the Mining Colony (Hub 1) and the Corporate Outpost (Hub 2). Hub 1 currently has four main nodes: Homestead Quarter, Trade Row, Stellarium Mine, and Maintenance Tunnels (unlocks late and leads onward).

# 4) Combat & Skill System (Cooldown-Driven)

Starborn uses a cooldown-based skill system instead of traditional resource pools (MP, mana, stamina, etc.). All skills are available based on cooldown state and tactical conditions, not consumable points.

**Core Principles:**
*   **Baseline Equality:** Every encounter starts with the same mechanical baseline.
*   **Tactical Gating:** Power is gated by player decisions, not attrition.
*   **Sequencing over Stats:** Strategy and sequencing matter more than luck or stat abuse.
*   **Earned Power:** Strong actions must be earned within the fight (setup/conditions), not front-loaded.

## 4.1 Skill Categories (The "12 Skill" Limit)

Each character has **12 total skills**, split into two distinct categories:

### A. Core Skills (6 per character)
Core skills define a character’s identity and combat role.
*   **Availability:** Always available once learned (governed by cooldowns).
*   **Function:** Form the backbone of moment-to-moment combat.
*   **Design:** Designed to interact with enemy states, positioning, turn order, or ally actions.
*   **No Nukes:** Core skills are intentionally not raw "nukes." Strong effects require setup (e.g., target debuffed, ally acted earlier, defensive stance held). This prevents "open with the best move" syndrome.

### B. Source Arts (6 per character)
Source Arts are **Relic-granted abilities**, acquired when the party discovers and interacts with a world Relic.
*   **Acquisition:** One Relic per world = One new Source Art per character per world.
*   **Total:** 6 Source Arts per character across the full game.
*   **Philosophy (No "Ultimates"):**
    *   They are **Tactical Tools**, not damage spikes.
    *   Situationally powerful, not universally optimal.
    *   Designed to change *how* combat is approached, not end it.
    *   *Example:* A Source Art might reset cooldowns, force enemy turns to skip, or swap positions—powerful, but requires timing.

## 4.2 Cooldown & Condition Model

All skills use cooldowns, but cooldown alone is not the primary limiter. Skills often require **Conditions** to be effective:
*   **Enemy State:** Target must be debuffed, charging, or exposed.
*   **Player State:** User guarded last turn, or repositioned.
*   **Rhythm:** Skill is stronger if used second in a chain.

**The Rule:** Cooldowns define *when* a skill can be used again. Conditions define *whether* it should be used at all.

## 4.3 Encounter Balance

*   **Standard Enemies:** Allow aggressive openings. Core skills feel responsive. Source Arts provide efficiency.
*   **Elites:** Punish sloppy sequencing. Require setup before big actions. Source Arts solve specific mechanical problems.
*   **Bosses:**
    *   Designed with telegraphed actions and phases.
    *   Punish premature skill use.
    *   **Tactical Flow:** Opening with the strongest skill is often suboptimal. Players must prepare the moment. Source Arts are clutch interventions (e.g., interrupting a wipe mechanic).

## 4.4 Sprite Standards

Combat character sprites are defined in data (JSON) and rendered using pixel-art assets (PNG).
*   **Player Characters:** 64x64 or similar standard size.
*   **Enemies:** Varying sizes based on tier (Standard, Elite, Boss).
*   **Animations:** Idle (Breathing), Ready (Targeted), Attack, Hit, Death.

## 4.5 The Snack Slot (Combat Consumables)
(Retained from previous design)
*   **Structure:** Dedicated equipment slot for one consumable type.
*   **Mechanic:** Functions as a **Skill with a Cooldown** (e.g., 5 turns).
*   **Targeting:** Self-only. Prevents item spam and reliance on potion hoarding.

---

# 5) Inventory, Equipment, and Shops

- Inventory distinguishes consumables vs equipment.
- **Equipment Slots (4 per Character):**
    - **Weapon:** (Attack Power). Has 2 Mod Slots.
    - **Armor:** (Defense/Health). Has 2 Mod Slots.
    - **Accessory:** (Passive Stats/Traits).
    - **Snack:** (Combat Consumable). Self-targeting, Cooldown-based.
- **Mod Slot Unlock Rule:** Mod slots are locked by default. They are unlocked automatically by **Main Story Milestones** (e.g., reaching a new World or defeating a major Boss), ensuring character power scales with the narrative.
- Shops and commerce are introduced early in World 1 (Trade Row).

# 6) Crafting: Tinkering

Tinkering is a core crafting system accessible via the **Main Menu** (anytime/Camping), not restricted to physical benches.

**Scope:**
- **Creating Mods:** Building the item itself to slot into gear.
- **Puzzle Solving:** Combining Key Items (e.g., "Broken Key" + "Glue").
- **Field Survival:** Crafting basic supplies.

# 7) Progression & Scope (Summary)

*   **Core Skills (6):** Establish consistency and mastery. Learned via leveling/quests.
*   **Source Arts (6):** Reflect narrative progression and world discovery (Relics).
*   **Total Complexity:** 12 Active Skills = Readable for mobile, expressive without bloat.
*   **Pacing:** Because Source Arts arrive gradually (one per world), players learn naturally without overload.

# 8) Tutorials and Pacing (World 1, Hub 1 Plan)

World 1, Hub 1 tutorials should be delivered via a limited number of quests (not excessive). Covered topics include: movement, tapping highlighted words, NPC interaction, item usage, equipping gear, consumables vs equipment, combat consumables, shops/commerce, quest tracking, basic combat, group combat, Synergy skills, and tinkering.

Combat onboarding should stay lean: prioritize Physical/Shock/Burn (+ optional Acid/Corrosion), make Guard Break/Stagger the clear "puzzle key" before the Warden, and delay tactical statuses like Jammed/Marked until World 2 (Zeke joins too late in World 1 to build lessons around them).

# 9) Cinematics and Accessibility

Text-based cinematicare planned using animated 2D text and subtle visual effects (flickering lights, fades). Accessibility settings should exist for these sequences (e.g., reduce motion, reduce flicker, speed controls).

# 10) Visual Framing Rule (Backgrounds)

Background images follow a framing rule: canvas remains 1080x1920 (9:16), but add equal black vertical bars on left and right, each exactly 5/42 of the canvas width (~11.9048%), making the effective inner frame 9:21. Bars are pure black with no detail or text.
