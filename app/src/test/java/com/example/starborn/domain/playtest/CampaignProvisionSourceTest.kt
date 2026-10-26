package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.data.repository.*
import com.example.starborn.domain.crafting.*
import com.example.starborn.domain.fishing.*
import com.example.starborn.domain.fishing.MinigameResult
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.shop.ShopController
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CampaignProvisionSourceTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)

    @Test fun cookingIngredientsHaveExternalSourcesInsteadOfDependingOnTheirOwnRecipes() {
        val world = WorldAssetDataSource(reader)
        val source = CraftingAssetDataSource(reader)
        val roots = world.loadRooms().flatMap { it.items }.toMutableSet()
        world.loadRooms().flatMap { it.actions }.forEach { action ->
            roots += (action["items"] as? List<*>)?.filterIsInstance<String>().orEmpty()
        }
        roots += world.loadEnemies().flatMap { it.drops }.filter { it.chance > 0 }.map { it.id }
        roots += ShopAssetDataSource(reader).loadShops().flatMap { it.sells.items + it.sells.rotationPool }
        roots += FishingAssetDataSource(reader).loadFishingData().zones.values.flatten().map { it.itemId }
        fun rewards(actions: List<EventAction>) {
            actions.forEach { action ->
                if (action.type == "give_item") roots += listOfNotNull(action.itemId, action.item)
                roots += action.reward?.items.orEmpty().map { it.itemId }
                roots += action.rewardItems.orEmpty().map { it.itemId }
                rewards(action.`do`.orEmpty() + action.elseDo.orEmpty() + action.onComplete.orEmpty())
            }
        }
        reader.readList<GameEvent>("events.json").forEach { rewards(it.actions) }
        val recipes = source.loadCookingRecipes()
        val tinkering = source.loadTinkeringRecipes()
        roots += setOf(
            "hyperion_logic_board",
            "sector9_optic_board",
            "hyperion_mainframe_chip",
            "thermite_logic_gate",
            "zenith_telemetry_array",
            "prismatic_tuning_fork"
        )
        var changed: Boolean
        do {
            val size = roots.size
            recipes.filter { it.ingredients.keys.all(roots::contains) }.forEach { roots += it.result }
            tinkering.filter { it.ingredients.keys.all(roots::contains) && it.tools.all(roots::contains) }
                .forEach { roots += it.result }
            changed = size != roots.size
        } while (changed)
        val missing = recipes.flatMap { recipe -> recipe.ingredients.keys.filterNot(roots::contains).map { "${recipe.id}: $it" } }
        assertTrue("Cooking ingredients without an authored source: $missing", missing.isEmpty())
        val missingTinkering = tinkering.flatMap { recipe ->
            (recipe.ingredients.keys + recipe.tools).filterNot(roots::contains).map { "${recipe.id}: $it" }
        }
        assertTrue("Tinkering ingredients without an authored source: $missingTinkering", missingTinkering.isEmpty())
    }

    @Test fun regionalPepperPurchaseCanCompletePulseCitrusAtItsDiscoveryStation() = runTest {
        val catalog = ItemRepository(ItemAssetDataSource(reader)).apply { load() }
        val shops = ShopRepository(ShopAssetDataSource(reader)).apply { load() }
        val inventory = InventoryService(catalog).apply { loadItems(); restore(mapOf("raw_glowfish" to 2, "herb" to 1)) }
        val store = GameSessionStore().apply { setPlayer("nova"); setPartyMembers(listOf("nova")); addCredits(100) }
        val shop = ShopController("sentinel_scraps", shops, catalog, inventory, store, this)
        try {
            val pepper = shop.uiState.value.itemsForSale.single { it.id == "fiery_pepper" }
            assertFalse(pepper.locked)
            assertTrue(pepper.canAfford)
            shop.buyItem("fiery_pepper", 2)
            assertEquals(100 - 2 * pepper.price, store.state.value.playerCredits)
            assertEquals(2, inventory.snapshot()["fiery_pepper"])
            assertTrue(shops.shopById("noodle_row_stall")!!.sells.items.contains("fiery_pepper"))
            val crafting = CraftingService(CraftingAssetDataSource(reader), inventory, store)
            assertTrue(crafting.cookMeal("snack_pulse_citrus") is CraftingOutcome.Failure)
            store.setRoom("sector9_ridge_plateau")
            store.setRoomState("sector9_ridge_plateau", "cooking_discovered", true)
            store.setRoom("sector9_beach_cove")
            assertTrue(crafting.cookMeal("snack_pulse_citrus") is CraftingOutcome.Success)
            assertEquals(1, inventory.snapshot()["pulse_citrus"])
            assertFalse(inventory.hasItem("fiery_pepper"))
            assertFalse(inventory.hasItem("raw_glowfish"))
            assertFalse(inventory.hasItem("herb"))
        } finally { shop.close() }
    }

    @Test fun starterGearAndAnEarlierPurchasedBaseCanProduceTheResinUpgrade() = runTest {
        val catalog = ItemRepository(ItemAssetDataSource(reader)).apply { load() }
        val inventory = InventoryService(catalog).apply { loadItems(); restore(mapOf("wooden_rod" to 1, "basic_lure" to 1)) }
        val store = GameSessionStore().apply { addCredits(200) }
        val shop = ShopController("davids_provisions", ShopRepository(ShopAssetDataSource(reader)).apply { load() },
            catalog, inventory, store, this)
        try { shop.buyItem("fiberglass_rod", 1) } finally { shop.close() }
        assertTrue(inventory.hasItem("fiberglass_rod"))
        val source = FishingAssetDataSource(reader)
        val data = source.loadFishingData()
        val random = kotlin.random.Random(42)
        val fishing = FishingService(source, inventory, random, store)
        val rod = data.rods.single { it.id == "wooden_rod" }
        val lure = data.lures.single { it.id == "basic_lure" }
        val zone = requireNotNull(fishing.getFishingZone("spire_runoff"))
        var casts = 0
        while ((!inventory.hasItem("wiring_bundle") || !inventory.hasItem("circuit_board")) && casts++ < 1000) {
            val encounter = requireNotNull(fishing.prepareEncounter(zone, rod, lure))
            val fight = FishingFight(encounter.behavior, rod, !encounter.catch.isNativeFish(), random)
            repeat(500) {
                if (!fight.successful && !fight.failed) fight.tick(fight.phase !in setOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE) && fight.tension < .65f)
            }
            assertTrue(fight.successful)
            fishing.secureCatch(fishing.resolveEncounter(encounter, if (fight.perfect) MinigameResult.PERFECT else MinigameResult.SUCCESS)
                .copy(zoneId = zone.id, cleanCatch = fight.perfect))
        }
        assertTrue("Starter gear must obtain the upgrade components", casts < 1000)
        val crafting = CraftingService(CraftingAssetDataSource(reader), inventory, store)
        assertTrue(crafting.craftTinkering("gear_reinforced_resin_rod") is CraftingOutcome.Success)
        assertTrue(inventory.hasItem("reinforced_resin_rod"))
        assertTrue(inventory.hasItem("wooden_rod"))
        assertTrue(inventory.hasItem("basic_lure"))
        assertFalse(inventory.hasItem("fiberglass_rod"))
    }
}
