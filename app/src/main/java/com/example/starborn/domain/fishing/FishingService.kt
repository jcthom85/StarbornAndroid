package com.example.starborn.domain.fishing

import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.domain.inventory.InventoryService
import kotlin.math.roundToInt
import kotlin.random.Random

class FishingService(
    private val fishingAssetDataSource: FishingAssetDataSource,
    private val inventoryService: InventoryService,
    private val random: Random = Random.Default
) {

    private val fishingData: FishingData by lazy { fishingAssetDataSource.loadFishingData() }

    fun getAvailableRods(): List<FishingRod> {
        return fishingData.rods.filter { inventoryService.hasItem(it.id) }
    }

    fun getAvailableLures(): List<FishingLure> {
        return fishingData.lures.filter { inventoryService.hasItem(it.id) }
    }

    fun getFishingZone(zoneId: String): FishingZone? {
        return fishingData.zones.firstOrNull { it.id == zoneId }
    }

    fun getCatchResult(
        zone: FishingZone,
        rod: FishingRod,
        lure: FishingLure,
        minigameResult: MinigameResult
    ): FishingResult? {
        val weightedCatches = zone.catches.flatMap { catchDef ->
            val lureMultiplier = (1.0 + lure.rarityBonus).coerceAtLeast(0.1)
            val adjustedWeight = (catchDef.weight * lureMultiplier).roundToInt().coerceAtLeast(1)
            List(adjustedWeight) { catchDef }
        }
        if (weightedCatches.isEmpty()) return null

        val selectedCatch = weightedCatches[random.nextInt(weightedCatches.size)]

        val quantity = random.nextInt(selectedCatch.minQuantity, selectedCatch.maxQuantity + 1)

        // Apply minigame result and rod/lure bonuses
        val baseQuantity = when (minigameResult) {
            MinigameResult.PERFECT -> (quantity * 1.5).toInt()
            MinigameResult.SUCCESS -> quantity
            MinigameResult.FAIL -> (quantity * 0.5).toInt().coerceAtLeast(0)
        }
        val rodMultiplier = rod.power.coerceIn(0.5, 3.0)
        val adjustedQuantity = (baseQuantity * rodMultiplier).roundToInt()
        val ensuredQuantity = if (minigameResult == MinigameResult.FAIL) {
            adjustedQuantity.coerceAtLeast(0)
        } else {
            adjustedQuantity.coerceAtLeast(1)
        }

        val itemName = inventoryService.itemDisplayName(selectedCatch.itemId)
        val outcomeMessage = when (minigameResult) {
            MinigameResult.PERFECT -> "Perfect catch! ${itemName.ifBlank { "Loot" }} secured."
            MinigameResult.SUCCESS -> "You reeled in ${itemName.ifBlank { "something" }}."
            MinigameResult.FAIL -> "The fish slipped away."
        }

        return FishingResult(
            itemId = selectedCatch.itemId,
            quantity = ensuredQuantity,
            message = outcomeMessage
        )
    }
}

enum class MinigameResult {
    PERFECT,
    SUCCESS,
    FAIL
}

data class FishingResult(
    val itemId: String,
    val quantity: Int,
    val message: String
)
