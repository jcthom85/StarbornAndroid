package com.example.starborn.debug

import com.example.starborn.di.AppServices

/**
 * Builds realistic, verified campaign checkpoints across all 6 worlds and side quests.
 * Sets believable progression, inventory, party members, skills, and prerequisite milestones.
 */
object DebugCampaignFixtureBuilder {

    fun prepare(services: AppServices, procedure: DebugTestProcedure): Boolean {
        val questId = procedure.targetId ?: return false
        val quest = services.questRepository.questById(questId) ?: return false
        val roomId = DebugCampaignScenarios.startRooms[questId] ?: return false
        val room = services.worldDataSource.loadRooms().firstOrNull { it.id == roomId } ?: return false
        val node = services.worldDataSource.loadHubNodes().firstOrNull { roomId in it.rooms || roomId == it.entryRoom } ?: return false
        val hub = services.worldDataSource.loadHubs().firstOrNull { it.id == node.hubId } ?: return false
        val worldNum = when {
            hub.worldId == "world_1" -> 1
            hub.worldId == "world_2" -> 2
            hub.worldId == "world_3" -> 3
            hub.worldId == "world_4" -> 4
            hub.worldId == "world_5" -> 5
            hub.worldId == "world_6" -> 6
            else -> 1
        }

        val party = when {
            worldNum >= 3 -> listOf("nova", "zeke", "orion", "gh0st")
            worldNum == 2 -> if (questId in setOf("w2_mq04", "w2_mq05", "w2_sq04", "w2_sq05")) {
                listOf("nova", "zeke", "orion")
            } else {
                listOf("nova", "zeke")
            }
            else -> listOf("nova")
        }

        val skills = mutableSetOf("nova_arc_tether")
        if (worldNum >= 2) skills.addAll(listOf("nova_cryo_vent", "zeke_shatter_blow"))
        if (worldNum >= 3 || (worldNum == 2 && party.contains("orion"))) skills.add("orion_prism_lance")
        if (worldNum >= 3) skills.addAll(listOf("gh0st_headshot", "nova_blast_wave", "source_art_scan"))
        if (worldNum >= 4) skills.addAll(listOf("gh0st_phase_counter", "source_art_construct"))
        if (worldNum >= 5) skills.add("source_art_stasis")
        if (worldNum >= 6) skills.add("source_art_tune_world")

        val milestones = mutableSetOf<String>()
        // Prior worlds completion milestones
        if (worldNum >= 2) {
            (1..5).forEach { milestones.add("ms_w1_mq0$it" + "_complete") }
            milestones.add("ms_w2_access_unlocked")
        }
        if (worldNum >= 3) {
            (1..5).forEach { milestones.add("ms_w2_mq0$it" + "_complete") }
            milestones.add("ms_w3_access_unlocked")
        }
        if (worldNum >= 4) {
            (11..15).forEach { milestones.add("ms_w3_mq$it" + "_complete") }
            milestones.add("ms_w4_access_unlocked")
        }
        if (worldNum >= 5) {
            (16..20).forEach { milestones.add("ms_w4_mq$it" + "_complete") }
            milestones.add("ms_w5_access_unlocked")
        }
        if (worldNum >= 6) {
            (21..25).forEach { milestones.add("ms_w5_mq$it" + "_complete") }
            milestones.add("ms_w6_access_unlocked")
        }

        // Specific prerequisites within current world main quests
        if (questId == "w1_mq02") {
            milestones.addAll(listOf("ms_w1_mq01_complete", "ms_w1_mq01_jed_talked", "ms_w1_mq01_workshop_briefed", "ms_w1_mq01_cutter_surge"))
        } else if (questId == "w1_mq03") {
            milestones.addAll(listOf("ms_w1_mq01_complete", "ms_w1_mq02_complete"))
        } else if (questId == "w1_mq04") {
            milestones.addAll(listOf("ms_w1_mq01_complete", "ms_w1_mq02_complete", "ms_w1_mq03_complete", "ms_w1_power_restored"))
        } else if (questId == "w1_mq05") {
            milestones.addAll(listOf("ms_w1_mq01_complete", "ms_w1_mq02_complete", "ms_w1_mq03_complete", "ms_w1_mq04_complete"))
        }

        val completedQuests = mutableSetOf<String>()
        if (worldNum >= 2) (1..5).forEach { completedQuests.add("w1_mq0$it") }
        if (worldNum >= 3) (1..5).forEach { completedQuests.add("w2_mq0$it") }
        if (worldNum >= 4) (11..15).forEach { completedQuests.add("w3_mq$it") }
        if (worldNum >= 5) (16..20).forEach { completedQuests.add("w4_mq$it") }
        if (worldNum >= 6) (21..25).forEach { completedQuests.add("w5_mq$it") }

        // Start the target quest at its first stage
        val firstStage = quest.stages.first()
        val stageMap = mapOf(quest.id to firstStage.id)
        val taskMap = mapOf(quest.id to emptySet<String>())

        val stock = mutableMapOf(
            "ration_pack" to 3,
            "medkit_i" to 2
        )
        if (worldNum >= 2) {
            stock["functional_cryo_inductor"] = 1
            stock["ghost_signal_cell"] = 1
        }
        if (worldNum >= 3) {
            stock["thermal_cutter"] = 1
        }
        if (worldNum >= 4) {
            stock["the_lens"] = 1
        }
        if (worldNum >= 5) {
            stock["the_anvil"] = 1
            stock["deep_core_engine"] = 1
        }
        if (worldNum >= 6) {
            stock["anchor_relic"] = 1
            stock["key_relic"] = 1
        }

        val level = when (worldNum) {
            1 -> 1
            2 -> 3
            3 -> 6
            4 -> 9
            5 -> 12
            6 -> 15
            else -> 1
        }

        val partyLevels = party.associateWith { level }
        val partyXp = party.associateWith { 0 }

        val store = services.sessionStore
        store.restore(store.state.value.copy(
            worldId = hub.worldId,
            hubId = hub.id,
            roomId = room.id,
            playerId = "nova",
            partyMembers = party,
            playerLevel = level,
            partyMemberLevels = partyLevels,
            partyMemberXp = partyXp,
            partyMemberHp = emptyMap(),
            playerCredits = worldNum * 500,
            activeQuests = setOf(quest.id),
            trackedQuestId = quest.id,
            completedQuests = completedQuests,
            failedQuests = emptySet(),
            questStageById = stageMap,
            questTasksCompleted = taskMap,
            completedMilestones = milestones,
            unlockedSkills = skills,
            inventory = stock,
            revealedNodes = setOf(node.id),
            unlockedNodes = setOf(node.id),
            visitedNodes = setOf(node.id),
            roomStates = emptyMap()
        ))
        services.inventoryService.restore(stock)
        return true
    }
}
