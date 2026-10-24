package com.example.starborn.debug

import com.example.starborn.di.AppServices

/** Rebuilt setup does not inherit the legacy full-inventory/checkpoint chains. */
object DebugFixtureBuilder {
    fun prepare(services: AppServices, procedure: DebugTestProcedure): Boolean {
        val fixture = procedure.fixture
        if (!services.startNewGame()) return false
        if (procedure.systemMode?.startsWith("crew_checkpoint_") == true) {
            val id = procedure.systemMode.orEmpty().removePrefix("crew_checkpoint_")
            val snapshot = services.worldDataSource.loadCrewCheckpoint(id) ?: return false
            services.clearDebugBootstrap()
            services.sessionStore.restore(snapshot)
            return true
        }
        if (fixture == DebugFixture.OPENING) {
            if (procedure.systemMode == "nova_builds") services.sessionStore.restore(
                services.sessionStore.state.value.copy(progressionPrototypeVersion = 1))
            return true
        }
        services.clearDebugBootstrap()
        if (fixture in setOf(DebugFixture.FISHING, DebugFixture.COOKING, DebugFixture.TINKERING, DebugFixture.SHOP)) {
            return DebugSystemFixtureBuilder.prepare(services, procedure)
        }
        if (fixture == DebugFixture.CAMPAIGN_CHECKPOINT) {
            return DebugCampaignFixtureBuilder.prepare(services, procedure)
        }
        if (fixture == DebugFixture.ARCADE_READY) {
            val cabinet = DebugArcadeCabinets.all.first { it.id == procedure.cabinetId }
            val store = services.sessionStore
            store.restore(store.state.value.copy(
                worldId = "world_astra", hubId = "hub_astra", roomId = "astra_common_room",
                activeQuests = emptySet(), trackedQuestId = null, questStageById = emptyMap(),
                questTasksCompleted = emptyMap(),
                completedMilestones = setOf("ms_w2_mq05_complete", cabinet.discoveryMilestone, cabinet.repairedMilestone),
                revealedNodes = setOf("astra_bridge_node"),
                unlockedNodes = setOf("astra_bridge_node"),
                visitedNodes = setOf("astra_bridge_node"),
                astraReturnWorldId = "world_3", astraReturnHubId = "hub_5_lower_city", astraReturnRoomId = "spire_vent_output",
                arcadeProgress = mapOf(cabinet.id to com.example.starborn.domain.session.ArcadeCabinetProgress(
                    discovered = true, repaired = true, installed = true))
            ))
            return true
        }
        // The recipe boundary needs a usable story context, not a partial opening.
        val baseline = services.worldDataSource.loadWorld1Checkpoint("w1_mq02") ?: return false
        val recipe = services.craftingService.tinkeringRecipes.first { it.id == "repair_cryo_inductor" }
        val stock = services.craftingService.ingredientsFor(recipe).toMutableMap()
        recipe.tools.forEach { stock[it] = maxOf(stock[it] ?: 0, 1) }
        if (fixture == DebugFixture.CRYO_MISSING) {
            check(stock.remove("scrap_metal") != null) { "Cryo recipe no longer requires scrap" }
        }
        val store = services.sessionStore
        store.restore(baseline.copy(
            equippedItems = emptyMap(),
            equippedWeapons = emptyMap(),
            equippedArmors = emptyMap(),
            unlockedWeapons = emptySet(),
            unlockedArmors = emptySet(),
            playerCredits = 0,
            inventory = stock
        ))
        services.inventoryService.restore(stock)
        return true
    }
}
