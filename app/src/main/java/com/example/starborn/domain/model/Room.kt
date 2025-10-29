package com.example.starborn.domain.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class Room(
    val id: String,
    val env: String,
    val title: String,
    @Json(name = "background_image")
    val backgroundImage: String,
    val description: String,
    val npcs: List<String>,
    val items: List<String>,
    val enemies: List<String>,
    val connections: Map<String, String>,
    val pos: List<Int>,
    val state: Map<String, Any>,
    val actions: List<Map<String, Any?>>,
    @Json(name = "blocked_directions")
    val blockedDirections: Map<String, BlockedDirection>? = null,
    @Json(name = "item_flavor")
    val itemFlavor: Map<String, String>? = null,
    @Json(name = "enemy_flavor")
    val enemyFlavor: Map<String, String>? = null,
    val dark: Boolean? = null,
    @Json(name = "description_dark")
    val descriptionDark: String? = null,
    @Json(name = "title_options")
    val titleOptions: TitleOptions? = null
)

data class BlockedDirection(
    val type: String,
    @Json(name = "enemy_id")
    val enemyId: String? = null,
    val requires: List<Requirement>? = null,
    @Json(name = "key_id")
    val keyId: String? = null,
    val consume: Boolean? = null,
    @Json(name = "message_locked")
    val messageLocked: String? = null,
    @Json(name = "message_unlock")
    val messageUnlock: String? = null
)

data class Requirement(
    @Json(name = "room_id")
    val roomId: String,
    @Json(name = "state_key")
    val stateKey: String,
    val value: Boolean
)

data class TitleOptions(
    @Json(name = "underline_adjust")
    val underlineAdjust: Int? = null,
    @Json(name = "wrap_nudge")
    val wrapNudge: Int? = null
)
