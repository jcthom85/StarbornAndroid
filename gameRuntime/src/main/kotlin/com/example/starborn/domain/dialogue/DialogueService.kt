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

    private val referencedNextIds: Set<String> = buildSet {
        for (line in dialogueLines) {
            line.next?.let { add(it) }
            line.options?.forEach { option ->
                option.next?.let { add(it) }
            }
        }
    }

    /** Honor an NPC's declared talk speaker when its display name has no dialogue catalog. */
    fun resolveNpcSpeaker(displayName: String, declaredLineId: String?): String {
        if (dialogueBySpeaker.containsKey(displayName.lowercase(Locale.getDefault()))) return displayName
        return declaredLineId?.let { dialogueById[it]?.speaker } ?: displayName
    }

    /** Scripted radio calls enter a specific line without changing the NPC's normal greeting. */
    fun startDialogueAt(lineId: String): DialogueSession? {
        val line = dialogueById[lineId] ?: return null
        if (!conditionEvaluator.isConditionMet(line.condition)) return null
        return DialogueSession(dialogueById, line, conditionEvaluator, triggerHandler)
    }

    fun startDialogue(speaker: String): DialogueSession? {
        val entries = dialogueBySpeaker[speaker.lowercase(Locale.getDefault())] ?: return null
        // Default greetings can be referenced by looping conversation topics and still be entry points.
        val candidates = entries.filter { !it.scriptedOnly && (it.id !in referencedNextIds || it.id.endsWith("_default_talk")) }
        val (conditioned, unconditioned) = candidates.partition { !it.condition.isNullOrBlank() }
        // Continue an eligible quest before offering another one. Preserve author
        // order within each group; an old optional offer must not hide progress.
        val ordered = conditioned.sortedByDescending { line ->
            line.condition.orEmpty().split(',').any { token ->
                token.trim().substringBefore(':') in setOf("quest", "quest_active")
            }
        } + unconditioned
        val start = ordered.firstOrNull { conditionEvaluator.isConditionMet(it.condition) } ?: return null
        return DialogueSession(dialogueById, start, conditionEvaluator, triggerHandler)
    }
}

class DialogueSession(
    private val dialogueById: Map<String, DialogueLine>,
    start: DialogueLine,
    private val conditionEvaluator: DialogueConditionEvaluator,
    private val triggerHandler: DialogueTriggerHandler
) {
    companion object { const val RESPONSE_EXIT_ID = "__response_leave" }
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
        val available = options.filter { conditionEvaluator.isConditionMet(it.condition) }
        if (available.isEmpty()) return emptyList()
        val exits = available.filter { option ->
            option.next == null && option.trigger.isNullOrBlank() &&
                (option.id.contains("leave", true) || option.text.contains("later", true) ||
                    option.text.contains("step away", true) || option.text.contains("never mind", true) ||
                    option.text.contains("busy", true))
        }
        return if (exits.isNotEmpty()) available.filterNot { it in exits } + exits
        else {
            val isControl = listOf("control", "console", "terminal").any { line.speaker.contains(it, true) }
            available + DialogueOption(RESPONSE_EXIT_ID,
                if (isControl) "Step away from the controls." else "Let's leave it there for now.")
        }
    }

    fun choose(optionId: String): DialogueLine? {
        val currentLine = current()
        val choice = choices().firstOrNull { it.id.equals(optionId, ignoreCase = true) }
            ?: return currentLine
        if (choice.id == RESPONSE_EXIT_ID) {
            currentId = null
            return null
        }
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
