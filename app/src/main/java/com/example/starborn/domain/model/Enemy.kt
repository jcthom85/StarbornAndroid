package com.example.starborn.domain.model

import com.squareup.moshi.Json

data class Enemy(
    override val id: String,
    override val name: String,
    val tier: String,
    val hp: Int,
    val strength: Int,
    val vitality: Int,
    val agility: Int,
    val focus: Int,
    val luck: Int,
    val speed: Int,
    val element: String,
    val resistances: Resistances,
    val abilities: List<String>,
    val flavor: String,
    @Json(name = "xp_reward")
    val xpReward: Int,
    @Json(name = "credit_reward")
    val creditReward: Int,
    val drops: List<Drop>,
    override val description: String,
    val portrait: String,
    val sprite: List<String>,
    val attack: Int,
    @Json(name = "ap_reward")
    val apReward: Int,
    val behavior: String? = null,
    @Json(name = "alert_delay")
    val alertDelay: Int? = null
) : Entity(id, name, description)

data class Resistances(
    val fire: Int? = null,
    val ice: Int? = null,
    val lightning: Int? = null,
    val poison: Int? = null,
    val radiation: Int? = null,
    val psychic: Int? = null,
    val void: Int? = null,
    val physical: Int? = null
)

data class Drop(
    val id: String,
    val chance: Double,
    @Json(name = "qty_min")
    val qtyMin: Int? = null,
    @Json(name = "qty_max")
    val qtyMax: Int? = null,
    val quantity: Int? = null
)
