package com.example.starborn.domain.leveling

import com.squareup.moshi.Json
import java.io.Serializable

data class ProgressionData(
    @Json(name = "level_up_skills")
    val levelUpSkills: Map<String, Map<String, String>> = emptyMap()
) : Serializable

data class LevelUpSummary(
    val characterId: String,
    val characterName: String,
    val levelsGained: Int,
    val newLevel: Int,
    val unlockedSkills: List<SkillUnlockSummary>,
    val statChanges: List<StatDeltaSummary> = emptyList()
) : Serializable

data class SkillUnlockSummary(
    val id: String,
    val name: String
) : Serializable

data class StatDeltaSummary(
    val label: String,
    val value: String
) : Serializable
