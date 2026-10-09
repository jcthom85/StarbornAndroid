package com.example.starborn.domain.fishing

import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.domain.inventory.InventoryService
import kotlin.random.Random

class FishingService(
    private val fishingAssetDataSource: FishingAssetDataSource,
    private val inventoryService: InventoryService,
    private val random: Random = Random.Default,
    private val sessionStore: com.example.starborn.domain.session.GameSessionStore? = null,
    private val craftingService: com.example.starborn.domain.crafting.CraftingService? = null
) {

    private val fishingData: FishingData by lazy { fishingAssetDataSource.loadFishingData() }

    fun getAvailableRods(): List<FishingRod> =
        fishingData.rods.filter { inventoryService.hasItem(it.id) }

    fun getAvailableLures(): List<FishingLure> =
        fishingData.lures.filter { inventoryService.hasItem(it.id) }

    private fun preferredId(kind: String): String? = sessionStore?.state?.value?.roomStates
        ?.get("fishing_preferences")?.entries?.firstOrNull { it.key.startsWith("$kind:") && it.value }
        ?.key?.substringAfter(':')

    fun preferredRod(): FishingRod? = getAvailableRods().let { owned ->
        owned.firstOrNull { it.id == preferredId("rod") } ?: owned.maxByOrNull { it.fishingPower + it.stability }
    }

    fun preferredLure(): FishingLure? = getAvailableLures().let { owned ->
        owned.firstOrNull { it.id == preferredId("lure") } ?: owned.firstOrNull()
    }

    fun rememberGear(kind: String, id: String) {
        val ids = when (kind) {
            "rod" -> getAvailableRods().map { it.id }
            "lure" -> getAvailableLures().map { it.id }
            else -> return
        }
        if (id !in ids) return
        sessionStore?.state?.value?.roomStates?.get("fishing_preferences")?.keys
            ?.filter { it.startsWith("$kind:") && it != "$kind:$id" }
            ?.forEach { sessionStore?.setRoomState("fishing_preferences", it, false) }
        sessionStore?.setRoomState("fishing_preferences", "$kind:$id", true)
    }

    fun hasHookBriefing(): Boolean = sessionStore?.state?.value?.completedMilestones
        ?.contains("ms_fishing_hook_briefed") == true || hasReelBriefing()

    fun markHookBriefing() { sessionStore?.setMilestone("ms_fishing_hook_briefed") }

    fun getFishingZone(zoneId: String): FishingZone? {
        val catches = fishingData.zones[zoneId] ?: return null
        val water = when (zoneId) {
            "colony_pit_drain" -> "images/rooms/world_1/mine_landing.webp" to "Mineral runoff glows beneath the mine's safety beacons. Eels stir below the sediment."
            "sector9_stream" -> "images/rooms/world_2/beach_pools.webp" to "Quiet tide pools catch the canopy's light. Watch for a ripple beneath the surface."
            "spire_runoff" -> "images/rooms/world_3/spire_sewers_passage.webp" to "Rainwater carries neon reflections through the Spire's drains. Something pulls against the current."
            "foundry_cooling_runoff" -> "images/rooms/world_4/foundry_cooling_springs.webp" to "Cooling water steams between the furnace channels. Silver shapes gather beyond the heat."
            "orbital_false_tide" -> "images/rooms/world_5/orbital_solarium.webp" to "An artificial tide moves through the solarium. Its fish follow a rhythm of their own."
            "singularity_ether_well" -> "images/rooms/world_6/source_new_world_node_scale_final.webp" to "The ether well folds its reflections inward. Give its strange inhabitants room to surge."
            else -> null to "Watch the water for a bite."
        }
        return FishingZone(
            id = zoneId,
            name = formatZoneName(zoneId),
            catches = catches,
            backgroundImage = water.first,
            description = water.second
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

    fun catchDisplayName(itemId: String): String = inventoryService.itemDisplayName(itemId).ifBlank { itemId }

    fun hasReelBriefing(): Boolean = sessionStore?.state?.value?.completedMilestones
        ?.contains("ms_fishing_reel_briefed") == true

    fun markReelBriefing() { sessionStore?.setMilestone("ms_fishing_reel_briefed") }

    fun getJournal(): List<FishingJournalEntry> = fishingData.zones.map { (zoneId, catches) ->
        val recorded = sessionStore?.state?.value?.roomStates?.get(journalKey(zoneId)).orEmpty()
        FishingJournalEntry(zoneId, formatZoneName(zoneId), catches.filter { it.isNativeFish() }.map {
            FishingJournalSpecies(it.itemId, catchDisplayName(it.itemId),
                recorded["caught:${it.itemId}"] == true, recorded["clean:${it.itemId}"] == true)
        })
    }

    private fun journalKey(zoneId: String) = "fishing_journal:$zoneId"

    fun secureCatch(result: FishingResult): FishingResult {
        if (result.quantity <= 0 || result.secured) return result
        inventoryService.addItem(result.itemId, result.quantity)
        val rewards = mutableListOf<String>()
        val zone = result.zoneId?.let { getFishingZone(it) }
        val catch = zone?.catches?.firstOrNull { it.itemId == result.itemId }
        if (sessionStore != null && zone != null && catch?.isNativeFish() == true) {
            sessionStore.setRoomState(journalKey(zone.id), "caught:${result.itemId}", true)
            if (result.cleanCatch) sessionStore.setRoomState(journalKey(zone.id), "clean:${result.itemId}", true)
            val journal = getJournal()
            val cleanSpecies = journal.flatMap { it.species }.filter { it.clean }.map { it.itemId }.toSet().size
            fun reward(milestone: String, itemId: String, text: String) {
                if (milestone in sessionStore.state.value.completedMilestones) return
                sessionStore.setMilestone(milestone)
                inventoryService.addItem(itemId, 1)
                rewards += text
            }
            if (cleanSpecies >= 1) reward("ms_fishing_first_clean", "shiny_lure", "First clean catch: Glimmer Lure unlocked.")
            if (cleanSpecies >= 3) reward("ms_fishing_clean_collection", "mystery_lure", "Three species caught cleanly: Ghost-Signal Lure unlocked.")
            if (journal.isNotEmpty() && journal.all { entry -> entry.species.any { it.caught } }) {
                sessionStore.setMilestone("ms_master_angler")
            }
            // A separate grant also upgrades saves that earned the old, craftable reward.
            if ("ms_master_angler" in sessionStore.state.value.completedMilestones) {
                reward("ms_master_angler_reward_v2", "six_water_lure", "Master Angler: Six-Water Lure unlocked. An exclusive lure for every fishing water.")
            }
            val species = journal.flatMap { it.species }.groupBy { it.itemId }
            if (species.isNotEmpty() && species.values.all { records -> records.any { it.caught } }) {
                reward("ms_fishing_all_species", "angler_field_medallion", "Complete field collection: Angler's Field Medallion earned. Every native species recorded.")
            }
            if (journal.isNotEmpty() && journal.all { entry -> entry.species.any { it.clean } } &&
                "ms_fishing_clean_waters" !in sessionStore.state.value.completedMilestones) {
                sessionStore.setMilestone("ms_fishing_clean_waters")
                rewards += "Clean-Water Angler: a clean catch recorded in every water."
            }
        }
        sessionStore?.setInventory(inventoryService.snapshot())
        return result.copy(secured = true, displayName = catchDisplayName(result.itemId),
            uses = craftingService?.usesFor(result.itemId).orEmpty(), rewards = rewards,
            cleanCatch = result.cleanCatch && catch?.isNativeFish() == true)
    }

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
            success && !catch.isNativeFish() -> "Recovered $displayName."
            success -> "You caught $displayName."
            !catch.isNativeFish() -> "The salvage slipped back into the water."
            else -> "The fish slipped away."
        }
        return FishingResult(
            itemId = catch.itemId,
            quantity = quantity,
            message = message,
            rarity = catch.rarity,
            flavorText = if (success && !catch.isNativeFish()) "Recovered from the water."
                else if (success) fishFlavor(catch.itemId) ?: flavorTextFor(catch.rarity) else null,
            behavior = encounter.behavior,
            displayName = displayName
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
            val attracted = catch.itemId.lowercase() in normalizedAttracts
            val attractionMultiplier = if (attracted) 1.5 * zoneBonus else 1.0
            val adjusted = (base * rarityFactor * attractionMultiplier).coerceAtLeast(0.1)
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
        val bonus = rodPower.coerceAtLeast(0.0)
        val lureFactor = 1.0 + lureBonus.coerceAtLeast(0.0)
        return when (rarity) {
            FishingRarity.JUNK -> 1.0
            FishingRarity.COMMON -> 1.0
            FishingRarity.UNCOMMON -> (1 + 0.05 * bonus) * (1 + lureBonus.coerceAtLeast(0.0) * 0.5)
            FishingRarity.RARE -> (1 + 0.1 * bonus) * lureFactor
            FishingRarity.EPIC -> (1 + 0.15 * bonus) * lureFactor
            FishingRarity.EXOTIC -> (1 + 0.25 * bonus) * lureFactor
        }
    }

    private fun fishFlavor(itemId: String): String? = when (itemId) {
        "raw_glowfish" -> "Its mineral light flickers softly through your fingers."
        "resonance_carp" -> "A low hum runs down the line before the carp settles."
        "chime_minnow" -> "A tiny glass-clear note rings out as you lift it from the water."
        "stellarium_eel" -> "Blind eyes turn toward the vibration of your footsteps."
        "frequency_tetra" -> "Its scales pulse twice, as if answering a distant chord."
        "void_ray" -> "For a heartbeat, its shadow arrives before the ray does."
        "chronos_guppy" -> "One last ripple follows it a moment too late."
        else -> null
    }

    private fun flavorTextFor(rarity: FishingRarity): String = when (rarity) {
        FishingRarity.JUNK -> "Well, at least it's something."
        FishingRarity.COMMON -> "A solid haul."
        FishingRarity.UNCOMMON -> "Not bad at all."
        FishingRarity.RARE -> "That one feels special."
        FishingRarity.EPIC -> "An incredible catch!"
        FishingRarity.EXOTIC -> "An impossible catch!"
    }

    private fun formatZoneName(zoneId: String): String = when (zoneId) {
        "colony_pit_drain" -> "Colony Drain Pool"
        "sector9_stream" -> "Sector 9 Tide Pools"
        "spire_runoff" -> "Spire Rainwater Runoff"
        "foundry_cooling_runoff" -> "Foundry Cooling Springs"
        "orbital_false_tide" -> "Orbital False Tide"
        "singularity_ether_well" -> "Source Ether Well"
        else -> zoneId.split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
            .ifBlank { zoneId }
    }
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
    val behavior: FishBehaviorDefinition? = null,
    val secured: Boolean = false,
    val uses: List<String> = emptyList(),
    val zoneId: String? = null,
    val cleanCatch: Boolean = false,
    val displayName: String = "",
    val rewards: List<String> = emptyList()
)

data class FishingEncounter(
    val catch: FishingCatchDefinition,
    val behavior: FishBehaviorDefinition?
)

data class FishingJournalSpecies(val itemId: String, val name: String, val caught: Boolean, val clean: Boolean)
data class FishingJournalEntry(val zoneId: String, val name: String, val species: List<FishingJournalSpecies>)

fun FishingCatchDefinition.isNativeFish(): Boolean = rarity != FishingRarity.JUNK &&
    itemId !in setOf("scrap_metal", "wiring_bundle", "circuit_board", "nano_filament")
