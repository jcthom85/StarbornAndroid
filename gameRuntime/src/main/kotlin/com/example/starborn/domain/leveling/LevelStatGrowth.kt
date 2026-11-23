package com.example.starborn.domain.leveling

/** Tangible per-level RPG stat growth (Lv 2–12) so every level-up visibly lifts HP and combat numbers alongside gear tiers. */
object LevelStatGrowth {

    data class Growth(
        val hpBonus: Int = 0,
        val strength: Int = 0,
        val vitality: Int = 0,
        val agility: Int = 0,
        val focus: Int = 0
    )

    private const val DEFAULT_HP_PER_LEVEL = 24

    fun forLevel(characterId: String, level: Int): Growth {
        val gained = (level - 1).coerceAtLeast(0)
        if (gained == 0) return Growth()
        val primary = gained * 2     // +2 every level (+22 by Lv 12)
        val secondary = gained       // +1 every level (+11 by Lv 12)
        val tertiary = gained / 2    // +1 every 2 levels (+5 by Lv 12)
        return when (characterId.trim().lowercase()) {
            "nova" -> Growth(
                hpBonus = gained * 24,
                strength = primary,
                vitality = tertiary,
                agility = secondary,
                focus = secondary
            )
            "zeke" -> Growth(
                hpBonus = gained * 28,
                strength = primary,
                vitality = secondary,
                agility = gained / 3,
                focus = gained / 3
            )
            "orion" -> Growth(
                hpBonus = gained * 22,
                strength = gained / 3,
                vitality = tertiary,
                agility = tertiary,
                focus = primary
            )
            "gh0st" -> Growth(
                hpBonus = gained * 22,
                strength = primary,
                vitality = tertiary,
                agility = secondary,
                focus = secondary
            )
            else -> Growth(
                hpBonus = gained * DEFAULT_HP_PER_LEVEL,
                strength = secondary,
                vitality = tertiary,
                agility = tertiary,
                focus = secondary
            )
        }
    }

    fun deltaSummary(characterId: String, fromLevel: Int, toLevel: Int): List<StatDeltaSummary> {
        if (toLevel <= fromLevel) return emptyList()
        val before = forLevel(characterId, fromLevel)
        val after = forLevel(characterId, toLevel)
        val hpDelta = (after.hpBonus - before.hpBonus) + (after.vitality - before.vitality) * 10
        val strDelta = after.strength - before.strength
        val vitDelta = after.vitality - before.vitality
        val agiDelta = after.agility - before.agility
        val focDelta = after.focus - before.focus
        return buildList {
            if (hpDelta > 0) add(StatDeltaSummary("Max HP", "+$hpDelta"))
            if (strDelta > 0) add(StatDeltaSummary("Strength", "+$strDelta"))
            if (vitDelta > 0) add(StatDeltaSummary("Vitality", "+$vitDelta"))
            if (agiDelta > 0) add(StatDeltaSummary("Agility", "+$agiDelta"))
            if (focDelta > 0) add(StatDeltaSummary("Focus", "+$focDelta"))
        }
    }
}
