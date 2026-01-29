# Systems: Combat AI

## Overview
Enemy AI is driven by **behaviors** (how it thinks), **roles** (what it is), and **context** (current fight state).
The system scores skill + target pairs and selects the highest‑value action each turn.

**Goals**
- Make enemy choices feel intentional, not random.
- Keep gameplay readable (telegraph high‑impact actions).
- Allow per‑enemy overrides without hard scripting.

---

## Behavior Profiles (Brain)
Each enemy resolves a behavior profile (via `combat_behavior`, or inferred if missing):

- **Aggressive**: favors burst damage and finishing low‑HP targets.
- **Defensive**: favors shields/guard and heals when threatened.
- **Trickster**: favors debuffs, blinds, and control.
- **Summoner**: prioritizes summon/support actions early, then defends.
- **Balanced**: default blend.

These profiles scale skill scores rather than hard‑locking actions.

---

## Roles (Body)
Roles can be declared per enemy (`combat_role`) or inferred from skills:

- **Striker**: raw damage.
- **Tank**: guard/shield/invulnerable kits.
- **Support**: heals/buffs/allies.
- **Controller**: debuffs/disruption.
- **Summoner**: summon or spawn kits.

Roles add another layer of weighting (e.g., Tanks prefer guard skills).

---

## Targeting Logic
Target selection is **score‑based**, not random:

- **Damage skills**: prefer low‑HP targets, weakness matches, and fragile foes.
- **Debuffs**: prefer high‑threat or evasive targets, and avoid already‑debuffed targets.
- **Guard‑break**: prefer shielded/guarding targets.
- **Support/Heals**: prefer lowest‑HP allies or allies missing buffs.

AoE skills are scored against **all valid targets** to estimate total value.

---

## Skill Scoring (Core)
Each skill gets a score based on:

1. **Impact** (base power or healing value)
2. **Elemental affinity** (weakness/resist)
3. **Status value** (new debuff/buff worth more)
4. **Behavior profile weight**
5. **Role weight**
6. **Diversity penalty** (avoid spamming same skill)

---

## Telegraphing (Readability)
High‑impact skills show a brief **telegraph cue** before execution.
This should:
- give the player a moment to react,
- call attention to the danger without pausing combat too long.

---

## Diversity & Memory
AI tracks recent skill use per enemy:
- repeating the same skill is penalized,
- encourages mix‑ups and varied pacing.

---

## Tier-Based Intelligence (Anticipation)
Not all enemies think alike. We separate "Trash Mobs" from "Elites/Bosses" to create distinct gameplay feels.

*   **Common Enemies (Reactive):** They only look at the current board state. They are impulsive and will often attack a player who is charging a laser, walking right into the trap. This makes the player feel powerful.
*   **Elites & Bosses (Proactive):** They have an **Anticipation** layer. They can sense hidden states like a player's `WeaponCharge`.
    *   *Logic:* If a player is `Charging`, Elites receive a massive score bonus to `Defend`.
    *   *Result:* They turtle up before a big hit, forcing the player to feint or break their guard.

---

## JSON Overrides
Per‑enemy overrides (optional):

```json
"combat_behavior": "aggressive",
"combat_role": "tank",
"broken_turns": 2
```

If omitted, behavior/role are inferred from skill kit and tier defaults.

