package com.example.starborn.domain.tutorial

import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.prompt.UIPromptState
import com.example.starborn.domain.session.GameSessionStore
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TutorialRuntimeManager(
    private val sessionStore: GameSessionStore,
    private val promptManager: UIPromptManager,
    private val scripts: TutorialScriptRepository?,
    private val scope: CoroutineScope
) {

    private val lock = Any()
    private val scheduledJobs: MutableMap<String, Job> = mutableMapOf()
    private val _runtimeState = MutableStateFlow(TutorialRuntimeState())
    val runtimeState: StateFlow<TutorialRuntimeState> = _runtimeState.asStateFlow()

    val completedTutorials: StateFlow<Set<String>> = sessionStore.state
        .map { it.tutorialCompleted }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())
    val seenTutorials: StateFlow<Set<String>> = sessionStore.state
        .map { it.tutorialSeen }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    init {
        scope.launch {
            combine(
                sessionStore.state,
                promptManager.state
            ) { session, promptState ->
                buildRuntimeState(session.tutorialSeen, session.tutorialCompleted, session.tutorialRoomsSeen, promptState)
            }.collect { state ->
                _runtimeState.value = state
            }
        }
    }

    fun enqueue(
        entry: TutorialEntry,
        allowDuplicates: Boolean = false,
        onDismiss: (() -> Unit)? = null
    ) {
        if (entry.key != null && hasCompleted(entry.key)) return
        val normalizedKey = entry.key?.normalized()
        if (!allowDuplicates && normalizedKey != null) {
            if (completedTutorials.value.any { it.normalized() == normalizedKey }) {
                return
            }
        }
        entry.key?.let { sessionStore.markTutorialSeen(it) }
        promptManager.enqueue(TutorialPrompt(entry, onDismiss), allowDuplicates)
    }

    fun markRoomVisited(roomId: String) {
        sessionStore.markTutorialRoomVisited(roomId)
    }

    fun hasCompleted(key: String): Boolean =
        completedTutorials.value.any { it.equals(key, ignoreCase = true) }

    fun hasSeen(key: String): Boolean =
        seenTutorials.value.any { it.equals(key, ignoreCase = true) }

    fun markCompleted(key: String) {
        if (key.isBlank()) return
        sessionStore.markTutorialCompleted(key)
    }

    fun showOnce(
        key: String,
        message: String,
        context: String? = null,
        metadata: Map<String, String> = emptyMap(),
        delayMs: Long = 0L,
        onDismiss: (() -> Unit)? = null
    ) {
        if (hasCompleted(key)) return
        val entry = TutorialEntry(
            key = key,
            context = context,
            message = message,
            metadata = metadata
        )
        showOnce(entry, allowDuplicates = false, delayMs = delayMs, onDismiss = onDismiss)
    }

    fun showOnce(
        entry: TutorialEntry,
        allowDuplicates: Boolean = false,
        delayMs: Long = 0L,
        onDismiss: (() -> Unit)? = null
    ) {
        val key = entry.key
        if (key != null && hasCompleted(key)) return
        if (delayMs > 0) {
            schedule(
                entry.key ?: entry.hashCode().toString(),
                delayMs,
                allowDuplicates = allowDuplicates,
                onDismiss = onDismiss
            ) { entry }
        } else {
            enqueue(entry, allowDuplicates, onDismiss)
        }
    }

    fun playScript(
        scriptId: String,
        allowDuplicates: Boolean = false,
        onComplete: (() -> Unit)? = null
    ): Boolean {
        val script = scripts?.script(scriptId) ?: return false
        var scheduled = false
        script.steps.forEachIndexed { index, step ->
            val entry = TutorialEntry(
                key = step.key ?: scriptId,
                context = step.context,
                message = step.message,
                metadata = step.metadata
            )
            val delayMs = step.delayMs ?: 0L
            val isLast = index == script.steps.lastIndex
            val completionCallback = if (isLast) onComplete else null
            showOnce(entry, allowDuplicates, delayMs, completionCallback)
            scheduled = true
        }
        if (!scheduled) {
            onComplete?.invoke()
        }
        return scheduled
    }

    fun scheduleScript(
        key: String,
        scriptId: String,
        delayMs: Long,
        allowDuplicates: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        schedule(
            key = key,
            delayMs = delayMs,
            allowDuplicates = allowDuplicates
        ) {
            playScript(scriptId, allowDuplicates, onComplete)
            null
        }
    }

    fun schedule(
        key: String,
        delayMs: Long,
        allowDuplicates: Boolean = false,
        onDismiss: (() -> Unit)? = null,
        builder: () -> TutorialEntry?
    ) {
        if (delayMs <= 0) {
            builder()?.let { enqueue(it, allowDuplicates, onDismiss) }
            return
        }
        synchronized(lock) {
            scheduledJobs[key]?.cancel()
            val job = scope.launch {
                delay(delayMs)
                val entry = builder()
                if (entry != null) {
                    enqueue(entry, allowDuplicates, onDismiss)
                }
                synchronized(lock) {
                    scheduledJobs.remove(key)
                }
            }
            scheduledJobs[key] = job
        }
    }

    fun cancel(key: String) {
        synchronized(lock) {
            scheduledJobs.remove(key)?.cancel()
        }
    }

    fun cancelAllScheduled() {
        synchronized(lock) {
            scheduledJobs.values.forEach { it.cancel() }
            scheduledJobs.clear()
        }
    }

    private fun buildRuntimeState(
        seen: Set<String>,
        completed: Set<String>,
        roomsVisited: Set<String>,
        promptState: UIPromptState
    ): TutorialRuntimeState {
        val currentPrompt = promptState.current as? TutorialPrompt
        val current = currentPrompt?.entry
        val queue = promptState.queue.filterIsInstance<TutorialPrompt>().map { it.entry }
        return TutorialRuntimeState(
            current = current,
            queue = queue,
            seen = seen,
            completed = completed,
            roomsVisited = roomsVisited
        )
    }

    private fun String.normalized(): String = lowercase(Locale.getDefault())
}
