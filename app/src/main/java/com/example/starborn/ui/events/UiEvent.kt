package com.example.starborn.ui.events

sealed interface UiEvent {
    data class ShowQuestBanner(
        val type: QuestBannerType,
        val questId: String,
        val questTitle: String,
        val objectives: List<QuestObjectiveStatus> = emptyList()
    ) : UiEvent

    data class ShowToast(
        val id: String,
        val text: String
    ) : UiEvent

    data class ShowQuestSummary(
        val entries: List<QuestSummaryEntry>
    ) : UiEvent

    data class JournalBadgeDelta(
        val delta: Int
    ) : UiEvent

    data class ShowQuestDetail(
        val questId: String,
        val type: QuestBannerType,
        val questTitle: String,
        val summary: String,
        val objectives: List<String>
    ) : UiEvent
}

enum class QuestBannerType { NEW, COMPLETED, FAILED, PROGRESS }

data class QuestSummaryEntry(
    val questId: String,
    val type: SummaryType,
    val questTitle: String,
    val objectiveTitle: String? = null
)

enum class SummaryType { ACCEPTED, UPDATED, COMPLETED, FAILED }

data class QuestObjectiveStatus(
    val id: String,
    val text: String,
    val completed: Boolean
)
