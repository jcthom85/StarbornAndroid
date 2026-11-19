package com.example.starborn.domain.leveling

/** Modest per-level base stat growth so every level-up (Lv 2–12) feels tangible without overshadowing gear or skill trees. */
object LevelStatGrowth {

    data class Growth(
        val hpBonus: Int = 0,
        val strength: Int = 0,
        val vitality: Int = 0,
        val agility: Int = 0,
        val focus: Int = 0
    )

    private const val HP_PER_LEVEL = 6

    fun forLevel(characterId: String, level: Int): Growth {
        val gained = (level - 1).coerceAtLeast(0)
        if (gained == 0) return Growth()
        val hpBonus = gained * HP_PER_LEVEL
        val primary = gained / 2 // +1 at Lv 3, 5, 7, 9, 11 (+5 max)
        val secondary = gained / 3 // +1 at Lv 4, 7, 10 (+3 max)
        return when (characterId.trim().lowercase()) {
            "nova" -> Growth(hpBonus = hpBonus, strength = secondary, agility = primary, focus = secondary)
            "zeke" -> Growth(hpBonus = hpBonus, strength = primary, vitality = secondary)
            "orion" -> Growth(hpBonus = hpBonus, agility = secondary, focus = primary)
            "gh0st" -> Growth(hpBonus = hpBonus, strength = secondary, agility = primary)
            else -> Growth(hpBonus = hpBonus, strength = secondary, agility = secondary, focus = secondary)
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
