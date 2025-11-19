package com.example.starborn.domain.fishing

import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.domain.inventory.InventoryService
import io.mockk.every
import io.mockk.mockk
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FishingServiceTest {

    private lateinit var fishingAssetDataSource: FishingAssetDataSource
    private lateinit var inventoryService: InventoryService
    private lateinit var fishingService: FishingService

    private val rod = FishingRod(id = "wooden_rod", name = "Wooden Rod", fishingPower = 1.0, stability = 0.8)
    private val lure = FishingLure(
        id = "basic_lure",
        name = "Basic Lure",
        rarityBonus = 0.0,
        attracts = listOf("raw fish"),
        zoneBonuses = mapOf("forest" to 2)
    )
    private val commonCatch = FishingCatchDefinition(
        itemId = "Raw Fish",
        weight = 50,
        rarity = FishingRarity.COMMON,
        behaviorId = "gentle_wobble"
    )
    private val rareCatch = FishingCatchDefinition(
        itemId = "Glowing Minnow",
        weight = 5,
        rarity = FishingRarity.RARE,
        behaviorId = "steady_pull"
    )

    @Before
    fun setup() {
        fishingAssetDataSource = mockk()
        inventoryService = mockk()

        every { fishingAssetDataSource.loadFishingData() } returns FishingData(
            rods = listOf(rod),
            lures = listOf(lure),
            zones = mapOf(
                "forest" to listOf(commonCatch, rareCatch),
                "empty" to emptyList()
            ),
            fishBehaviors = mapOf(
                "gentle_wobble" to FishBehaviorDefinition(pattern = FishPattern.SINE, basePull = 0.3, burstPull = 0.0, stamina = 8.0),
                "steady_pull" to FishBehaviorDefinition(pattern = FishPattern.LINEAR, basePull = 0.5, burstPull = 0.2, stamina = 12.0)
            ),
            minigameRules = mapOf(
                "easy" to FishingMinigameRule(targetSize = 0.4, barSpeed = 1.2),
                "medium" to FishingMinigameRule(targetSize = 0.25, barSpeed = 1.8),
                "hard" to FishingMinigameRule(targetSize = 0.15, barSpeed = 2.4)
            )
        )

        every { inventoryService.itemDisplayName(any()) } answers {
            val id = firstArg<String>()
            id
        }

        every { inventoryService.hasItem(any()) } returns true

        fishingService = FishingService(fishingAssetDataSource, inventoryService, zeroRandom())
    }

    @Test
    fun `getAvailableRods filters inventory`() {
        every { inventoryService.hasItem(rod.id) } returns true

        val result = fishingService.getAvailableRods()
        assertEquals(1, result.size)
        assertEquals(rod, result.first())
    }

    @Test
    fun `getAvailableLures filters inventory`() {
        every { inventoryService.hasItem(lure.id) } returns true

        val result = fishingService.getAvailableLures()
        assertEquals(listOf(lure), result)
    }

    @Test
    fun `getFishingZone returns formatted name`() {
        val zone = fishingService.getFishingZone("forest")
        assertNotNull(zone)
        assertEquals("Forest", zone?.name)
        assertEquals(2, zone?.catches?.size)
    }

    @Test
    fun `getFishingZone returns null when missing`() {
        assertNull(fishingService.getFishingZone("unknown"))
    }

    @Test
    fun `getCatchResult returns null for empty zone`() {
        val emptyZone = fishingService.getFishingZone("empty")
        requireNotNull(emptyZone)

        val result = fishingService.getCatchResult(emptyZone, rod, lure, MinigameResult.SUCCESS)
        assertNull(result)
    }

    @Test
    fun `success catch returns quantity and flavor text`() {
        val zone = fishingService.getFishingZone("forest")
        requireNotNull(zone)

        val result = fishingService.getCatchResult(zone, rod, lure, MinigameResult.SUCCESS)
        assertNotNull(result)
        requireNotNull(result)
        assertEquals("Raw Fish", result.itemId)
        assertEquals(1, result.quantity)
        assertEquals(FishingRarity.COMMON, result.rarity)
        assertEquals("A solid haul.", result.flavorText)
        assertNotNull(result.behavior)
        assertEquals(FishPattern.SINE, result.behavior?.pattern)
    }

    @Test
    fun `prepareEncounter and resolveEncounter reuse same catch`() {
        val zone = fishingService.getFishingZone("forest")
        requireNotNull(zone)
        val encounter = fishingService.prepareEncounter(zone, rod, lure)
        requireNotNull(encounter)
        val result = fishingService.resolveEncounter(encounter, MinigameResult.SUCCESS)
        assertEquals(encounter.catch.itemId, result.itemId)
        assertEquals(encounter.behavior?.pattern, result.behavior?.pattern)
    }

    @Test
    fun `failed catch has zero quantity`() {
        val zone = fishingService.getFishingZone("forest")
        requireNotNull(zone)

        val result = fishingService.getCatchResult(zone, rod, lure, MinigameResult.FAIL)
        assertNotNull(result)
        requireNotNull(result)
        assertEquals(0, result.quantity)
        assertEquals(null, result.flavorText)
        assertEquals("The fish slipped away.", result.message)
    }

    @Test
    fun `minigame difficulty derived from rod power`() {
        assertEquals(FishingDifficulty.HARD, fishingService.minigameDifficultyForRod(rod))
        assertEquals(FishingDifficulty.MEDIUM, fishingService.minigameDifficultyForRod(rod.copy(fishingPower = 2.1)))
        assertEquals(FishingDifficulty.EASY, fishingService.minigameDifficultyForRod(rod.copy(fishingPower = 3.2)))
    }

    private fun zeroRandom(): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }
}
