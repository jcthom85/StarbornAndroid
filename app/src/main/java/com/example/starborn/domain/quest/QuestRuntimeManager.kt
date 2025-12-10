package com.example.starborn.domain.quest

import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.QuestStage
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.ui.events.QuestBannerType
import com.example.starborn.ui.events.QuestSummaryEntry
import com.example.starborn.ui.events.UiEvent
import com.example.starborn.ui.events.UiEventBus
import com.example.starborn.ui.events.QuestObjectiveStatus
import java.util.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class QuestObjectiveEntry(
    val id: String,
    val text: String,
    val completed: Boolean
)

data class QuestJournalEntry(
    val id: String,
    val title: String,
    val summary: String,
    val stageTitle: String?,
    val stageDescription: String?,
    val objectives: List<QuestObjectiveEntry>,
    val completed: Boolean,
    val stageIndex: Int,
    val totalStages: Int
)

data class QuestLogEntry(
    val questId: String,
    val message: String,
    val stageId: String?,
    val timestamp: Long,
    val type: QuestLogEntryType = QuestLogEntryType.UPDATE,
    val questTitle: String? = null
)

enum class QuestLogEntryType {
    UPDATE,
    NEW_QUEST,
    QUEST_COMPLETED
}

data class QuestRuntimeState(
    val activeQuestIds: Set<String> = emptySet(),
    val completedQuestIds: Set<String> = emptySet(),
    val failedQuestIds: Set<String> = emptySet(),
    val trackedQuestId: String? = null,
    val activeJournal: List<QuestJournalEntry> = emptyList(),
    val completedJournal: List<QuestJournalEntry> = emptyList(),
    val stageProgress: Map<String, String> = emptyMap(),
    val completedTasks: Map<String, Set<String>> = emptyMap(),
    val recentLog: List<QuestLogEntry> = emptyList()
)

class QuestRuntimeManager(
    private val questRepository: QuestRepository,
    private val sessionStore: GameSessionStore,
    private val scope: CoroutineScope,
    private val uiEventBus: UiEventBus
) {

    private companion object {
        // Flip to true to restore quest banner notifications.
        private const val SHOW_QUEST_BANNERS = false
        // Keep progress banners visible so players see objectives completing.
        private const val SHOW_PROGRESS_BANNERS = true
        private const val MAX_LOG_ENTRIES = 20
    }

    private val mutex = Mutex()
    private val stageProgress: MutableMap<String, String> = mutableMapOf()
    private val completedTasks: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private val recentLog: ArrayDeque<QuestLogEntry> = ArrayDeque()
    private val questCompletionListeners: MutableSet<(String) -> Unit> = mutableSetOf()

    private val _state = MutableStateFlow(QuestRuntimeState())
    val state: StateFlow<QuestRuntimeState> = _state.asStateFlow()

    init {
        scope.launch {
            sessionStore.state.collect { session ->
                val seededStages = mutableListOf<Pair<String, String>>()
                mutex.withLock {
                    val trackedIds = session.activeQuests + session.completedQuests
                    // Remove data for quests that are no longer tracked.
                    stageProgress.keys.retainAll(trackedIds)
                    completedTasks.keys.retainAll(trackedIds)

                    // Merge persisted stage/task data from the session.
                    session.questStageById.forEach { (questId, stageId) ->
                        if (trackedIds.contains(questId)) {
                            stageProgress[questId] = stageId
                        }
                    }
                    session.questTasksCompleted.forEach { (questId, tasks) ->
                        if (trackedIds.contains(questId)) {
                            completedTasks[questId] = tasks.toMutableSet()
                        }
                    }

                    // Ensure quest progression reflects completed tasks and stages.
                    val newlyCompleted = mutableListOf<String>()
                    trackedIds.forEach { questId ->
                        if (!session.completedQuests.contains(questId)) {
                            val quest = questRepository.questById(questId)
                            maybeProgressQuestLocked(
                                questId = questId,
                                quest = quest,
                                completedTasks = completedTasks[questId].orEmpty(),
                                newlyCompleted = newlyCompleted
                            )
                        }
                    }

                    // Seed defaults for any active/completed quest missing stage info.
                    trackedIds.forEach { questId ->
                        if (!stageProgress.containsKey(questId)) {
                            val seeded = ensureStageDefault(questId, persist = false)
                            if (seeded != null) seededStages += questId to seeded
                        }
                    }

                    emitStateLocked(session)
                    // Notify outside the lock after emitStateLocked
                    val listeners = questCompletionListeners.toList()
                    newlyCompleted.forEach { completedId ->
                        listeners.forEach { it(completedId) }
                    }
                }
                seededStages.forEach { (questId, stageId) ->
                    sessionStore.setQuestStage(questId, stageId)
                }
            }
        }
    }


    fun resetQuest(questId: String) {
        scope.launch {
            mutex.withLock {
                completedTasks.remove(questId)
                ensureStageDefault(questId)
                emitStateLocked(sessionStore.state.value)
            }
            sessionStore.clearQuestTasks(questId)
        }
    }

    fun setStage(questId: String, stageId: String) {
        scope.launch {
            mutex.withLock {
                val previous = stageProgress[questId]
                if (previous == stageId) return@withLock
                stageProgress[questId] = stageId
                logStageChange(questId, stageId)
                emitStateLocked(sessionStore.state.value)
                emitQuestProgressBanner(questId, questRepository.questById(questId))
            }
            sessionStore.setQuestStage(questId, stageId)
        }
    }

    fun markTaskComplete(questId: String, taskId: String) {
        scope.launch {
            val newlyCompleted = mutableListOf<String>()
            mutex.withLock {
                if (sessionStore.state.value.completedQuests.contains(questId)) return@withLock
                val tasks = completedTasks.getOrPut(questId) { mutableSetOf() }
                if (tasks.add(taskId)) {
                    val quest = questRepository.questById(questId)
                    appendLog(
                        questId,
                        "Objective completed",
                        stageId = stageProgress[questId]
                    )
                    sessionStore.setQuestTaskCompleted(questId, taskId, completed = true)
                    maybeProgressQuestLocked(questId, quest, tasks, newlyCompleted)
                    emitStateLocked(sessionStore.state.value)
                    emitQuestProgressBanner(questId, quest)
                }
            }
            newlyCompleted.forEach { notifyQuestCompleted(it) }
        }
    }

    fun markQuestCompleted(questId: String) {
        scope.launch {
            mutex.withLock {
                val quest = questRepository.questById(questId) ?: return@withLock
                val finalStage = quest.stages.lastOrNull()
                if (finalStage != null) {
                    stageProgress[questId] = finalStage.id
                    sessionStore.setQuestStage(questId, finalStage.id)
                    val tasks = completedTasks.getOrPut(questId) { mutableSetOf() }
                    finalStage.tasks.forEach { task -> tasks.add(task.id) }
                    sessionStore.setQuestTasksCompleted(questId, tasks.toSet())
                }
                emitStateLocked(sessionStore.state.value)
            }
        }
    }

    fun markQuestFailed(questId: String, reason: String? = null) {
        scope.launch {
            mutex.withLock {
                sessionStore.failQuest(questId)
                val message = reason?.takeIf { it.isNotBlank() } ?: "Quest failed"
                appendLog(questId, message, stageProgress[questId])
                emitStateLocked(sessionStore.state.value)
                val title = questRepository.questById(questId)?.title?.takeIf { it.isNotBlank() } ?: questId
                if (SHOW_QUEST_BANNERS) {
                    uiEventBus.tryEmit(
                        UiEvent.ShowQuestBanner(
                            type = QuestBannerType.FAILED,
                            questId = questId,
                            questTitle = title
                        )
                    )
                }
                uiEventBus.tryEmit(UiEvent.JournalBadgeDelta(+1))
            }
        }
    }

    fun currentStageId(questId: String): String? = stageProgress[questId]

    fun completedTaskIds(questId: String): Set<String> = completedTasks[questId]?.toSet().orEmpty()

    fun trackQuest(questId: String?) {
        scope.launch {
            mutex.withLock {
                sessionStore.setTrackedQuest(questId)
                emitStateLocked(sessionStore.state.value)
            }
        }
    }

    fun postSceneQuestSummary(entries: List<QuestSummaryEntry>) {
        if (entries.isEmpty()) return
        uiEventBus.tryEmit(UiEvent.ShowQuestSummary(entries))
    }

    fun recordQuestStarted(questId: String) {
        scope.launch {
            mutex.withLock {
                if (questId.isBlank()) return@withLock
                completedTasks.remove(questId)
                stageProgress.remove(questId)
                ensureStageDefault(questId)
                val quest = questRepository.questById(questId)
                val title = quest?.title?.takeIf { it.isNotBlank() }
                val message = title?.let { "New Quest: $it" } ?: "New quest started"
                appendLog(
                    questId = questId,
                    message = message,
                    stageId = stageProgress[questId],
                    type = QuestLogEntryType.NEW_QUEST,
                    questTitle = title
                )
                emitStateLocked(sessionStore.state.value)
                if (SHOW_QUEST_BANNERS) {
                    val bannerTitle = title ?: questId
                    uiEventBus.tryEmit(
                        UiEvent.ShowQuestBanner(
                            type = QuestBannerType.NEW,
                            questId = questId,
                            questTitle = bannerTitle
                        )
                    )
                }
                uiEventBus.tryEmit(UiEvent.JournalBadgeDelta(+1))
                quest?.let {
                    val stage = resolveStage(it, completed = false)
                    emitQuestDetail(QuestBannerType.NEW, it, stage)
                }
            }
        }
        sessionStore.clearQuestTasks(questId)
    }

    fun recordQuestCompleted(questId: String) {
        scope.launch {
            mutex.withLock {
                appendQuestCompletedLog(questId)
                emitStateLocked(sessionStore.state.value)
                val quest = questRepository.questById(questId)
                val title = quest?.title?.takeIf { it.isNotBlank() } ?: questId
                if (SHOW_QUEST_BANNERS) {
                    uiEventBus.tryEmit(
                        UiEvent.ShowQuestBanner(
                            type = QuestBannerType.COMPLETED,
                            questId = questId,
                            questTitle = title
                        )
                    )
                }
                uiEventBus.tryEmit(UiEvent.JournalBadgeDelta(+1))
                quest?.let {
                    val stage = it.stages.lastOrNull()
                    emitQuestDetail(QuestBannerType.COMPLETED, it, stage)
                }
            }
            notifyQuestCompleted(questId)
        }
    }

    fun addQuestCompletionListener(listener: (String) -> Unit) {
        questCompletionListeners.add(listener)
    }

    fun removeQuestCompletionListener(listener: (String) -> Unit) {
        questCompletionListeners.remove(listener)
    }

    private fun notifyQuestCompleted(questId: String) {
        questCompletionListeners.toList().forEach { it(questId) }
    }

    fun resetAll() {
        scope.launch {
            mutex.withLock {
                stageProgress.clear()
                completedTasks.clear()
                recentLog.clear()
                emitStateLocked(sessionStore.state.value)
            }
            sessionStore.resetQuestProgress()
        }
    }

    private fun emitStateLocked(session: GameSessionState) {
        val active = buildJournalEntries(session.activeQuests, false)
        val completed = buildJournalEntries(session.completedQuests, true)
        _state.value = QuestRuntimeState(
            activeQuestIds = session.activeQuests,
            completedQuestIds = session.completedQuests,
            failedQuestIds = session.failedQuests,
            trackedQuestId = session.trackedQuestId,
            activeJournal = active,
            completedJournal = completed,
            stageProgress = stageProgress.toMap(),
            completedTasks = completedTasks.mapValues { it.value.toSet() },
            recentLog = recentLog.toList()
        )
    }

    private fun buildJournalEntries(ids: Set<String>, completed: Boolean): List<QuestJournalEntry> {
        return ids.mapNotNull { questRepository.questById(it) }.map { quest ->
            val stage = resolveStage(quest, completed)
            val tasks = stage?.tasks.orEmpty()
            val completedIds = completedTasks[quest.id].orEmpty()
            QuestJournalEntry(
                id = quest.id,
                title = quest.title,
                summary = quest.summary,
                stageTitle = stage?.title,
                stageDescription = stage?.description,
                objectives = tasks.map { task ->
                    QuestObjectiveEntry(
                        id = task.id,
                        text = task.text,
                        completed = completed || task.done || completedIds.contains(task.id)
                    )
                },
                completed = completed,
                stageIndex = quest.stages.indexOf(stage).takeIf { it >= 0 } ?: 0,
                totalStages = quest.stages.size.coerceAtLeast(1)
            )
        }
    }

    private fun resolveStage(quest: Quest, completed: Boolean): QuestStage? {
        if (quest.stages.isEmpty()) return null
        if (completed) {
            return quest.stages.lastOrNull()
        }
        val stageId = stageProgress[quest.id]
        return quest.stages.firstOrNull { it.id == stageId } ?: quest.stages.firstOrNull()
    }

    private fun ensureStageDefault(
        questId: String,
        persist: Boolean = true
    ): String? {
        val existing = stageProgress[questId]
        if (existing != null) return existing
        val quest = questRepository.questById(questId) ?: return null
        val first = quest.stages.firstOrNull() ?: return null
        stageProgress[questId] = first.id
        if (persist) {
            sessionStore.setQuestStage(questId, first.id)
        }
        return first.id
    }

    private fun logStageChange(questId: String, stageId: String) {
        val quest = questRepository.questById(questId) ?: return
        val stage = quest.stages.firstOrNull { it.id == stageId } ?: return
        appendLog(questId, "Advanced to stage \"${stage.title}\"", stageId)
    }

    private fun appendLog(
        questId: String,
        message: String,
        stageId: String?,
        type: QuestLogEntryType = QuestLogEntryType.UPDATE,
        questTitle: String? = null
    ) {
        val logEntry = QuestLogEntry(
            questId = questId,
            message = message,
            stageId = stageId,
            timestamp = System.currentTimeMillis(),
            type = type,
            questTitle = questTitle ?: questRepository.questById(questId)?.title?.takeIf { it.isNotBlank() }
        )
        if (recentLog.size >= MAX_LOG_ENTRIES) {
            recentLog.removeFirst()
        }
        recentLog.addLast(logEntry)
    }

    private fun appendQuestCompletedLog(questId: String) {
        if (questId.isBlank()) return
        val quest = questRepository.questById(questId) ?: return
        val finalStage = quest.stages.lastOrNull()
        val title = quest.title.takeIf { it.isNotBlank() }
        val message = title?.let { "Quest Completed: $it" } ?: "Quest completed"
        appendLog(
            questId = questId,
            message = message,
            stageId = finalStage?.id,
            type = QuestLogEntryType.QUEST_COMPLETED,
            questTitle = title
        )
    }

    private fun maybeMarkQuestComplete(
        questId: String,
        quest: Quest?,
        completedTasks: Set<String>
    ) = maybeProgressQuestLocked(questId, quest, completedTasks)

    private fun maybeProgressQuestLocked(
        questId: String,
        quest: Quest?,
        completedTasks: Set<String>,
        newlyCompleted: MutableList<String> = mutableListOf()
    ) {
        val session = sessionStore.state.value
        if (session.completedQuests.contains(questId)) return
        val data = quest ?: questRepository.questById(questId) ?: return
        val stages = data.stages
        if (stages.isEmpty()) return

        var currentStageId = stageProgress[questId] ?: stages.first().id
        var currentIndex = stages.indexOfFirst { it.id == currentStageId }.let { idx ->
            if (idx >= 0) idx else 0
        }
        var progressed = false
        while (true) {
            val stage = stages.getOrNull(currentIndex) ?: break
            val allDone = stage.tasks.all { task -> task.done || completedTasks.contains(task.id) }
            val isLast = currentIndex == stages.lastIndex
            if (!allDone) break

            if (isLast) {
                completeQuestLocked(questId, data, completedTasks, newlyCompleted)
                return
            }

            val nextStage = stages[currentIndex + 1]
            stageProgress[questId] = nextStage.id
            sessionStore.setQuestStage(questId, nextStage.id)
            logStageChange(questId, nextStage.id)
            currentStageId = nextStage.id
            currentIndex += 1
            progressed = true
        }

        if (progressed) {
            val tasks = this.completedTasks.getOrPut(questId) { mutableSetOf() }
            tasks.addAll(completedTasks)
        }
    }

    private fun completeQuestLocked(
        questId: String,
        quest: Quest,
        completedTasks: Set<String>,
        newlyCompleted: MutableList<String>
    ) {
        if (sessionStore.state.value.completedQuests.contains(questId)) return
        sessionStore.completeQuest(questId)
        val finalStage = quest.stages.lastOrNull()
        finalStage?.let { stageProgress[questId] = it.id }
        finalStage?.let { stage ->
            this.completedTasks.getOrPut(questId) { mutableSetOf() }.apply {
                addAll(completedTasks)
                stage.tasks.forEach { add(it.id) }
                sessionStore.setQuestTasksCompleted(questId, this.toSet())
            }
            sessionStore.setQuestStage(questId, stage.id)
        }
        appendQuestCompletedLog(questId)
        emitStateLocked(sessionStore.state.value)
        val title = quest.title.takeIf { it.isNotBlank() } ?: questId
        if (SHOW_QUEST_BANNERS) {
            uiEventBus.tryEmit(
                UiEvent.ShowQuestBanner(
                    type = QuestBannerType.COMPLETED,
                    questId = questId,
                    questTitle = title
                )
            )
        }
        uiEventBus.tryEmit(UiEvent.JournalBadgeDelta(+1))
        val stage = quest.stages.lastOrNull()
        emitQuestDetail(QuestBannerType.COMPLETED, quest, stage)
        newlyCompleted.add(questId)
    }

    private fun emitQuestDetail(
        type: QuestBannerType,
        quest: Quest,
        stage: QuestStage?
    ) {
        val summary = quest.summary.takeIf { it.isNotBlank() }
            ?: quest.description.takeIf { it.isNotBlank() }
            ?: ""
        val objectives = stage?.tasks.orEmpty()
            .map { it.text }
            .filter { it.isNotBlank() }
        uiEventBus.tryEmit(
            UiEvent.ShowQuestDetail(
                questId = quest.id,
                type = type,
                questTitle = quest.title,
                summary = summary,
                objectives = objectives
            )
        )
    }

    private fun emitQuestProgressBanner(
        questId: String,
        quest: Quest? = null
    ) {
        if (!SHOW_PROGRESS_BANNERS) return
        val data = quest ?: questRepository.questById(questId) ?: return
        val objectives = questObjectivesForBanner(questId, data)
        uiEventBus.tryEmit(
            UiEvent.ShowQuestBanner(
                type = QuestBannerType.PROGRESS,
                questId = questId,
                questTitle = data.title,
                objectives = objectives
            )
        )
    }

    private fun questObjectivesForBanner(questId: String, quest: Quest): List<QuestObjectiveStatus> {
        val stageId = stageProgress[questId]
        val stage = quest.stages.firstOrNull { it.id == stageId } ?: quest.stages.firstOrNull()
        val completed = completedTasks[questId].orEmpty()
        return stage?.tasks.orEmpty().map { task ->
            QuestObjectiveStatus(
                id = task.id,
                text = task.text,
                completed = task.done || completed.contains(task.id)
            )
        }
    }
}
