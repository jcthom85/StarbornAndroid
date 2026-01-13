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
- Tapping a highlighted word opens an action menu (e.g., examine, use, talk, take, toggle).
- Actions can change the room description (stateful text) and can reveal new exits.
- Exits appear in the bottom-right area of the UI; discovered exits persist for navigation clarity.
- No typing: all movement and actions are UI-driven.
## 3.2 World/Hub/Node Structure

The overworld structure is hierarchical: World → Hub → Node → Room. Generally each world will have two hubs. In World 1 there are currently two hubs: the Mining Colony (Hub 1) and the Corporate Outpost (Hub 2). Hub 1 currently has four main nodes: Homestead Quarter, Trade Row, Stellarium Mine, and Maintenance Tunnels (unlocks late and leads onward).

# 4) Combat System

## 4.1 Core Combat Flow

- ATB-based combat inspired by classic JRPGs with modernized UX.
- Party grows from 1 up to 4 characters over time.
- Players select actions for characters in any order (strategic planning emphasis).
- Most encounters are player-initiated; ambushes where enemies go first are less common and may be story-related.

## 4.2 Combat UI Transition (Canonical)

When combat begins, exploration UI elements (journal/inventory/directional controls) collapse and fade, while a settings gear icon remains consistent. Character sprites fan out from the bottom-left into a cross-like formation for easy selection (mobile-first). Sprites idle with a breathing animation until set; once a character’s action is selected, they shift into a ready stance and freeze to show they are locked in.

## 4.3 The Shared Burden (Distributed Relics)

### Narrative Mechanic
- **Concept:** Relics are too volatile for one person. Physical contact with a Relic triggers a **Team-Wide Unlock**, granting a unique ability to every party member present.
- **The Loop:** Every crew member gains a new power instantly upon touching the Relic (or later via the ship's Array).
- **The Result:** The party shares the burden. Specific Relics unlock "Source Arts" (Ultimates) for specific characters, reinforcing their combat roles (see `Characters.md`). The Relic refracts through the user—e.g., The Tuning Fork (Force) gives Nova a Blast (External push), but gives Gh0st a Stomp (Internal weight).

### The Cooldown System (Tempo)
Instead of a shared resource pool (MP/SP), combat is governed by **Cooldowns (CD)** measured in turns. This shifts the focus from hoarding resources to managing tempo and rotation.
- **Standard Skills:** 0-2 Turn CD. Available frequently for core rotations.
- **Source Arts (Ultimates):** 3-5 Turn CD. High-impact abilities that must be timed for maximum effect.

### Elemental & Type Logic
- **Damage Types:** Physical (Kinetic, Thermal) vs. Source (Phase, Void).
- **Stacking:** Dealing Source damage builds "Instability" on enemies. At 3 stacks, the next physical hit triggers a **Disruption Break** (massive damage + status effect based on the element).

## 4.4 The Snack Slot (Combat Consumables)

Combat consumables are managed via a dedicated **Snack Slot** to streamline UI and prevent item spam.
- **Preparation:** Players equip one "Snack" (Consumable) to a dedicated slot per character.
- **Usage:** Snacks behave like Skills. They have a default **5-Turn Cooldown** to prevent reliance on healing spam.
- **Targeting:** Snacks are **Self-Only** (or AoE centered on self). Characters consume their own snack; they cannot feed others. This removes the need for a target selection menu, making usage instant.
- **Restocking:** Charges are refilled upon returning to the ship or resting at a safe zone.

## 4.5 Sprite Standards (Canonical)

Combat character sprites are defined in data as ASCII block grids: characters use a 5-row x 7-column grid; enemies use a 7-row x 11-column grid. These are stored in each character/enemy JSON file using filled block characters.

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

# 7) Progression and Skills

## 7.1 Open Skill Access System
Characters gradually unlock a suite of ~11 skills throughout the game. Unlike a loadout system, **all unlocked active skills are available in combat**.
*   **Combat Menu:** The "Skills" command opens a scrolling list (or categorized view) of all available abilities.
*   **Source Arts:** These are special high-impact skills listed prominently in the menu, governed by **long Cooldowns**.

## 7.2 The "10+ Skill" Progression Formula
The game targets a flexible kit size where each character builds a unique arsenal of ~10 permanent skills plus equipment bonuses.
1.  **Starting Kit (2 Skills):** Characters join with just two core abilities (e.g., a Basic Attack and a signature Class Skill).
2.  **Relic Skills (5 Skills):** The 5 Combat Relics grant **one unique skill to every party member**.
    *   **Thematic Adaptation:** The skill reflects how that character's archetype interprets the Relic's nature.
        *   *Example:* **The Tuning Fork** (Force/Disruption) grants Nova *Blast Wave* (AoE push), but grants Gh0st *Concussive Round* (Single-target stun).
    *   **Catch-Up:** If a character joins the party after a Relic has been acquired, they gain the skill by physically interacting with the Relic stored on the ship (Sync).
3.  **Quest Rewards (~3 Skills):** Additional specialized skills are awarded via Main Quests, Side Quests, or Character Arc milestones.
4.  **Equipment Skills (Temporary):** Certain high-tier Weapons, Armor, or Mods can grant an active skill while equipped, allowing players to "rent" powers for specific strategies.

## 7.3 Passive Traits
Passives are "always on" once unlocked and represent permanent character growth (e.g., stat boosts, resistances). These are typically rewards for leveling up or specific achievements.

# 8) Tutorials and Pacing (World 1, Hub 1 Plan)

World 1, Hub 1 tutorials should be delivered via a limited number of quests (not excessive). Covered topics include: movement, tapping highlighted words, NPC interaction, item usage, equipping gear, consumables vs equipment, combat consumables, shops/commerce, quest tracking, basic combat, group combat, Synergy skills, and tinkering. Deeper systems are pushed to Hub 2 or later.

# 9) Cinematics and Accessibility

Text-based cinematics are planned using animated 2D text and subtle visual effects (flickering lights, fades). Accessibility settings should exist for these sequences (e.g., reduce motion, reduce flicker, speed controls).

# 10) Visual Framing Rule (Backgrounds)

Background images follow a framing rule: canvas remains 1080x1920 (9:16), but add equal black vertical bars on left and right, each exactly 5/42 of the canvas width (~11.9048%), making the effective inner frame 9:21. Bars are pure black with no detail or text.