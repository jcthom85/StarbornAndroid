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
    @Json(name = "title_gap") val titleGap: Int? = null
) {
    @JsonClass(generateAdapter = true)
    data class PositionHint(
        @Json(name = "center_x") val centerX: Float = 0.5f,
        @Json(name = "center_y") val centerY: Float = 0.5f
    )
}
