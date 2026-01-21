package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class Skill(
    val id: String,
    val name: String,
    val character: String,
    val type: String,
    @Json(name = "base_power")
    val basePower: Int,
    val cooldown: Int = 0,
    val description: String,
    @Json(name = "max_rank")
    val maxRank: Int? = null,
    @Json(name = "combat_tags")
    val combatTags: List<String>? = null,
    val scaling: String? = null,
    @Json(name = "status_applications")
    val statusApplications: List<String>? = null,
    @Json(name = "uses_per_battle")
    val usesPerBattle: Int? = null,
    val conditions: List<String>? = null
)
