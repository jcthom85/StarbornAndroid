package com.example.starborn.desktop

import com.example.starborn.domain.fishing.*
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.math.roundToInt

/** Fishing-only acquisition cost: assumes zones/gear and ingredients without a fishing source are available. */
class FishingEconomyAuditTest {
    @Test fun auditRecipeAcquisitionAndMasteryYield() {
        val directory = Files.createTempDirectory("starborn-fishing-economy-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            val data = services.fishingDataSource.loadFishingData()
            val catchIds = data.zones.values.flatten().map { it.itemId }.toSet()
            val recipes = services.craftingDataSource.loadCookingRecipes().map { Triple(it.id, it.ingredients, "cooking") } +
                services.craftingDataSource.loadTinkeringRecipes().map { Triple(it.id, it.ingredients, "tinkering") }
            val weighting = FishingService::class.java.getDeclaredMethod("buildWeightedCatches", FishingZone::class.java,
                FishingRod::class.java, FishingLure::class.java).apply { isAccessible = true }
            val output = mutableListOf("recipe,system,fish_ingredients,salvage_ingredients,gear,mastery,seeds,mean_casts,p90_casts,mean_seconds,p90_seconds")
            var measured = 0
            for ((id, ingredients, system) in recipes) {
                val needed = ingredients.filterKeys { it in catchIds }
                if (needed.isEmpty()) continue
                for (advanced in listOf(false, true)) for (mastery in listOf(false, true)) {
                    val trials = (0..49).map { seed ->
                        val inventory = InventoryService(services.itemRepository).apply { loadItems() }
                        val store = GameSessionStore()
                        val rng = Random(seed)
                        val fishing = FishingService(services.fishingDataSource, inventory, rng, store)
                        val rod = if (advanced) data.rods.maxBy { it.fishingPower } else data.rods.first()
                        val lures = if (advanced) data.lures else listOf(data.lures.first())
                        var casts = 0
                        var seconds = 0.0
                        while (needed.any { (item, quantity) -> (inventory.snapshot()[item] ?: 0) < quantity } && casts < 2000) {
                            val target = needed.keys.first { (inventory.snapshot()[it] ?: 0) < needed.getValue(it) }
                            val choices = data.zones.keys.flatMap { zoneId ->
                                val zone = requireNotNull(fishing.getFishingZone(zoneId))
                                lures.map { lure ->
                                    @Suppress("UNCHECKED_CAST")
                                    val weighted = weighting.invoke(fishing, zone, rod, lure) as List<Pair<FishingCatchDefinition, Double>>
                                    Triple(zone, lure, weighted.filter { it.first.itemId == target }.sumOf { it.second } / weighted.sumOf { it.second })
                                }
                            }
                            val best = choices.maxBy { it.third }
                            assertTrue("Fishing ingredient must be obtainable: $target", best.third > 0)
                            val encounter = requireNotNull(fishing.prepareEncounter(best.first, rod, best.second))
                            val fight = FishingFight(encounter.behavior, rod, !encounter.catch.isNativeFish(), rng)
                            repeat(500) {
                                if (!fight.successful && !fight.failed) fight.tick(fight.phase !in setOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE) && fight.tension < .65f)
                            }
                            assertTrue("Attentive fishing policy should finish", fight.successful)
                            val result = fishing.resolveEncounter(encounter, if (fight.perfect) MinigameResult.PERFECT else MinigameResult.SUCCESS)
                                .copy(zoneId = best.first.id, cleanCatch = mastery && fight.perfect)
                            fishing.secureCatch(result)
                            casts++
                            seconds += rng.nextLong(1800, 3500) / 1000.0 + fight.elapsedMs / 1000.0
                        }
                        assertTrue("Recipe acquisition exceeded audit bound: $id", casts < 2000)
                        casts to seconds
                    }
                    fun mean(values: List<Double>) = "%.2f".format(java.util.Locale.US, values.average())
                    val nativeIds = data.zones.values.flatten().filter { it.isNativeFish() }.map { it.itemId }.toSet()
                    val native = needed.keys.filter { it in nativeIds }.joinToString(";")
                    val salvage = needed.keys.filter { it !in nativeIds }.joinToString(";")
                    output += "$id,$system,$native,$salvage,${if (advanced) "best_available" else "starter"},$mastery,50," +
                        "${mean(trials.map { it.first.toDouble() })},${trials.map { it.first }.sorted()[44]}," +
                        "${mean(trials.map { it.second })},${"%.2f".format(java.util.Locale.US, trials.map { it.second }.sorted()[44])}"
                    measured++
                }
            }
            assertTrue("Audit must include fishing-dependent recipes", measured > 0)
            val shops = services.shopDataSource.loadShops()
            fun saleValue(id: String): Int {
                val item = requireNotNull(services.itemRepository.findItem(id))
                if (item.unsellable) return 0
                return shops.filter { shop ->
                    shop.buys?.blacklist.orEmpty().none { it.equals(item.id, true) || it.equals(item.name, true) } &&
                        (shop.buys?.acceptTypes.isNullOrEmpty() || shop.buys?.acceptTypes.orEmpty().any { it.equals(item.type, true) })
                }.maxOfOrNull { shop ->
                    val base = item.resaleValue ?: maxOf(item.value, item.buyPrice ?: 0)
                    maxOf(1, (base * (shop.pricing?.buyMarkdown ?: .35)).roundToInt())
                } ?: 0
            }
            val income = mutableListOf("zone,gear,mastery,casts,fish_copies,bonus_copies,seconds,gross_credits,gross_credits_per_minute")
            for (zoneId in data.zones.keys) for (advanced in listOf(false, true)) for (mastery in listOf(false, true)) {
                val rng = Random(1234)
                val inventory = InventoryService(services.itemRepository).apply { loadItems() }
                val fishing = FishingService(services.fishingDataSource, inventory, rng, GameSessionStore())
                val rod = if (advanced) data.rods.maxBy { it.fishingPower } else data.rods.first()
                val lure = data.lures.first()
                val zone = requireNotNull(fishing.getFishingZone(zoneId))
                var seconds = 0.0
                var bonus = 0
                var fish = 0
                var credits = 0L
                repeat(1000) {
                    val encounter = requireNotNull(fishing.prepareEncounter(zone, rod, lure))
                    val fight = FishingFight(encounter.behavior, rod, !encounter.catch.isNativeFish(), rng)
                    repeat(500) {
                        if (!fight.successful && !fight.failed) fight.tick(fight.phase !in setOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE) && fight.tension < .65f)
                    }
                    assertTrue(fight.successful)
                    val result = fishing.secureCatch(fishing.resolveEncounter(encounter,
                        if (fight.perfect) MinigameResult.PERFECT else MinigameResult.SUCCESS)
                        .copy(zoneId = zoneId, cleanCatch = mastery && fight.perfect))
                    bonus += result.masteryBonusQuantity
                    if (encounter.catch.isNativeFish()) fish += result.quantity
                    credits += result.quantity.toLong() * saleValue(result.itemId)
                    seconds += rng.nextLong(1800, 3500) / 1000.0 + fight.elapsedMs / 1000.0
                }
                income += "$zoneId,${if (advanced) "best_rod" else "starter"},$mastery,1000,$fish,$bonus," +
                    "${"%.2f".format(java.util.Locale.US, seconds)},$credits,${"%.2f".format(java.util.Locale.US, credits * 60 / seconds)}"
            }
            File("build/reports/fishing-income.csv").apply { parentFile.mkdirs(); writeText(income.joinToString("\n") + "\n") }
            File("build/reports/fishing-economy.csv").apply { parentFile.mkdirs(); writeText(output.joinToString("\n") + "\n") }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
