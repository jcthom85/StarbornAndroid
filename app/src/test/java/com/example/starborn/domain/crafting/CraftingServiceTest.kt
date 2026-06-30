package com.example.starborn.domain.crafting

import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
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
    fun craftProvisionFailsWhenRequiredToolIsMissing() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "raw_glowfish", name = "Raw Glowfish"),
                item(id = "herb", name = "Herb"),
                item(id = "glowfish_broth", name = "Glowfish Broth"),
                item(id = "portable_stove", name = "Portable Stove", type = "utility")
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
                    result = "Glowfish Broth",
                    tools = listOf("portable_stove")
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftTinkering("provision_glowfish_broth")

        assertTrue(outcome is CraftingOutcome.Failure)
        assertEquals("Missing components or tools", (outcome as CraftingOutcome.Failure).message)
        assertFalse(inventory.hasItem("Glowfish Broth"))
        assertTrue(inventory.hasItem("Raw Glowfish"))
        assertTrue(inventory.hasItem("Herb"))
    }

    @Test
    fun craftProvisionWithToolDoesNotConsumeTool() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "raw_glowfish", name = "Raw Glowfish"),
                item(id = "herb", name = "Herb"),
                item(id = "glowfish_broth", name = "Glowfish Broth"),
                item(id = "portable_stove", name = "Portable Stove", type = "utility")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Raw Glowfish", 2)
            addItem("Herb", 1)
            addItem("Portable Stove", 1)
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
                    result = "Glowfish Broth",
                    tools = listOf("portable_stove")
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val outcome = service.craftTinkering("provision_glowfish_broth")

        assertTrue(outcome is CraftingOutcome.Success)
        assertEquals("glowfish_broth", outcome.itemId)
        assertTrue(inventory.hasItem("Glowfish Broth"))
        assertTrue(inventory.hasItem("Portable Stove"))
        assertFalse(inventory.hasItem("Raw Glowfish"))
        assertFalse(inventory.hasItem("Herb"))
    }

    @Test
    fun craftedStoveUnlocksCookingWithoutBeingConsumed() {
        val catalog = TestItemCatalog(
            listOf(
                item(id = "scrap_metal", name = "Scrap Metal", type = "component"),
                item(id = "wiring_bundle", name = "Wiring Bundle", type = "component"),
                item(id = "portable_stove", name = "Portable Stove", type = "utility"),
                item(id = "ration_pack", name = "Ration Pack", type = "consumable"),
                item(id = "herb", name = "Herb"),
                item(id = "ration_soup", name = "Ration Soup", type = "consumable")
            )
        )
        val inventory = InventoryService(catalog).apply {
            loadItems()
            addItem("Scrap Metal", 2)
            addItem("Wiring Bundle", 1)
            addItem("Ration Pack", 1)
            addItem("Herb", 1)
        }
        val recipes = TestRecipeSource(
            tinkering = listOf(
                TinkeringRecipe(
                    id = "gear_portable_stove",
                    name = "Portable Stove",
                    description = null,
                    category = "gear",
                    method = "mod",
                    ingredients = mapOf("Scrap Metal" to 2, "Wiring Bundle" to 1),
                    result = "Portable Stove"
                ),
                TinkeringRecipe(
                    id = "provision_ration_soup",
                    name = "Ration Soup",
                    description = null,
                    category = "provision",
                    method = "field_cook",
                    ingredients = mapOf("Ration Pack" to 1, "Herb" to 1),
                    result = "Ration Soup",
                    tools = listOf("portable_stove")
                )
            )
        )
        val service = CraftingService(recipes, inventory, GameSessionStore())

        val stoveOutcome = service.craftTinkering("gear_portable_stove")
        val soupOutcome = service.craftTinkering("provision_ration_soup")

        assertTrue(stoveOutcome is CraftingOutcome.Success)
        assertEquals("portable_stove", stoveOutcome.itemId)
        assertTrue(soupOutcome is CraftingOutcome.Success)
        assertEquals("ration_soup", soupOutcome.itemId)
        assertTrue(inventory.hasItem("Portable Stove"))
        assertTrue(inventory.hasItem("Ration Soup"))
        assertFalse(inventory.hasItem("Scrap Metal"))
        assertFalse(inventory.hasItem("Wiring Bundle"))
        assertFalse(inventory.hasItem("Ration Pack"))
        assertFalse(inventory.hasItem("Herb"))
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
}

private class EmptyItemCatalog : ItemCatalog {
    override fun load() {
        // no-op
    }

    override fun findItem(idOrAlias: String): Item? = null
}

private class EmptyRecipeSource : CraftingRecipeSource {
    override fun loadTinkeringRecipes(): List<TinkeringRecipe> = emptyList()
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
    private val firstAid: List<FirstAidRecipe> = emptyList()
) : CraftingRecipeSource {
    override fun loadTinkeringRecipes(): List<TinkeringRecipe> = tinkering
    override fun loadFirstAidRecipes(): List<FirstAidRecipe> = firstAid
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
