package com.example.starborn.domain.fishing

import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.data.assets.CraftingRecipeSource
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.TinkeringRecipe
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.fishing.viewmodel.FishingState
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel
import com.squareup.moshi.Types
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class FishingRefinementTest {
    private val assets = listOf(File("app/src/main/assets"), File("src/main/assets")).first { it.isDirectory }
    private val data = MoshiProvider.instance.adapter(FishingData::class.java)
        .fromJson(File(assets, "recipes_fishing.json").readText())!!
    private val items = MoshiProvider.instance.adapter<List<Item>>(Types.newParameterizedType(List::class.java, Item::class.java))
        .fromJson(File(assets, "items.json").readText())!!.associateBy { it.id }
    private val dispatcher = StandardTestDispatcher()
    private lateinit var inventory: InventoryService
    private lateinit var store: GameSessionStore
    private lateinit var service: FishingService

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        inventory = InventoryService(object : ItemCatalog {
            override fun load() {}
            override fun findItem(idOrAlias: String) = items[idOrAlias]
        }).apply { loadItems(); addItem("wooden_rod"); addItem("basic_lure") }
        store = GameSessionStore()
        val source = mockk<FishingAssetDataSource> { every { loadFishingData() } returns data }
        service = FishingService(source, inventory, object : Random() { override fun nextBits(bitCount: Int) = 0 }, store)
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun everyAuthoredRodCanLandEveryBehaviorByReadingTheFish() {
        for (rod in data.rods) for ((id, behavior) in data.fishBehaviors) {
            val fight = FishingFight(behavior, rod)
            repeat(150) {
                val hold = fight.phase !in setOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE) && fight.tension < 0.65f
                fight.tick(hold)
            }
            assertTrue("${rod.id} / $id must be catchable within 15 seconds", fight.successful && fight.elapsedMs <= 15_000)
            assertTrue("${rod.id} / $id must reward reading surges", fight.perfect)
            assertTrue("${rod.id} / $id should contain a fight", fight.elapsedMs >= 6_000)
        }
    }

    @Test fun strongestRodStillRequiresGivingSlack() {
        val fight = FishingFight(data.fishBehaviors.getValue("ether_teleport"), data.rods.last())
        repeat(300) { fight.tick(true) }
        assertTrue("Continuous holding must snap the line, even with the strongest rod", fight.failed)
        assertFalse(fight.successful)
    }

    @Test fun releasingDuringSurgeProtectsProgressAndRecoversTension() {
        val fight = FishingFight(data.fishBehaviors.getValue("ether_teleport"), data.rods.first())
        while (fight.phase != FishingFightPhase.SURGE && !fight.failed) {
            fight.tick(fight.phase != FishingFightPhase.WARNING && fight.tension < 0.65f)
        }
        val progress = fight.progress
        val tension = fight.tension
        fight.tick(false)
        assertTrue(progress - fight.progress < 0.002f)
        assertTrue(fight.tension <= tension)
    }

    @Test fun noInputEventuallyLosesTheFish() {
        val fight = FishingFight(data.fishBehaviors.getValue("gentle_wobble"), data.rods.first())
        repeat(500) { fight.tick(false) }
        assertTrue(fight.failed)
    }

    @Test fun rewardsAndRecordsArePerCatchAndDoNotRepeat() {
        fun clean(id: String) = service.secureCatch(FishingResult(id, 1, "Caught", zoneId = "sector9_stream", cleanCatch = true))
        val first = clean("raw_glowfish")
        assertEquals(1, inventory.snapshot()["shiny_lure"])
        service.secureCatch(first)
        clean("raw_glowfish")
        clean("resonance_carp")
        clean("chime_minnow")
        assertEquals(1, inventory.snapshot()["shiny_lure"])
        assertEquals(1, inventory.snapshot()["mystery_lure"])
        inventory.removeItem("raw_glowfish", 2)
        assertTrue(service.getJournal().first { it.zoneId == "sector9_stream" }.species.first { it.itemId == "raw_glowfish" }.clean)
        val saved = store.state.value
        store.restore(saved)
        assertEquals(3, service.getJournal().first { it.zoneId == "sector9_stream" }.species.count { it.caught })
    }

    @Test fun repeatedSpeciesAcrossWatersDoesNotCountAsThreeCleanSpecies() {
        for (zone in listOf("sector9_stream", "spire_runoff", "colony_pit_drain")) {
            service.secureCatch(FishingResult("raw_glowfish", 1, "Caught", zoneId = zone, cleanCatch = true))
        }
        assertFalse("ms_fishing_clean_collection" in store.state.value.completedMilestones)
    }

    @Test fun masterAnglerRequiresNativeFishInAllSixZones() {
        for ((id, catches) in data.zones) {
            catches.firstOrNull { !it.isNativeFish() }?.let {
                service.secureCatch(FishingResult(it.itemId, 1, "Salvage", zoneId = id, cleanCatch = true))
            }
        }
        assertFalse("ms_master_angler" in store.state.value.completedMilestones)
        data.zones.entries.forEachIndexed { index, (id, catches) ->
            service.secureCatch(FishingResult(catches.first { it.isNativeFish() }.itemId, 1, "Caught", zoneId = id))
            assertEquals(index == 5, "ms_master_angler" in store.state.value.completedMilestones)
        }
        assertEquals(1, inventory.snapshot()["harmonic_spool_lure"])
    }

    @Test fun invalidZoneCannotCreateJournalProgress() {
        service.secureCatch(FishingResult("raw_glowfish", 1, "Caught", zoneId = "missing", cleanCatch = true))
        assertTrue(service.getJournal().all { it.species.none { species -> species.caught } })
    }

    @Test fun everyFishingGearHasACampaignSourceOrReachableCraft() {
        val gear = (data.rods.map { it.id } + data.lures.map { it.id }).toSet()
        val reachable = mutableSetOf<String>()
        fun collect(value: Any?) {
            when (value) {
                is String -> if (value in gear) reachable += value
                is Map<*, *> -> value.values.forEach(::collect)
                is List<*> -> value.forEach(::collect)
            }
        }
        for (file in listOf("events.json", "rooms.json", "shops.json")) {
            collect(MoshiProvider.instance.adapter(Any::class.java).fromJson(File(assets, file).readText()))
        }
        val recipes = MoshiProvider.instance.adapter<List<TinkeringRecipe>>(Types.newParameterizedType(List::class.java, TinkeringRecipe::class.java))
            .fromJson(File(assets, "recipes_tinkering.json").readText())!!
        repeat(gear.size) {
            recipes.filter { it.result in gear && (it.ingredients.keys + it.tools).filter { id -> id in gear }.all { id -> id in reachable } }
                .forEach { reachable += it.result }
        }
        assertTrue("Unreachable campaign gear: ${gear - reachable}", reachable.containsAll(gear))
    }

    @Test fun everyZoneUsesAnExistingBackgroundAsset() {
        val worldAssets = listOf(File("world_assets/src/main/assets"), File("../world_assets/src/main/assets")).first { it.isDirectory }
        for (zoneId in data.zones.keys) {
            val zone = service.getFishingZone(zoneId)!!
            assertTrue("$zoneId background is missing", File(worldAssets, zone.backgroundImage!!).isFile)
        }
    }

    @Test fun firstHookPausesForBriefingAndCancelPreservesIt() = runTest(dispatcher) {
        val vm = FishingViewModel(service, "sector9_stream", Random(1))
        runCurrent()
        vm.startFishing()
        advanceTimeBy(3_600); runCurrent()
        assertEquals(FishingState.HOOKSET, vm.uiState.value.fishingState)
        vm.onHookButtonPressed()
        assertEquals(FishingState.READY, vm.uiState.value.fishingState)
        advanceTimeBy(20_000); runCurrent()
        assertEquals(FishingState.READY, vm.uiState.value.fishingState)
        vm.cancelFishing()
        assertFalse(service.hasReelBriefing())
        vm.startFishing(); advanceTimeBy(3_600); runCurrent(); vm.onHookButtonPressed(); vm.beginReeling()
        assertTrue(service.hasReelBriefing())
        vm.cancelFishing()
    }

    @Test fun backgroundPausesHookAndReelTimers() = runTest(dispatcher) {
        service.markReelBriefing()
        val vm = FishingViewModel(service, "sector9_stream", Random(1))
        runCurrent(); vm.startFishing(); advanceTimeBy(3_600); runCurrent()
        vm.setPaused(true)
        val remaining = vm.uiState.value.hookState!!.timeRemainingMs
        advanceTimeBy(5_000); runCurrent()
        assertEquals(remaining, vm.uiState.value.hookState!!.timeRemainingMs)
        vm.setPaused(false); vm.onHookButtonPressed(); vm.onReelPressed()
        advanceTimeBy(240); runCurrent(); vm.setPaused(true)
        val paused = vm.uiState.value.reelState
        advanceTimeBy(5_000); runCurrent()
        assertEquals(paused, vm.uiState.value.reelState)
        vm.cancelFishing()
    }

    @Test fun backgroundPausesWaitingWithoutStartingTheHookWindow() = runTest(dispatcher) {
        val vm = FishingViewModel(service, "sector9_stream", Random(1))
        runCurrent(); vm.startFishing(); vm.setPaused(true)
        val waiting = vm.uiState.value.waitingState
        advanceTimeBy(10_000); runCurrent()
        assertEquals(FishingState.WAITING, vm.uiState.value.fishingState)
        assertEquals(waiting, vm.uiState.value.waitingState)
        vm.cancelFishing()
    }

    @Test fun repeatedRealCastsRecordBeforeReturningToExploration() = runTest(dispatcher) {
        service.markReelBriefing()
        val vm = FishingViewModel(service, "sector9_stream", Random(1))
        runCurrent()
        repeat(3) {
            vm.startFishing(); advanceTimeBy(3_600); runCurrent(); vm.onHookButtonPressed()
            repeat(300) {
                val reel = vm.uiState.value.reelState
                if (reel != null && reel.phase !in setOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE) && reel.tension < 0.65f)
                    vm.onReelPressed() else vm.onReelReleased()
                advanceTimeBy(FishingFight.TICK_MS); runCurrent()
            }
            assertEquals(FishingState.RESULT, vm.uiState.value.fishingState)
            assertTrue(vm.uiState.value.lastCatchResult!!.secured)
            assertTrue(vm.uiState.value.journal.first { it.zoneId == "sector9_stream" }.species.any { it.caught })
            vm.resetFishing()
        }
        assertEquals(3, inventory.snapshot()["raw_glowfish"])
        assertEquals(1, inventory.snapshot()["shiny_lure"])
    }

    @Test fun craftingSalvageLureRetainsTheStarterLure() {
        val recipes = MoshiProvider.instance.adapter<List<TinkeringRecipe>>(Types.newParameterizedType(List::class.java, TinkeringRecipe::class.java))
            .fromJson(File(assets, "recipes_tinkering.json").readText())!!
        val crafting = CraftingService(object : CraftingRecipeSource {
            override fun loadCookingRecipes() = emptyList<com.example.starborn.domain.model.CookingRecipe>()
            override fun loadTinkeringRecipes() = recipes
        }, inventory, store)
        inventory.addItem("scrap_metal", 2); inventory.addItem("wiring_bundle")
        crafting.craftTinkering("gear_salvage_lure")
        assertEquals(1, inventory.snapshot()["basic_lure"])
        assertEquals(1, inventory.snapshot()["salvage_lure"])
    }
}
