package com.example.starborn.data.assets

import com.example.starborn.domain.fishing.FishingData

class FishingAssetDataSource(
    private val reader: AssetJsonReader
) {

    fun loadFishingData(): FishingData {
        return reader.readObject<FishingData>("recipes_fishing.json") ?: FishingData()
    }
}
