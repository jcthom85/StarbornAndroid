package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HubNode(
    val id: String,
    @Json(name = "hub_id") val hubId: String,
    val title: String,
    @Json(name = "entry_room") val entryRoom: String,
    @Json(name = "icon_image") val iconImage: String? = null,
    @Json(name = "pos_hint") val position: PositionHint = PositionHint(),
    val size: List<Int> = emptyList(),
    val rooms: List<String> = emptyList(),
    val discovered: Boolean = false,
    @Json(name = "title_gap") val titleGap: Int? = null,
    @Json(name = "initial_visibility") val initialVisibility: String? = null,
    @Json(name = "entry_policy") val entryPolicy: String = "hub",
    @Json(name = "unlock_conditions") val unlockConditions: List<NodeRequirement> = emptyList(),
    @Json(name = "return_policy") val returnPolicy: String = "entry",
    @Json(name = "locked_message") val lockedMessage: String? = null
) {
    @JsonClass(generateAdapter = true)
    data class PositionHint(
        @Json(name = "center_x") val centerX: Float = 0.5f,
        @Json(name = "center_y") val centerY: Float = 0.5f
    )
}

@JsonClass(generateAdapter = true)
data class NodeRequirement(
    val type: String,
    val id: String? = null,
    @Json(name = "quest_id") val questId: String? = null,
    @Json(name = "task_id") val taskId: String? = null,
    @Json(name = "item_id") val itemId: String? = null
)
