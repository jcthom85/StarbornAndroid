package com.example.starborn.domain.fishing

import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.domain.inventory.InventoryService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class FishingServiceTest {

    private lateinit var fishingAssetDataSource: FishingAssetDataSource
    private lateinit var inventoryService: InventoryService
    private lateinit var fishingService: FishingService

    private val basicRod = FishingRod(id = "basic_rod", name = "Basic Rod", power = 1.0)
    private val basicLure = FishingLure(id = "basic_lure", name = "Basic Lure", rarityBonus = 0.0)
    private val commonFish = FishingCatchDefinition(itemId = "common_fish", weight = 100, minQuantity = 1, maxQuantity = 3)
    private val uncommonFish = FishingCatchDefinition(itemId = "uncommon_fish", weight = 20, minQuantity = 1, maxQuantity = 1)
    private val defaultZone = FishingZone(
        id = "default_fishing_zone",
        name = "Default Fishing Zone",
        catches = listOf(commonFish, uncommonFish)
    )
    private val emptyZone = FishingZone(
        id = "empty_zone",
        name = "Empty Zone",
        catches = emptyList()
    )

    @Before
    fun setup() {
        fishingAssetDataSource = mockk()
        inventoryService = mockk()

        every { fishingAssetDataSource.loadFishingData() } returns FishingData(
            rods = listOf(basicRod),
            lures = listOf(basicLure),
            zones = listOf(defaultZone, emptyZone)
        )
        every { inventoryService.itemDisplayName(any()) } answers {
            val id = firstArg<String>()
            id.replace('_', ' ').replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
            }
        }

        fishingService = FishingService(fishingAssetDataSource, inventoryService)
    }

    @Test
    fun `getAvailableRods returns only rods present in inventory`() {
        every { inventoryService.hasItem("basic_rod") } returns true
        every { inventoryService.hasItem("advanced_rod") } returns false

        val availableRods = fishingService.getAvailableRods()
        assertEquals(1, availableRods.size)
        assertEquals(basicRod, availableRods.first())
    }

    @Test
    fun `getAvailableLures returns only lures present in inventory`() {
        every { inventoryService.hasItem("basic_lure") } returns true
        every { inventoryService.hasItem("shiny_lure") } returns false

        val availableLures = fishingService.getAvailableLures()
        assertEquals(1, availableLures.size)
        assertEquals(basicLure, availableLures.first())
    }

    @Test
    fun `getFishingZone returns correct zone by id`() {
        val zone = fishingService.getFishingZone("default_fishing_zone")
        assertEquals(defaultZone, zone)
    }

    @Test
    fun `getFishingZone returns null for unknown id`() {
        val zone = fishingService.getFishingZone("unknown_zone")
        assertNull(zone)
    }

    @Test
    fun `getCatchResult returns null for zone with no catches`() {
        val result = fishingService.getCatchResult(emptyZone, basicRod, basicLure, MinigameResult.SUCCESS)
        assertNull(result)
    }

    @Test
    fun `getCatchResult returns a valid result for SUCCESS minigame`() {
        val result = fishingService.getCatchResult(defaultZone, basicRod, basicLure, MinigameResult.SUCCESS)
        assertNotNull(result)
        assertEquals(true, result?.itemId in listOf(commonFish.itemId, uncommonFish.itemId))
        assertEquals(true, result?.quantity in 1..3)
    }

    @Test
    fun `getCatchResult returns increased quantity for PERFECT minigame`() {
        // Mock Random to ensure a predictable quantity for base calculation
        val mockRandom = object : Random() {
            override fun nextInt(from: Int, until: Int): Int = 2 // Always return 2 for quantity
            override fun nextInt(until: Int): Int = 0 // Always select the first catch
            override fun nextBits(bitCount: Int): Int = 0
        }
        val serviceWithMockRandom = FishingService(fishingAssetDataSource, inventoryService, mockRandom)

        // For simplicity, let's assume the internal Random is directly replaceable or we test the logic
        // Since Random is internal to getCatchResult, we'll test the outcome based on expected behavior
        // A perfect result should yield 1.5x the base quantity, rounded down.
        val result = serviceWithMockRandom.getCatchResult(defaultZone, basicRod, basicLure, MinigameResult.PERFECT)
        assertNotNull(result)
        assertEquals(true, result?.itemId in listOf(commonFish.itemId, uncommonFish.itemId))
        assertEquals(3, result?.quantity) // 2 * 1.5 = 3
    }

    @Test
    fun `getCatchResult returns decreased quantity for FAIL minigame`() {
        val mockRandom = object : Random() {
            override fun nextInt(from: Int, until: Int): Int = 2 // Always return 2 for quantity
            override fun nextInt(until: Int): Int = 0 // Always select the first catch
            override fun nextBits(bitCount: Int): Int = 0
        }
        val serviceWithMockRandom = FishingService(fishingAssetDataSource, inventoryService, mockRandom)

        val result = serviceWithMockRandom.getCatchResult(defaultZone, basicRod, basicLure, MinigameResult.FAIL)
        assertNotNull(result)
        assertEquals(true, result?.itemId in listOf(commonFish.itemId, uncommonFish.itemId))
        assertEquals(1, result?.quantity) // 2 * 0.5 = 1
    }

    @Test
    fun `getCatchResult returns 0 quantity for FAIL minigame if base is 0`() {
        val mockRandom = object : Random() {
            override fun nextInt(from: Int, until: Int): Int = 0 // Always return 0 for quantity
            override fun nextInt(until: Int): Int = 0 // Always select the first catch
            override fun nextBits(bitCount: Int): Int = 0
        }
        val serviceWithMockRandom = FishingService(fishingAssetDataSource, inventoryService, mockRandom)

        val result = serviceWithMockRandom.getCatchResult(defaultZone, basicRod, basicLure, MinigameResult.FAIL)
        assertNotNull(result)
        assertEquals(true, result?.itemId in listOf(commonFish.itemId, uncommonFish.itemId))
        assertEquals(0, result?.quantity)
    }

    @Test
    fun `rod power scales quantity upwards`() {
        val powerfulRod = basicRod.copy(power = 2.0)
        val mockRandom = object : Random() {
            override fun nextInt(from: Int, until: Int): Int = 1
            override fun nextInt(until: Int): Int = 0
            override fun nextBits(bitCount: Int): Int = 0
        }
        val serviceWithMockRandom = FishingService(fishingAssetDataSource, inventoryService, mockRandom)

        val result = serviceWithMockRandom.getCatchResult(defaultZone, powerfulRod, basicLure, MinigameResult.SUCCESS)
        assertNotNull(result)
        // Base quantity would be 1; with power 2.0 we expect at least 2
        assertEquals(2, result?.quantity)
    }

    @Test
    fun `result message references item name`() {
        val mockRandom = object : Random() {
            override fun nextInt(from: Int, until: Int): Int = from
            override fun nextInt(until: Int): Int = 0
            override fun nextBits(bitCount: Int): Int = 0
        }
        val serviceWithMockRandom = FishingService(fishingAssetDataSource, inventoryService, mockRandom)

        val result = serviceWithMockRandom.getCatchResult(defaultZone, basicRod, basicLure, MinigameResult.SUCCESS)
        assertNotNull(result)
        assertTrue(result!!.message.contains("Common fish", ignoreCase = true))
    }
}
