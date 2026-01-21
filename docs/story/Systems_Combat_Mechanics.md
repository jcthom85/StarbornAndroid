# Combat Mechanics: Elements, Statuses, and Cooldowns

**Status:** Locked
**Context:** World 1 (Mining Colony) & Global Core Mechanics

This document defines the core mechanical interactions of Starborn's combat system, specifically tailored to the "Cosmic Frequency" lore and the Mining Colony setting.

---

# 1. Elemental Damage & Weaknesses

In the style of classic JRPGs, Elements primarily function as **Damage Multipliers**. Every enemy has a "Resistance Profile" that dictates how they react to these six types.

### **The Multipliers**
*   **Weakness (++)**: 200% Damage. (Critical hit visuals/sound).
*   **Vantage (+)**: 150% Damage. (The standard "effective" hit).
*   **Neutral**: 100% Damage.
*   **Resistant (-)**: 50% Damage.
*   **Immune (X)**: 0 Damage.

### **Elemental Roles (World 1 Focus)**
| Element | Primary Weakness Target | Logic / Lore |
| :--- | :--- | :--- |
| **Physical** | Soft/Unarmored Targets | Brute force physics. |
| **Shock** | Robotic / Electronic | Overloads circuits and internal sensors. |
| **Burn** | Biological / Freeze Environments | Rapidly destroys organic tissue. |
| **Freeze** | Overheated / Fragile Material | Halts movement; makes armor brittle. |
| **Acid** | Heavily Plated / Industrial | Breaks down molecular bonds and rusts metal. |
| **Source** | Hardened Tech / Source Beasts | Bypasses standard armor via frequency. |

### **The Reward: Source Instability**
Weakness isn’t just a multiplier. It’s you landing the right hit in the right place and knocking the target out of tune.

*   **Tempo (Cooldown Momentum):** Landing a **Weakness (++)** hit instantly reduces the attacker’s active Cooldowns by **1 Turn** (to a minimum of 0). You tuned the target and stole a step.
*   **Stagger (Stability Damage):** Weakness hits deal **2x** damage to the enemy’s hidden **Stability** meter, making them Break/Stagger faster.
*   **No Free Status:** Weakness hits do **not** automatically apply Status Effects. If you want **Stun**, **Bleed**, **Brittle**, etc., they must come from a specific Skill, Snack, or Weapon Mod.

---

# 2. Status Effects (Tactical Payloads)

Status effects are **not** automatically applied by every elemental hit. Instead, they are **Tactical Payloads** attached to specific **Skills**, **Snacks**, or **Weapon Mods**. This prevents "status spam" and makes Skill usage a calculated decision.

## A. Control & Denial (Applied by high-CD Skills)
*   **Stunned** (General) / **Short** (Robotic): Target skips their next turn.
*   **Staggered**: Target is pushed to the bottom of the current turn order.
*   **Jammed**: Target is "Silenced"—cannot use Skills or special attacks.

## B. Strategic Debuffs (Setup for big hits)
*   **Exposed**: Reduces Defense to 0 for the next hit.
*   **Brittle**: Increases incoming **Physical** damage by +50%.
*   **Obscured**: Reduces Target Accuracy by 50% (Blind).
*   **Marked**: All incoming hits against this target are Critical.

## C. Damage Over Time (Attrition)
*   **Bleeding**: Physical damage per turn (Organic only).
*   **Meltdown**: Burn damage per turn.
*   **Erosion**: Acid damage that increases every turn.

## D. Player States (Buffs & Stances)
*   **Guarding**: -50% Damage taken. Prevents Stagger/Stun.
*   **Charged**: A telegraph state. The unit is winding up a powerful move.
*   **Regen**: Heals a percentage of Max HP every turn.

---

# 3. Cooldown Mechanics

Starborn uses a **Global Cooldown System** (Turns) instead of MP/Mana.

## A. Skills (Active Abilities)
*   **Structure:** Each character has 6 Core Skills and 6 Source Arts (unlocked over time).
*   **Cooldowns:** range from **1 to 5 Turns**.
    *   **CD 1:** Utility/Basic skills that can be used every turn.
    *   **CD 5:** Powerful tactical abilities with significant wait times.
    *   **Once per Battle:** Some ultimate Source Arts may be restricted to a single use per encounter.
*   **Start of Combat:**
    *   **Core Skills:** Start **READY** (0 CD).
    *   **Source Arts:** Start on **COOLDOWN** (usually 3 turns) to prevent immediate boss-nuking.

## B. Snacks (Combat Consumables)
*   **Slot:** The "Snack Slot" is a dedicated equipment slot (1 item type per battle).
*   **Mechanic:** Functionally a Skill that provides utility or healing.
*   **Cooldown:** **1 to 5 Turns**.
    *   **CD 1:** Light snacks (e.g., small focus boost) usable every turn.
    *   **CD 5:** Heavy meals or potent medical injectors.
    *   **Once per Battle:** High-tier consumables (e.g., full party revive) may be restricted to a single use per encounter.
*   **Uses:** Infinite (not consumed on use), allowing the player to build their strategy around a reliable "5th skill" rather than hoarding items.
