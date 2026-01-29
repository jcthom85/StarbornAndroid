package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatusDefinition(
    val id: String,
    val name: String? = null,
    @Json(name = "display_name")
    val displayName: String? = null,
    @Json(name = "default_duration")
    val defaultDuration: Int? = null,
    @Json(name = "skip_reason")
    val skipReason: String? = null,
    val tick: StatusTick? = null,
    @Json(name = "outgoing_multiplier")
    val outgoingMultiplier: Double? = null,
    @Json(name = "accuracy_multiplier")
    val accuracyMultiplier: Double? = null,
    @Json(name = "incoming_multiplier")
    val incomingMultiplier: Double? = null,
    @Json(name = "incoming_multiplier_physical")
    val incomingMultiplierPhysical: Double? = null,
    @Json(name = "flat_defense_bonus")
    val flatDefenseBonus: Int? = null,
    @Json(name = "block_skills")
    val blockSkills: Boolean? = null,
    @Json(name = "force_crit")
    val forceCrit: Boolean? = null,
    @Json(name = "defense_multiplier")
    val defenseMultiplier: Double? = null,
    val target: String? = null,
    val aliases: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class StatusTick(
    val mode: String,
    @Json(name = "per_max_hp_divisor")
    val perMaxHpDivisor: Int? = null,
    val amount: Int? = null,
    val element: String? = null,
    val min: Int? = null
)
