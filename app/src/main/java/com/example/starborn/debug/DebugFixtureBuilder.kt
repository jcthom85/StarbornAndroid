package com.example.starborn.debug

import com.example.starborn.di.AppServices

/** Rebuilt setup does not inherit the legacy full-inventory/checkpoint chains. */
object DebugFixtureBuilder {
    fun prepare(services: AppServices, procedure: DebugTestProcedure): Boolean {
        val fixture = procedure.fixture
        if (!services.startNewGame()) return false
        if (fixture == DebugFixture.OPENING) return true
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
        val quest = requireNotNull(services.questRepository.questById("w1_mq01"))
        val stage = quest.stages.first { it.id == "report_to_jed" }
        check(stage.tasks.any { it.id == "use_tinkering_table" }) { "Opening crafting checkpoint has changed" }
        val completedTasks = quest.stages.takeWhile { it.id != stage.id }.flatMap { it.tasks }.map { it.id } +
            stage.tasks.takeWhile { it.id != "use_tinkering_table" }.map { it.id }
        val recipe = services.craftingService.tinkeringRecipes.first { it.id == "repair_cryo_inductor" }
        val stock = services.craftingService.ingredientsFor(recipe).toMutableMap()
        recipe.tools.forEach { stock[it] = maxOf(stock[it] ?: 0, 1) }
        if (fixture == DebugFixture.CRYO_MISSING) {
            check(stock.remove("scrap_metal") != null) { "Cryo recipe no longer requires scrap" }
        }
        val store = services.sessionStore
        store.restore(store.state.value.copy(
            roomId = "workshop_floor",
            questStageById = mapOf(quest.id to stage.id),
            questTasksCompleted = mapOf(quest.id to completedTasks.toSet()),
            completedMilestones = setOf("ms_w1_mq01_jed_talked", "ms_w1_mq01_workshop_briefed",
                "ms_w1_mq01_loader_inspected", "ms_w1_mq01_loader_cleared"),
            roomStates = mapOf("workshop_yard" to mapOf("loader_cleared" to true)),
            visitedNodes = setOf("pit", "workshop"),
            revealedNodes = setOf("pit", "workshop"),
            unlockedNodes = setOf("pit", "workshop"),
            inventory = stock
        ))
        services.inventoryService.restore(stock)
        return true
    }
}
