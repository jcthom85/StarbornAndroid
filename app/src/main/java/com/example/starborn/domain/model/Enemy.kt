package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Enemy(
    override val id: String,
    override val name: String,
    val tier: String = "common",
    val hp: Int,
    val strength: Int,
    val vitality: Int,
    val agility: Int,
    val focus: Int,
    val luck: Int,
    val speed: Int,
    val stability: Int = 100,
    val element: String = "none",
    val resistances: Resistances = Resistances(),
    val abilities: List<String> = emptyList(),
    val flavor: String = "",
    @Json(name = "xp_reward")
    val xpReward: Int,
    @Json(name = "credit_reward")
    val creditReward: Int,
    val drops: List<Drop>,
    override val description: String = "",
    val portrait: String? = null,
    val sprite: List<String> = emptyList(),
    val attack: Int? = null,
    @Json(name = "ap_reward")
    val apReward: Int? = null,
    val behavior: String? = null,
    @Json(name = "alert_delay")
    val alertDelay: Int? = null,
    val composite: CompositePart? = null
) : Entity(id, name, description)

@JsonClass(generateAdapter = true)
data class CompositePart(
    val group: String,
    val role: String? = null,
    @Json(name = "group_offset_x")
    val groupOffsetX: Float = 0f,
    @Json(name = "group_offset_y")
    val groupOffsetY: Float = 0f,
    @Json(name = "offset_x")
    val offsetX: Float = 0f,
    @Json(name = "offset_y")
    val offsetY: Float = 0f,
    @Json(name = "width_scale")
    val widthScale: Float = 1f,
    @Json(name = "height_scale")
    val heightScale: Float = 1f,
    val z: Float = 0f
)

@JsonClass(generateAdapter = true)
data class Resistances(
    val burn: Int? = null,
    val freeze: Int? = null,
    val shock: Int? = null,
    val acid: Int? = null,
    val radiation: Int? = null,
    val source: Int? = null,
    val physical: Int? = null
)

@JsonClass(generateAdapter = true)
data class Drop(
    val id: String,
    val chance: Double,
    @Json(name = "qty_min")
    val qtyMin: Int? = null,
    @Json(name = "qty_max")
    val qtyMax: Int? = null,
    val quantity: Int? = null
)
