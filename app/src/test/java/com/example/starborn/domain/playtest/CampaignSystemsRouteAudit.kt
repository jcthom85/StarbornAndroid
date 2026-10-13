package com.example.starborn.domain.playtest

import com.example.starborn.data.assets.*
import com.example.starborn.data.repository.*
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.fishing.*
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.node.NodeProgressionEvaluator
import com.example.starborn.domain.session.*
import com.example.starborn.feature.exploration.viewmodel.ExplorationController
import com.example.starborn.feature.shop.ShopController
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.*
import java.io.File
import kotlin.random.Random

/** Observations on the assisted campaign runner; hypothetical side trips never mutate its state. */
class CampaignSystemsRouteAudit(reader: AssetJsonReader) {
    private val world = WorldAssetDataSource(reader)
    private val rooms = world.loadRooms().associateBy { it.id }
    private val nodes = world.loadHubNodes()
    private val items = ItemRepository(ItemAssetDataSource(reader)).apply { load() }
    private val shops = ShopRepository(ShopAssetDataSource(reader)).apply { load() }
    private val fishingSource = FishingAssetDataSource(reader)
    private val fishingData = fishingSource.loadFishingData()
    private val enemies = world.loadEnemies().associateBy { it.id }
    private val events = reader.readList<GameEvent>("events.json")
    private val records = mutableListOf<List<Any?>>()
    private val recipes = mutableListOf<List<Any?>>()
    private val income = mutableListOf<List<Any?>>()
    private val incomeKeys = mutableSetOf<String>()
    private val earlierPotential = mutableSetOf<String>()
    private var previous: GameSessionState? = null
    private var step = 0
    private val cls = ExplorationController::class.java
    private val visible = cls.getDeclaredMethod("visibleConnections", Room::class.java).apply { isAccessible = true }
    private val modeClass = cls.declaredClasses.single { it.simpleName == "DirectionEvaluationMode" }
    private val checkMode = modeClass.enumConstants.orEmpty().single { it.toString() == "CHECK" }
    private val directionCheck = cls.getDeclaredMethod("evaluateDirection", Room::class.java, String::class.java, modeClass).apply { isAccessible = true }
    private val blockedProperty = cls.declaredClasses.single { it.simpleName == "DirectionEvaluation" }
        .getDeclaredMethod("getBlocked").apply { isAccessible = true }
    private val hubReturn = cls.getDeclaredMethod("canReturnToHub", Room::class.java).apply { isAccessible = true }

    fun capture(controller: ExplorationController, store: GameSessionStore, inventory: InventoryService,
                crafting: CraftingService, assisted: Int, scope: CoroutineScope) {
        val state = store.state.value.copy(battleCheckpoint = null)
        if (state == previous) return
        previous = state
        step++
        val accessible = linkedSetOf<String>()
        val queue = java.util.ArrayDeque<String>()
        state.roomId?.let(queue::add)
        var hubExpanded = false
        var astraExpanded = false
        val nodePolicy = NodeProgressionEvaluator()
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!accessible.add(id)) continue
            val room = rooms[id] ?: continue
            @Suppress("UNCHECKED_CAST")
            val connections = visible.invoke(controller, room) as Map<String, String>
            for ((direction, destination) in connections) {
                // Named special exits become TravelAction and use the same production gate check.
                if (blockedProperty.invoke(directionCheck.invoke(controller, room, direction, checkMode)) == true) continue
                if (destination in rooms) queue.add(destination)
            }
            if (!hubExpanded && hubReturn.invoke(controller, room) == true) {
                hubExpanded = true
                nodes.filter { it.hubId == state.hubId && nodePolicy.evaluate(it, state).canEnterFromHub }
                    .forEach { queue.add(it.entryRoom) }
                if ("ms_w2_mq05_complete" in state.completedMilestones) {
                    astraExpanded = true
                    AstraTravel.availableDestinations(state).forEach { destination ->
                        queue.add(destination.roomId)
                        nodes.filter { it.hubId == destination.hubId && nodePolicy.evaluate(it, state).canEnterFromHub }
                            .forEach { queue.add(it.entryRoom) }
                    }
                }
            }
        }
        fun permitted(action: Map<String, Any?>): Boolean {
            val required = (action["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty() +
                listOfNotNull(action["requires_milestone"] as? String)
            return required.all { it in state.completedMilestones }
        }
        val actions = accessible.mapNotNull(rooms::get).flatMap { room -> room.actions.filter(::permitted).map { room.id to it } }
        val localShopIds = actions.filter { it.second["type"] == "shop" }.mapNotNull { it.second["shop_id"] as? String }.distinct()
        val zones = actions.filter { it.second["type"] == "fishing" }.mapNotNull { it.second["zone_id"] as? String }.distinct()
        val kitchens = actions.filter { (_, action) -> action["cook_source"] != null }.map { it.first }.toSet()
        val shopControllers = localShopIds.map { ShopController(it, shops, items, inventory, store, scope) }
        try {
            val stock = shopControllers.flatMap { it.uiState.value.itemsForSale }.filterNot { it.locked }
            val buyPrices = stock.groupBy { it.id }.mapValues { (_, entries) -> entries.minOf { it.price } }
            val owned = inventory.snapshot().filterValues { it > 0 }.keys
            val potential = (owned + buyPrices.keys + accessible.flatMap { rooms[it]?.items.orEmpty() }
                .mapNotNull { items.findItem(it)?.id }).toMutableSet()
            val eventManager = com.example.starborn.domain.event.EventManager(events, store)
            val conditions = eventManager.javaClass.getDeclaredMethod("conditionsSatisfied", List::class.java, GameSessionState::class.java)
                .apply { isAccessible = true }
            actions.forEach { (roomId, action) ->
                potential += (action["items"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                val actionId = action["action_event"] as? String
                events.filter { (actionId != null && it.trigger.action == actionId) || it.trigger.room == roomId || it.trigger.roomId == roomId }
                    .filter { it.repeatable || it.id !in state.completedEvents }
                    .filter { conditions.invoke(eventManager, it.conditions, state.copy(roomId = roomId)) == true }
                    .flatMap { it.actions }.forEach { reward ->
                        if (reward.type == "give_item") potential += listOfNotNull(reward.itemId, reward.item)
                        potential += reward.reward?.items.orEmpty().map { it.itemId }
                        potential += reward.rewardItems.orEmpty().map { it.itemId }
                    }
            }
            // Potential enemy drops are candidates, not guaranteed loot or a battle-success claim.
            accessible.flatMap { rooms[it]?.enemies.orEmpty() }.mapNotNull(enemies::get)
                .flatMap { it.drops }.filter { it.chance > 0 }.forEach { potential += it.id }
            fun addCatchSources() {
                if (fishingData.rods.any { it.id in potential } && fishingData.lures.any { it.id in potential }) {
                    zones.flatMap { fishingData.zones[it].orEmpty() }.forEach { potential += it.itemId }
                    // Production collection rewards can bootstrap later lure recipes.
                    if (zones.isNotEmpty()) potential += "shiny_lure"
                    if (zones.flatMap { fishingData.zones[it].orEmpty() }.filter { it.isNativeFish() }.map { it.itemId }.toSet().size >= 3)
                        potential += "mystery_lure"
                }
            }
            // Least fixed point: a recipe cannot provide its own missing ingredient or tool.
            var changed: Boolean
            do {
                val size = potential.size
                addCatchSources()
                crafting.tinkeringRecipes.filter { !it.requiresSchematic || crafting.isSchematicLearned(it.id) }
                    .filter { crafting.ingredientsFor(it).keys.all(potential::contains) && it.tools.all(potential::contains) }
                    .forEach { potential += it.result }
                changed = potential.size != size
            } while (changed)
            val carryPlan = (earlierPotential + potential).toMutableSet()
            do {
                val size = carryPlan.size
                crafting.tinkeringRecipes.filter { !it.requiresSchematic || crafting.isSchematicLearned(it.id) }
                    .filter { crafting.ingredientsFor(it).keys.all(carryPlan::contains) && it.tools.all(carryPlan::contains) }
                    .forEach { carryPlan += it.result }
                changed = carryPlan.size != size
            } while (changed)
            earlierPotential += carryPlan
            val rods = fishingData.rods.filter { it.id in owned }
            val lures = fishingData.lures.filter { it.id in owned }
            records += listOf(step, state.worldId, state.roomId, assisted, state.playerCredits, accessible.size,
                localShopIds.joinToString(";"), zones.joinToString(";"), rods.joinToString(";") { it.id },
                lures.joinToString(";") { it.id }, kitchens.joinToString(";"), state.learnedSchematics.joinToString(";"), accessible.joinToString(";"), astraExpanded)
            fun addRecipe(id: String, type: String, result: String, requirements: Map<String, Int>, tools: List<String>,
                          discovered: Boolean, discoveryCandidate: Boolean, craftable: Boolean) {
                val missing = requirements.filter { (item, count) -> crafting.availableForCraft(item) < count }
                val missingTools = tools.filterNot(owned::contains)
                val cost = if (missing.keys.all(buyPrices::containsKey) && missingTools.all(buyPrices::containsKey))
                    missing.entries.sumOf { (item, count) -> (count - crafting.availableForCraft(item)) * buyPrices.getValue(item) } +
                        missingTools.sumOf { buyPrices.getValue(it) } else null
                recipes += listOf(step, state.worldId, state.roomId, assisted, id, type, result, discovered, discoveryCandidate,
                    craftable && (if (type == "tinkering")
                        actions.any { it.second["type"] == "tinkering" } || "ms_tinkering_tutorial_done" in state.completedMilestones
                        else kitchens.isNotEmpty()), craftable, missing.keys.joinToString(";"), missingTools.joinToString(";"),
                    (requirements.keys + tools).filterNot(potential::contains).joinToString(";"), cost,
                    cost?.let { it <= state.playerCredits }, (requirements.keys + tools).filterNot(carryPlan::contains).joinToString(";"))
            }
            crafting.cookingRecipes.forEach {
                addRecipe(it.id, it.category, it.result, it.ingredients, emptyList(), crafting.isRecipeDiscovered(it),
                    it.discoveryRoom == null || it.discoveryRoom in kitchens, crafting.canCook(it))
            }
            crafting.tinkeringRecipes.forEach {
                addRecipe(it.id, "tinkering", it.result, crafting.ingredientsFor(it), it.tools,
                    !it.requiresSchematic || crafting.isSchematicLearned(it.id), !it.requiresSchematic || crafting.isSchematicLearned(it.id),
                    crafting.canCraft(it))
            }
            if (rods.isNotEmpty() && lures.isNotEmpty() && shopControllers.isNotEmpty()) {
                val entries = fishingData.zones.values.flatten().map { it.itemId }.distinct()
                    .map { InventoryEntry(requireNotNull(items.findItem(it)), 10) }
                val sellMethod = ShopController::class.java.getDeclaredMethod("buildSellItems", ShopDefinition::class.java, List::class.java)
                    .apply { isAccessible = true }
                val sales = localShopIds.zip(shopControllers).flatMap { (id, ctrl) ->
                    @Suppress("UNCHECKED_CAST")
                    (sellMethod.invoke(ctrl, requireNotNull(shops.shopById(id)), entries) as List<com.example.starborn.feature.shop.SellItemUi>)
                        .filter { it.canSell }
                }.groupBy { it.id }.mapValues { (_, values) -> values.maxOf { it.price } }
                for (zoneId in zones) {
                    val rod = rods.maxBy { it.fishingPower }
                    val lure = lures.first()
                    val key = "$zoneId/${rod.id}/${lure.id}/${sales.toSortedMap()}/${buyPrices.toSortedMap()}"
                    if (!incomeKeys.add(key)) continue
                    val simulationInventory = InventoryService(items).apply { loadItems() }
                    val simulationStore = GameSessionStore()
                    val rng = Random(41)
                    val service = FishingService(fishingSource, simulationInventory, rng, simulationStore)
                    val zone = requireNotNull(service.getFishingZone(zoneId))
                    var seconds = 0.0
                    var credits = 0L
                    repeat(300) {
                        val encounter = requireNotNull(service.prepareEncounter(zone, rod, lure))
                        val fight = FishingFight(encounter.behavior, rod, !encounter.catch.isNativeFish(), rng)
                        repeat(500) {
                            if (!fight.successful && !fight.failed) fight.tick(fight.phase !in setOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE) && fight.tension < .65f)
                        }
                        assertTrue(fight.successful)
                        val secured = service.secureCatch(service.resolveEncounter(encounter,
                            if (fight.perfect) MinigameResult.PERFECT else MinigameResult.SUCCESS)
                            .copy(zoneId = zoneId, cleanCatch = fight.perfect))
                        credits += secured.quantity.toLong() * (sales[secured.itemId] ?: 0)
                        seconds += fight.elapsedMs / 1000.0 + rng.nextLong(1800, 3500) / 1000.0
                    }
                    val rate = credits * 60 / seconds
                    val healing = buyPrices["medkit_i"] ?: buyPrices["medkit"]
                    val upgradedHealing = buyPrices["medkit_plus"]
                    income += listOf(step, state.worldId, state.roomId, assisted, zoneId, rod.id, lure.id,
                        localShopIds.joinToString(";"), rate, healing, healing?.let { it / rate }, state.playerCredits,
                        buyPrices.filterKeys { id -> fishingData.rods.any { it.id == id } }.toString(), astraExpanded,
                        upgradedHealing, upgradedHealing?.let { it / rate })
                }
            }
        } finally { shopControllers.forEach { it.close() } }
    }

    fun writeReports(assistance: List<String>) {
        assertTrue("Campaign audit needs all six worlds", records.map { it[1] }.toSet().containsAll((1..6).map { "world_$it" }))
        assertTrue("Campaign audit must observe services", records.any { it[6].toString().isNotEmpty() })
        val directory = File("build/reports/campaign-systems").apply { mkdirs() }
        fun csv(name: String, header: String, rows: List<List<Any?>>) {
            fun cell(value: Any?): String = "\"${(value?.toString() ?: "").replace("\"", "\"\"")}\""
            File(directory, name).writeText(header + "\n" + rows.joinToString("\n") { it.joinToString(",", transform = ::cell) } + "\n")
        }
        csv("checkpoints.csv", "step,world,room,assistance_count,credits,reachable_rooms,shops,zones,owned_rods,owned_lures,kitchens,schematics,reachable_room_ids,astra_return_enabled", records)
        csv("recipes.csv", "step,world,room,assistance_count,recipe,type,result,discovered,discovery_candidate,held_ingredients_and_station,held_ingredients_craftable,missing_ingredients,missing_tools,unresolved_potential_sources,purchase_cost,affordable_purchase,unresolved_carry_plan_sources", recipes)
        csv("income.csv", "step,world,room,assistance_count,zone,rod,lure,reachable_shops,gross_credits_per_minute,medkit_price,medkit_fishing_minutes,credits,rod_prices,astra_return_enabled,medkit_plus_price,medkit_plus_fishing_minutes", income)
        File(directory, "assistance.txt").writeText(assistance.joinToString("\n") + "\n")
    }
}
