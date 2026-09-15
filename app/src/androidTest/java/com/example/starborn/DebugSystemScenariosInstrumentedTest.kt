package com.example.starborn

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.starborn.debug.DebugSystemScenarios
import com.example.starborn.di.AppServices
import com.example.starborn.domain.crafting.CraftingOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebugSystemScenariosInstrumentedTest {
    @Test fun everyFishingZoneHasUsableGearAndItsActualRoomAction() = runBlocking(Dispatchers.Main) {
        val services = AppServices(IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>()), true)
        try {
            delay(250)
            DebugSystemScenarios.fishingLocations.forEach { location ->
                val launched = services.startDebugScenario("fish_${location.zone}")
                assertTrue("${location.zone}: ${services.debugScenarioError}", launched)
                assertEquals(location.room, services.sessionStore.state.value.roomId)
                val room = services.worldDataSource.loadRooms().first { it.id == location.room }
                assertTrue(room.actions.any { it["type"] == "fishing" && it["zone_id"] == location.zone })
                val zone = requireNotNull(services.fishingService.getFishingZone(location.zone))
                assertFalse(services.fishingService.getAvailableRods().isEmpty())
                assertFalse(services.fishingService.getAvailableLures().isEmpty())
                services.fishingService.getAvailableRods().forEach { rod ->
                    services.fishingService.getAvailableLures().forEach { lure ->
                        assertNotNull(services.fishingService.prepareEncounter(zone, rod, lure))
                    }
                }
            }
        } finally { services.release() }
    }

    @Test fun stockedWorkbenchesCanProduceEveryCurrentRecipe() = runBlocking(Dispatchers.Main) {
        val services = AppServices(IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>()), true)
        try {
            delay(250)
            assertTrue(services.startDebugScenario("system_tinkering"))
            services.craftingService.tinkeringRecipes.forEach { recipe ->
                assertTrue(recipe.id, services.craftingService.isSchematicLearned(recipe.id))
                assertTrue(recipe.id, services.craftingService.craftTinkering(recipe.id) is CraftingOutcome.Success)
                assertTrue(recipe.result, (services.inventoryService.snapshot()[recipe.result] ?: 0) > 0)
            }
            assertTrue(services.startDebugScenario("system_cooking"))
            assertEquals(setOf("nova", "zeke", "orion", "gh0st"), services.sessionStore.state.value.partyMembers.toSet())
            services.craftingService.cookingRecipes.forEach { recipe ->
                assertTrue(recipe.id, services.craftingService.cookMeal(recipe.id, "nova") is CraftingOutcome.Success)
                assertTrue(recipe.result, (services.inventoryService.snapshot()[recipe.result] ?: 0) > 0)
            }
            val inventory = services.inventoryService.snapshot()
            services.saveSlot(3)
            assertTrue(services.startDebugScenario("recipe_cryo_missing"))
            assertTrue(services.loadSlot(3))
            assertEquals(inventory, services.inventoryService.snapshot())
        } finally { services.release() }
    }

    @Test fun shopWorkbenchTransactionsAndCreditConstraints() = runBlocking(Dispatchers.Main) {
        val services = AppServices(IsolatedProgressionContext(ApplicationProvider.getApplicationContext<Context>()), true)
        try {
            delay(250)
            DebugSystemScenarios.shopIds.forEach { shopId ->
                // Test 1: Empty wallet scenario
                assertTrue(services.startDebugScenario("shop_${shopId}_empty"))
                assertEquals(0, services.sessionStore.state.value.playerCredits)
                val emptyVm = com.example.starborn.feature.shop.ShopViewModel(
                    shopId = shopId,
                    shopCatalog = services.shopRepository,
                    itemCatalog = services.itemRepository,
                    inventoryService = services.inventoryService,
                    sessionStore = services.sessionStore
                )
                val emptyState = emptyVm.uiState.value
                assertFalse(emptyState.isLoading)
                assertNull(emptyState.unavailableMessage)
                assertEquals(0, emptyState.credits)
                // Any item for sale with price > 0 cannot be afforded
                emptyState.itemsForSale.filter { it.price > 0 }.forEach { item ->
                    assertFalse("${shopId} item ${item.id} should not be affordable with 0 credits", item.canAfford)
                }

                // Test 2: Stocked scenario (10,000 credits)
                assertTrue(services.startDebugScenario("shop_$shopId"))
                assertEquals(10_000, services.sessionStore.state.value.playerCredits)
                val stockedVm = com.example.starborn.feature.shop.ShopViewModel(
                    shopId = shopId,
                    shopCatalog = services.shopRepository,
                    itemCatalog = services.itemRepository,
                    inventoryService = services.inventoryService,
                    sessionStore = services.sessionStore
                )
                val stockedState = stockedVm.uiState.value
                assertEquals(10_000, stockedState.credits)

                // Verify buying first unlocked purchasable item
                val purchasable = stockedState.itemsForSale.firstOrNull { !it.locked && it.canAfford }
                if (purchasable != null) {
                    val creditsBefore = services.sessionStore.state.value.playerCredits
                    val countBefore = services.inventoryService.snapshot()[purchasable.id] ?: 0
                    stockedVm.buyItem(purchasable.id, 1)
                    assertEquals(creditsBefore - purchasable.price, services.sessionStore.state.value.playerCredits)
                    val itemDef = services.itemRepository.findItem(purchasable.id)
                    val isGear = itemDef?.let { it.type == "weapon" || it.type.startsWith("armor_") || it.equipment?.slot in setOf("weapon", "armor") } == true
                    if (isGear) {
                        assertTrue(purchasable.id in services.sessionStore.state.value.unlockedWeapons || purchasable.id in services.sessionStore.state.value.unlockedArmors)
                    } else {
                        assertEquals(countBefore + 1, services.inventoryService.snapshot()[purchasable.id])
                    }
                }

                // Verify selling a permitted item if any can be sold
                val sellable = stockedVm.uiState.value.sellInventory.firstOrNull { it.canSell && it.quantity > 0 }
                if (sellable != null) {
                    val creditsBefore = services.sessionStore.state.value.playerCredits
                    val countBefore = services.inventoryService.snapshot()[sellable.id] ?: 0
                    stockedVm.sellItem(sellable.id, 1)
                    assertEquals(creditsBefore + sellable.price, services.sessionStore.state.value.playerCredits)
                    assertEquals(countBefore - 1, services.inventoryService.snapshot()[sellable.id] ?: 0)
                }
            }
        } finally { services.release() }
    }
}

