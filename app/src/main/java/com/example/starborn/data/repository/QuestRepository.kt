package com.example.starborn.data.repository

import com.example.starborn.data.assets.QuestAssetDataSource
import com.example.starborn.domain.model.Quest

class QuestRepository(
    private val dataSource: QuestAssetDataSource
) {
    private val questsById: MutableMap<String, Quest> = mutableMapOf()

    fun load() {
        val quests = dataSource.loadQuests()
        questsById.clear()
        quests.forEach { quest ->
            questsById[quest.id] = quest
            questsById[quest.id.lowercase(java.util.Locale.US)] = quest
        }
    }

    fun questById(id: String): Quest? = questsById[id] ?: questsById[id.lowercase(java.util.Locale.US)]

    fun allQuests(): Collection<Quest> = questsById.values
}
