package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NodeTransition(
    val id: String,
    @Json(name = "from_node") val fromNode: String,
    @Json(name = "from_room") val fromRoom: String,
    val direction: String,
    @Json(name = "to_node") val toNode: String,
    @Json(name = "to_room") val toRoom: String,
    @Json(name = "transition_text") val transitionText: String? = null
)

fun NodeTransition.edgeKey(): String = "${fromRoom.trim().lowercase()}::${direction.trim().lowercase()}"
