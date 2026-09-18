package com.example.starborn.debug

import com.example.starborn.feature.mainmenu.DebugScenario
import com.example.starborn.feature.mainmenu.DebugScenarioCatalog
import com.example.starborn.feature.mainmenu.DebugScenarioCategory
import com.example.starborn.feature.mainmenu.DebugScenarioDestination

enum class DebugFixture { OPENING, CRYO_EXACT, CRYO_MISSING, ARCADE_READY, FISHING, COOKING, TINKERING, SHOP, CAMPAIGN_CHECKPOINT }

data class DebugTestProcedure(
    val fixture: DebugFixture,
    val startingState: String,
    val steps: List<String>,
    val expected: List<String>,
    val coverageIds: Set<String>,
    val limitations: String,
    val revision: Int = 1,
    val cabinetId: String? = null,
    val targetId: String? = null,
    val emptyWallet: Boolean = false
)

data class DebugArcadeCabinet(val id: String, val title: String, val discoveryMilestone: String, val repairedMilestone: String)

object DebugArcadeCabinets {
    val all = listOf(
        DebugArcadeCabinet("deep_mine_asteroid_drill", "Deep Mine Asteroid Drill", "ms_arcade_deep_mine_discovered", "ms_arcade_cabinet_01_repaired"),
        DebugArcadeCabinet("canopy_hopper", "Canopy Hopper", "ms_arcade_canopy_hopper_discovered", "ms_arcade_cabinet_02_repaired"),
        DebugArcadeCabinet("spire_infiltrator", "Spire Infiltrator", "ms_arcade_spire_infiltrator_discovered", "ms_arcade_cabinet_03_repaired"),
        DebugArcadeCabinet("slag_catcher", "Slag Catcher", "ms_arcade_slag_catcher_discovered", "ms_arcade_cabinet_04_repaired"),
        DebugArcadeCabinet("orbital_defense", "Orbital Defense 2000", "ms_arcade_orbital_defense_discovered", "ms_arcade_cabinet_05_repaired"),
        DebugArcadeCabinet("harmonic_pulse", "Harmonic Pulse", "ms_arcade_harmonic_pulse_discovered", "ms_arcade_cabinet_06_repaired")
    )
}

/** Only rebuilt fixtures belong here. Legacy shortcuts remain visible in a separate migration list. */
object DebugTestRegistry {
    val scenarios = listOf(
        test("campaign_w1_mq01", "Opening / Wake Up Call", DebugScenarioCategory.STORY,
            DebugTestProcedure(DebugFixture.OPENING,
                "Normal New Game in Nova's bunk; Nova alone, normal starter progression, tutorials enabled by game settings.",
                listOf("Play the intro. Turn on the bunk light, inspect the door control, and leave the sleeping level.",
                    "Find Jed and follow the workshop objective. Inspect the loader and defeat it using its Shock opening.",
                    "Receive starter gear, craft the Functional Cryo-Inductor, and finish the cutter preparation actions.",
                    "Complete Wake Up Call, inspect the journal/rewards, and follow the next objective."),
                listOf("Room actions and tutorials leave controls usable; the loader can be beaten with normal resources.",
                    "Crafting consumes the required items and unlocks the intended cutter ability.",
                    "Wake Up Call completes once and Shift Clearance can proceed without a debug repair."),
                setOf("quests:w1_mq01", "enemies:faulted_loader", "recipes_tinkering:repair_cryo_inductor"),
                "This starts a normal campaign. No seeded quest completion or extra resources; gameplay acceptance is pending.")),
        test("recipe_cryo_exact", "Tinkering / Exact Cryo Requirements", DebugScenarioCategory.SYSTEM,
            DebugTestProcedure(DebugFixture.CRYO_EXACT,
                "Jed's workshop after the briefing, with exactly the current Cryo-Inductor recipe requirements.",
                listOf("Open Tinkering from the menu and select Functional Cryo-Inductor.",
                    "Record quantities, craft once, and inspect the result and Cryo Vent availability.",
                    "Attempt the recipe again, then save to a test slot and reload that slot."),
                listOf("The first craft succeeds and consumes exactly the recipe requirements.",
                    "A second craft is refused without consuming anything; reload preserves the result."),
                setOf("recipes_tinkering:repair_cryo_inductor"),
                "Synthetic crafting boundary; extra story supplies are absent. Use Opening to prove quest continuation.")),
        test("recipe_cryo_missing", "Tinkering / Missing Scrap", DebugScenarioCategory.SYSTEM,
            DebugTestProcedure(DebugFixture.CRYO_MISSING,
                "The exact-recipe fixture with scrap metal removed; no other state difference.",
                listOf("Open Tinkering and attempt Functional Cryo-Inductor.",
                    "Compare inventory before/after the refused action, then leave and reopen Tinkering.",
                    "Save to a test slot and reload. Confirm the recipe still requires scrap."),
                listOf("Crafting is refused with useful feedback and consumes no remaining item.",
                    "Closing or loading does not grant a result or bypass the missing material."),
                setOf("recipes_tinkering:repair_cryo_inductor"),
                "Tests refusal only. Recovering spent resources through exploration needs a separate legal-route test.")),
        test("recovery_opening", "Recovery / Opening Save and Reload", DebugScenarioCategory.SYSTEM,
            DebugTestProcedure(DebugFixture.OPENING,
                "Normal New Game with isolated test saves; start before opening actions.",
                listOf("Finish the intro, turn on the bunk light, open the door, and record room and journal state.",
                    "Save to test slot 1. Move to another room, then load slot 1 without relaunching this fixture.",
                    "Relaunch the app in Test Session mode and load test slot 1 again. Continue toward Jed."),
                listOf("Both loads restore the saved location, inventory, and completed opening tasks.",
                    "Already completed actions remain completed; the route forward stays usable."),
                setOf("quests:w1_mq01"),
                "Manual-slot recovery only; autosave, mid-battle recovery and process-death timing have separate acceptance cases."))
    ) + DebugArcadeCabinets.all.map { cabinet ->
        test("arcade_${cabinet.id}", "Arcade / ${cabinet.title}", DebugScenarioCategory.SYSTEM,
            DebugTestProcedure(DebugFixture.ARCADE_READY,
                "Astra common room with ${cabinet.title} installed, no scores or claimed reward tiers, and Nova alone.",
                listOf("Select ${cabinet.title}. Read and dismiss its installation introduction, then start a game.",
                    "Play a run, inspect its result, and retry. Check controls, score, exit, and reward feedback.",
                    "Reach a reward tier, record the payout, and replay that tier. Save and reload the test slot."),
                listOf("The correct arcade game opens and controls, retry and exit remain usable.",
                    "High score and play count update correctly. Each tier bonus is paid once; repeat-run earnings follow the game's rules.",
                    "Loading preserves installation, score, claimed tiers, and awarded inventory/credits."),
                setOf("arcade:${cabinet.id}"),
                "Installed directly in a synthetic ship workbench. Discovery, repair, campaign access and all-cabinet completion need separate tests.",
                cabinetId = cabinet.id), world = "The Astra")
    }

    val allScenarios: List<DebugScenario> get() = DebugScenarioCatalog.burgfestScenarios + scenarios + DebugCampaignScenarios.scenarios + DebugSystemScenarios.scenarios

    fun find(id: String): DebugScenario? = allScenarios.firstOrNull { it.id == id }

    private fun test(id: String, title: String, category: DebugScenarioCategory, procedure: DebugTestProcedure,
                     world: String = "World 1: The Mines") =
        DebugScenario(id, title, procedure.startingState, category, DebugScenarioDestination.EXPLORATION,
            world, procedure)
}
