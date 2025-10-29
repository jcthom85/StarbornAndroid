package com.example.starborn.domain.model

import com.squareup.moshi.Json

sealed interface RoomAction {
    val name: String
}

data class ToggleAction(
    override val name: String,
    @Json(name = "state_key")
    val stateKey: String,
    @Json(name = "popup_title")
    val popupTitle: String?,
    @Json(name = "label_on")
    val labelOn: String,
    @Json(name = "label_off")
    val labelOff: String,
    @Json(name = "action_event_on")
    val actionEventOn: String?,
    @Json(name = "action_event_off")
    val actionEventOff: String?
) : RoomAction

data class TinkeringAction(
    override val name: String,
    @Json(name = "shop_id")
    val shopId: String? = null
) : RoomAction

data class ContainerAction(
    override val name: String,
    @Json(name = "state_key")
    val stateKey: String?,
    val items: List<String> = emptyList(),
    @Json(name = "action_event")
    val actionEvent: String? = null,
    @Json(name = "already_open_message")
    val alreadyOpenMessage: String? = null,
    @Json(name = "popup_title")
    val popupTitle: String? = null
) : RoomAction

data class GenericAction(
    override val name: String,
    val type: String,
    @Json(name = "action_event")
    val actionEvent: String? = null,
    @Json(name = "zone_id")
    val zoneId: String? = null,
    @Json(name = "condition_unmet_message")
    val conditionUnmetMessage: String? = null
) : RoomAction
