package com.example.starborn.domain.event

import com.example.starborn.domain.model.EventAction
import com.example.starborn.domain.model.EventTrigger
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore

class EventManager(
    private val events: List<GameEvent>,
    private val sessionStore: GameSessionStore,
    private val eventHooks: EventHooks = EventHooks()
) {
    private val eventsByTriggerType: Map<String, List<GameEvent>> =
        events.groupBy { it.trigger.type.lowercase() }

    fun handleTrigger(type: String, payload: EventPayload = EventPayload.Empty) {
        val candidates = eventsByTriggerType[type.lowercase()].orEmpty()
        val state = sessionStore.state.value
        for (event in candidates) {
            if (!event.repeatable && event.id in state.completedMilestones) {
                continue
            }
            if (!matchesTrigger(event.trigger, payload, state)) continue
            if (executeActions(event.actions, state)) {
                // Mark completion for non-repeatable events
                if (!event.repeatable) {
                    sessionStore.setMilestone("evt_completed_${event.id}")
                }
            }
        }
    }

    private fun matchesTrigger(
        trigger: EventTrigger,
        payload: EventPayload,
        state: GameSessionState
    ): Boolean {
        return when (trigger.type.lowercase()) {
            "talk_to" -> payload is EventPayload.TalkTo && payload.npc.equals(trigger.npc, true)
            "npc_interaction" -> payload is EventPayload.TalkTo && payload.npc.equals(trigger.npc, true)
            "enter_room" -> {
                val roomId = (payload as? EventPayload.EnterRoom)?.roomId ?: state.roomId
                trigger.roomId == null || trigger.roomId == roomId
            }
            "player_action" -> {
                val actionPayload = payload as? EventPayload.Action ?: return false
                val actionMatches = trigger.action.isNullOrBlank() ||
                    actionPayload.action.equals(trigger.action, ignoreCase = true)
                if (!actionMatches) return false
                val requiredItem = trigger.itemId ?: trigger.item
                if (!requiredItem.isNullOrBlank()) {
                    val payloadItem = actionPayload.itemId
                    if (!requiredItem.equals(payloadItem, ignoreCase = true)) return false
                }
                true
            }
            "quest_stage_complete" -> payload is EventPayload.QuestStage && payload.questId == trigger.questId
            "encounter_victory" -> matchesOutcome(trigger, payload, EventPayload.EncounterOutcome.Outcome.VICTORY)
            "encounter_defeat" -> payload is EventPayload.EncounterOutcome &&
                payload.outcome == EventPayload.EncounterOutcome.Outcome.DEFEAT &&
                (trigger.enemies.isNullOrEmpty() || payload.enemyIds.any { it in trigger.enemies })
            "encounter_retreat" -> payload is EventPayload.EncounterOutcome &&
                payload.outcome == EventPayload.EncounterOutcome.Outcome.RETREAT &&
                (trigger.enemies.isNullOrEmpty() || payload.enemyIds.any { it in trigger.enemies })
            "item_acquired" -> true // Inventory system pending
            else -> true
        }
    }

    private fun executeActions(actions: List<EventAction>, state: GameSessionState): Boolean {
        var executed = false
        for (action in actions) {
            executed = when (action.type.lowercase()) {
                "if_quest_active" -> executeConditionalBranch(action.questId in state.activeQuests, action, state)
                "if_milestone_set" -> executeConditionalBranch(
                    action.milestone != null && action.milestone in state.completedMilestones,
                    action,
                    state
                )
                "if_milestone_not_set" -> executeConditionalBranch(
                    action.milestone != null && action.milestone !in state.completedMilestones,
                    action,
                    state
                )
                "if_milestones_set" -> executeConditionalBranch(
                    action.milestones.orEmpty().all { it in state.completedMilestones },
                    action,
                    state
                )
                "set_milestone" -> {
                    action.milestone?.let {
                        sessionStore.setMilestone(it)
                        eventHooks.onMilestoneSet(it)
                    }
                    true
                }
                "clear_milestone" -> {
                    action.milestone?.let { sessionStore.clearMilestone(it) }
                    true
                }
                "set_milestones" -> {
                    action.setMilestones.orEmpty().forEach {
                        sessionStore.setMilestone(it)
                        eventHooks.onMilestoneSet(it)
                    }
                    true
                }
                "clear_milestones" -> {
                    action.clearMilestones.orEmpty().forEach { sessionStore.clearMilestone(it) }
                    true
                }
                "start_quest" -> {
                    val updated = action.startQuest?.let {
                        sessionStore.startQuest(it)
                        eventHooks.onQuestStarted(it)
                        true
                    } ?: false
                    if (updated) eventHooks.onQuestUpdated()
                    updated
                }
                "complete_quest" -> {
                    val updated = action.completeQuest?.let {
                        sessionStore.completeQuest(it)
                        eventHooks.onQuestCompleted(it)
                        true
                    } ?: false
                    if (updated) eventHooks.onQuestUpdated()
                    updated
                }
                "fail_quest" -> {
                    val updated = action.questId?.let {
                        sessionStore.failQuest(it)
                        eventHooks.onQuestFailed(it, action.message)
                        eventHooks.onQuestUpdated()
                        true
                    } ?: false
                    updated
                }
                "track_quest" -> {
                    action.questId?.let {
                        sessionStore.setTrackedQuest(it)
                        eventHooks.onQuestUpdated()
                        true
                    } ?: false
                }
                "untrack_quest" -> {
                    sessionStore.setTrackedQuest(null)
                    eventHooks.onQuestUpdated()
                    true
                }
                "play_cinematic" -> {
                    action.sceneId?.let { eventHooks.onPlayCinematic(it) }
                    true
                }
                "show_message" -> {
                    action.message?.let { eventHooks.onMessage(it) }
                    true
                }
                "give_reward", "grant_reward" -> {
                    eventHooks.onReward(toReward(action))
                    true
                }
                "set_room_state" -> {
                    if (action.stateKey != null) {
                        eventHooks.onSetRoomState(action.roomId, action.stateKey, action.value ?: true)
                        true
                    } else false
                }
                "toggle_room_state" -> {
                    if (action.stateKey != null) {
                        eventHooks.onToggleRoomState(action.roomId, action.stateKey)
                        true
                    } else false
                }
                "spawn_encounter" -> {
                    eventHooks.onSpawnEncounter(action.encounterId, action.roomId)
                    true
                }
                "give_item", "give_item_to_player" -> {
                    when {
                        action.item != null || action.itemId != null -> {
                            val id = action.item ?: action.itemId
                            if (id != null) {
                                val qty = action.quantity ?: 1
                                eventHooks.onGiveItem(id, qty)
                            }
                        }
                        else -> action.rewardItems.orEmpty().forEach { rewardItem ->
                            val qty = rewardItem.quantity ?: 1
                            eventHooks.onGiveItem(rewardItem.itemId, qty)
                        }
                    }
                    true
                }
                "reveal_hidden_item", "spawn_item_on_ground" -> {
                    val itemId = action.itemId ?: action.item
                    if (!itemId.isNullOrBlank()) {
                        val qty = action.quantity ?: 1
                        eventHooks.onSpawnGroundItem(action.roomId, itemId, qty.coerceAtLeast(1))
                        true
                    } else {
                        false
                    }
                }
                "give_xp" -> {
                    action.xp?.let { eventHooks.onGiveXp(it) }
                    true
                }
                "set_quest_task_done" -> {
                    eventHooks.onQuestTaskUpdated(action.questId, action.taskId)
                    eventHooks.onAdvanceQuest(action.questId)
                    eventHooks.onQuestUpdated()
                    true
                }
                "advance_quest", "advance_quest_if_active" -> {
                    eventHooks.onAdvanceQuest(action.questId)
                    eventHooks.onQuestUpdated()
                    true
                }
                "advance_quest_stage" -> {
                    eventHooks.onQuestStageAdvanced(action.questId, action.toStageId)
                    eventHooks.onAdvanceQuest(action.questId)
                    eventHooks.onQuestUpdated()
                    true
                }
                "begin_node" -> {
                    eventHooks.onBeginNode(action.roomId)
                    true
                }
                "system_tutorial" -> {
                    eventHooks.onSystemTutorial(action.sceneId, action.context)
                    action.onComplete?.let { executeActions(it, state) }
                    true
                }
                "play_cinematic", "trigger_cutscene" -> {
                    action.sceneId?.let { eventHooks.onPlayCinematic(it) }
                    action.onComplete?.let { executeActions(it, state) }
                    true
                }
                "unlock_room_search" -> {
                    eventHooks.onUnlockRoomSearch(action.roomId, action.note)
                    true
                }
                "rebuild_ui", "wait_for_draw" -> true
                "narrate" -> {
                    val text = action.message ?: action.text
                    if (!text.isNullOrBlank()) {
                        val tap = action.tapToDismiss ?: false
                        eventHooks.onNarration(text, tap)
                        if (!tap) {
                            eventHooks.onMessage(text)
                        }
                        true
                    } else {
                        false
                    }
                }
                "player_action" -> {
                    action.action?.let { actionId ->
                        handleTrigger("player_action", EventPayload.Action(actionId, action.itemId ?: action.item))
                    }
                    true
                }
                else -> false
            } || executed
        }
        return executed
    }

    private fun executeConditionalBranch(
        condition: Boolean,
        action: EventAction,
        state: GameSessionState
    ): Boolean {
        val branch = if (condition) action.`do` else action.elseDo
        return branch?.let { executeActions(it, state) } ?: false
    }

    private fun matchesOutcome(
        trigger: EventTrigger,
        payload: EventPayload,
        expected: EventPayload.EncounterOutcome.Outcome
    ): Boolean {
        return when (payload) {
            is EventPayload.EnemyVictory -> expected == EventPayload.EncounterOutcome.Outcome.VICTORY &&
                (trigger.enemies.isNullOrEmpty() || payload.enemyIds.any { it in trigger.enemies })
            is EventPayload.EncounterOutcome -> payload.outcome == expected &&
                (trigger.enemies.isNullOrEmpty() || payload.enemyIds.any { it in trigger.enemies })
            else -> false
        }
    }

    private fun toReward(action: EventAction): EventReward {
        val base = action.reward ?: EventReward()
        val xp = action.xp ?: base.xp
        val credits = action.credits ?: base.credits
        val ap = action.ap ?: base.ap
        val items = action.rewardItems ?: base.items
        return EventReward(xp = xp, credits = credits, ap = ap, items = items)
    }
}

sealed interface EventPayload {
    data object Empty : EventPayload
    data class TalkTo(val npc: String) : EventPayload
    data class Action(val action: String, val itemId: String? = null) : EventPayload
    data class EnterRoom(val roomId: String) : EventPayload
    data class QuestStage(val questId: String) : EventPayload
    data class EnemyVictory(val enemyIds: List<String>) : EventPayload
    data class EncounterOutcome(
        val enemyIds: List<String>,
        val outcome: Outcome
    ) : EventPayload {
        enum class Outcome {
            VICTORY,
            DEFEAT,
            RETREAT
        }
    }
}

data class EventHooks(
    val onPlayCinematic: (sceneId: String) -> Unit = {},
    val onMessage: (message: String) -> Unit = {},
    val onReward: (reward: EventReward) -> Unit = {},
    val onSetRoomState: (roomId: String?, stateKey: String, value: Boolean) -> Unit = { _, _, _ -> },
    val onToggleRoomState: (roomId: String?, stateKey: String) -> Unit = { _, _ -> },
    val onSpawnEncounter: (encounterId: String?, roomId: String?) -> Unit = { _, _ -> },
    val onGiveItem: (itemId: String, quantity: Int) -> Unit = { _, _ -> },
    val onGiveXp: (amount: Int) -> Unit = {},
    val onAdvanceQuest: (questId: String?) -> Unit = {},
    val onQuestUpdated: () -> Unit = {},
    val onMilestoneSet: (milestoneId: String) -> Unit = {},
    val onQuestTaskUpdated: (questId: String?, taskId: String?) -> Unit = { _, _ -> },
    val onQuestStageAdvanced: (questId: String?, stageId: String?) -> Unit = { _, _ -> },
    val onQuestStarted: (questId: String?) -> Unit = {},
    val onQuestCompleted: (questId: String?) -> Unit = {},
    val onQuestFailed: (questId: String?, reason: String?) -> Unit = { _, _ -> },
    val onBeginNode: (roomId: String?) -> Unit = {},
    val onSystemTutorial: (sceneId: String?, context: String?) -> Unit = { _, _ -> },
    val onNarration: (message: String, tapToDismiss: Boolean) -> Unit = { _, _ -> },
    val onSpawnGroundItem: (roomId: String?, itemId: String, quantity: Int) -> Unit = { _, _, _ -> },
    val onUnlockRoomSearch: (roomId: String?, note: String?) -> Unit = { _, _ -> }
)
