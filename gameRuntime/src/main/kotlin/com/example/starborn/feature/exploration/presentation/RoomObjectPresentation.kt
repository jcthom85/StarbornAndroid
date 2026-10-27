package com.example.starborn.feature.exploration.presentation

import com.example.starborn.domain.model.*

data class RoomObjectOffer(val action: RoomAction, val title: String, val detail: String, val confirmLabel: String)

/** Rest and cooking are explicit service controls; room prose remains descriptive. */
fun restStopServiceLabel(action: RestStopAction): String = if (action.cookSource != null) "Cook" else "Rest"

fun roomActionLabel(action: RoomAction): String = when (action) {
    is RestStopAction -> if (action.cookSource != null) "Cook at ${action.name}" else "Rest at ${action.name}"
    is ShopAction -> "Visit ${action.name}"
    is TinkeringAction -> "Use ${action.name}"
    is TuningPuzzleAction -> "Examine ${action.name}"
    is GenericAction -> when {
        action.type == "fishing" -> "Fish at ${action.name}"
        action.type.startsWith("arcade") -> "Examine ${action.name}"
        action.type in setOf("inspect", "generic") && action.actionEvent == null -> "Inspect ${action.name}"
        else -> action.name.replaceFirstChar { it.titlecase() }
    }
    else -> action.name.replaceFirstChar { it.titlecase() }
}

fun roomObjectOffer(action: RoomAction): RoomObjectOffer? {
    val (detail, verb) = when (action) {
        is RestStopAction -> if (action.cookSource != null) "Prepare a meal with your available ingredients." to "Cook"
            else "A place to rest and recover the party's HP." to "Rest"
        is ShopAction -> "Browse the goods and choose what to buy or sell." to "Browse shop"
        is TinkeringAction -> "Inspect your recipes, tools and available materials." to "Open workbench"
        is TuningPuzzleAction -> "Examine the mechanism and its controls." to "Examine"
        is GenericAction -> when {
            action.type == "fishing" -> "Choose your gear and fish here." to "Fish"
            action.type == "arcade_discovery" -> (action.conditionUnmetMessage ?: "Examine the damaged equipment and any parts worth salvaging.") to "Examine"
            action.type.startsWith("arcade") -> "Examine this cabinet and its available game." to "Open cabinet"
            else -> return null
        }
        else -> return null
    }
    return RoomObjectOffer(action, action.name.replaceFirstChar { it.titlecase() }, detail, verb)
}
