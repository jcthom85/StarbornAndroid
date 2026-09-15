package com.example.starborn.domain.session

private val openingMilestoneAliases = mapOf(
    "ms_w1_mq01_resonance_visible" to "ms_w1_mq01_fault_visible",
    "ms_w1_mq01_resonance_traced" to "ms_w1_mq01_safety_fault_inspected",
    "ms_w1_mq01_loader_read" to "ms_w1_mq01_loader_inspected"
)

/**
 * Preserves progress from the discarded prototype opening without carrying its
 * resonance language into the current game state.
 */
fun GameSessionState.migrateOpeningNarrativeState(): GameSessionState {
    val migratedMilestones = completedMilestones.mapTo(linkedSetOf()) {
        openingMilestoneAliases[it] ?: it
    }
    val migratedHistory = milestoneHistory.map { openingMilestoneAliases[it] ?: it }.toMutableList()

    val oldTasks = questTasksCompleted["w1_mq01"].orEmpty()
    val migratedTasks = oldTasks.toMutableSet().apply {
        if (remove("trace_resonance")) add("inspect_safety_fault")
        if ("ms_w1_mq01_loader_read" in completedMilestones) add("inspect_loader_relay")
    }

    val oldBunkState = roomStates["pit_nova_bunk"].orEmpty()
    val migratedBunkState = oldBunkState
        .filterKeys { it != "resonance_visible" && it != "resonance_investigated" }
        .toMutableMap()
        .apply {
            if (oldBunkState["resonance_visible"] == true) this["fault_visible"] = true
            if (oldBunkState["resonance_investigated"] == true) this["conduit_isolated"] = true
        }

    val migratedQuestTasks = questTasksCompleted.mapValues { (_, tasks) -> tasks.toMutableSet() }.toMutableMap()
    if (oldTasks.isNotEmpty()) migratedQuestTasks["w1_mq01"] = migratedTasks
    val migratedRoomStates = roomStates.mapValues { (_, states) -> states.toMutableMap() }.toMutableMap()
    if ("pit_nova_bunk" in roomStates) migratedRoomStates["pit_nova_bunk"] = migratedBunkState
    val migratedInventory = inventory.toMutableMap()
    val migratedSkills = unlockedSkills.toMutableSet()

    fun backfillMilestone(id: String) {
        if (migratedMilestones.add(id)) migratedHistory.add(id)
    }
    fun roomState(roomId: String, key: String) {
        migratedRoomStates.getOrPut(roomId) { mutableMapOf() }[key] = true
    }
    fun task(questId: String, taskId: String) {
        migratedQuestTasks.getOrPut(questId) { mutableSetOf() }.add(taskId)
    }

    if ("w1_mq01" in activeQuests && "w1_mq01" !in completedQuests &&
        "equip_starter_gear" in migratedTasks &&
        migratedInventory.getOrDefault("cryo_inductor", 0) == 0 &&
        migratedInventory.getOrDefault("functional_cryo_inductor", 0) == 0) {
        // Recover a sold unique part without granting sellable replacements repeatedly.
        if ("ms_w1_mq01_cryo_repaired" in migratedMilestones) {
            migratedInventory["functional_cryo_inductor"] = 1
        } else {
            migratedInventory["cryo_inductor"] = 1
        }
    }

    if ("w1_mq01" in completedQuests) {
        backfillMilestone("ms_w1_mq01_cryo_repaired")
        backfillMilestone("ms_w1_mq01_cutter_surge")
        backfillMilestone("ms_w1_mq01_complete")
        task("w1_mq01", "patch_flux_liner")
        task("w1_mq01", "confirm_governor_bypass")
        task("w1_mq01", "test_cutter")
        roomState("workshop_floor", "cutter_surge_complete")
    }

    if ("w1_mq03" in completedQuests) {
        backfillMilestone("ms_w1_mq03_echo_marked")
        backfillMilestone("ms_w1_mq03_liner_ground_spent")
        backfillMilestone("ms_w1_mq03_complete")
        roomState("echo_heart", "relic_synced")
        if (migratedInventory.getOrDefault("tuning_fork", 0) < 1) migratedInventory["tuning_fork"] = 1
        if (migratedInventory.getOrDefault("nova_flux_liner", 0) < 1) migratedInventory["nova_flux_liner"] = 1
        migratedInventory["functional_cryo_inductor"]?.takeIf {
            "ms_w1_mq03_liner_ground_spent" !in completedMilestones
        }?.let { count ->
            if (count <= 1) migratedInventory.remove("functional_cryo_inductor")
            else migratedInventory["functional_cryo_inductor"] = count - 1
        }
    }

    val bridgeInstalled = "ms_w2_bridge_installed" in migratedMilestones ||
        "w2_mq05" in completedQuests ||
        "reboot_bridge_relic" in migratedQuestTasks["w2_mq05"].orEmpty()
    if ("w2_mq03" in completedQuests) {
        backfillMilestone("ms_w2_bridge_recovered")
        task("w2_mq03", "recover_bridge_relic")
        if (!bridgeInstalled && migratedInventory.getOrDefault("bridge_relic", 0) < 1) {
            migratedInventory["bridge_relic"] = 1
        }
    }
    if (bridgeInstalled) {
        backfillMilestone("ms_w2_bridge_installed")
        migratedInventory.remove("bridge_relic")
    }

    val ridgeTasks = migratedQuestTasks["w2_mq04"].orEmpty()
    if ("confront_stalker" in ridgeTasks || "w2_mq04" in completedQuests) roomState("sector9_canopy_ridge", "hunter_confronted")
    if ("defeat_the_beast" in ridgeTasks || "w2_mq04" in completedQuests) roomState("sector9_canopy_ridge", "beast_defeated")
    if ("complete_anchor_drill" in ridgeTasks || "w2_mq04" in completedQuests) roomState("sector9_canopy_ridge", "anchor_drill_complete")
    if ("w2_mq04" in completedQuests) {
        task("w2_mq04", "complete_anchor_drill")
        migratedSkills.add("nova_link")
        backfillMilestone("ms_w2_link_unlocked")
        backfillMilestone("ms_w2_mq04_complete")
    }

    return copy(
        completedEvents = if ("w1_mq05" in activeQuests && "w1_mq05" !in completedQuests &&
            "ms_w1_mq05_complete" !in migratedMilestones && "scene_launch_crash" !in pendingEventCinematics)
            completedEvents - "w1_mq05_use_nav_console" else completedEvents,
        completedMilestones = migratedMilestones,
        milestoneHistory = migratedHistory.distinct(),
        questTasksCompleted = migratedQuestTasks,
        roomStates = migratedRoomStates,
        inventory = migratedInventory,
        unlockedSkills = migratedSkills
    )
}
