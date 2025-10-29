package com.example.starborn.domain.dialogue

import com.example.starborn.domain.model.DialogueLine
import java.util.Locale

class DialogueService(
    dialogueLines: List<DialogueLine>,
    private val conditionEvaluator: DialogueConditionEvaluator,
    private val triggerHandler: DialogueTriggerHandler
) {
    private val dialogueById: Map<String, DialogueLine> = dialogueLines.associateBy { it.id }
    private val dialogueBySpeaker: Map<String, List<DialogueLine>> =
        dialogueLines.groupBy { it.speaker.lowercase(Locale.getDefault()) }

    fun startDialogue(speaker: String): DialogueSession? {
        val entries = dialogueBySpeaker[speaker.lowercase(Locale.getDefault())] ?: return null
        val start = entries.firstOrNull { conditionEvaluator.isConditionMet(it.condition) } ?: return null
        return DialogueSession(dialogueById, start, conditionEvaluator, triggerHandler)
    }
}

class DialogueSession(
    private val dialogueById: Map<String, DialogueLine>,
    start: DialogueLine,
    private val conditionEvaluator: DialogueConditionEvaluator,
    private val triggerHandler: DialogueTriggerHandler
) {
    private var currentId: String? = start.id

    fun current(): DialogueLine? = currentId?.let { dialogueById[it] }

    fun advance(): DialogueLine? {
        val currentLine = current()
        currentLine?.trigger?.let { triggerHandler.handleTrigger(it) }

        var nextId = currentLine?.next
        while (nextId != null) {
            val candidate = dialogueById[nextId]
            if (candidate == null) {
                currentId = null
                return null
            }
            if (conditionEvaluator.isConditionMet(candidate.condition)) {
                currentId = candidate.id
                return candidate
            }
            // Skip candidate but trigger if defined
            candidate.trigger?.let { triggerHandler.handleTrigger(it) }
            nextId = candidate.next
        }
        currentId = null
        return null
    }

    fun isFinished(): Boolean = current() == null
}

fun interface DialogueConditionEvaluator {
    fun isConditionMet(condition: String?): Boolean
}

fun interface DialogueTriggerHandler {
    fun handleTrigger(trigger: String)
}
