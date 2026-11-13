package com.example.starborn.ui.events

import androidx.compose.runtime.*

/** Survives exploration leaving composition (combat, hubs and side activities). */
class QuestPresentationState {
    sealed interface Important {
        data class Detail(val event: UiEvent.ShowQuestDetail) : Important
        data class Summary(val event: UiEvent.ShowQuestSummary) : Important
    }
    val important = mutableStateListOf<Important>()
    val updates = mutableStateListOf<UiEvent.ShowQuestBanner>()
    val toasts = mutableStateListOf<UiEvent.ShowToast>()
    private val recentUpdates = mutableMapOf<String, Long>()
    private val started = mutableSetOf<Important>()
    private val acceptedDetails = mutableSetOf<Pair<String, QuestBannerType>>()
    private val finishedQuests = mutableSetOf<String>()
    private val acknowledgedObjectives = mutableSetOf<Pair<String, String>>()
    var journalBadge by mutableIntStateOf(0)
        private set

    fun accept(event: UiEvent, now: Long = System.nanoTime() / 1_000_000) {
        when (event) {
            is UiEvent.ShowQuestDetail -> {
                if (event.type == QuestBannerType.NEW && finishedQuests.remove(event.questId)) {
                    acknowledgedObjectives.removeAll { it.first == event.questId }
                    acceptedDetails.removeAll { it.first == event.questId }
                }
                if (!acceptedDetails.add(event.questId to event.type)) return
                if (event.type == QuestBannerType.COMPLETED || event.type == QuestBannerType.FAILED) {
                    finishedQuests.add(event.questId)
                    updates.removeAll { it.questId == event.questId }
                }
                if (important.none {
                    it is Important.Detail && it.event.questId == event.questId && it.event.type == event.type
                }) important.add(Important.Detail(event))
            }
            is UiEvent.ShowQuestSummary -> if (event.entries.isNotEmpty() && important.none { it is Important.Summary && it.event == event }) important.add(Important.Summary(event))
            is UiEvent.ShowQuestBanner -> if (event.type == QuestBannerType.PROGRESS && event.questId !in finishedQuests) {
                val key = "${event.type}:${event.questId}"
                val pending = updates.indexOfFirst { it.questId == event.questId }
                val checked = event.checkedObjectives()
                if (pending >= 0) {
                    // Stage advancement often arrives beside task credit. Keep any checkmarks
                    // that have yet to be shown alongside the latest next objective.
                    val unshown = updates[pending].checkedObjectives()
                        .filter { checked.isEmpty() || (event.questId to it.id) !in acknowledgedObjectives }
                    val merged = (unshown + checked).distinctBy { it.id }
                    updates[pending] = if (merged.isEmpty()) event else event.copy(
                        objectives = merged + event.objectives.filter { objective ->
                            objective.role != QuestObjectiveRole.JUST_COMPLETED && merged.none { it.id == objective.id }
                        }
                    )
                    recentUpdates[key] = now
                } else if (checked.any { (event.questId to it.id) !in acknowledgedObjectives } ||
                    recentUpdates[key]?.let { now - it <= 2000 } != true) {
                    recentUpdates[key] = now
                    updates.add(event)
                }
                recentUpdates.entries.removeAll { now - it.value > 2000 }
            }
            is UiEvent.ShowToast -> if (toasts.none { it.id == event.id && it.text == event.text }) toasts.add(event)
            is UiEvent.JournalBadgeDelta -> journalBadge = (journalBadge + event.delta).coerceAtLeast(0)
        }
    }
    fun markObjectivesShown(update: UiEvent.ShowQuestBanner): Boolean {
        var newlyShown = false
        for (objective in update.checkedObjectives()) {
            if (acknowledgedObjectives.add(update.questId to objective.id)) newlyShown = true
        }
        return newlyShown
    }

    fun markStarted(entry: Important) = started.add(entry)
    fun dismissImportant() { important.firstOrNull()?.let(::dismissImportant) }
    fun dismissImportant(expected: Important) {
        if (important.firstOrNull() != expected) return
        started.remove(expected)
        important.removeAt(0)
    }
    fun readJournal() { journalBadge = 0 }
    fun clear() {
        important.clear(); updates.clear(); toasts.clear(); recentUpdates.clear()
        started.clear(); acceptedDetails.clear(); finishedQuests.clear(); acknowledgedObjectives.clear(); journalBadge = 0
    }
}

internal fun UiEvent.ShowQuestBanner.checkedObjectives() =
    objectives.filter { it.completed && it.role == QuestObjectiveRole.JUST_COMPLETED }
