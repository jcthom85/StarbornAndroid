package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.ItemAssetDataSource
import com.example.starborn.data.assets.ShopAssetDataSource
import com.example.starborn.data.repository.ItemRepository
import com.example.starborn.data.assets.CraftingAssetDataSource
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.model.CookingRecipe
import com.example.starborn.domain.model.TinkeringRecipe
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

/** Catalog checks are not proof of affordability or route access. */
class EconomyCatalogTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val items = ItemRepository(ItemAssetDataSource(reader)).apply { load() }
    private val shops = ShopAssetDataSource(reader).loadShops()

    @Test fun `shop funded crafting chains have no positive expected resale profit`() {
        data class RecipeCost(val id: String, val result: String, val yield: Double, val ingredients: Map<String, Int>)
        fun canonical(token: String) = requireNotNull(items.findItem(token)).id
        val recipes = reader.readList<TinkeringRecipe>("recipes_tinkering.json").map {
            RecipeCost(it.id, canonical(it.result), it.resultQuantity.toDouble(), it.ingredients.ifEmpty {
                (listOfNotNull(it.base) + it.components).groupingBy { it }.eachCount()
            })
        } + reader.readList<CookingRecipe>("recipes_cooking.json").map {
            // One-item batches maximize the 15% masterwork extra portion per input.
            RecipeCost(it.id, canonical(it.result), it.resultQuantity + 0.15, it.ingredients)
        }
        val costs = mutableMapOf<String, Double>()
        shops.forEach { shop ->
            (shop.sells.items + shop.sells.rotationPool).forEach { token ->
                val item = requireNotNull(items.findItem(token))
                val price = (((item.buyPrice ?: item.value).coerceAtLeast(1)) *
                    (shop.pricing?.sellMarkup ?: 1.0)).roundToInt().coerceAtLeast(1).toDouble()
                costs[item.id] = minOf(costs[item.id] ?: Double.POSITIVE_INFINITY, price)
            }
        }
        // Deliberately generous access: all shops/gates/tools/recipes available.
        // Fractional expected output is a lower cost bound, not a playable batch.
        var converged = false
        for (pass in 0..100) {
            var changed = false
            recipes.forEach { recipe ->
                if (recipe.ingredients.keys.all { canonical(it) in costs }) {
                    val cost = recipe.ingredients.entries.sumOf { (id, qty) -> costs.getValue(canonical(id)) * qty } / recipe.yield
                    if (cost + 0.000001 < (costs[recipe.result] ?: Double.POSITIVE_INFINITY)) {
                        costs[recipe.result] = cost
                        changed = true
                    }
                }
            }
            if (!changed) { converged = true; break }
        }
        assertTrue("Craft cost relaxation did not converge; inspect cyclic recipes", converged)
        val violations = mutableListOf<String>()
        costs.forEach { (id, cost) ->
            val item = requireNotNull(items.findItem(id))
            shops.forEach { shop ->
                val buys = shop.buys
                val allowed = !item.unsellable && (buys?.acceptTypes.isNullOrEmpty() ||
                    buys!!.acceptTypes.any { it.equals(item.type, true) }) &&
                    buys?.blacklist.orEmpty().none { it.equals(item.id, true) || it.equals(item.name, true) }
                if (allowed) {
                    val resale = ((item.resaleValue ?: maxOf(item.value, item.buyPrice ?: 0)) *
                        (shop.pricing?.buyMarkdown ?: 0.35)).roundToInt().coerceAtLeast(1)
                    if (resale > cost + 0.000001) violations += "$id at ${shop.id}: cost=$cost resale=$resale"
                }
            }
        }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test fun `regional salvage funds both recipes and cannot be collected twice after restore`() {
        val events = reader.readList<GameEvent>("events.json")
        val rooms = reader.readList<Room>("rooms.json").map { it.id }.toSet()
        val cases = listOf(
            Triple("w4_salvage_pure_iron", "pure_iron", listOf("repair_slag_catcher_cabinet", "mod_thermite_core")),
            Triple("w5_salvage_composite_plate", "composite_plate", listOf("repair_orbital_defense_cabinet", "mod_orbital_deflector")),
            Triple("w6_salvage_astral_thread", "astral_thread", listOf("repair_harmonic_pulse_cabinet", "mod_astral_weave"))
        )
        cases.forEach { (eventId, material, recipes) ->
            val event = events.single { it.id == eventId }
            val room = requireNotNull(event.trigger.room)
            assertTrue("Salvage room must exist", room in rooms)
            val inventory = InventoryService(items).apply { loadItems() }
            val store = GameSessionStore()
            fun manager(target: GameSessionStore) = EventManager(listOf(event), target,
                EventHooks(onGiveItem = { id, qty ->
                    inventory.addItem(id, qty)
                    target.setInventory(inventory.snapshot())
                }))
            manager(store).handleTrigger("enter_room", EventPayload.EnterRoom(room))
            assertEquals(mapOf(material to 4), inventory.snapshot())
            val restored = GameSessionStore().apply { restore(store.state.value) }
            manager(restored).handleTrigger("enter_room", EventPayload.EnterRoom(room))
            assertEquals(mapOf(material to 4), inventory.snapshot())
            val crafting = CraftingService(CraftingAssetDataSource(reader), inventory, restored)
            recipes.forEach { recipeId ->
                val recipe = crafting.tinkeringRecipes.single { it.id == recipeId }
                // Other components are seeded: this proves the new material's
                // acquisition-to-consumption path, not all ingredient sources.
                crafting.ingredientsFor(recipe).filterKeys { it != material }.forEach { (id, qty) ->
                    inventory.addItem(id, qty)
                }
                recipe.tools.forEach { inventory.addItem(it, 1) }
                assertTrue(crafting.canCraft(recipe))
                assertTrue(crafting.craftTinkering(recipeId) is CraftingOutcome.Success)
                assertTrue(inventory.hasItem(recipe.result, recipe.resultQuantity))
            }
            assertFalse(inventory.hasItem(material))
            manager(restored).handleTrigger("enter_room", EventPayload.EnterRoom(room))
            assertFalse("Consumed cache must not respawn", inventory.hasItem(material))
        }
    }

    @Test fun `stock resolves and direct cross shop resale cannot generate credits`() {
        assertTrue(shops.isNotEmpty())
        shops.forEach { seller ->
            (seller.sells.items + seller.sells.rotationPool).forEach { token ->
                val item = requireNotNull(items.findItem(token)) { "${seller.id}: unknown $token" }
                val buy = (((item.buyPrice ?: item.value).coerceAtLeast(1)) *
                    (seller.pricing?.sellMarkup ?: 1.0)).roundToInt().coerceAtLeast(1)
                shops.forEach { buyer ->
                    val allowed = !item.unsellable &&
                        (buyer.buys?.acceptTypes.isNullOrEmpty() || buyer.buys!!.acceptTypes.any { it.equals(item.type, true) }) &&
                        buyer.buys?.blacklist.orEmpty().none { it.equals(item.id, true) || it.equals(item.name, true) }
                    if (allowed) {
                        val resale = ((item.resaleValue ?: maxOf(item.value, item.buyPrice ?: 0)) *
                            (buyer.pricing?.buyMarkdown ?: 0.35)).roundToInt().coerceAtLeast(1)
                        assertTrue("${item.id}: buy at ${seller.id} for $buy, sell at ${buyer.id} for $resale", resale <= buy)
                    }
                }
            }
        }
    }

    @Test fun `recipe quantities and outputs are valid catalog entries`() {
        val cooking = reader.readList<CookingRecipe>("recipes_cooking.json")
        val tinkering = reader.readList<TinkeringRecipe>("recipes_tinkering.json")
        assertTrue(cooking.isNotEmpty() && tinkering.isNotEmpty())
        val missingIngredients = mutableSetOf<String>()
        fun check(id: String, result: String, count: Int, ingredients: Map<String, Int>) {
            assertNotNull("$id result $result", items.findItem(result))
            assertTrue("$id output count", count > 0)
            assertTrue("$id ingredients", ingredients.isNotEmpty())
            ingredients.forEach { (token, qty) ->
                if (items.findItem(token) == null) missingIngredients += "$id:$token"
                assertTrue("$id quantity $token", qty > 0)
            }
        }
        cooking.forEach { check(it.id, it.result, it.resultQuantity, it.ingredients) }
        tinkering.forEach { recipe ->
            val requirements = recipe.ingredients.ifEmpty {
                (listOfNotNull(recipe.base) + recipe.components).groupingBy { it }.eachCount()
            }
            check(recipe.id, recipe.result, recipe.resultQuantity, requirements)
            recipe.tools.forEach { assertNotNull("${recipe.id} tool $it", items.findItem(it)) }
        }
        assertEquals(emptySet<String>(), missingIngredients)
    }
}
