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
        val completedEvents = state.completedEvents.toMutableSet()
        for (event in candidates) {
            val eventId = event.id
            if (!event.repeatable && completedEvents.contains(eventId)) {
                continue
            }
            if (!conditionsSatisfied(event.conditions, state)) continue
            if (!matchesTrigger(event.trigger, payload, state)) continue
            if (executeActions(event.actions, state)) {
                if (!event.repeatable) {
                    completedEvents.add(eventId)
                    sessionStore.markEventCompleted(eventId)
                }
                if (eventId.isNotBlank()) {
                    eventHooks.onEventCompleted(eventId)
                }
            }
        }
    }

    fun performActions(actions: List<EventAction>) {
        if (actions.isEmpty()) return
        executeActions(actions, sessionStore.state.value)
    }

    private fun matchesTrigger(
        trigger: EventTrigger,
        payload: EventPayload,
        state: GameSessionState
    ): Boolean {
        return when (trigger.type.lowercase()) {
            "talk_to" -> payload is EventPayload.TalkTo && payload.npc.equals(trigger.npc, true)
            "npc_interaction" -> payload is EventPayload.TalkTo && payload.npc.equals(trigger.npc, true)
            "dialogue_closed", "dialogue_dismissed" ->
                payload is EventPayload.TalkTo && payload.npc.equals(trigger.npc, true)
            "enter_room" -> {
                val roomId = (payload as? EventPayload.EnterRoom)?.roomId ?: state.roomId
                val triggerRoom = trigger.roomId ?: trigger.room
                triggerRoom == null || triggerRoom == roomId
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
            "item_acquired" -> {
                val itemPayload = (payload as? EventPayload.ItemAcquired)?.itemId
                val triggerItem = trigger.itemId ?: trigger.item
                if (triggerItem.isNullOrBlank()) {
                    itemPayload != null
                } else {
                    triggerItem.equals(itemPayload, ignoreCase = true)
                }
            }
            else -> true
        }
    }

    private fun executeActions(actions: List<EventAction>, state: GameSessionState): Boolean {
        var executed = false
        for (action in actions) {
            executed = when (action.type.lowercase()) {
                "if_quest_active" -> executeConditionalBranch(
                    action.questId?.let { it in state.activeQuests } ?: false,
                    action,
                    state
                )
                "if_quest_not_started" -> executeConditionalBranch(
                    action.questId?.let { questId ->
                        questId !in state.activeQuests &&
                            questId !in state.completedQuests &&
                            questId !in state.failedQuests
                    } ?: false,
                    action,
                    state
                )
                "if_quest_completed" -> executeConditionalBranch(
                    action.questId?.let { it in state.completedQuests } ?: false,
                    action,
                    state
                )
                "if_quest_not_completed" -> executeConditionalBranch(
                    action.questId?.let { it !in state.completedQuests } ?: false,
                    action,
                    state
                )
                "if_quest_failed" -> executeConditionalBranch(
                    action.questId?.let { it in state.failedQuests } ?: false,
                    action,
                    state
                )
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
                    val questId = action.startQuest ?: action.questId
                    val updated = questId?.let {
                        sessionStore.startQuest(it)
                        eventHooks.onQuestStarted(it)
                        true
                    } ?: false
                    if (updated) eventHooks.onQuestUpdated()
                    updated
                }
                "complete_quest" -> {
                    val questId = action.completeQuest ?: action.questId
                    val updated = questId?.let {
                        sessionStore.completeQuest(it)
                        eventHooks.onQuestCompleted(it)
                        eventHooks.onQuestUpdated()
                        true
                    } ?: false
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
                "play_cinematic", "trigger_cutscene" -> {
                    val sceneId = action.sceneId
                    if (sceneId.isNullOrBlank()) {
                        action.onComplete?.let { executeActions(it, state) }
                        action.onComplete != null
                    } else {
                        var callbackInvoked = false
                        eventHooks.onPlayCinematic(sceneId) {
                            if (!callbackInvoked) {
                                callbackInvoked = true
                                action.onComplete?.let { executeActions(it, state) }
                            }
                        }
                        true
                    }
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
                    if ((action.item != null || action.itemId != null) && (action.quantity ?: 1) <= 0) {
                        println("DEBUG: give_item requested with non-positive quantity for ${action.item ?: action.itemId}")
                    }
                    when {
                        action.item != null || action.itemId != null -> {
                            val id = action.item ?: action.itemId
                            if (id != null) {
                                val qty = action.quantity ?: 1
                                println("DEBUG: give_item action id=$id qty=$qty")
                                eventHooks.onGiveItem(id, qty)
                            }
                        }
                        else -> action.rewardItems.orEmpty().forEach { rewardItem ->
                            val qty = rewardItem.quantity ?: 1
                            println("DEBUG: give_item rewardItems id=${rewardItem.itemId} qty=$qty")
                            eventHooks.onGiveItem(rewardItem.itemId, qty)
                        }
                    }
                    true
                }
                "take_item" -> {
                    val id = action.item ?: action.itemId
                    if (id.isNullOrBlank()) {
                        false
                    } else {
                        val qty = action.quantity ?: 1
                        eventHooks.onTakeItem(id, qty.coerceAtLeast(1))
                    }
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
                "player_action" -> {
                    action.action?.let { actionId ->
                        handleTrigger("player_action", EventPayload.Action(actionId, action.itemId ?: action.item))
                    }
                    true
                }
                "add_party_member" -> {
                    action.itemId?.let { memberId ->
                        sessionStore.addPartyMember(memberId)
                        eventHooks.onPartyMemberJoined(memberId)
                    }
                    true
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
                    var callbackInvoked = false
                    eventHooks.onSystemTutorial(action.sceneId, action.context) {
                        if (!callbackInvoked) {
                            callbackInvoked = true
                            action.onComplete?.let { executeActions(it, state) }
                        }
                    }
                    true
                }
                "audio_layer" -> {
                    val command = AudioLayerCommandSpec(
                        layer = action.audioLayer,
                        cueId = action.audioCueId,
                        gain = action.audioGain,
                        fadeMs = action.audioFadeMs,
                        loop = action.audioLoop,
                        stop = action.audioStop == true
                    )
                    eventHooks.onAudioLayerCommand(command)
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
                else -> false
            } || executed
        }
        return executed
    }

    private fun conditionsSatisfied(
        conditions: List<com.example.starborn.domain.model.EventCondition>,
        state: GameSessionState
    ): Boolean {
        if (conditions.isEmpty()) return true
        return conditions.all { condition ->
            when (condition.type.lowercase()) {
                "milestone_not_set" -> condition.milestone.isNullOrBlank() ||
                    condition.milestone !in state.completedMilestones
                "milestone_set" -> condition.milestone.isNullOrBlank() ||
                    condition.milestone in state.completedMilestones
                "quest_active" -> condition.questId?.let { state.activeQuests.contains(it) } ?: false
                "quest_not_started" -> condition.questId?.let { questId ->
                    questId !in state.activeQuests &&
                        questId !in state.completedQuests &&
                        questId !in state.failedQuests
                } ?: false
                "quest_completed" -> condition.questId?.let { state.completedQuests.contains(it) } ?: false
                "quest_not_completed" -> condition.questId?.let { questId ->
                    questId !in state.completedQuests
                } ?: false
                "quest_failed" -> condition.questId?.let { state.failedQuests.contains(it) } ?: false
                "quest_stage" -> {
                    val questId = condition.questId
                    val stageId = condition.stageId
                    if (questId.isNullOrBlank() || stageId.isNullOrBlank()) {
                        false
                    } else {
                        state.questStageById[questId]?.equals(stageId, ignoreCase = true) == true
                    }
                }
                "quest_stage_not" -> {
                    val questId = condition.questId
                    val stageId = condition.stageId
                    if (questId.isNullOrBlank() || stageId.isNullOrBlank()) {
                        true
                    } else {
                        state.questStageById[questId]?.equals(stageId, ignoreCase = true) != true
                    }
                }
                "quest_task_done" -> {
                    val questId = condition.questId
                    val taskId = condition.taskId
                    if (questId.isNullOrBlank() || taskId.isNullOrBlank()) {
                        false
                    } else {
                        state.questTasksCompleted[questId]?.contains(taskId) == true
                    }
                }
                "quest_task_not_done" -> {
                    val questId = condition.questId
                    val taskId = condition.taskId
                    if (questId.isNullOrBlank() || taskId.isNullOrBlank()) {
                        true
                    } else {
                        state.questTasksCompleted[questId]?.contains(taskId) != true
                    }
                }
                "event_completed" -> condition.eventId?.let { state.completedEvents.contains(it) } ?: false
                "event_not_completed" -> condition.eventId?.let { id ->
                    id !in state.completedEvents
                } ?: false
                "tutorial_completed" -> condition.tutorialId?.let { id ->
                    id in state.tutorialCompleted
                } ?: false
                "tutorial_not_completed" -> condition.tutorialId?.let { id ->
                    id !in state.tutorialCompleted
                } ?: false
                "item" -> condition.hasInventory(state)
                "item_not" -> !condition.hasInventory(state)
                else -> true
            }
        }
    }

    private fun com.example.starborn.domain.model.EventCondition.hasInventory(
        state: GameSessionState
    ): Boolean {
        val id = (itemId ?: item)?.takeUnless { it.isNullOrBlank() } ?: return false
        val qty = (quantity ?: 1).coerceAtLeast(1)
        val available = state.inventory.entries.any { (key, value) ->
            key.equals(id, ignoreCase = true) && value >= qty
        }
        return available
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
    data class ItemAcquired(val itemId: String) : EventPayload
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

data class AudioLayerCommandSpec(
    val layer: String?,
    val cueId: String?,
    val gain: Float?,
    val fadeMs: Long?,
    val loop: Boolean?,
    val stop: Boolean = false
)

data class EventHooks(
    val onPlayCinematic: (sceneId: String, onComplete: () -> Unit) -> Unit = { _, done -> done() },
    val onMessage: (message: String) -> Unit = {},
    val onReward: (reward: EventReward) -> Unit = {},
    val onSetRoomState: (roomId: String?, stateKey: String, value: Boolean) -> Unit = { _, _, _ -> },
    val onToggleRoomState: (roomId: String?, stateKey: String) -> Unit = { _, _ -> },
    val onSpawnEncounter: (encounterId: String?, roomId: String?) -> Unit = { _, _ -> },
    val onGiveItem: (itemId: String, quantity: Int) -> Unit = { _, _ -> },
    val onTakeItem: (itemId: String, quantity: Int) -> Boolean = { _, _ -> true },
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
    val onSystemTutorial: (sceneId: String?, context: String?, onComplete: () -> Unit) -> Unit = { _, _, done -> done() },
    val onNarration: (message: String, tapToDismiss: Boolean) -> Unit = { _, _ -> },
    val onSpawnGroundItem: (roomId: String?, itemId: String, quantity: Int) -> Unit = { _, _, _ -> },
    val onUnlockRoomSearch: (roomId: String?, note: String?) -> Unit = { _, _ -> },
    val onEventCompleted: (eventId: String) -> Unit = {},
    val onPartyMemberJoined: (memberId: String) -> Unit = {},
    val onAudioLayerCommand: (AudioLayerCommandSpec) -> Unit = {}
)
