package com.example.starborn.domain.crafting

import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.domain.fishing.*
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.crafting.CraftingViewModel
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel
import com.squareup.moshi.Types
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ProvisionIntegrationTest {
    private val assets = listOf(File("app/src/main/assets"), File("src/main/assets")).first { it.isDirectory }
    private inline fun <reified T> list(name: String): List<T> = MoshiProvider.instance.adapter<List<T>>(
        Types.newParameterizedType(List::class.java, T::class.java)).fromJson(File(assets,name).readText()).orEmpty()
    private val items by lazy { list<Item>("items.json").associateBy { it.id } }
    private val cooking by lazy { list<CookingRecipe>("recipes_cooking.json") }
    private val tinkering by lazy { list<TinkeringRecipe>("recipes_tinkering.json") }
    private val fishing by lazy { MoshiProvider.instance.adapter(FishingData::class.java)
        .fromJson(File(assets,"recipes_fishing.json").readText())!! }
    private val dispatcher = StandardTestDispatcher()
    private lateinit var inventory: InventoryService
    private lateinit var store: GameSessionStore
    private lateinit var crafting: CraftingService
    private lateinit var source: FishingAssetDataSource

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        inventory = InventoryService(object: ItemCatalog {
            override fun load() {}
            override fun findItem(idOrAlias: String) = items[idOrAlias]
        }).apply { loadItems() }
        store = GameSessionStore().apply { setPartyMembers(listOf("nova")); setPlayer("nova") }
        crafting = CraftingService(object: CraftingRecipeSource {
            override fun loadCookingRecipes() = cooking
            override fun loadTinkeringRecipes() = tinkering
        }, inventory, store)
        source = mockk { every { loadFishingData() } returns fishing }
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun repeatedFishingKeepsEveryCatchBeforeReturning() {
        val service = FishingService(source, inventory, sessionStore = store, craftingService = crafting)
        val vm = FishingViewModel(service, "sector9_stream")
        val emit = FishingViewModel::class.java.getDeclaredMethod("emitResult", FishingResult::class.java,
            com.example.starborn.domain.fishing.MinigameResult::class.java).apply { isAccessible = true }
        repeat(3) {
            emit.invoke(vm, FishingResult("raw_glowfish",1,"Caught"), com.example.starborn.domain.fishing.MinigameResult.SUCCESS)
            assertTrue(vm.uiState.value.lastCatchResult!!.secured)
            assertTrue(vm.uiState.value.lastCatchResult!!.uses.any { it.contains("Broth") })
            vm.resetFishing()
        }
        assertEquals(3, inventory.snapshot()["raw_glowfish"])
        assertEquals(inventory.snapshot(), store.state.value.inventory)
        val secured = service.secureCatch(FishingResult("raw_glowfish",1,"Caught"))
        service.secureCatch(secured)
        assertEquals(4, inventory.snapshot()["raw_glowfish"])
    }

    @Test fun everyAuthoredRecipeCanLoadRefreshAndCraftFromBench() {
        tinkering.forEach { recipe ->
            inventory.restore(crafting.ingredientsFor(recipe) + recipe.tools.associateWith { 1 })
            assertTrue("${recipe.id} has an invalid base", recipe.base in crafting.ingredientsFor(recipe))
            val vm = CraftingViewModel(crafting, inventory, store)
            vm.autoFill(recipe.id)
            dispatcher.scheduler.runCurrent()
            assertEquals(recipe.id, vm.uiState.value.bench.preview?.recipeId)
            assertTrue("${recipe.id} should be craftable", vm.uiState.value.bench.canCraftSelection)
            vm.craftFromBench()
            dispatcher.scheduler.runCurrent()
            assertTrue("${recipe.id} output missing", inventory.hasItem(recipe.result))
        }
    }

    @Test fun caughtFishCanBecomeAMealWhoseEffectsAndChefApplyOnlyWhenEaten() = runTest {
        inventory.restore(mapOf("raw_glowfish" to 2,"herb" to 1))
        assertTrue(crafting.cookMeal("provision_glowfish_broth","nova") is CraftingOutcome.Success)
        assertNull(store.state.value.activeMealBuff)
        val use = ItemUseController(inventory, crafting, store) { emptyMap() }
        assertTrue(use.useItem("glowfish_broth") is ItemUseController.Result.Success)
        val buff = store.state.value.activeMealBuff!!
        assertEquals(5, buff.accuracyBonus)
        assertEquals(10, buff.focusBonus)
        assertEquals("nova", buff.chefId)
        assertEquals(3, buff.remainingEncounters)
        repeat(3) { store.decrementMealBuffEncounter() }
        assertNull(store.state.value.activeMealBuff)
    }

    @Test fun allAuthoredFoodStatsSurviveConsumptionAndMealsReplaceRatherThanStack() = runTest {
        val use = ItemUseController(inventory, crafting, store) { emptyMap() }
        cooking.forEach { recipe ->
            inventory.addItem(recipe.result)
            assertNotNull("${recipe.result} needs an effect", items.getValue(recipe.result).effect)
            assertTrue(use.useItem(recipe.result) is ItemUseController.Result.Success)
            val buff = store.state.value.activeMealBuff!!
            assertEquals(recipe.result, buff.recipeId)
            items.getValue(recipe.result).effect!!.buffs.orEmpty().forEach { effect ->
                val actual = when(effect.stat) {
                    "accuracy" -> buff.accuracyBonus
                    "evasion" -> buff.evasionBonus
                    "strength" -> buff.strengthBonus
                    "defense" -> buff.defenseBonus
                    "agility" -> buff.agilityBonus
                    "luck" -> buff.luckBonus
                    "focus" -> buff.focusBonus - 10
                    "speed" -> buff.speedBonus
                    "crit" -> (buff.critBonus * 100).toInt()
                    "resist" -> buff.statusResistBonus
                    else -> error("Unsupported food stat ${effect.stat}")
                }
                assertEquals("${recipe.result}: ${effect.stat}", effect.value, actual)
            }
        }
    }

    @Test fun unrecruitedChefCannotGrantPerksOrSpendIngredients() {
        inventory.restore(mapOf("raw_glowfish" to 2,"herb" to 1))
        val before = inventory.snapshot()
        assertTrue(crafting.cookMeal("provision_glowfish_broth","orion") is CraftingOutcome.Failure)
        assertEquals(before, inventory.snapshot())
        assertNull(store.state.value.activeMealBuff)
    }

    @Test fun salvageIsOptionalAndEveryCatchHasARealItem() {
        val materialIds = setOf("scrap_metal","wiring_bundle","circuit_board","nano_filament")
        fishing.zones.forEach { (zone, catches) ->
            catches.forEach { assertTrue("$zone: ${it.itemId}", it.itemId in items) }
            assertTrue(catches.any { it.itemId in materialIds })
            assertTrue(catches.filter { it.itemId in materialIds }.sumOf { it.weight } < catches.sumOf { it.weight } / 5)
        }
        assertTrue(fishing.lures.any { it.id == "bioluminescent_lure" })
        assertTrue(fishing.lures.any { it.id == "salvage_lure" })
    }

    @Test fun salvageLureAndZoneBonusChangeActualCatchOdds() {
        val zone = fishing.zones.getValue("colony_pit_drain").let { FishingZone("colony_pit_drain","Drain",it) }
        fun catches(lure: FishingLure): Int {
            val service = FishingService(source,inventory,Random(42))
            return (1..5000).count { service.prepareEncounter(zone,fishing.rods.first(),lure)!!.catch.itemId in setOf("scrap_metal","wiring_bundle") }
        }
        val lure = fishing.lures.first { it.id == "salvage_lure" }
        assertTrue(catches(lure) > catches(lure.copy(zoneBonuses = emptyMap())))
        assertTrue(catches(lure) > catches(fishing.lures.first { it.id == "basic_lure" }))
    }
}
