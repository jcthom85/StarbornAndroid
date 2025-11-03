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
    val shopId: String? = null,
    @Json(name = "condition_unmet_message")
    val conditionUnmetMessage: String? = null
) : RoomAction

data class CookingAction(
    override val name: String,
    @Json(name = "station_id")
    val stationId: String? = null,
    @Json(name = "requires_milestones")
    val requiresMilestones: List<String>? = null,
    @Json(name = "requires_milestone")
    val requiresMilestone: String? = null,
    @Json(name = "condition_unmet_message")
    val conditionUnmetMessage: String? = null,
    @Json(name = "action_event")
    val actionEvent: String? = null
) : RoomAction

data class FirstAidAction(
    override val name: String,
    @Json(name = "station_id")
    val stationId: String? = null,
    @Json(name = "requires_milestones")
    val requiresMilestones: List<String>? = null,
    @Json(name = "requires_milestone")
    val requiresMilestone: String? = null,
    @Json(name = "condition_unmet_message")
    val conditionUnmetMessage: String? = null,
    @Json(name = "action_event")
    val actionEvent: String? = null
) : RoomAction

data class ShopAction(
    override val name: String,
    @Json(name = "shop_id")
    val shopId: String?,
    @Json(name = "requires_milestones")
    val requiresMilestones: List<String>? = null,
    @Json(name = "requires_milestone")
    val requiresMilestone: String? = null,
    @Json(name = "action_event")
    val actionEvent: String? = null,
    @Json(name = "condition_unmet_message")
    val conditionUnmetMessage: String? = null
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

fun RoomAction.actionKey(): String = when (this) {
    is ToggleAction -> listOf("toggle", stateKey, actionEventOn, actionEventOff, name).joinToString(":")
    is TinkeringAction -> listOf("tinkering", shopId, name).joinToString(":")
    is CookingAction -> listOf("cooking", stationId, actionEvent, name).joinToString(":")
    is FirstAidAction -> listOf("first_aid", stationId, actionEvent, name).joinToString(":")
    is ShopAction -> listOf("shop", shopId, actionEvent, name).joinToString(":")
    is ContainerAction -> listOf("container", stateKey, actionEvent, name).joinToString(":")
    is GenericAction -> listOf("generic", type, actionEvent, zoneId, name).joinToString(":")
}

fun RoomAction.serviceTag(): String? = when (this) {
    is TinkeringAction -> "Tinkering"
    is CookingAction -> "Cooking"
    is FirstAidAction -> "First Aid"
    is ShopAction -> "Shop"
    else -> null
}
