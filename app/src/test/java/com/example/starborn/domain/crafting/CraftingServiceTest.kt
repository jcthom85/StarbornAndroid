package com.example.starborn.domain.crafting

import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.FirstAidRecipe
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.domain.model.FirstAidOutput
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.crafting.MinigameResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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

    @Test
    fun craftCookingSuccessConsumesIngredientsAndAddsResult() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "raw_fish", name = "Raw Fish"),
                item(id = "herb", name = "Herb"),
                item(id = "fish_stew", name = "Fish Stew")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Raw Fish", 2)
            addItem("Herb", 1)
        }
        val recipes = TestRecipeSource(
            cooking = listOf(
                CookingRecipe(
                    id = "fish_stew",
                    name = "Fish Stew",
                    description = null,
                    ingredients = mapOf("Raw Fish" to 2, "Herb" to 1),
                    result = "Fish Stew"
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftCooking("fish_stew", MinigameResult.SUCCESS)

        assertTrue(outcome is CraftingOutcome.Success)
        assertEquals("Fish Stew", outcome.itemId)
        assertTrue(inventory.hasItem("Fish Stew"))
        assertFalse(inventory.hasItem("Raw Fish"))
    }

    @Test
    fun craftCookingFailureConsumesIngredientsWithoutReward() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "raw_fish", name = "Raw Fish"),
                item(id = "herb", name = "Herb"),
                item(id = "fish_stew", name = "Fish Stew")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Raw Fish", 2)
            addItem("Herb", 1)
        }
        val recipes = TestRecipeSource(
            cooking = listOf(
                CookingRecipe(
                    id = "fish_stew",
                    name = "Fish Stew",
                    description = null,
                    ingredients = mapOf("Raw Fish" to 2, "Herb" to 1),
                    result = "Fish Stew"
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftCooking("fish_stew", MinigameResult.FAILURE)

        assertTrue(outcome is CraftingOutcome.Failure)
        assertFalse(inventory.hasItem("Fish Stew"))
        assertFalse(inventory.hasItem("Raw Fish"))
    }

    @Test
    fun craftFirstAidPerfectUsesPerfectOutput() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "synth_gel", name = "Synth-Gel"),
                item(id = "bandages", name = "Sterile Bandages"),
                item(id = "medkit", name = "Medkit"),
                item(id = "medkit_plus", name = "Medkit+")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Synth-Gel", 2)
            addItem("Sterile Bandages", 1)
        }
        val recipes = TestRecipeSource(
            firstAid = listOf(
                FirstAidRecipe(
                    id = "medkit",
                    name = "Medkit",
                    description = null,
                    ingredients = mapOf("Synth-Gel" to 2, "Sterile Bandages" to 1),
                    minigame = null,
                    output = FirstAidOutput(
                        perfect = "Medkit+",
                        success = "Medkit",
                        failure = "Sloppy Bandages"
                    )
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftFirstAid("medkit", MinigameResult.PERFECT)

        assertTrue(outcome is CraftingOutcome.Success)
        assertEquals("Medkit+", outcome.itemId)
        assertTrue(inventory.hasItem("Medkit+"))
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

private class TestItemCatalog(private val items: List<Item>) : ItemCatalog {
    private val byKey: MutableMap<String, Item> = mutableMapOf()

    override fun load() {
        byKey.clear()
        items.forEach { item ->
            byKey[item.id.lowercase()] = item
            item.aliases.forEach { alias ->
                byKey[alias.lowercase()] = item
            }
            byKey[item.name.lowercase()] = item
        }
    }

    override fun findItem(idOrAlias: String): Item? = byKey[idOrAlias.lowercase()]
}

private class TestRecipeSource(
    private val tinkering: List<TinkeringRecipe> = emptyList(),
    private val cooking: List<CookingRecipe> = emptyList(),
    private val firstAid: List<FirstAidRecipe> = emptyList()
) : CraftingRecipeSource {
    override fun loadTinkeringRecipes(): List<TinkeringRecipe> = tinkering
    override fun loadCookingRecipes(): List<CookingRecipe> = cooking
    override fun loadFirstAidRecipes(): List<FirstAidRecipe> = firstAid
}

private fun item(id: String, name: String): Item = Item(
    id = id,
    name = name,
    aliases = listOf(name),
    type = "ingredient",
    value = 10
)
