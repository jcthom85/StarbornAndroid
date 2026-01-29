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

### **Elemental Roles (World 1 Teaching Focus)**
| Element | Primary Weakness Target | Logic / Lore |
| :--- | :--- | :--- |
| **Physical** | Soft/Unarmored Targets | Brute force physics. |
| **Shock** | Robotic / Electronic | Overloads circuits and internal sensors. |
| **Burn** | Biological / Freeze Environments | Rapidly destroys organic tissue. |
| **Freeze** | Overheated / Fragile Material | Halts movement; makes armor brittle. |
| **Acid** | Heavily Plated / Industrial | Breaks down molecular bonds and rusts metal. |
| **Source** | Hardened Tech / Source Beasts | Bypasses standard armor via frequency. |

**World 1 onboarding note:** Do not tutorialize every element at once. World 1 primarily teaches **Physical/Shock/Burn**, introduces **Acid** via an optional anti-armor lesson, and frames **Source** as a late/rare Relic tool. **Freeze** is held for later worlds.

### **The Reward: Harmonic Instability**
Weakness isn’t just a multiplier. It’s you landing the right hit in the right place, tuning the target, and gaining momentum.

*   **Tempo (Cooldown Momentum):** Landing a **Weakness (++)** hit (**200% Damage**) instantly reduces the attacker’s active Cooldowns by **1 Turn** (to a minimum of 0). You tuned the target and stole a step.
*   **Stagger (Stability Damage):** Weakness hits deal **2x** damage to the enemy’s hidden **Stability** meter, making them Break/Stagger faster.
*   **No Free Status:** Weakness hits do **not** automatically apply Status Effects. Those payloads must come from specific Skills.

---

# 2. Status Effects (Tactical Payloads)

Status effects are **not** automatically applied by every elemental hit. Instead, they are **Tactical Payloads** attached to specific **Skills**, **Snacks**, or **Weapon Mods**. This prevents "status spam" and makes Skill usage a calculated decision.

## World 1 Teaching Scope (Keep It Lean)
World 1 is a tutorial world. It should focus on reactive/brute-force payloads and one clear "puzzle key" mechanic.
*   **Teach in World 1:** Stun (Short/Concussed), Blind, Corrosion (Acid DoT), and Guard Break/Stagger.
*   **Delay to World 2+:** **Jammed** (Silence/caster denial) and **Marked** (setup → payoff / crit setup).

## A. Control & Denial (Applied by high-CD Skills)
*   **Stunned** (Organic, also called **Concussed**) / **Short** (Machine): Target skips their next turn. **UI:** In World 1, group these under a single **Stunned** icon to reduce visual clutter.
*   **Staggered (Guard Break):** Target is Guard Broken (shields/guard drop) and pushed to the bottom of the current turn order. This is a primary World 1 mechanic.
*   **Jammed (Silence):** Target cannot use Skills or special attacks. Basic attacks only.

## B. Strategic Debuffs (Setup for big hits)
*   **Exposed**: Reduces Defense to 0 for the next hit.
*   **Brittle**: Increases incoming **Physical** damage by +50%.
*   **Blind**: Reduces Target Accuracy by 50%.
*   **Marked:** The next incoming hit against this target is a guaranteed **Critical Hit**.

## C. Damage Over Time (Attrition)
*   **Bleeding**: Physical damage per turn (Organic only).
*   **Meltdown**: Burn damage per turn.
*   **Corrosion**: Acid damage that increases every turn.

## D. Player States (Buffs & Stances)
*   **Guarding**: -50% Damage taken. Prevents Stagger/Stun.
*   **Charged**: A telegraph state. The unit is winding up a powerful move.
*   **Regen**: Heals a percentage of Max HP every turn.
*   **Overdrive**: Increases Outgoing Damage by +50%. Used by mechs/constructs.

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
