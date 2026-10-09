package com.example.starborn.feature.exploration.presentation

/** Authored Android catalogs, shared without rewriting titles, descriptions or identifiers. */
data class SimulationProgramDefinition(val title: String, val category: String, val enemyIds: List<String>, val description: String, val accentArgb: Long)

object AstraCatalog {
    val simulations = listOf(
        SimulationProgramDefinition("Sentinel Target Droid", "TRAINING PROTOCOLS", listOf("sentinel_mki"), "Basic targeting calibration & ATB cadence.", 0xFF00E5FFL),
        SimulationProgramDefinition("Faulted Loader Sub-Routine", "TRAINING PROTOCOLS", listOf("faulted_loader"), "Three-beat armored cycle with exposed Shock openings.", 0xFF00E5FFL),
        SimulationProgramDefinition("Ruin-Guardian Defense Matrix", "TRAINING PROTOCOLS", listOf("ruin_guardian"), "Heavy armor barrier penetration and stability shattering.", 0xFF00E5FFL),

        SimulationProgramDefinition("Apex I: The Iron Warden", "APEX BOSS ARCHIVES", listOf("the_iron_warden"), "World 1 heavy enforcer benchmark. Ground slams & seismic pulse.", 0xFFFFB703L),
        SimulationProgramDefinition("Apex II: The Mire Beast", "APEX BOSS ARCHIVES", listOf("the_beast"), "World 2 jungle apex bio-construct. High vitality and acid spit.", 0xFFFFB703L),
        SimulationProgramDefinition("Apex III: Dominion Administrator", "APEX BOSS ARCHIVES", listOf("administrator_boss"), "World 3 corporate mastermind with defensive subroutines.", 0xFFFFB703L),
        SimulationProgramDefinition("Apex IV: Titan Walker Siegemaster", "APEX BOSS ARCHIVES", listOf("titan_walker_boss"), "World 4 foundry dreadnought with explosive artillery.", 0xFFFFB703L),
        SimulationProgramDefinition("Apex V: Compliance Avatar", "APEX BOSS ARCHIVES", listOf("compliance_avatar"), "World 5 digital construct of executive control.", 0xFFFFB703L),
        SimulationProgramDefinition("Apex VI: Ascended Vale", "APEX BOSS ARCHIVES", listOf("ascended_vale"), "World 6 pre-singularity manifestation with psionic power.", 0xFFFFB703L),

        SimulationProgramDefinition("Crucible I: Foundry Smelter Hazard", "THE RESONANCE CRUCIBLE", listOf("magma_drone", "slag_golem", "welder_bot"), "Thermal combat against high-heat automated factory defenders.", 0xFFFF5252L),
        SimulationProgramDefinition("Crucible II: Corporate Strike Team", "THE RESONANCE CRUCIBLE", listOf("riot_guard", "corporate_assassin", "heavy_mech"), "Coordinated suppression assault. Prioritize high-threat targets.", 0xFFFF5252L),
        SimulationProgramDefinition("Crucible III: Void Security Overdrive", "THE RESONANCE CRUCIBLE", listOf("elite_guard", "void_turret", "hk_droid"), "Dominion supreme hunter-killer droid with 900 HP chassis.", 0xFFFF5252L),
        SimulationProgramDefinition("Crucible IV: Twin Titans of Steel", "THE RESONANCE CRUCIBLE", listOf("the_iron_warden", "titan_walker_boss"), "Simultaneous dual-boss encounter testing ultimate defensive survival.", 0xFFFF5252L),
        SimulationProgramDefinition("Crucible Omega: The Ascended God", "THE RESONANCE CRUCIBLE", listOf("ascended_god"), "The supreme combat challenge. 1,800 HP cosmic singularity testing build perfection.", 0xFFFF1744L)
    )


    val films = listOf(
        Triple("vhs_tape_01", "gf_01_unpayable_debt", "Film 01: Unpayable Debt" to "Mining Pit - Supply Stash"),
        Triple("vhs_tape_02", "gf_02_memories_of_another_life", "Film 02: Memories of Another Life" to "Colony - Jed's Office"),
        Triple("vhs_tape_03", "gf_03_showdown_in_the_rain", "Film 03: Showdown in the Rain" to "Coast - Glow-Moss Cavern"),
        Triple("vhs_tape_04", "gf_04_the_road_at_night", "Film 04: The Road at Night" to "Sector 9 - Ridge Plateau"),
        Triple("vhs_tape_05", "gf_05_the_black_city", "Film 05: The Black City" to "Spire - Night Market"),
        Triple("vhs_tape_06", "gf_06_refuge", "Film 06: Refuge" to "Spire - SkyPark Pavilion"),
        Triple("vhs_tape_07", "gf_07_reclamation", "Film 07: Reclamation" to "Foundry - Smelter Waste"),
        Triple("vhs_tape_08", "gf_08_the_end_of_the_beginning", "Film 08: The End of the Beginning" to "Foundry - Titan Dock"),
        Triple("vhs_tape_09", "gf_09_reconciliation", "Film 09: Reconciliation" to "Void Ring - Solarium"),
        Triple("vhs_tape_10", "gf_10_shackles", "Film 10: Shackles" to "Source - Memory Bridge")
    )
}
