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
    val migratedHistory = milestoneHistory.map { openingMilestoneAliases[it] ?: it }.distinct()

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

    return copy(
        completedMilestones = migratedMilestones,
        milestoneHistory = migratedHistory,
        questTasksCompleted = if (oldTasks.isEmpty()) {
            questTasksCompleted
        } else {
            questTasksCompleted + ("w1_mq01" to migratedTasks)
        },
        roomStates = if ("pit_nova_bunk" !in roomStates) {
            roomStates
        } else {
            roomStates + ("pit_nova_bunk" to migratedBunkState)
        }
    )
}
