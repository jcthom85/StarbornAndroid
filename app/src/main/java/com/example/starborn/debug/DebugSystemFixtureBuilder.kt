package com.example.starborn.debug

import com.example.starborn.di.AppServices
import org.json.JSONObject

object DebugSystemFixtureBuilder {
    fun prepare(services: AppServices, procedure: DebugTestProcedure): Boolean {
        val fishing = procedure.fixture == DebugFixture.FISHING
        val roomId = if (fishing) DebugSystemScenarios.fishingLocations.first { it.zone == procedure.targetId }.room
            else "astra_common_room"
        val room = services.worldDataSource.loadRooms().first { it.id == roomId }
        val node = services.worldDataSource.loadHubNodes().single { roomId in it.rooms || roomId == it.entryRoom }
        val hub = services.worldDataSource.loadHubs().first { it.id == node.hubId }
        val stock = mutableMapOf<String, Int>()
        val milestones = mutableSetOf<String>()
        val schematics = mutableSetOf<String>()
        if (fishing) {
            // Read gear from the same authored catalog as FishingService, not an obsolete hand-maintained list.
            val data = JSONObject(services.readDebugAsset("recipes_fishing.json"))
            listOf("rods", "lures").forEach { key ->
                val gear = data.getJSONArray(key)
                repeat(gear.length()) { stock[gear.getJSONObject(it).getString("id")] = 1 }
            }
            val action = room.actions.single { it["type"] == "fishing" && it["zone_id"] == procedure.targetId }
            (action["requires_milestone"] as? String)?.let(milestones::add)
            (action["requires_milestones"] as? List<*>)?.filterIsInstance<String>()?.let(milestones::addAll)
            check(services.fishingService.getFishingZone(requireNotNull(procedure.targetId)) != null)
        } else {
            milestones.add("ms_w2_mq05_complete")
            if (procedure.fixture == DebugFixture.SHOP) {
                check(services.shopRepository.shopById(procedure.targetId) != null) { "Unknown shop ${procedure.targetId}" }
                stock.putAll(mapOf("scrap_metal" to 3, "ration_pack" to 3, "item_arcade_token" to 5))
            } else if (procedure.fixture == DebugFixture.COOKING) {
                services.craftingService.cookingRecipes.forEach { recipe ->
                    recipe.ingredients.forEach { (id, count) -> stock[id] = (stock[id] ?: 0) + count * 3 }
                }
            } else {
                services.craftingService.tinkeringRecipes.forEach { recipe ->
                    schematics.add(recipe.id)
                    services.craftingService.ingredientsFor(recipe).forEach { (id, count) ->
                        stock[id] = (stock[id] ?: 0) + count * 3
                    }
                    recipe.tools.forEach { stock[it] = maxOf(stock[it] ?: 0, 1) }
                }
            }
        }
        val party = if (procedure.fixture == DebugFixture.COOKING) listOf("nova", "zeke", "orion", "gh0st") else listOf("nova")
        val store = services.sessionStore
        store.restore(store.state.value.copy(
            worldId = hub.worldId, hubId = hub.id, roomId = room.id,
            activeQuests = emptySet(), trackedQuestId = null, questStageById = emptyMap(), questTasksCompleted = emptyMap(),
            completedMilestones = milestones, learnedSchematics = schematics,
            playerCredits = if (procedure.fixture == DebugFixture.SHOP && !procedure.emptyWallet) 10_000 else 0,
            partyMembers = party, partyMemberHp = if (procedure.fixture == DebugFixture.COOKING) party.associateWith { 1 } else emptyMap(),
            partyMemberLevels = party.associateWith { 1 }, partyMemberXp = party.associateWith { 0 },
            revealedNodes = setOf(node.id), unlockedNodes = setOf(node.id), visitedNodes = setOf(node.id), inventory = stock
        ))
        if (!fishing) {
            store.setAstraReturnLocation("world_3", "hub_5_lower_city", "spire_vent_output")
        }
        services.inventoryService.restore(stock)
        return true
    }
}
