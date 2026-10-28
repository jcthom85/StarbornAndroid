package com.example.starborn.feature.exploration.presentation

/** Stable campaign groups, independent of quest titles and localized prose. */
object QuestJournalGrouping {
    fun group(questId: String): String = Regex("^w([1-6])_", RegexOption.IGNORE_CASE)
        .find(questId)?.groupValues?.get(1)?.let { "World $it" } ?: "Other quests"
}
