package com.example.starborn.feature.shop

import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ShopBuys
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.domain.model.ShopPricing
import com.example.starborn.domain.model.ShopSells
import com.example.starborn.domain.model.ShopGate
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.shop.ShopCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var itemCatalog: FakeItemCatalog
    private lateinit var inventoryService: InventoryService
    private lateinit var sessionStore: GameSessionStore
    private lateinit var viewModel: ShopViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        val items = mapOf(
            MEDKIT_ID to Item(
                id = MEDKIT_ID,
                name = "Field Medkit",
                aliases = listOf("medkit"),
                type = "consumable",
                value = 120,
                buyPrice = 150,
                description = "Restores a decent chunk of HP."
            ),
            SCRAP_ID to Item(
                id = SCRAP_ID,
                name = "Scrap Bundle",
                aliases = emptyList(),
                type = "junk",
                value = 40,
                buyPrice = 50,
                description = "A bundle of spare parts."
            ),
            RELIC_ID to Item(
                id = RELIC_ID,
                name = "Family Keepsake",
                aliases = emptyList(),
                type = "quest",
                value = 0,
                description = "Too sentimental to pawn.",
                unsellable = true
            )
        )

        itemCatalog = FakeItemCatalog(items)
        inventoryService = InventoryService(itemCatalog).apply { loadItems() }
        sessionStore = GameSessionStore()

        val shop = ShopDefinition(
            id = "mechanic",
            name = "Mechanic's Wares",
            portrait = null,
            pricing = ShopPricing(buyMarkdown = 0.35, sellMarkup = 1.0),
            sells = ShopSells(items = listOf(MEDKIT_ID, SCRAP_ID)),
            buys = ShopBuys(acceptTypes = listOf("consumable", "junk"))
        )
        val shopCatalog: ShopCatalog = FakeShopCatalog(mapOf(shop.id to shop))

        sessionStore.addCredits(500)

        viewModel = ShopViewModel(
            shopId = shop.id,
            shopCatalog = shopCatalog,
            itemCatalog = itemCatalog,
            inventoryService = inventoryService,
            sessionStore = sessionStore
        )

        dispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun buyItemConsumesCreditsAndAddsInventory() = runTest(dispatcher) {
        viewModel.buyItem(MEDKIT_ID, 2)
        advanceUntilIdle()

        assertEquals(200, sessionStore.state.value.playerCredits)
        assertTrue(inventoryService.hasItem(MEDKIT_ID, 2))
    }

    @Test
    fun sellItemRestoresCreditsAndRemovesInventory() = runTest(dispatcher) {
        inventoryService.addItem(SCRAP_ID, 3)
        advanceUntilIdle()

        viewModel.sellItem(SCRAP_ID, 2)
        advanceUntilIdle()

        // Sell price uses buyMarkdown (0.35) applied to value 50 → 18 credits each (rounded).
        assertEquals(536, sessionStore.state.value.playerCredits)
        assertTrue(inventoryService.hasItem(SCRAP_ID, 1))
    }

    @Test
    fun sellItemHonorsUnsellableFlag() = runTest(dispatcher) {
        inventoryService.addItem(RELIC_ID, 1)
        advanceUntilIdle()

        val messageDeferred = async { viewModel.messages.first() }
        viewModel.sellItem(RELIC_ID, 1)
        advanceUntilIdle()

        assertEquals("Cannot sell this item.", messageDeferred.await())
        assertTrue(inventoryService.hasItem(RELIC_ID, 1))
    }

    @Test fun unstockedCatalogItemCannotBePurchased() = runTest(dispatcher) {
        viewModel.buyItem(RELIC_ID, 1)
        assertEquals(500, sessionStore.state.value.playerCredits)
        assertTrue(inventoryService.snapshot().isEmpty())
    }

    @Test fun overflowingPurchaseDoesNotGrantFreeItems() = runTest(dispatcher) {
        viewModel.buyItem(MEDKIT_ID, Int.MAX_VALUE)
        assertEquals(500, sessionStore.state.value.playerCredits)
        assertTrue(inventoryService.snapshot().isEmpty())
    }

    @Test fun repeatedSalesWithoutCollectorUpdatesCannotPayTwice() = runTest(dispatcher) {
        inventoryService.addItem(SCRAP_ID, 1)
        advanceUntilIdle()
        viewModel.sellItem(SCRAP_ID, 1)
        viewModel.sellItem(SCRAP_ID, 1)
        assertEquals(518, sessionStore.state.value.playerCredits)
        assertTrue(inventoryService.snapshot().isEmpty())
        assertEquals(inventoryService.snapshot(), sessionStore.state.value.inventory)
    }

    @Test fun purchaseByAliasImmediatelyPersistsInventory() = runTest(dispatcher) {
        viewModel.buyItem("medkit", 1)
        assertEquals(mapOf(MEDKIT_ID to 1), sessionStore.state.value.inventory)
        assertEquals(350, sessionStore.state.value.playerCredits)
    }

    @Test fun saleCannotOverflowCreditBalanceOrConsumeStock() = runTest(dispatcher) {
        inventoryService.addItem(SCRAP_ID, 1)
        sessionStore.addCredits(Int.MAX_VALUE - 500)
        viewModel.sellItem(SCRAP_ID, 1)
        assertEquals(Int.MAX_VALUE, sessionStore.state.value.playerCredits)
        assertEquals(1, inventoryService.snapshot()[SCRAP_ID])
    }

    @Test fun aliasGateUsesCurrentMilestonesWithoutWaitingForCollectors() = runTest(dispatcher) {
        val shop = ShopDefinition(id = "gated", name = "Gated", sells = ShopSells(
            items = listOf("medkit"), gates = mapOf("medkit" to ShopGate(listOf("ready")))))
        val vm = ShopViewModel(shop.id, FakeShopCatalog(mapOf(shop.id to shop)), itemCatalog,
            inventoryService, sessionStore)
        assertTrue(vm.uiState.value.itemsForSale.single().locked)
        vm.buyItem(MEDKIT_ID, 1)
        assertEquals(500, sessionStore.state.value.playerCredits)
        sessionStore.setMilestone("ready")
        vm.buyItem(MEDKIT_ID, 1)
        assertEquals(350, sessionStore.state.value.playerCredits)
        assertEquals(mapOf(MEDKIT_ID to 1), inventoryService.snapshot())
    }

    @Test fun explicitResaleValueDoesNotChangePurchasePrice() = runTest(dispatcher) {
        val item = Item(id = "crafted", name = "Crafted", type = "component", value = 300,
            buyPrice = 600, resaleValue = 80)
        val catalog = FakeItemCatalog(mapOf(item.id to item))
        val inventory = InventoryService(catalog).apply { loadItems(); addItem(item.id, 1) }
        val shop = ShopDefinition(id = "resale", name = "Resale",
            pricing = ShopPricing(buyMarkdown = 0.5, sellMarkup = 1.2),
            sells = ShopSells(items = listOf(item.id)))
        val vm = ShopViewModel(shop.id, FakeShopCatalog(mapOf(shop.id to shop)), catalog, inventory, sessionStore)
        assertEquals(720, vm.uiState.value.itemsForSale.single().price)
        assertEquals(40, vm.uiState.value.sellInventory.single().price)
        vm.sellItem(item.id, 1)
        assertEquals(540, sessionStore.state.value.playerCredits)
        assertTrue(inventory.snapshot().isEmpty())
    }

    private class FakeItemCatalog(
        private val items: Map<String, Item>
    ) : ItemCatalog {
        override fun load() = Unit

        override fun findItem(idOrAlias: String): Item? {
            items[idOrAlias]?.let { return it }
            val normalized = idOrAlias.lowercase()
            return items.values.firstOrNull { item ->
                item.id.equals(normalized, ignoreCase = true) ||
                    item.name.equals(idOrAlias, ignoreCase = true) ||
                    item.aliases.any { it.equals(idOrAlias, ignoreCase = true) }
            }
        }
    }

    private class FakeShopCatalog(
        private val shops: Map<String, ShopDefinition>
    ) : ShopCatalog {
        override fun shopById(id: String?): ShopDefinition? = shops[id]
        override fun allShops(): Collection<ShopDefinition> = shops.values
    }

    companion object {
        private const val MEDKIT_ID = "medkit_basic"
        private const val SCRAP_ID = "scrap_bundle"
        private const val RELIC_ID = "family_relic"
    }
}
