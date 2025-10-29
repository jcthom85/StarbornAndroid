package com.example.starborn.domain.crafting

import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.FirstAidRecipe
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CraftingServiceTest {

    private lateinit var craftingService: CraftingService
    private lateinit var sessionStore: GameSessionStore

    @Before
    fun setup() {
        val inventoryService = InventoryService(EmptyItemCatalog()).apply { loadItems() }
        sessionStore = GameSessionStore()
        val recipeSource = EmptyRecipeSource()
        craftingService = CraftingService(recipeSource, inventoryService, sessionStore)
    }

    @Test
    fun learnSchematicPersistsToSessionStore() {
        val learned = craftingService.learnSchematic("mod_power_lens_1")

        assertTrue(learned)
        assertTrue(craftingService.isSchematicLearned("mod_power_lens_1"))
        assertTrue("mod_power_lens_1" in sessionStore.state.value.learnedSchematics)
    }

    @Test
    fun learnSchematicReturnsFalseWhenAlreadyKnown() {
        val first = craftingService.learnSchematic("mod_power_lens_1")
        val second = craftingService.learnSchematic("mod_power_lens_1")

        assertTrue(first)
        assertFalse(second)
    }

    @Test
    fun isSchematicLearnedReflectsExistingSessionState() {
        sessionStore.learnSchematic("mod_insulated_lining_1")

        assertTrue(craftingService.isSchematicLearned("mod_insulated_lining_1"))
    }
}

private class EmptyItemCatalog : ItemCatalog {
    override fun load() {
        // no-op
    }

    override fun findItem(idOrAlias: String): Item? = null
}

private class EmptyRecipeSource : CraftingRecipeSource {
    override fun loadTinkeringRecipes(): List<TinkeringRecipe> = emptyList()
    override fun loadCookingRecipes(): List<CookingRecipe> = emptyList()
    override fun loadFirstAidRecipes(): List<FirstAidRecipe> = emptyList()
}
