package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.domain.model.*
import com.example.starborn.feature.exploration.viewmodel.MinimapService

/** Map labels describe what the interaction actually offers. */
fun mapServicesForActions(actions: List<RoomAction>, isHubExit: Boolean, allowRecreationShop: Boolean): Set<MinimapService> = buildSet {
    if (isHubExit) add(MinimapService.EXIT)
    actions.forEach { action -> when (action) {
        is ShopAction -> add(MinimapService.SHOP)
        is GenericAction -> if (action.actionEvent == "astra_recreation" && allowRecreationShop) add(MinimapService.SHOP)
        is TinkeringAction -> add(MinimapService.TINKERING)
        is RestStopAction -> {
            add(MinimapService.REST)
            if (!action.cookSource.isNullOrBlank()) add(MinimapService.COOKING)
        }
        else -> Unit
    } }
}
