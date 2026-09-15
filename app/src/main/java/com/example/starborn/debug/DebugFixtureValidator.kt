package com.example.starborn.debug

import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.HubNode
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.session.GameSessionState

/** Validate relationships, not just whether an asset ID exists somewhere. */
object DebugFixtureValidator {
    fun validate(state: GameSessionState, hubs: List<Hub>, nodes: List<HubNode>, rooms: Set<String>,
                 quests: Collection<Quest>, items: Set<String>): List<String> = buildList {
        val hub = hubs.firstOrNull { it.id == state.hubId }
        if (hub == null) add("Unknown hub ${state.hubId}")
        else if (hub.worldId != state.worldId) add("Hub ${hub.id} does not belong to ${state.worldId}")
        state.roomId?.let { room ->
            if (room !in rooms) add("Unknown room $room")
            if (nodes.none { it.hubId == state.hubId && (room in it.rooms || it.entryRoom == room) })
                add("Room $room is not in hub ${state.hubId}")
        }
        if (state.partyMembers.isEmpty() || state.playerId !in state.partyMembers) add("Invalid active party")
        if (state.activeQuests.intersect(state.completedQuests).isNotEmpty()) add("Quest both active and complete")
        val byId = quests.associateBy { it.id }
        (state.activeQuests + state.completedQuests + state.failedQuests).forEach {
            if (it !in byId) add("Unknown quest $it")
        }
        state.questStageById.forEach { (quest, stage) ->
            if (byId[quest]?.stages?.none { it.id == stage } != false) add("Unknown stage $quest/$stage")
        }
        state.questTasksCompleted.forEach { (quest, tasks) ->
            val known = byId[quest]?.stages.orEmpty().flatMap { it.tasks }.map { it.id }.toSet()
            tasks.filterNot { it in known }.forEach { add("Unknown task $quest/$it") }
        }
        state.inventory.forEach { (item, quantity) ->
            if (item !in items || quantity <= 0) add("Invalid inventory $item=$quantity")
        }
        (state.equippedWeapons.values + state.equippedArmors.values + state.equippedItems.values).forEach {
            if (it !in items) add("Unknown equipped item $it")
        }
        if (state.pendingBattleJson.isNotEmpty() || state.battleCheckpoint != null || state.pendingEventCinematics.isNotEmpty())
            add("Unexpected pending recovery state in fresh fixture")
    }
}
