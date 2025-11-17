package com.example.starborn.domain.milestone

import com.example.starborn.data.repository.MilestoneRepository
import com.example.starborn.domain.model.MilestoneDefinition
import com.example.starborn.domain.model.MilestoneEffects
import com.example.starborn.domain.prompt.MilestonePrompt
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.starborn.domain.milestone.MilestoneEvent

class MilestoneRuntimeManager(
    private val repository: MilestoneRepository,
    private val sessionStore: GameSessionStore,
    private val promptManager: UIPromptManager,
    private val scope: CoroutineScope,
    private val applyEffects: (MilestoneEffects) -> Unit = {}
) {

    val completedMilestones: StateFlow<Set<String>> = sessionStore.state
        .map { it.completedMilestones }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())
    private val _history = MutableStateFlow<List<MilestoneEvent>>(emptyList())
    val history: StateFlow<List<MilestoneEvent>> = _history.asStateFlow()

    fun definition(id: String?): MilestoneDefinition? = repository.milestoneById(id)

    fun messageFor(id: String): String = buildMessage(id)

    fun showMilestone(milestoneId: String, message: String) {
        handleMilestone(milestoneId, message, triggerPrompt = false)
    }

    fun handleMilestone(
        milestoneId: String,
        message: String? = null,
        triggerPrompt: Boolean = false
    ): MilestoneEvent? {
        if (milestoneId.isBlank()) return null
        val event = MilestoneEvent(
            id = milestoneId,
            title = titleFor(milestoneId),
            message = message?.takeIf { it.isNotBlank() } ?: buildMessage(milestoneId),
            timestamp = System.currentTimeMillis()
        )
        recordEvent(event)
        if (triggerPrompt) {
            promptManager.enqueue(MilestonePrompt(event))
        }
        return event
    }

    fun onQuestCompleted(questId: String) {
        handleTrigger(MilestoneTriggerType.QUEST, questId)
    }

    fun onEventCompleted(eventId: String) {
        handleTrigger(MilestoneTriggerType.EVENT, eventId)
    }

    private fun handleTrigger(
        triggerType: String,
        targetId: String?
    ) {
        val id = targetId?.takeIf { it.isNotBlank() } ?: return
        val definitions = repository.definitionsForTrigger(triggerType, id)
        if (definitions.isEmpty()) return
        val alreadyCompleted = completedMilestones.value
        definitions.forEach { definition ->
            if (definition.id !in alreadyCompleted) {
                sessionStore.setMilestone(definition.id)
                handleMilestone(
                    milestoneId = definition.id,
                    message = definition.toastMessage,
                    triggerPrompt = false
                )
                definition.effects?.let(applyEffects)
            }
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    private fun titleFor(id: String): String {
        val definition = repository.milestoneById(id)
        val explicit = definition?.name?.takeIf { it.isNotBlank() }
        return explicit ?: formatIdentifier(id)
    }

    private fun buildMessage(id: String): String {
        val definition = repository.milestoneById(id)
        val toast = definition?.toastMessage?.takeIf { it.isNotBlank() }
        val description = definition?.description?.takeIf { it.isNotBlank() }
        return toast ?: description ?: "${formatIdentifier(id)} milestone updated"
    }

    private fun formatIdentifier(raw: String): String {
        val cleaned = raw.removePrefix("ms_").removePrefix("MS_")
        return cleaned
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifEmpty { raw }
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
    }

    private fun recordEvent(event: MilestoneEvent) {
        _history.update { history ->
            val updated = (history + event).takeLast(MAX_HISTORY)
            updated
        }
    }

    companion object {
        private const val MAX_HISTORY = 20
    }

    object MilestoneTriggerType {
        const val QUEST = "quest"
        const val EVENT = "event"
        const val BATTLE = "battle"
    }
}
