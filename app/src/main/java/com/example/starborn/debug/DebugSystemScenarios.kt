package com.example.starborn.debug

import com.example.starborn.feature.mainmenu.DebugScenario
import com.example.starborn.feature.mainmenu.DebugScenarioCategory
import com.example.starborn.feature.mainmenu.DebugScenarioDestination

data class DebugFishingLocation(val zone: String, val room: String, val world: String)

object DebugSystemScenarios {
    val shopIds = listOf("mechanic_shop", "weapon_shop", "armor_shop", "accessory_shop", "general_store",
        "scrappers_contraband", "tysons_provisions", "upper_city_lounge", "sentinel_scraps", "arcade_prize_shop")
    val fishingLocations = listOf(
        DebugFishingLocation("colony_pit_drain", "mine_landing", "World 1: The Mines"),
        DebugFishingLocation("sector9_stream", "sector9_beach_pools", "World 2: Sector 9"),
        DebugFishingLocation("spire_runoff", "spire_sewers_passage", "World 3: The Spire"),
        DebugFishingLocation("foundry_cooling_runoff", "foundry_cooling_springs", "World 4: The Foundry"),
        DebugFishingLocation("orbital_false_tide", "orbital_solarium", "World 5: The Void"),
        DebugFishingLocation("singularity_ether_well", "source_new_world_node_scale_final", "World 6: The Source")
    )

    val scenarios: List<DebugScenario> = fishingLocations.map { location ->
        scenario("fish_${location.zone}", "Fishing / ${location.zone.replace('_', ' ')}", location.world,
            DebugTestProcedure(DebugFixture.FISHING,
                "At ${location.room}, with all authored rods and lures; only the fishing action's prerequisite milestones are seeded.",
                listOf("Open the fishing action and compare rod/lure choices, including the salvage lure.",
                    "Catch a fish, fail a catch, and cancel a run. Compare inventory after each outcome.",
                    "Repeat with the weakest and strongest rods. Save and reload the test slot after a successful catch."),
                listOf("The correct zone opens; success grants its catch once, failure/cancel grants no catch.",
                    "Gear selection changes difficulty as described; rewards persist after reload."),
                setOf("fishing_zone:${location.zone}"),
                "Synthetic fishing workbench. Does not establish story access, gear acquisition, or route balance.",
                targetId = location.zone))
    } + listOf(
        scenario("system_cooking", "Cooking / All Recipes and Chefs", "The Astra",
            DebugTestProcedure(DebugFixture.COOKING,
                "Astra galley with the four playable crew members injured and ingredients for three batches of every cooking recipe.",
                listOf("Use the galley kitchenette and cook each recipe; compare ingredient consumption and normal/masterwork yield.",
                    "Select each available chef. Eat a meal from inventory and inspect recovery and bonuses.",
                    "Eat a different meal to test replacement. Save/reload and compare active meal, chef, inventory and HP."),
                listOf("Every current recipe is cookable; batches consume the displayed quantities.",
                    "Chef selection, meal replacement and disk reload preserve the expected state."),
                setOf("system_behavior:meals"),
                "Stocked workbench; does not prove ingredient economy or expiration through actual battles.")),
        scenario("system_tinkering", "Tinkering / All Schematics", "The Astra",
            DebugTestProcedure(DebugFixture.TINKERING,
                "Astra common room with every current schematic, required tools, and three batches of ingredients.",
                listOf("Open Tinkering from the field menu. Inspect each learned recipe and craft its result.",
                    "Compare tools, ingredient quantities, result quantity and descriptions with the displayed recipe.",
                    "Save/reload and verify crafted items and learned schematics remain available."),
                listOf("All authored schematics are available; crafting consumes ingredients and retains tools as specified.",
                    "Results and schematics survive loading without duplicate grants."),
                setOf("system_behavior:recipe_boundaries"),
                "Synthetic recipe workbench. Quest/cabinet handoffs and schematic discovery require their dedicated scenarios."))
    ) + shopIds.flatMap { shop ->
        listOf(false, true).map { empty ->
            scenario("shop_${shop}${if (empty) "_empty" else ""}",
                "Shop / ${shop.replace('_', ' ')} / ${if (empty) "No credits" else "Stocked"}", "System Workbench",
                DebugTestProcedure(DebugFixture.SHOP,
                    "Opens the shop directly; ${if (empty) "zero" else "10,000"} credits, sellable supplies, and no story gate overrides.",
                    listOf("Inspect buy/sell lists, item restrictions, prices and shop dialogue.",
                        if (empty) "Attempt a paid purchase. Verify refusal preserves credits and inventory. Sell a permitted item and retry."
                        else "Buy an unlocked item, verify quantity and credit changes, then sell a permitted item.",
                        "Leave the shop, save from the staging room and reload. Verify inventory and credits."),
                    listOf("Transactions respect displayed prices, available funds, quantity limits and item restrictions.",
                        "Rejected transactions change nothing; successful transactions persist without repeated payment."),
                    setOf("shops:$shop"),
                    "Direct shop workbench, not story access or regional economy proof. Locked stock stays locked; compare dispenser currency behavior against its text.",
                    targetId = shop, emptyWallet = empty))
        }
    }

    private fun scenario(id: String, title: String, world: String, procedure: DebugTestProcedure) =
        DebugScenario(id, title, procedure.startingState, DebugScenarioCategory.SYSTEM,
            DebugScenarioDestination.EXPLORATION, world, procedure)
}
