package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class Skill(
    val id: String,
    val name: String,
    val character: String,
    val type: String,
    @Json(name = "base_power")
    val basePower: Int,
    val cooldown: Int,
    val description: String,
    @Json(name = "max_rank")
    val maxRank: Int? = null,
    @Json(name = "combat_tags")
    val combatTags: List<String>? = null,
    val scaling: String? = null
)
