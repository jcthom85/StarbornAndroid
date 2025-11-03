package com.example.starborn.domain.dialogue

import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.DialogueOption
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
        if (currentLine != null && choices().isNotEmpty()) {
            return currentLine
        }
        currentLine?.trigger?.let { triggerHandler.handleTrigger(it) }

        val nextLine = resolveNext(currentLine?.next)
        currentId = nextLine?.id
        return nextLine
    }

    fun isFinished(): Boolean = current() == null

    fun choices(): List<DialogueOption> {
        val line = current() ?: return emptyList()
        val options = line.options ?: return emptyList()
        return options.filter { option ->
            conditionEvaluator.isConditionMet(option.condition)
        }
    }

    fun choose(optionId: String): DialogueLine? {
        val currentLine = current()
        val choice = choices().firstOrNull { it.id.equals(optionId, ignoreCase = true) }
            ?: return currentLine
        currentLine?.trigger?.let { triggerHandler.handleTrigger(it) }
        choice.trigger?.let { triggerHandler.handleTrigger(it) }
        val nextLine = resolveNext(choice.next)
        currentId = nextLine?.id
        return nextLine
    }

    private fun resolveNext(startId: String?): DialogueLine? {
        var nextId = startId
        while (nextId != null) {
            val candidate = dialogueById[nextId] ?: return null.also { currentId = null }
            if (conditionEvaluator.isConditionMet(candidate.condition)) {
                return candidate
            }
            candidate.trigger?.let { triggerHandler.handleTrigger(it) }
            nextId = candidate.next
        }
        currentId = null
        return null
    }
}

fun interface DialogueConditionEvaluator {
    fun isConditionMet(condition: String?): Boolean
}

fun interface DialogueTriggerHandler {
    fun handleTrigger(trigger: String)
}
