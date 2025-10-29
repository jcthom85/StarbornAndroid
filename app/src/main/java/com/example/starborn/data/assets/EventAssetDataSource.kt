package com.example.starborn.data.assets

import com.example.starborn.domain.model.GameEvent

class EventAssetDataSource(
    private val assetReader: AssetJsonReader
) {
    fun loadEvents(): List<GameEvent> = assetReader.readList("events.json")
}
