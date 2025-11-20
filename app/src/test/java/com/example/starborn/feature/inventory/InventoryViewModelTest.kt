package com.example.starborn.feature.inventory

import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.session.GameSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun useItemRestoresHpForTarget() {
        val catalog = TestCatalog(listOf(medkitItem(), samplerItem()))
        val inventoryService = InventoryService(catalog).apply { loadItems() }
        val craftingService = mock<CraftingService>()
        val sessionStore = GameSessionStore().apply {
            setPartyMembers(listOf("nova", "ollie"))
            setPartyMemberHp("nova", 40)
            setPartyMemberHp("ollie", 70)
        }
        val roster = listOf(
            Player("nova", "Nova", 1, 0, 120, 10, 8, 6, 5, 4, emptyList(), ""),
            Player("ollie", "Ollie", 1, 0, 100, 8, 7, 5, 4, 3, emptyList(), "")
        )
        val viewModel = InventoryViewModel(inventoryService, craftingService, sessionStore, roster)

        inventoryService.addItem("medkit_i")
        viewModel.useItem("medkit_i", "nova")
        dispatcher.scheduler.advanceUntilIdle()

        val updated = sessionStore.state.value.partyMemberHp["nova"]
        assertEquals(90, updated)
    }

    @Test
    fun partyTargetHealsEveryone() {
        val catalog = TestCatalog(listOf(medkitItem(), samplerItem()))
        val inventoryService = InventoryService(catalog).apply { loadItems() }
        val craftingService = mock<CraftingService>()
        val sessionStore = GameSessionStore().apply {
            setPartyMembers(listOf("nova", "ollie"))
            setPartyMemberHp("nova", 40)
            setPartyMemberHp("ollie", 10)
        }
        val roster = listOf(
            Player("nova", "Nova", 1, 0, 120, 10, 8, 6, 5, 4, emptyList(), ""),
            Player("ollie", "Ollie", 1, 0, 100, 8, 7, 5, 4, 3, emptyList(), "")
        )
        val viewModel = InventoryViewModel(inventoryService, craftingService, sessionStore, roster)

        inventoryService.addItem("ellies_signature_sampler")
        viewModel.useItem("ellies_signature_sampler")
        dispatcher.scheduler.advanceUntilIdle()

        val state = sessionStore.state.value
        assertEquals(120, state.partyMemberHp["nova"])
        assertEquals(100, state.partyMemberHp["ollie"])
    }

    private class TestCatalog(items: List<Item>) : ItemCatalog {
        private val itemsById = items.associateBy { it.id }

        override fun load() {}

        override fun findItem(idOrAlias: String): Item? = itemsById[idOrAlias]
    }

    private fun medkitItem() = Item(
        id = "medkit_i",
        name = "Medkit I",
        type = "consumable",
        effect = ItemEffect(target = "any", restoreHp = 50, restoreRp = 0)
    )

    private fun samplerItem() = Item(
        id = "ellies_signature_sampler",
        name = "Sampler",
        type = "consumable",
        effect = ItemEffect(target = "party", restoreHp = 9999, restoreRp = 0)
    )
}
