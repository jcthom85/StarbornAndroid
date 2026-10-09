package com.example.starborn.data.assets

import com.example.starborn.domain.model.Quest

class QuestAssetDataSource(
    private val assetReader: AssetJsonReader
) {
    fun loadQuests(): List<Quest> = assetReader.readList("quests.json")
}
