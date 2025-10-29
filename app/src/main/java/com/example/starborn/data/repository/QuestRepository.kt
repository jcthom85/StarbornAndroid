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
        quests.forEach { quest -> questsById[quest.id] = quest }
    }

    fun questById(id: String): Quest? = questsById[id]

    fun allQuests(): Collection<Quest> = questsById.values
}
