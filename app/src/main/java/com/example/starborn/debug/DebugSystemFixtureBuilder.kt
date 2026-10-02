package com.example.starborn.debug

import com.example.starborn.di.AppServices
import org.json.JSONObject

object DebugSystemFixtureBuilder {
    fun prepare(services: AppServices, procedure: DebugTestProcedure): Boolean {
        val fishing = procedure.fixture == DebugFixture.FISHING
        val mode = procedure.systemMode
        val cooking = procedure.fixture == DebugFixture.COOKING
        val roomId = when {
            fishing -> DebugSystemScenarios.fishingLocations.first { it.zone == procedure.targetId }.room
            cooking && mode == "discovery" -> "sector9_stream_falls"
            cooking && mode == "combat" -> "sector9_beach_pools"
            else -> "astra_common_room"
        }
        val room = services.worldDataSource.loadRooms().first { it.id == roomId }
        val node = services.worldDataSource.loadHubNodes().single { roomId in it.rooms || roomId == it.entryRoom }
        val hub = services.worldDataSource.loadHubs().first { it.id == node.hubId }
        val stock = mutableMapOf<String, Int>()
        val milestones = mutableSetOf<String>()
        val schematics = mutableSetOf<String>()
        val roomStates = mutableMapOf<String, Map<String, Boolean>>()
        var equippedItems = emptyMap<String, String>()
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
            if (roomId == "astra_common_room") milestones.add("ms_w2_mq05_complete")
            if (procedure.fixture == DebugFixture.SHOP) {
                check(services.shopRepository.shopById(procedure.targetId) != null) { "Unknown shop ${procedure.targetId}" }
                stock.putAll(mapOf("scrap_metal" to 3, "ration_pack" to 3, "item_arcade_token" to 5))
            } else if (cooking) {
                services.craftingService.cookingRecipes.forEach { recipe ->
                    recipe.ingredients.forEach { (id, count) -> stock[id] = (stock[id] ?: 0) + count * 3 }
                    if (mode != "discovery") recipe.discoveryRoom?.let {
                        roomStates[it] = mapOf("cooking_discovered" to true)
                    }
                    if (mode == "combat") stock[recipe.result] = if (recipe.category == "snack") 1 else 3
                }
                if (mode == "combat") {
                    stock["medkit_i"] = 6
                    stock["functional_cryo_inductor"] = 1
                    equippedItems = mapOf("nova:snack" to "mineral_trail_mix")
                    milestones.add("ms_w2_pod_examined")
                }
            } else if (mode == "discovery") {
                val recipe = services.craftingService.tinkeringRecipes.single { it.id == "mod_source_resin" }
                stock.putAll(services.craftingService.ingredientsFor(recipe))
                recipe.tools.forEach { stock[it] = maxOf(stock[it] ?: 0, 1) }
                stock["schematic_source_resin"] = 1
            } else if (mode == "boundaries") {
                milestones.add("ms_w1_mq01_complete") // Make the equipped mod slot visible in Party.
                schematics.addAll(services.craftingService.tinkeringRecipes.map { it.id })
                stock.putAll(mapOf("power_lens_mk_i" to 1, "ergonomic_grip" to 1, "functional_cryo_inductor" to 1))
                equippedItems = mapOf("nova:weapon_mod1" to "power_lens_mk_i")
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
        val party = if (cooking) listOf("nova", "zeke", "orion", "gh0st") else listOf("nova")
        val store = services.sessionStore
        store.restore(store.state.value.copy(
            worldId = hub.worldId, hubId = hub.id, roomId = room.id,
            activeQuests = emptySet(), trackedQuestId = null, questStageById = emptyMap(), questTasksCompleted = emptyMap(),
            completedMilestones = milestones, learnedSchematics = schematics,
            playerCredits = if (procedure.fixture == DebugFixture.SHOP && !procedure.emptyWallet) 10_000 else 0,
            partyMembers = party, partyMemberHp = if (cooking && mode != "combat") party.associateWith { 1 } else emptyMap(),
            playerLevel = if (mode == "combat") 3 else 1,
            partyMemberLevels = party.associateWith { if (mode == "combat") 3 else 1 }, partyMemberXp = party.associateWith { 0 },
            revealedNodes = setOf(node.id), unlockedNodes = setOf(node.id), visitedNodes = setOf(node.id), inventory = stock,
            equippedItems = equippedItems, roomStates = roomStates
        ))
        if (roomId == "astra_common_room") {
            store.setAstraReturnLocation("world_3", "hub_5_lower_city", "spire_vent_output")
        }
        services.inventoryService.restore(stock)
        return true
    }
}
