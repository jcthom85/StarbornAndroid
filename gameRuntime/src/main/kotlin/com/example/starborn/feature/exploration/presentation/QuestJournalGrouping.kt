package com.example.starborn.feature.exploration.presentation

/** Stable campaign groups, independent of quest titles and localized prose. */
object QuestJournalGrouping {
    private val titles = mapOf("1" to "The Mines", "2" to "Sector 9", "3" to "The Spire",
        "4" to "The Foundry", "5" to "The Void", "6" to "The Source")
    val comparator: Comparator<String> = compareBy { title ->
        titles.values.indexOf(title).takeIf { it >= 0 } ?: Int.MAX_VALUE
    }
    fun group(questId: String): String = Regex("^w([1-6])_", RegexOption.IGNORE_CASE)
        .find(questId)?.groupValues?.get(1)?.let { titles.getValue(it) } ?: "Other quests"
}
