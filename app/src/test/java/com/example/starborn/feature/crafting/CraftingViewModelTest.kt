package com.example.starborn.feature.crafting

import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.domain.session.GameSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CraftingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionStore: GameSessionStore
    private lateinit var inventoryService: InventoryService
    private lateinit var craftingService: CraftingService
    private lateinit var viewModel: CraftingViewModel

    private val testCatalog = object : ItemCatalog {
        private val items = listOf(
            Item(id = "cryo_inductor", name = "Broken Cryo-Inductor", description = "Damaged inductor loop", type = "crafting"),
            Item(id = "scrap_metal", name = "Scrap Metal", description = "Raw conduit alloys", type = "crafting"),
            Item(id = "functional_cryo_inductor", name = "Functional Cryo-Inductor", description = "Restored cold loop", type = "crafting")
        )
        private val byKey = mutableMapOf<String, Item>()

        override fun load() {
            byKey.clear()
            items.forEach { item ->
                byKey[item.id.lowercase()] = item
                item.aliases.forEach { alias -> byKey[alias.lowercase()] = item }
                byKey[item.name.lowercase()] = item
            }
        }

        override fun findItem(idOrAlias: String): Item? = byKey[idOrAlias.lowercase()]
    }

    private val testRecipes = object : CraftingRecipeSource {
        override fun loadTinkeringRecipes(): List<TinkeringRecipe> = listOf(
            TinkeringRecipe(
                id = "repair_cryo_inductor",
                name = "Functional Cryo-Inductor",
                description = "Patch cold loop",
                category = "repair",
                method = "synthesis",
                base = "cryo_inductor",
                components = listOf("scrap_metal"),
                ingredients = mapOf("cryo_inductor" to 1, "scrap_metal" to 1),
                result = "functional_cryo_inductor"
            )
        )
        override fun loadCookingRecipes(): List<CookingRecipe> = emptyList()
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionStore = GameSessionStore()
        inventoryService = InventoryService(testCatalog).apply {
            loadItems()
            addItem("Broken Cryo-Inductor", 1)
            addItem("Scrap Metal", 2)
        }
        craftingService = CraftingService(testRecipes, inventoryService, sessionStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun tutorialStepsThroughSlotBaseSlotComponentAndSynthesize() {
        // Given: Player completed briefing milestone
        sessionStore.setMilestone("ms_w1_mq01_workshop_briefed")
        viewModel = CraftingViewModel(craftingService, inventoryService, sessionStore)

        // Step 1: Initial state requires SLOT_BASE
        assertTrue(viewModel.uiState.value.isTutorialActive)
        assertEquals(TinkeringTutorialStep.SLOT_BASE, viewModel.uiState.value.tutorialStep)

        // Step 2: Slot Base item -> advances to SLOT_COMPONENT
        viewModel.selectMain("cryo_inductor")
        assertEquals(TinkeringTutorialStep.SLOT_COMPONENT, viewModel.uiState.value.tutorialStep)

        // Step 3: Slot Component -> advances to SYNTHESIZE
        viewModel.selectComponent(0, "scrap_metal")
        assertEquals(TinkeringTutorialStep.SYNTHESIZE, viewModel.uiState.value.tutorialStep)
        assertTrue(viewModel.uiState.value.bench.canCraftSelection)

        // Unslotting or clearing bench cleanly rolls back step
        viewModel.selectMain(null)
        assertEquals(TinkeringTutorialStep.SLOT_BASE, viewModel.uiState.value.tutorialStep)

        // Reslotting returns to SYNTHESIZE
        viewModel.selectMain("cryo_inductor")
        assertEquals(TinkeringTutorialStep.SYNTHESIZE, viewModel.uiState.value.tutorialStep)

        // When repaired milestone is marked, tutorial is inactive
        sessionStore.setMilestone("ms_w1_mq01_cryo_repaired")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isTutorialActive)
        assertNull(viewModel.uiState.value.tutorialStep)
    }
}
