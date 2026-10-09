package com.example.starborn.domain.leveling

import com.squareup.moshi.Json

data class LevelingData(
    @Json(name = "level_curve")
    val levelCurve: Map<String, Int> = emptyMap(),
    @Json(name = "xp_sources")
    val xpSources: Map<String, Map<String, Int>> = emptyMap()
)

data class LevelThreshold(
    val level: Int,
    val requiredXp: Int
)
