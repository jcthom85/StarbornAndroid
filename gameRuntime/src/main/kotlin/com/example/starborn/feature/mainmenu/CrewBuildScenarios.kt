package com.example.starborn.feature.mainmenu

import com.example.starborn.debug.DebugFixture
import com.example.starborn.debug.DebugTestProcedure
import com.example.starborn.domain.leveling.CrewBuildPrototype

/** Full-party builds use recorded campaign progression, never maximum-level debug inventory. */
object CrewBuildScenarios {
    val all = listOf(
        DebugScenario(CrewBuildPrototype.SCENARIO, "Crew Builds / Fresh opening",
            "Earn AP normally. All four specialization trees become available as the crew joins.",
            DebugScenarioCategory.SYSTEM, DebugScenarioDestination.EXPLORATION,
            procedure = DebugTestProcedure(DebugFixture.OPENING, "Normal opening, Nova alone, no free AP or XP.",
                listOf("Earn AP and buy a perk. Recruit the crew to open their trees.", "Compare paths with the free Reset build button; save to a test slot."),
                listOf("All 32 nodes have executable effects. AP is shared; free learned skills survive a reset."),
                setOf("skill_trees:nova", "skill_trees:zeke", "skill_trees:orion", "skill_trees:gh0st"),
                "Experimental balance; production migration is pending.", systemMode = "nova_builds"))
    ) + listOf(Triple("early", "Early full party / 26 AP", "World 4: The Foundry"),
        Triple("middle", "Middle campaign / 36 AP", "World 5: The Void"),
        Triple("late", "Late campaign / 44 AP", "World 6: The Source")).map { (id, title, world) ->
        DebugScenario("crew_build_$id", "Crew Builds / $title",
            "All four crew, recorded campaign gear/levels/resources, unspent AP, and free build resets.",
            DebugScenarioCategory.SYSTEM, DebugScenarioDestination.EXPLORATION, world,
            DebugTestProcedure(DebugFixture.OPENING,
                "Verified campaign save after World ${when (id) { "early" -> 3; "middle" -> 4; else -> 5 }}. Proposed quest/recruitment AP is reconstructed from completed sources; only logged test purchases are refunded.",
                listOf("Open Party and allocate shared AP across the four crew.", "Test a focused or mixed build against the next encounters.",
                    "Save before fighting. Reset a character build to refund its recorded AP and compare another approach."),
                listOf("Skills, receipts, free core abilities and unspent AP survive save/load.",
                    "Fourth nodes require Foundry completion; the early checkpoint keeps that gate locked."),
                setOf("skill_trees:nova", "skill_trees:zeke", "skill_trees:orion", "skill_trees:gh0st"),
                "Derived from seed-17 baseline public-route saves, with a documented starter recipe-component fixture. These checkpoints do not establish prototype balance.",
                systemMode = "crew_checkpoint_$id"))
    }
}
