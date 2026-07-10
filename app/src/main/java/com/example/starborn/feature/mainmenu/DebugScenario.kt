package com.example.starborn.feature.mainmenu

enum class DebugScenarioCategory(val label: String) {
    STORY("Story"),
    WORLD("Worlds"),
    CONTENT("Content"),
    SYSTEM("Systems")
}

enum class DebugScenarioDestination {
    HUB,
    EXPLORATION
}

data class DebugScenario(
    val id: String,
    val title: String,
    val description: String,
    val category: DebugScenarioCategory,
    val destination: DebugScenarioDestination,
    val worldLabel: String = "World 1: The Mines"
)

object DebugScenarioCatalog {
    val scenarios: List<DebugScenario> = listOf(
        scenario("story_w1_start", "Wake Up Call", "Start World 1 from Nova's bunk with normal starting gear.", DebugScenarioCategory.STORY),
        scenario("first_combat", "The Echo: First Combat", "Begin the Deep Mine descent at the first authored combat checkpoint.", DebugScenarioCategory.STORY),
        scenario("deep_mine", "The Echo: Deep Mine", "Resume MQ03 after the early mine encounters and guard-break training.", DebugScenarioCategory.STORY),
        scenario("red_alert", "Red Alert", "Resume MQ04 during the escape from the Logistics lockdown.", DebugScenarioCategory.STORY),
        scenario("launch", "The Launch", "Resume MQ05 in the launch bay after the Warden fight.", DebugScenarioCategory.STORY),
        scenario("w2_crash_start", "W2 / Crash Start", "Start A Strange Coast at the Sector 9 crash site.", DebugScenarioCategory.STORY),
        scenario("w2_temple_gate", "W2 / Temple Gate", "Resume The Signal at the Temple Gate after clearing the canopy route.", DebugScenarioCategory.STORY),
        scenario("w2_stasis_chamber", "W2 / Stasis Chamber", "Resume Sleeping Giant at Orion's stasis chamber.", DebugScenarioCategory.STORY),
        scenario("w2_hunter_canopy", "W2 / Hunter Canopy", "Resume The Hunter at Canopy Ridge with Orion recruited.", DebugScenarioCategory.STORY),
        scenario("w2_source_gate", "W2 / Source Gate", "Resume Liftoff at the Source Gate before bypassing the acoustic lock.", DebugScenarioCategory.STORY),
        scenario("w2_astra_repair", "W2 / Astra Repair", "Resume Liftoff in the hangar after recruiting the full crew.", DebugScenarioCategory.STORY),
        scenario("w3_sewers_entry", "W3 / Sewers Entry", "Start Homecoming beneath the Spire.", DebugScenarioCategory.STORY),
        scenario("w3_safehouse_plan", "W3 / Safehouse Plan", "Resume The Plan from Zeke's safehouse.", DebugScenarioCategory.STORY),
        scenario("w3_checkpoint_infiltration", "W3 / Checkpoint Infiltration", "Resume Social Engineering at the Upper City service route.", DebugScenarioCategory.STORY),
        scenario("w3_lens_archive", "W3 / Lens Archive", "Resume The Lens inside the Archive approach.", DebugScenarioCategory.STORY),
        scenario("w3_lockdown_escape", "W3 / Lockdown Escape", "Resume Burn Notice after the Lens alarm.", DebugScenarioCategory.STORY),
        scenario("w4_foundry_start", "W4 / Foundry Start", "Start Into the Fire on the Obsidian Shelf after the Astra slips the Shield.", DebugScenarioCategory.STORY),
        scenario("w4_phantom_records", "W4 / Phantom Records", "Resume Ghost in the Machine at the Waste Intake records terminal.", DebugScenarioCategory.STORY),
        scenario("w4_assembly_floor", "W4 / Assembly Floor", "Resume The Assembly on the Foundry conveyor floor.", DebugScenarioCategory.STORY),
        scenario("w4_anvil_forge", "W4 / Anvil Forge", "Resume The Anvil inside the Forge.", DebugScenarioCategory.STORY),
        scenario("w4_meltdown_escape", "W4 / Meltdown Escape", "Resume Meltdown after the Titan Walker fight with the core ready to steal.", DebugScenarioCategory.STORY),
        scenario("w5_docking_procedure", "W5 / Docking Procedure", "Start Docking Procedure at the Executive Dock.", DebugScenarioCategory.STORY),
        scenario("w5_zero_g", "W5 / Zero G", "Resume Zero G after boarding the Orbital Ring.", DebugScenarioCategory.STORY),
        scenario("w5_core_firewall", "W5 / Core Firewall", "Resume The Core Approach inside the Server Farm.", DebugScenarioCategory.STORY),
        scenario("w5_anchor_chamber", "W5 / Anchor Chamber", "Resume The Anchor at Elara's chamber.", DebugScenarioCategory.STORY),
        scenario("w5_critical_mass", "W5 / Critical Mass", "Resume Critical Mass in Vale's Throne Room.", DebugScenarioCategory.STORY),
        scenario("w6_fractured_minds", "W6 / Fractured Minds", "Start World 6 at the Source Campfire with the crew trapped in nightmares.", DebugScenarioCategory.STORY),
        scenario("w6_echo_mines", "W6 / Echo Mines", "Resume The Echo of the Mines after reassembling the crew.", DebugScenarioCategory.STORY),
        scenario("w6_crossing", "W6 / The Crossing", "Resume The Crossing at the Memory Bridge with the Key recovered.", DebugScenarioCategory.STORY),
        scenario("w6_spire", "W6 / Spire Ascent", "Resume The Spire of Thought at the Memory Stair.", DebugScenarioCategory.STORY),
        scenario("w6_finale", "W6 / Finale", "Resume The Final Note at the Center before confronting Vale.", DebugScenarioCategory.STORY),

        scenario("fun_w3_corporate_espionage", "Fun Audit / Corporate Espionage", "Play the complete Upper City ledger side quest.", DebugScenarioCategory.CONTENT),
        scenario("fun_w4_quality_control", "Fun Audit / Quality Control", "Play the complete rejected-unit side quest.", DebugScenarioCategory.CONTENT),
        scenario("fun_w5_ghost_shell", "Fun Audit / Ghost in the Shell", "Play the complete purged-backup side quest.", DebugScenarioCategory.CONTENT),
        scenario("fun_w6_hr_record", "Fun Audit / The HR Record", "Play the complete Infinite Cubicle side quest.", DebugScenarioCategory.CONTENT),

        hub("hub_1_homestead", "World 1 / Homestead Quarter", "The Mines", "Orient at the first World 1 hub."),
        hub("hub_2_logistics", "World 1 / Logistics Sector", "The Mines", "Orient at the second World 1 hub."),
        hub("hub_3_sector9", "World 2 / Jungle Ruins", "Sector 9", "Start the first main quest in Sector 9."),
        hub("hub_4_facility", "World 2 / Sector 9 Ruins", "Sector 9", "Orient at the ruined facility hub."),
        hub("hub_5_lower_city", "World 3 / Lower City", "The Spire", "Start the first main quest in the Spire."),
        hub("hub_6_upper_city", "World 3 / Upper City", "The Spire", "Orient at the Upper City hub."),
        hub("hub_7_slag_pits", "World 4 / Slag Pits", "The Foundry", "Start the first main quest in the Foundry."),
        hub("hub_8_assembly_line", "World 4 / Assembly Line", "The Foundry", "Orient at the Assembly Line hub."),
        hub("hub_9_orbital_ring", "World 5 / Orbital Ring", "The Void", "Start the first main quest on the Orbital Ring."),
        hub("hub_10_deep_ring", "World 5 / Deep Ring", "The Void", "Orient at the Deep Ring hub."),
        hub("hub_11_event_horizon", "World 6 / Event Horizon", "The Source", "Start the first main quest beyond the Tear."),
        hub("hub_12_singularity", "World 6 / Singularity", "The Source", "Orient at the final hub."),

        hubScenario("node_progression_w1", "Node Progression / World 1", "The Mines", "Open Homestead with normal discovery state and the Transit Checkpoint visibly locked."),
        hubScenario("node_progression_w2", "Node Progression / World 2", "Sector 9", "Open Jungle Ruins with only the Crash Site discovered; reveal other nodes through exploration."),
        hubScenario("astra_access", "Astra Access / Regional Hub", "The Spire", "Open the Lower City after repairing the Astra and test entering the ship from a regional hub."),
        hubScenario("astra_home", "Astra Home Base", "The Astra", "Start aboard the Astra with crew conversations, rest, disembark, and staged room locks available."),

        scenario("scavenger", "Scavenger's Stash", "Test the Trade Row side-quest entry state.", DebugScenarioCategory.CONTENT),
        scenario("heavy_lifting", "Heavy Lifting", "Test the required Workshop shield-training sequence.", DebugScenarioCategory.CONTENT),
        scenario("checkpoint", "Transit Checkpoint", "Test the guarded checkpoint sequence.", DebugScenarioCategory.CONTENT),
        scenario("room_items", "Room Items", "Open the Med-Bay storage room with a full inventory.", DebugScenarioCategory.CONTENT),
        scenario("lift_shaft", "Lift Shaft", "Enter the Pit shaft directly for traversal testing.", DebugScenarioCategory.CONTENT),

        scenario("full_inventory", "Full Inventory", "Start at the beginning with all gear, skills, party members, and credits.", DebugScenarioCategory.SYSTEM),
        scenario("weather_lab", "Weather Lab", "Open the dedicated weather-effects test room.", DebugScenarioCategory.SYSTEM),
        scenario("enemy_party", "Enemy Party Combat", "Test the launch-checkpoint enemy party encounter.", DebugScenarioCategory.SYSTEM),
        scenario("dynamic_patrol", "Dynamic Patrol", "Test a live Deep Mine patrol route.", DebugScenarioCategory.SYSTEM),
        scenario("party_sizes", "Enemy Party Sizes", "Compare supported enemy party layouts.", DebugScenarioCategory.SYSTEM),
        scenario("presence_stress", "Presence Stress", "Stress-test room NPC and entity presence.", DebugScenarioCategory.SYSTEM),
        scenario("hub_qa_w1_rest", "Hub QA / W1 Rest", "Open Nova's bunk for rest/cook hub testing.", DebugScenarioCategory.SYSTEM),
        scenario("hub_qa_w2_cookfire", "Hub QA / W2 Cookfire", "Open the Sector 9 falls cookfire for rest/cook hub testing.", DebugScenarioCategory.SYSTEM),
        scenario("hub_qa_w3_tuning", "Hub QA / W3 Tuning", "Open the Prism Gallery tuning puzzle.", DebugScenarioCategory.SYSTEM),
        scenario("hub_qa_w4_tuning", "Hub QA / W4 Forge", "Open the Foundry Forge environmental puzzle slice.", DebugScenarioCategory.SYSTEM),
        scenario("hub_qa_w6_source", "Hub QA / W6 Source", "Open the Source campfire with story and rest actions separated.", DebugScenarioCategory.SYSTEM)
    )

    fun find(id: String): DebugScenario? = scenarios.firstOrNull { it.id == id }

    private fun scenario(
        id: String,
        title: String,
        description: String,
        category: DebugScenarioCategory,
        destination: DebugScenarioDestination = DebugScenarioDestination.EXPLORATION
    ) = DebugScenario(id, title, description, category, destination)

    private fun hub(id: String, title: String, world: String, description: String) = DebugScenario(
        id = id,
        title = title,
        description = description,
        category = DebugScenarioCategory.WORLD,
        destination = DebugScenarioDestination.HUB,
        worldLabel = title.substringBefore(" /") + ": $world"
    )

    private fun hubScenario(id: String, title: String, world: String, description: String) = DebugScenario(
        id = id,
        title = title,
        description = description,
        category = DebugScenarioCategory.SYSTEM,
        destination = DebugScenarioDestination.HUB,
        worldLabel = world
    )
}
