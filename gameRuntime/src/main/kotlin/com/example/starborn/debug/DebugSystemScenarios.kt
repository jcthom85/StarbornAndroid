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

    val scenarios: List<DebugScenario> = focusedScenarios + fishingLocations.map { location ->
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
                "Astra galley with four injured crew members, all recipe discoveries unlocked, and ingredients for three batches of every recipe.",
                listOf("Use the galley kitchenette and cook each recipe; compare ingredient consumption and normal/masterwork yield.",
                    "Select each chef. Cook meals and snacks; use meals from inventory and equip snacks as combat abilities.",
                    "Try another meal: cancel replacement, then confirm. Save/reload and compare meal, chef, inventory and HP."),
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

    private val focusedScenarios: List<DebugScenario>
        get() = listOf(
            scenario("system_fishing_intro", "Fishing / First Kit Discovery", "World 2: Sector 9",
                DebugTestProcedure(DebugFixture.CAMPAIGN_CHECKPOINT,
                    "World 2 crash checkpoint before examining the pod; Nova and Zeke with modest campaign supplies, no fishing gear.",
                    listOf("Examine the pod and recover the fishing kit. Stabilize Zeke and follow the landing route.",
                        "Reach Beach Pools and catch a fish with the starter gear. Inspect its cooking use.",
                        "Save in slot 1 and reload; confirm gear and catch persist."),
                    listOf("The kit is obtained through the real pod action, not granted by the fixture.",
                        "Starter fishing is understandable and the first catch has a clear use."),
                    setOf("fishing_zone:sector9_stream", "quests:w2_mq01"),
                    "Synthetic World 2 checkpoint; does not establish the full World 1 economy.", targetId = "w2_mq01")),
            scenario("system_cooking_discovery", "Cooking / Discover Regional Recipes", "World 2: Sector 9",
                DebugTestProcedure(DebugFixture.COOKING,
                    "Stream Falls with four injured crew and cooking ingredients; regional recipe flags are unset.",
                    listOf("Use the cooking station and find Tideglass Delight and Mineral Trail Mix.",
                        "Cook a meal and craft the snack. Move to Ridge Plateau and discover Pulse Citrus.",
                        "Save in slot 1, reload, and revisit the recipe lists."),
                    listOf("Stations unlock their own recipes; later-world discoveries remain locked.",
                        "The snack cannot be consumed from Items or crafted again while owned; discoveries survive reload."),
                    setOf("system_behavior:recipe_discovery"),
                    "Ingredients are stocked; this checks station discovery and persistence, not ingredient availability.",
                    systemMode = "discovery")),
            scenario("system_food_combat", "Cooking / Meals and Snacks in Combat", "World 2: Sector 9",
                DebugTestProcedure(DebugFixture.COOKING,
                    "Beach Pools with a level 3 crew, prepared meals, all three snacks and recovery supplies; meals start inactive.",
                    listOf("Equip a snack through Party. Eat a meal and record its three-battle duration.",
                        "Find a nearby enemy. Use the snack twice to inspect its cooldown, then use a prepared meal as a combat item.",
                        "Complete or retreat from three battles; check meal duration after each. Save/reload between battles."),
                    listOf("Snacks remain owned, respect cooldowns, and are not consumable inventory items.",
                        "Combat food applies immediate effects without replacing Well-Fed; victory/retreat each spend one meal charge."),
                    setOf("system_behavior:combat_food"),
                    "Stocked combat fixture; approach real room enemies. Reset to restore cleared encounters.",
                    systemMode = "combat")),
            scenario("system_tinkering_discovery", "Tinkering / Learn a Schematic", "The Astra",
                DebugTestProcedure(DebugFixture.TINKERING,
                    "Astra workbench with an unlearned Source Resin schematic and exactly its required ingredients.",
                    listOf("Open Tinkering; find the discovered schematic and learn it directly.",
                        "Craft Source Resin and inspect the output. Attempt another craft with the spent ingredients.",
                        "Save in slot 1 and reload; confirm the learned recipe and output remain."),
                    listOf("The blueprint is visible before learning, and learning does not spend recipe materials.",
                        "Crafting consumes the displayed requirements; refusal consumes nothing."),
                    setOf("recipes_tinkering:mod_source_resin"),
                    "Blueprint is seeded in inventory; find it through Orion's Tideglass side quest to test acquisition.",
                    systemMode = "discovery")),
            scenario("system_tinkering_boundaries", "Tinkering / Salvage and Missing Materials", "The Astra",
                DebugTestProcedure(DebugFixture.TINKERING,
                    "Astra workbench with one equipped Power Lens, a spare Ergonomic Grip, a protected Cryo-Inductor, and no loose crafting materials.",
                    listOf("Inspect Scrap: the equipped lens and quest tool must be protected.",
                        "Salvage the spare grip and compare the stated returns with inventory.",
                        "Attempt Cryo-Inductor crafting with missing materials. Save/reload and inspect inventory."),
                    listOf("Protected items cannot be scrapped; the spare returns only authored materials.",
                        "Missing-material refusal and reloading do not duplicate supplies."),
                    setOf("system_behavior:salvage_boundaries"),
                    "Synthetic protection fixture; use the normal opening to judge early material pacing.",
                    systemMode = "boundaries"))
        )

    private fun scenario(id: String, title: String, world: String, procedure: DebugTestProcedure) =
        DebugScenario(id, title, procedure.startingState, DebugScenarioCategory.SYSTEM,
            DebugScenarioDestination.EXPLORATION, world, procedure)
}
