package com.example.starborn.domain.leveling

class LevelingManager(
    private val data: LevelingData
) {
    private val levels: List<LevelThreshold> = data.levelCurve
        .mapNotNull { (levelStr, requiredXp) ->
            levelStr.toIntOrNull()?.let { level -> LevelThreshold(level, requiredXp) }
        }
        .sortedBy(LevelThreshold::level)

    val maxLevel: Int = levels.maxOfOrNull(LevelThreshold::level) ?: 1

    fun levelForXp(totalXp: Int): Int {
        if (levels.isEmpty()) return 1
        var current = 1
        for (threshold in levels) {
            if (totalXp >= threshold.requiredXp) {
                current = threshold.level
            } else {
                break
            }
        }
        return current
    }

    fun xpForSource(source: String, rarity: String): Int {
        return data.xpSources[source]?.get(rarity) ?: 0
    }

    fun levelBounds(level: Int): Pair<Int, Int?> {
        if (levels.isEmpty()) return 0 to null
        val current = levels.firstOrNull { it.level == level } ?: return 0 to null
        val next = levels.firstOrNull { it.level == level + 1 }
        return current.requiredXp to next?.requiredXp
    }
}
