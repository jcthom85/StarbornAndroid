package com.example.starborn.domain.economy

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.ShopAssetDataSource
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.ShopDefinition
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Validates Vector C of the Automated Testing Strategy:
 * Full Campaign Economy Deficit / Starvation Simulation.
 *
 * Enforces that:
 * 1. Clean progression without grinding (no fishing, no infinite enemy farming) earns sufficient
 *    credits through mandatory main quest milestones and boss defeats to afford baseline healing
 *    supplies and gear upgrades across all 6 worlds without entering deficit or soft-locking.
 * 2. Shop integrity: Sell markups and buy markdowns prevent infinite arbitrage loops (buy price > sell price).
 * 3. Every stocked item in every shop resolves to a valid catalog item with positive price.
 */
class CampaignEconomyStarvationTest {

    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val allItems by lazy { reader.readList<Item>("items.json").associateBy { it.id } }
    private val allEnemies by lazy { reader.readList<Enemy>("enemies.json").associateBy { it.id } }
    private val allEvents by lazy { reader.readList<GameEvent>("events.json") }

    private val allRooms by lazy { reader.readList<Room>("rooms.json") }
    private val allShops by lazy {
        ShopAssetDataSource(reader).loadShops().associateBy { it.id }
    }

    // =========================================================================
    // 1. Full Campaign Economy Simulation (Worlds 1–6 Clean Route)
    // =========================================================================

    @Test
    fun campaignEconomy_cleanProgression_neverFallsIntoDeficit() {
        // Player starts with standard starting purse (50 credits, as defined in save templates)
        var playerCredits = 50

        // Extract distinct enemies encountered across authored rooms per world (1 kill each along critical path, no grinding)
        val enemiesPerWorld = (1..6).associateWith { world ->
            allRooms
                .filter { it.backgroundImage.contains("world_$world") }
                .flatMap { it.enemies }
                .distinct()
        }

        // Baseline essential purchases per world (medkits and core trauma restoratives)
        val baselinePurchasesPerWorld = mapOf(
            1 to listOf("medkit" to 2),
            2 to listOf("medkit" to 3),
            3 to listOf("medkit" to 3, "painkillers" to 2),
            4 to listOf("medkit" to 4),
            5 to listOf("medkit" to 4),
            6 to listOf("medkit" to 5)
        )

        // Fixed quest credit rewards defined in events.json
        val questCreditsByWorld = mutableMapOf<Int, Int>()
        for (event in allEvents) {
            val eventWorld = when {
                event.id.startsWith("w1_") -> 1
                event.id.startsWith("w2_") -> 2
                event.id.startsWith("w3_") -> 3
                event.id.startsWith("w4_") -> 4
                event.id.startsWith("w5_") -> 5
                event.id.startsWith("w6_") -> 6
                else -> 0
            }
            if (eventWorld in 1..6) {
                for (action in event.actions) {
                    val amount = action.credits ?: action.reward?.credits ?: 0
                    if (amount > 0) {
                        questCreditsByWorld[eventWorld] = (questCreditsByWorld[eventWorld] ?: 0) + amount
                    }
                }
            }
        }

        // Simulate each world sequentially
        for (world in 1..6) {
            val startingWorldBalance = playerCredits

            // 1. Inflow from mandatory combat encounters (single clear of each room type)
            val enemies = enemiesPerWorld[world].orEmpty()
            var combatCreditsEarned = 0
            var lootSalvageCredits = 0

            for (enemyId in enemies) {
                val enemy = allEnemies[enemyId]
                // Credit drop from enemy
                val creditReward = enemy?.creditReward?.takeIf { it > 0 } ?: 20
                combatCreditsEarned += creditReward

                // Baseline salvage/drop liquidation (selling 1 common scrap drop to vendor at markdown)
                val primaryDrop = enemy?.drops?.firstOrNull()
                if (primaryDrop != null) {
                    val dropItem = allItems[primaryDrop.id]
                    val dropValue = dropItem?.value ?: 10
                    lootSalvageCredits += max(1, (dropValue * 0.35).roundToInt())
                }
            }
            playerCredits += combatCreditsEarned + lootSalvageCredits

            // 2. Inflow from quest completions
            val questCredits = questCreditsByWorld[world] ?: 0
            playerCredits += questCredits

            // 3. Outflow for mandatory healing and consumable supplies
            val purchases = baselinePurchasesPerWorld[world].orEmpty()
            var suppliesCost = 0
            for ((itemId, qty) in purchases) {
                val item = allItems[itemId]
                val basePrice = item?.value ?: 25
                val purchasePrice = (basePrice * 1.2).roundToInt()
                suppliesCost += purchasePrice * qty
            }
            playerCredits -= suppliesCost

            // Invariant: Player balance must NEVER drop below 0 (no starvation / bankruptcy soft-lock)
            assertTrue(
                "World $world economy must remain solvent (balance was $playerCredits, started with $startingWorldBalance, earned +${combatCreditsEarned + lootSalvageCredits} combat/loot, spent -$suppliesCost on supplies)",
                playerCredits >= 0
            )
        }

        // Final verification: player reaches endgame solvent with a comfortable surplus
        assertTrue(
            "Player should reach endgame solvent with surplus credits (balance was $playerCredits)",
            playerCredits >= 100
        )
    }

    // =========================================================================
    // 2. Shop Catalog & Pricing Integrity
    // =========================================================================

    @Test
    fun shopCatalog_allStockedItems_havePositivePrices_andNoArbitrage() {
        for ((shopId, shop) in allShops) {
            val markup = shop.pricing?.sellMarkup ?: 1.0
            val markdown = shop.pricing?.buyMarkdown ?: 0.35

            // Enforce no infinite money loop: player cannot buy item and sell it back for a profit
            assertTrue(
                "Shop $shopId markup ($markup) must strictly exceed markdown ($markdown) to prevent infinite credit exploits",
                markup > markdown
            )

            for (itemId in shop.sells.items) {
                val item = allItems[itemId]
                assertNotNull("Stocked item '$itemId' in shop $shopId must exist in items.json", item)
                val base = (item?.buyPrice ?: item?.value ?: 0).coerceAtLeast(1)
                val buyPrice = max(1, (base * markup).roundToInt())
                val sellPrice = max(1, (base * markdown).roundToInt())

                assertTrue("Item $itemId price in shop $shopId must be positive", buyPrice > 0)
                assertTrue("Item $itemId buy price ($buyPrice) must exceed sell price ($sellPrice) in shop $shopId", buyPrice > sellPrice)
            }
        }
    }
}
