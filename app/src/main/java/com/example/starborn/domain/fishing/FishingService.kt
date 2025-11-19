package com.example.starborn.domain.fishing

import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.domain.inventory.InventoryService
import kotlin.random.Random

class FishingService(
    private val fishingAssetDataSource: FishingAssetDataSource,
    private val inventoryService: InventoryService,
    private val random: Random = Random.Default
) {

    private val fishingData: FishingData by lazy { fishingAssetDataSource.loadFishingData() }

    fun getAvailableRods(): List<FishingRod> =
        fishingData.rods.filter { inventoryService.hasItem(it.id) }

    fun getAvailableLures(): List<FishingLure> =
        fishingData.lures.filter { inventoryService.hasItem(it.id) }

    fun getFishingZone(zoneId: String): FishingZone? {
        val catches = fishingData.zones[zoneId] ?: return null
        return FishingZone(
            id = zoneId,
            name = formatZoneName(zoneId),
            catches = catches
        )
    }

    fun minigameDifficultyForRod(rod: FishingRod?): FishingDifficulty {
        val power = rod?.fishingPower ?: 1.0
        return when {
            power >= 3.0 -> FishingDifficulty.EASY
            power >= 2.0 -> FishingDifficulty.MEDIUM
            else -> FishingDifficulty.HARD
        }
    }

    fun getMinigameRule(difficulty: FishingDifficulty): FishingMinigameRule {
        val key = when (difficulty) {
            FishingDifficulty.EASY -> "easy"
            FishingDifficulty.MEDIUM -> "medium"
            FishingDifficulty.HARD -> "hard"
        }
        return fishingData.minigameRules[key] ?: FishingMinigameRule()
    }

    fun getVictoryScreen(): VictoryScreenConfig? = fishingData.victoryScreen

    fun prepareEncounter(
        zone: FishingZone,
        rod: FishingRod,
        lure: FishingLure
    ): FishingEncounter? {
        if (zone.catches.isEmpty()) return null
        val weighted = buildWeightedCatches(zone, rod, lure)
        if (weighted.isEmpty()) return null
        val selectedCatch = selectCatch(weighted)
        val behavior = selectedCatch.behaviorId?.let { fishingData.fishBehaviors[it] }
        return FishingEncounter(selectedCatch, behavior)
    }

    fun resolveEncounter(
        encounter: FishingEncounter,
        minigameResult: MinigameResult
    ): FishingResult {
        val catch = encounter.catch
        val success = minigameResult != MinigameResult.FAIL
        val quantity = if (success) 1 else 0
        val displayName = inventoryService.itemDisplayName(catch.itemId).ifBlank { catch.itemId }
        val message = when {
            success && minigameResult == MinigameResult.PERFECT -> "Perfect catch! $displayName secured."
            success -> "You caught $displayName."
            else -> "The fish slipped away."
        }
        return FishingResult(
            itemId = catch.itemId,
            quantity = quantity,
            message = message,
            rarity = catch.rarity,
            flavorText = if (success) flavorTextFor(catch.rarity) else null,
            behavior = encounter.behavior
        )
    }

    fun getCatchResult(
        zone: FishingZone,
        rod: FishingRod,
        lure: FishingLure,
        minigameResult: MinigameResult
    ): FishingResult? {
        val encounter = prepareEncounter(zone, rod, lure) ?: return null
        return resolveEncounter(encounter, minigameResult)
    }

    private fun buildWeightedCatches(zone: FishingZone, rod: FishingRod, lure: FishingLure): List<Pair<FishingCatchDefinition, Double>> {
        val normalizedAttracts = lure.attracts.map { it.lowercase() }
        val zoneBonus = 1.0 + (lure.zoneBonuses[zone.id]?.toDouble() ?: 0.0) / 10.0
        val rarityBoost = { rarity: FishingRarity ->
            rarityFactor(rarity, rod.fishingPower, lure.rarityBonus)
        }
        return zone.catches.map { catch ->
            val base = catch.weight.toDouble().coerceAtLeast(0.1)
            val rarityFactor = rarityBoost(catch.rarity)
            val attractionMultiplier = if (normalizedAttracts.any { it == catch.itemId.lowercase() }) 1.5 else 1.0
            val adjusted = (base * rarityFactor * zoneBonus * attractionMultiplier).coerceAtLeast(0.1)
            catch to adjusted
        }
    }

    private fun selectCatch(weighted: List<Pair<FishingCatchDefinition, Double>>): FishingCatchDefinition {
        val total = weighted.sumOf { it.second }
        if (total <= 0.0) return weighted.last().first
        val roll = random.nextDouble(total)
        var cumulative = 0.0
        for ((catch, weight) in weighted) {
            cumulative += weight
            if (roll <= cumulative) return catch
        }
        return weighted.last().first
    }

    private fun rarityFactor(rarity: FishingRarity, rodPower: Double, lureBonus: Double): Double {
        val bonus = (rodPower + lureBonus / 10.0).coerceAtLeast(0.0)
        return when (rarity) {
            FishingRarity.JUNK -> 1.0
            FishingRarity.COMMON -> 1.0
            FishingRarity.UNCOMMON -> 1 + 0.05 * bonus
            FishingRarity.RARE -> 1 + 0.1 * bonus
            FishingRarity.EPIC -> 1 + 0.15 * bonus
        }
    }

    private fun flavorTextFor(rarity: FishingRarity): String = when (rarity) {
        FishingRarity.JUNK -> "Well, at least it's something."
        FishingRarity.COMMON -> "A solid haul."
        FishingRarity.UNCOMMON -> "Not bad at all."
        FishingRarity.RARE -> "That one feels special."
        FishingRarity.EPIC -> "An incredible catch!"
    }

    private fun formatZoneName(zoneId: String): String =
        zoneId.split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
            .ifBlank { zoneId }
}

enum class FishingDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class MinigameResult {
    PERFECT,
    SUCCESS,
    FAIL
}

data class FishingResult(
    val itemId: String,
    val quantity: Int,
    val message: String,
    val rarity: FishingRarity? = null,
    val flavorText: String? = null,
    val behavior: FishBehaviorDefinition? = null
)

data class FishingEncounter(
    val catch: FishingCatchDefinition,
    val behavior: FishBehaviorDefinition?
)
