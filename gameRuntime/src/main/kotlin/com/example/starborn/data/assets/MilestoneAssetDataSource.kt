package com.example.starborn.data.assets

import com.example.starborn.domain.model.MilestoneDefinition

class MilestoneAssetDataSource(
    private val assetReader: AssetJsonReader
) {
    fun loadMilestones(): List<MilestoneDefinition> =
        assetReader.readList("milestones.json")
}
