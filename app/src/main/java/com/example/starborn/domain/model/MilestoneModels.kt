package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MilestoneDefinition(
    val id: String,
    val name: String = "",
    val description: String? = null,
    val category: String? = null,
    @Json(name = "toast")
    val toastMessage: String? = null,
    @Json(name = "icon")
    val iconPath: String? = null,
    val trigger: MilestoneTrigger? = null,
    val effects: MilestoneEffects? = null
)

@JsonClass(generateAdapter = true)
data class MilestoneTrigger(
    val type: String,
    @Json(name = "quest_id")
    val questId: String? = null,
    @Json(name = "event_id")
    val eventId: String? = null,
    @Json(name = "battle_id")
    val battleId: String? = null
) {
    fun targetId(): String? = questId ?: eventId ?: battleId
}

@JsonClass(generateAdapter = true)
data class MilestoneEffects(
    @Json(name = "unlock_areas")
    val unlockAreas: List<String>? = null,
    @Json(name = "unlock_abilities")
    val unlockAbilities: List<String>? = null,
    @Json(name = "unlock_exits")
    val unlockExits: List<MilestoneExitUnlock>? = null
)

@JsonClass(generateAdapter = true)
data class MilestoneExitUnlock(
    @Json(name = "room_id")
    val roomId: String?,
    val direction: String?
)
