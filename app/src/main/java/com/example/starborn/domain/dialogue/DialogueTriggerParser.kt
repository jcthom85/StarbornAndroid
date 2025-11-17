package com.example.starborn.domain.dialogue

import com.example.starborn.domain.model.EventAction
import java.util.Locale

object DialogueTriggerParser {

    fun parse(trigger: String?): List<EventAction> {
        if (trigger.isNullOrBlank()) return emptyList()
        val actions = mutableListOf<EventAction>()
        trigger.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { token ->
                val parts = token.split(':', limit = 2)
                if (parts.isEmpty()) return@forEach
                val type = parts[0].trim().lowercase(Locale.getDefault())
                val value = parts.getOrNull(1)?.trim().orEmpty()
                parseToken(type, value)?.let { actions += it }
            }
        return actions
    }

    private fun parseToken(type: String, value: String): EventAction? {
        return when (type) {
            "start_quest" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "start_quest", startQuest = it, questId = it)
            }
            "complete_quest" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "complete_quest", completeQuest = it, questId = it)
            }
            "fail_quest" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "fail_quest", questId = it)
            }
            "track_quest" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "track_quest", questId = it)
            }
            "untrack_quest" -> EventAction(type = "untrack_quest")
            "set_milestone" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "set_milestone", milestone = it)
            }
            "clear_milestone" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "clear_milestone", milestone = it)
            }
            "give_item" -> parseIdQuantity(value)?.let { (itemId, qty) ->
                EventAction(type = "give_item", itemId = itemId, quantity = qty)
            }
            "take_item" -> parseIdQuantity(value)?.let { (itemId, qty) ->
                EventAction(type = "take_item", itemId = itemId, quantity = qty)
            }
            "give_credits" -> value.toIntOrNull()?.takeIf { it > 0 }?.let { amount ->
                EventAction(type = "give_reward", credits = amount)
            }
            "give_xp" -> value.toIntOrNull()?.takeIf { it > 0 }?.let { amount ->
                EventAction(type = "give_xp", xp = amount)
            }
            "set_quest_task_done" -> parseQuestTaskValue(value)?.let { (questId, taskId) ->
                EventAction(type = "set_quest_task_done", questId = questId, taskId = taskId)
            }
            "advance_quest_stage" -> parseQuestStageValue(value)?.let { (questId, stageId) ->
                EventAction(type = "advance_quest_stage", questId = questId, toStageId = stageId)
            }
            "advance_quest" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "advance_quest", questId = it)
            }
            "recruit" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "add_party_member", itemId = it)
            }
            "system_tutorial" -> {
                val (sceneId, context) = parseSceneContextValue(value)
                sceneId?.let {
                    EventAction(type = "system_tutorial", sceneId = it, context = context)
                }
            }
            "play_cinematic" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "play_cinematic", sceneId = it)
            }
            "player_action" -> value.takeIf { it.isNotBlank() }?.let {
                EventAction(type = "player_action", action = it)
            }
            else -> null
        }
    }

    private fun parseIdQuantity(raw: String): Pair<String, Int>? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val delimiters = listOf('*', 'x', '|')
        var id = trimmed
        var qty = 1
        run breaking@{
            delimiters.forEach { delimiter ->
                val idx = trimmed.indexOf(delimiter)
                if (idx > 0) {
                    id = trimmed.substring(0, idx).trim()
                    qty = trimmed.substring(idx + 1).trim().toIntOrNull() ?: 1
                    return@breaking
                }
            }
        }
        if (id.isBlank()) return null
        return id to qty.coerceAtLeast(1)
    }

    private fun parseQuestTaskValue(value: String): Pair<String, String>? {
        val parts = value.split(':', limit = 2)
        if (parts.size < 2) return null
        val questId = parts[0].trim()
        val taskId = parts[1].trim()
        if (questId.isEmpty() || taskId.isEmpty()) return null
        return questId to taskId
    }

    private fun parseQuestStageValue(value: String): Pair<String, String>? {
        val parts = value.split(':', limit = 2)
        if (parts.size < 2) return null
        val questId = parts[0].trim()
        val stageId = parts[1].trim()
        if (questId.isEmpty() || stageId.isEmpty()) return null
        return questId to stageId
    }

    private fun parseSceneContextValue(value: String): Pair<String?, String?> {
        if (value.isBlank()) return null to null
        val parts = value.split('|', limit = 2)
        val sceneId = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrEmpty() }
        val context = parts.getOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
        return sceneId to context
    }
}
