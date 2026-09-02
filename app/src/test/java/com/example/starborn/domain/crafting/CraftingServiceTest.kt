package com.example.starborn.domain.crafting

import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.TinkeringRecipe
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
    fun craftProvisionConsumesIngredientsAndAddsResult() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "raw_glowfish", name = "Raw Glowfish"),
                item(id = "herb", name = "Herb"),
                item(id = "glowfish_broth", name = "Glowfish Broth")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Raw Glowfish", 2)
            addItem("Herb", 1)
        }
        val recipes = TestRecipeSource(
            tinkering = listOf(
                TinkeringRecipe(
                    id = "provision_glowfish_broth",
                    name = "Glowfish Broth",
                    description = null,
                    category = "provision",
                    method = "field_cook",
                    ingredients = mapOf("Raw Glowfish" to 2, "Herb" to 1),
                    result = "Glowfish Broth"
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftTinkering("provision_glowfish_broth")

        assertTrue(outcome is CraftingOutcome.Success)
        assertEquals("glowfish_broth", outcome.itemId)
        assertTrue(inventory.hasItem("Glowfish Broth"))
        assertFalse(inventory.hasItem("Raw Glowfish"))
    }

    @Test
    fun craftProvisionFailsWithoutConsumingWhenMissingIngredients() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "raw_glowfish", name = "Raw Glowfish"),
                item(id = "herb", name = "Herb"),
                item(id = "glowfish_broth", name = "Glowfish Broth")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Herb", 1)
        }
        val recipes = TestRecipeSource(
            tinkering = listOf(
                TinkeringRecipe(
                    id = "provision_glowfish_broth",
                    name = "Glowfish Broth",
                    description = null,
                    category = "provision",
                    method = "field_cook",
                    ingredients = mapOf("Raw Glowfish" to 2, "Herb" to 1),
                    result = "Glowfish Broth"
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftTinkering("provision_glowfish_broth")

        assertTrue(outcome is CraftingOutcome.Failure)
        assertFalse(inventory.hasItem("Glowfish Broth"))
        assertTrue(inventory.hasItem("Herb"))
    }

    @Test
    fun craftGearModConsumesComponentsAndGrantsResult() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "focusing_lens", name = "Focusing Lens", type = "component"),
                item(id = "wiring_bundle", name = "Wiring Bundle", type = "component"),
                item(id = "power_lens_mk_i", name = "Power Lens Mk. I", type = "gear")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Focusing Lens", 1)
            addItem("Wiring Bundle", 1)
        }
        val recipes = TestRecipeSource(
            tinkering = listOf(
                TinkeringRecipe(
                    id = "mod_power_lens_1",
                    name = "Power Lens Mk. I",
                    description = null,
                    category = "gear",
                    method = "mod",
                    ingredients = mapOf("Focusing Lens" to 1, "Wiring Bundle" to 1),
                    result = "Power Lens Mk. I"
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftTinkering("mod_power_lens_1")

        assertTrue(outcome is CraftingOutcome.Success)
        assertEquals("power_lens_mk_i", outcome.itemId)
        assertTrue(inventory.hasItem("Power Lens Mk. I"))
        assertFalse(inventory.hasItem("Focusing Lens"))
        assertFalse(inventory.hasItem("Wiring Bundle"))
    }

    @Test
    fun craftRecipeWithoutToolsStillSucceeds() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "scrap_metal", name = "Scrap Metal", type = "component"),
                item(id = "armor_plating_mk_i", name = "Armor Plating Mk. I", type = "component")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Scrap Metal", 1)
        }
        val recipes = TestRecipeSource(
            tinkering = listOf(
                TinkeringRecipe(
                    id = "mod_armor_plating_1",
                    name = "Armor Plating Mk. I",
                    description = null,
                    category = "gear",
                    method = "mod",
                    ingredients = mapOf("Scrap Metal" to 1),
                    result = "Armor Plating Mk. I"
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftTinkering("mod_armor_plating_1")

        assertTrue(outcome is CraftingOutcome.Success)
        assertEquals("armor_plating_mk_i", outcome.itemId)
        assertTrue(inventory.hasItem("Armor Plating Mk. I"))
        assertFalse(inventory.hasItem("Scrap Metal"))
    }

    @Test
    fun craftTinkeringAddsResultAndConsumesParts() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "broken_projector", name = "Broken Projector", type = "misc"),
                item(id = "circuit_board", name = "Circuit Board", type = "component"),
                item(
                    id = "repaired_projector",
                    name = "Repaired Projector",
                    type = "misc",
                    categoryOverride = "supplies"
                )
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Broken Projector", 1)
            addItem("Circuit Board", 1)
        }
        val recipes = TestRecipeSource(
            tinkering = listOf(
                TinkeringRecipe(
                    id = "repaired_projector",
                    name = "Repaired Projector",
                    description = null,
                    base = "Broken Projector",
                    components = listOf("Circuit Board"),
                    result = "Repaired Projector",
                    successMessage = "Fixed it."
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftTinkering("repaired_projector")

        assertTrue(outcome is CraftingOutcome.Success)
        assertEquals("repaired_projector", outcome.itemId)
        assertTrue(inventory.hasItem("repaired_projector"))
        assertFalse(inventory.hasItem("Broken Projector"))
        assertFalse(inventory.hasItem("Circuit Board"))
    }

    @Test
    fun cookMeal_consumes_ingredients_and_adds_meal() {
        val inventory = InventoryService(
            TestItemCatalog(
                listOf(
                    item("ration_pack", "Ration Pack"),
                    item("herb", "Herb"),
                    item("ration_soup", "Ration Soup", type = "consumable")
                )
            )
        ).apply {
            loadItems()
            addItem("ration_pack", 1)
            addItem("herb", 1)
        }
        val recipes = TestRecipeSource(
            cooking = listOf(
                CookingRecipe(
                    id = "provision_ration_soup",
                    name = "Ration Soup",
                    ingredients = mapOf("ration_pack" to 1, "herb" to 1),
                    result = "ration_soup",
                    successMessage = "Simmered Ration Soup."
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        assertTrue(service.canCook(recipes.loadCookingRecipes().first()))
        val outcome = service.cookMeal("provision_ration_soup")

        assertTrue(outcome is CraftingOutcome.Success)
        assertEquals("ration_soup", (outcome as CraftingOutcome.Success).itemId)
        assertTrue(inventory.hasItem("ration_soup"))
        assertFalse(inventory.hasItem("ration_pack"))
        assertFalse(inventory.hasItem("herb"))
    }

    @Test
    fun batchCookingConsumesScaledIngredientsAndMultipliesYield() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "ration_pack", name = "Ration Pack"),
                item(id = "herb", name = "Herb"),
                item(id = "ration_soup", name = "Ration Soup")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("ration_pack", 5)
            addItem("herb", 5)
        }
        val recipes = TestRecipeSource(
            cooking = listOf(
                CookingRecipe(
                    id = "provision_ration_soup",
                    name = "Ration Soup",
                    ingredients = mapOf("ration_pack" to 1, "herb" to 1),
                    result = "ration_soup",
                    resultQuantity = 1,
                    successMessage = "Simmered Ration Soup."
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        assertTrue(service.canCook(recipes.loadCookingRecipes().first(), batch = 3))
        val outcome = service.cookMeal("provision_ration_soup", batch = 3)

        assertTrue(outcome is CraftingOutcome.Success)
        // Yield is at least 3 (or 4 if 15% masterwork procced)
        val soupCount = inventory.snapshot()["ration_soup"] ?: 0
        assertTrue("Expected at least 3 soups, got $soupCount", soupCount >= 3)
        // Ingredients remaining: 5 - 3 = 2
        assertEquals(2, inventory.snapshot()["ration_pack"])
        assertEquals(2, inventory.snapshot()["herb"])
    }

    @Test
    fun chefSelectionAppliesUniquePerkToSessionStore() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "ration_pack", name = "Ration Pack"),
                item(id = "herb", name = "Herb"),
                item(id = "ration_soup", name = "Ration Soup")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("ration_pack", 1)
            addItem("herb", 1)
        }
        val recipes = TestRecipeSource(
            cooking = listOf(
                CookingRecipe(
                    id = "provision_ration_soup",
                    name = "Ration Soup",
                    ingredients = mapOf("ration_pack" to 1, "herb" to 1),
                    result = "ration_soup"
                )
            )
        )
        val store = GameSessionStore()
        val service = CraftingService(recipes, inventory, store)

        service.cookMeal("provision_ration_soup", chefId = "nova")

        val activeBuff = store.state.value.activeMealBuff
        org.junit.Assert.assertNotNull(activeBuff)
        assertEquals("nova", activeBuff?.chefId)
        assertEquals(10, activeBuff?.focusBonus)
        assertEquals(3, activeBuff?.remainingEncounters)
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
    private val cooking: List<CookingRecipe> = emptyList()
) : CraftingRecipeSource {
    override fun loadTinkeringRecipes(): List<TinkeringRecipe> = tinkering
    override fun loadCookingRecipes(): List<CookingRecipe> = cooking
}

private fun item(
    id: String,
    name: String,
    type: String = "ingredient",
    categoryOverride: String? = null
): Item = Item(
    id = id,
    name = name,
    aliases = listOf(name),
    type = type,
    categoryOverride = categoryOverride,
    value = 10
)
