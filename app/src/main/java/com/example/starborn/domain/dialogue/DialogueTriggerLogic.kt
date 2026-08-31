package com.example.starborn.domain.dialogue

import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale

fun isDialogueConditionMet(
    condition: String?,
    state: GameSessionState,
    inventoryService: InventoryService
): Boolean {
    if (condition.isNullOrBlank()) return true
    val tokens = condition.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    for (token in tokens) {
        val parts = token.split(':', limit = 2)
        if (parts.isEmpty()) continue
        val type = parts[0].trim().lowercase()
        val value = parts.getOrNull(1)?.trim().orEmpty()
        val met = when (type) {
            "quest" -> value in state.activeQuests || value in state.completedQuests
            "quest_active" -> value in state.activeQuests
            "quest_completed" -> value in state.completedQuests
            "quest_not_started" -> value.isNotBlank() &&
                value !in state.activeQuests &&
                value !in state.completedQuests &&
                value !in state.failedQuests
            "quest_failed" -> value in state.failedQuests
            "quest_stage" -> {
                val (questId, stageId) = parseQuestStageCondition(value)
                questId != null && stageId != null &&
                    state.questStageById[questId]?.equals(stageId, ignoreCase = true) == true
            }
            "quest_stage_not" -> {
                val (questId, stageId) = parseQuestStageCondition(value)
                questId == null || stageId == null ||
                    state.questStageById[questId]?.equals(stageId, ignoreCase = true) != true
            }
            "milestone" -> value in state.completedMilestones
            "milestone_not_set" -> value !in state.completedMilestones
            "item" -> value.isNotBlank() && inventoryService.hasItem(value)
            "item_not" -> value.isNotBlank() && !inventoryService.hasItem(value)
            "event_completed" -> value in state.completedEvents
            "event_not_completed" -> value.isNotBlank() && value !in state.completedEvents
            "tutorial_completed" -> value in state.tutorialCompleted
            "tutorial_not_completed" -> value.isNotBlank() && value !in state.tutorialCompleted
            else -> true
        }
        if (!met) return false
    }
    return true
}

private fun parseQuestStageCondition(raw: String): Pair<String?, String?> {
    if (raw.isBlank()) return null to null
    val parts = raw.split(':', limit = 2)
    val questId = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrEmpty() }
    val stageId = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
    return questId to stageId
}

fun handleDialogueTrigger(
    trigger: String,
    sessionStore: GameSessionStore,
    questRuntimeManager: QuestRuntimeManager,
    inventoryService: InventoryService? = null,
    onMilestoneSet: ((String) -> Unit)? = null
) {
    val actions = DialogueTriggerParser.parse(trigger)
    if (actions.isEmpty()) return
    actions.forEach { action ->
        when (action.type.lowercase(Locale.getDefault())) {
            "start_quest" -> action.startQuest?.let {
                sessionStore.startQuest(it, track = true)
                questRuntimeManager.recordQuestStarted(it)
            }
            "complete_quest" -> action.completeQuest?.let {
                sessionStore.completeQuest(it)
                questRuntimeManager.markQuestCompleted(it)
                questRuntimeManager.recordQuestCompleted(it)
            }
            "fail_quest" -> action.questId?.let {
                sessionStore.failQuest(it)
                questRuntimeManager.markQuestFailed(it)
            }
            "set_milestone" -> action.milestone?.let {
                sessionStore.setMilestone(it)
                onMilestoneSet?.invoke(it)
            }
            "clear_milestone" -> action.milestone?.let { sessionStore.clearMilestone(it) }
            "track_quest" -> sessionStore.setTrackedQuest(action.questId)
            "untrack_quest" -> sessionStore.setTrackedQuest(null)
            "set_quest_task_done" -> {
                val questId = action.questId
                val taskId = action.taskId
                if (!questId.isNullOrBlank() && !taskId.isNullOrBlank()) {
                    questRuntimeManager.markTaskComplete(questId, taskId)
                }
            }
            "advance_quest_stage" -> {
                val questId = action.questId
                val stageId = action.toStageId
                if (!questId.isNullOrBlank() && !stageId.isNullOrBlank()) {
                    questRuntimeManager.setStage(questId, stageId)
                }
            }
            "give_xp" -> action.xp?.takeIf { it > 0 }?.let { sessionStore.addXp(it) }
            "give_reward" -> action.credits?.takeIf { it > 0 }?.let { sessionStore.addCredits(it) }
            "add_party_member" -> action.itemId?.let { sessionStore.addPartyMember(it) }
            "give_item" -> if (inventoryService != null) {
                val itemId = action.itemId ?: action.item
                val quantity = action.quantity ?: 1
                if (!itemId.isNullOrBlank() && quantity > 0) {
                    inventoryService.addItem(itemId, quantity)
                }
            }
            "take_item" -> if (inventoryService != null) {
                val itemId = action.itemId ?: action.item
                val quantity = action.quantity ?: 1
                if (!itemId.isNullOrBlank() && quantity > 0) {
                    inventoryService.removeItem(itemId, quantity)
                }
            }
        }
    }
}
