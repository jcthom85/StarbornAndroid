package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Enemy(
    override val id: String,
    override val name: String,
    val tier: String = "common",
    @Json(name = "combat_size") val combatSize: String = "medium",
    @Json(name = "combat_width") val combatWidth: Float = 1f,
    val hp: Int,
    val strength: Int,
    val vitality: Int,
    val agility: Int,
    val focus: Int,
    val luck: Int,
    val speed: Int,
    val stability: Int? = null,
    @Json(name = "broken_turns")
    val brokenTurns: Int? = null,
    @Json(name = "combat_behavior")
    val combatBehavior: String? = null,
    @Json(name = "combat_role")
    val combatRole: String? = null,
    val element: String = "none",
    val resistances: Resistances = Resistances(),
    val tags: List<String> = emptyList(),
    val abilities: List<String> = emptyList(),
    @Json(name = "recovery_after")
    val recoveryAfter: Map<String, String> = emptyMap(),
    @Json(name = "opening_skill")
    val openingSkill: String? = null,
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
    val composite: CompositePart? = null,
    val authored: AuthoredEnemyPattern? = null
) : Entity(id, name, description)

/** Opt-in encounter direction. Unconfigured enemies retain their existing AI. */
data class AuthoredEnemyPattern(
    val cycle: List<String> = emptyList(),
    val windups: Map<String, String> = emptyMap(),
    @Json(name = "duplicate_cycle_offset") val duplicateCycleOffset: Int = 0,
    @Json(name = "interrupt_on_damage") val interruptOnDamage: Set<String> = emptySet(),
    @Json(name = "resource_labels") val resourceLabels: Map<String, String> = emptyMap(),
    @Json(name = "linked_to") val linkedTo: String? = null,
    @Json(name = "linked_actions") val linkedActions: Map<String, String> = emptyMap(),
    @Json(name = "disarmed_cycle") val disarmedCycle: List<String> = emptyList(),
    val phase: AuthoredEnemyPhase? = null,
    val reinforcements: AuthoredReinforcements? = null
)

data class AuthoredEnemyPhase(
    @Json(name = "hp_below") val hpBelow: Double,
    val name: String,
    @Json(name = "opening_skill") val openingSkill: String,
    val cycle: List<String>,
    @Json(name = "remove_statuses") val removeStatuses: List<String> = emptyList()
)

data class AuthoredReinforcements(
    @Json(name = "skill_id") val skillId: String,
    val pool: List<String>,
    @Json(name = "max_alive") val maxAlive: Int = 2
)

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
