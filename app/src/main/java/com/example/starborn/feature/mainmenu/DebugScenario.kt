package com.example.starborn.feature.mainmenu

enum class DebugScenarioCategory(val label: String) {
    BURGFEST("BurgQuest"),
    STORY("Story"),
    TUTORIAL("Tutorials"),
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
    val worldLabel: String = "World 1: The Mines",
    val procedure: com.example.starborn.debug.DebugTestProcedure? = null
)

object DebugScenarioCatalog {
    val burgfestScenarios: List<DebugScenario> = listOf(
        scenario("burgfest_story", "BURGFEST: 1. Story Opening (The Awakening)", "Nova wakes in the bunk with exploration, dialogue, and first combat.", DebugScenarioCategory.BURGFEST, worldLabel = "World 1: The Mines"),
        scenario("burgfest_combat", "BURGFEST: 2. Tactical Combat (Canopy Skirmish)", "Full party combat in Sector 9 showcasing party switching, guard breaks, snacks, and Source Arts.", DebugScenarioCategory.BURGFEST, worldLabel = "World 2: Sector 9"),
        scenario("burgfest_astra", "BURGFEST: 3. The Astra Flagship (Hub, Crafting & Arcade)", "Explore the Astra ship base with crew dialogue, tinkering workshop, and playable Arcade Cabinet.", DebugScenarioCategory.BURGFEST, worldLabel = "The Astra"),
        scenario("burgfest_boss", "BURGFEST: 4. Boss Encounter (Titan Walker)", "High-stakes boss battle against the Titan Walker at the Foundry.", DebugScenarioCategory.BURGFEST, worldLabel = "World 4: The Foundry")
    )

    val scenarios: List<DebugScenario> = burgfestScenarios + listOf(
        // --- TUTORIAL SCENARIOS ---
        scenario("tut_npc_dialogue", "Tutorial: NPC & Dialogue", "Start in Jed's bunk with the NPC interaction and dialogue prompt.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 1: The Mines"),
        scenario("tut_gear_inventory", "Tutorial: Gear & Inventory", "Start in the workshop yard with unlocked, unequipped starter gear and inventory tutorial.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 1: The Mines"),
        scenario("tut_save_system", "Tutorial: Save System", "Start in Workshop Yard with Shift Clearance active to test the save prompt.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 1: The Mines"),
        scenario("tut_journal_quests", "Tutorial: Journal & Quests", "Start in Market Plaza with active errands to test journal tracking.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 1: The Mines"),
        scenario("tut_combat_snacks", "Tutorial: Combat Snacks", "Enter combat with snack tutorial prompt and Starbar Crunch snack slot ready.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 1: The Mines"),
        scenario("tut_party_combat", "Tutorial: Party Switching", "Enter combat with Nova & Zeke to test character switching and turn rotation.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 2: Sector 9"),
        scenario("tut_world2_debuffs", "Tutorial: Status Debuffs", "Enter Sector 9 combat with status debuff tutorial prompt active.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 2: Sector 9"),
        scenario("tut_source_blast_wave", "Tutorial: Source Art - Blast Wave", "Enter combat with Blast Wave unlocked to test AOE guard break and stun.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 1: The Mines"),
        scenario("tut_source_link", "Tutorial: Source Art - Link", "Enter combat with Link unlocked to test party regen and bridge tutorial.", DebugScenarioCategory.TUTORIAL, worldLabel = "World 2: Sector 9"),
        scenario("tut_rest_recovery", "Tutorial: Rest & Recovery", "Start aboard the Astra with damaged party to test rest recovery.", DebugScenarioCategory.TUTORIAL, worldLabel = "The Astra"),

        // --- STORY SCENARIOS ---
        // World 1
        scenario("story_w1_start", "Wake Up Call", "Start World 1 from Nova's bunk with normal starting gear.", DebugScenarioCategory.STORY, worldLabel = "World 1: The Mines"),
        scenario("tinkering_tutorial", "Wake Up Call: Jed's Bench", "Start in Jed's Workshop with Broken Cryo-Inductor, Scrap Metal, and Flux Liner ready to tinker.", DebugScenarioCategory.STORY, worldLabel = "World 1: The Mines"),
        scenario("first_combat", "The Echo: First Combat", "Begin the Deep Mine descent at the first authored combat checkpoint vs Faulted Loader.", DebugScenarioCategory.STORY, worldLabel = "World 1: The Mines"),
        scenario("deep_mine", "The Echo: Deep Mine", "Resume MQ03 after early mine encounters with Shield Bulwark guard-break training.", DebugScenarioCategory.STORY, worldLabel = "World 1: The Mines"),
        scenario("red_alert", "Red Alert", "Resume MQ04 during the escape from the Logistics lockdown.", DebugScenarioCategory.STORY, worldLabel = "World 1: The Mines"),
        scenario("launch", "The Launch", "Resume MQ05 in the launch bay after the Warden fight.", DebugScenarioCategory.STORY, worldLabel = "World 1: The Mines"),

        // World 2
        scenario("w2_crash_start", "W2 / Crash Start", "Start A Strange Coast at the Sector 9 crash site.", DebugScenarioCategory.STORY, worldLabel = "World 2: Sector 9"),
        scenario("w2_temple_gate", "W2 / Temple Gate", "Resume The Signal at the Temple Gate after clearing the canopy route.", DebugScenarioCategory.STORY, worldLabel = "World 2: Sector 9"),
        scenario("w2_stasis_chamber", "W2 / Stasis Chamber", "Resume Sleeping Giant at Orion's stasis chamber.", DebugScenarioCategory.STORY, worldLabel = "World 2: Sector 9"),
        scenario("w2_hunter_canopy", "W2 / Hunter Canopy", "Resume The Hunter at Canopy Ridge with Orion recruited.", DebugScenarioCategory.STORY, worldLabel = "World 2: Sector 9"),
        scenario("w2_source_gate", "W2 / Source Gate", "Resume Liftoff at the Source Gate before bypassing the acoustic lock.", DebugScenarioCategory.STORY, worldLabel = "World 2: Sector 9"),
        scenario("w2_astra_repair", "W2 / Astra Repair", "Resume Liftoff in the hangar after recruiting the full crew.", DebugScenarioCategory.STORY, worldLabel = "World 2: Sector 9"),

        // World 3
        scenario("w3_sewers_entry", "W3 / Sewers Entry", "Start Homecoming beneath the Spire at the runoff locks.", DebugScenarioCategory.STORY, worldLabel = "World 3: The Spire"),
        scenario("w3_safehouse_plan", "W3 / Safehouse Plan", "Resume The Plan from Zeke's safehouse.", DebugScenarioCategory.STORY, worldLabel = "World 3: The Spire"),
        scenario("w3_checkpoint_infiltration", "W3 / Checkpoint Infiltration", "Resume Social Engineering at the Upper City service route.", DebugScenarioCategory.STORY, worldLabel = "World 3: The Spire"),
        scenario("w3_lens_archive", "W3 / Lens Archive", "Resume The Lens inside the Archive approach.", DebugScenarioCategory.STORY, worldLabel = "World 3: The Spire"),
        scenario("w3_lockdown_escape", "W3 / Lockdown Escape", "Resume Burn Notice after the Lens alarm.", DebugScenarioCategory.STORY, worldLabel = "World 3: The Spire"),

        // World 4
        scenario("w4_foundry_start", "W4 / Foundry Start", "Start Into the Fire on the Obsidian Shelf after the Astra slips the Shield.", DebugScenarioCategory.STORY, worldLabel = "World 4: The Foundry"),
        scenario("w4_phantom_records", "W4 / Phantom Records", "Resume Ghost in the Machine at the Waste Intake records terminal.", DebugScenarioCategory.STORY, worldLabel = "World 4: The Foundry"),
        scenario("w4_assembly_floor", "W4 / Assembly Floor", "Resume The Assembly on the Foundry conveyor floor.", DebugScenarioCategory.STORY, worldLabel = "World 4: The Foundry"),
        scenario("w4_anvil_forge", "W4 / Anvil Forge", "Resume The Anvil inside the Forge.", DebugScenarioCategory.STORY, worldLabel = "World 4: The Foundry"),
        scenario("w4_meltdown_escape", "W4 / Meltdown Escape", "Resume Meltdown after the Titan Walker fight with the core ready to steal.", DebugScenarioCategory.STORY, worldLabel = "World 4: The Foundry"),

        // World 5
        scenario("w5_docking_procedure", "W5 / Docking Procedure", "Start Docking Procedure at the Executive Dock.", DebugScenarioCategory.STORY, worldLabel = "World 5: The Void"),
        scenario("w5_zero_g", "W5 / Zero G", "Resume Zero G after boarding the Orbital Ring.", DebugScenarioCategory.STORY, worldLabel = "World 5: The Void"),
        scenario("w5_core_firewall", "W5 / Core Firewall", "Resume The Core Approach inside the Server Farm.", DebugScenarioCategory.STORY, worldLabel = "World 5: The Void"),
        scenario("w5_anchor_chamber", "W5 / Anchor Chamber", "Resume The Anchor at Elara's chamber.", DebugScenarioCategory.STORY, worldLabel = "World 5: The Void"),
        scenario("w5_critical_mass", "W5 / Critical Mass", "Resume Critical Mass in Vale's Throne Room.", DebugScenarioCategory.STORY, worldLabel = "World 5: The Void"),

        // World 6
        scenario("w6_fractured_minds", "W6 / Fractured Minds", "Start World 6 at the Source Campfire with the crew trapped in nightmares.", DebugScenarioCategory.STORY, worldLabel = "World 6: The Source"),
        scenario("w6_echo_mines", "W6 / Echo Mines", "Resume The Echo of the Mines after reassembling the crew.", DebugScenarioCategory.STORY, worldLabel = "World 6: The Source"),
        scenario("w6_crossing", "W6 / The Crossing", "Resume The Crossing at the Memory Bridge with the Key recovered.", DebugScenarioCategory.STORY, worldLabel = "World 6: The Source"),
        scenario("w6_spire", "W6 / Spire Ascent", "Resume The Spire of Thought at the Memory Stair.", DebugScenarioCategory.STORY, worldLabel = "World 6: The Source"),
        scenario("w6_finale", "W6 / Finale", "Resume The Final Note at the Center before confronting Vale.", DebugScenarioCategory.STORY, worldLabel = "World 6: The Source"),

        // --- BOSS FIGHTS ---
        scenario("boss_warden", "Boss: The Iron Warden", "Load Pod Bay before the authored Warden encounter.", DebugScenarioCategory.STORY, worldLabel = "World 1: The Mines"),
        scenario("boss_hunter", "Boss: The Hunter Beast", "Load Canopy Ridge before Confront stalker and Face the Beast.", DebugScenarioCategory.STORY, worldLabel = "World 2: Sector 9"),
        scenario("boss_titan_walker", "Boss: Titan Walker", "Load Titan Dock before confronting Rylos and the Titan Walker.", DebugScenarioCategory.STORY, worldLabel = "World 4: The Foundry"),
        scenario("boss_ascended_vale", "Boss: Ascended Vale", "Confront Ascended Vale at the Center of the Source.", DebugScenarioCategory.STORY, worldLabel = "World 6: The Source"),

        // --- CONTENT & AUDIT SCENARIOS ---
        scenario("fun_w3_corporate_espionage", "Fun Audit / Corporate Espionage", "Play the complete Upper City ledger side quest.", DebugScenarioCategory.CONTENT, worldLabel = "World 3: The Spire"),
        scenario("fun_w4_quality_control", "Fun Audit / Quality Control", "Play the complete rejected-unit side quest.", DebugScenarioCategory.CONTENT, worldLabel = "World 4: The Foundry"),
        scenario("fun_w5_ghost_shell", "Fun Audit / Ghost in the Shell", "Play the complete purged-backup side quest.", DebugScenarioCategory.CONTENT, worldLabel = "World 5: The Void"),
        scenario("fun_w6_hr_record", "Fun Audit / The HR Record", "Play the complete Infinite Cubicle side quest.", DebugScenarioCategory.CONTENT, worldLabel = "World 6: The Source"),
        scenario("scavenger", "Scavenger's Stash", "Test the Trade Row side-quest entry state.", DebugScenarioCategory.CONTENT, worldLabel = "World 1: The Mines"),
        scenario("heavy_lifting", "Heavy Lifting", "Test the required Workshop shield-training sequence.", DebugScenarioCategory.CONTENT, worldLabel = "World 1: The Mines"),
        scenario("checkpoint", "Transit Checkpoint", "Test the guarded checkpoint sequence.", DebugScenarioCategory.CONTENT, worldLabel = "World 1: The Mines"),
        scenario("hub2_overview", "Hub 2 Overview", "Open the Logistics Sector map with every node revealed.", DebugScenarioCategory.CONTENT, worldLabel = "World 1: The Mines"),

        // --- HUB NAVIGATION SCENARIOS ---
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

        // --- PROGRESSION & ASTRA BASE SCENARIOS ---
        hubScenario("node_progression_w1", "Node Progression / World 1", "World 1: The Mines", "Open Homestead with normal discovery state and the Transit Checkpoint visibly locked."),
        hubScenario("node_progression_w2", "Node Progression / World 2", "World 2: Sector 9", "Open Jungle Ruins with only the Crash Site discovered; reveal other nodes through exploration."),
        hubScenario("astra_access", "Astra Access / Regional Hub", "World 3: The Spire", "Open the Lower City after repairing the Astra and test entering the ship from a regional hub."),
        hubScenario("astra_home", "Astra Home Base", "The Astra", "Explore the connected ship, talk to the crew, rest, and disembark through the cargo ramp."),

        // --- SYSTEM & CRAFTING QA SCENARIOS ---
        scenario("full_inventory", "Full Inventory", "Start at the beginning with all gear, skills, party members, and credits.", DebugScenarioCategory.SYSTEM, worldLabel = "World 1: The Mines"),
        scenario("crafting_cooking_kitchen", "Cooking Kitchen / Cookfire", "Start at Sector 9 cookfire with full fish, beast meat, noodles, spices, and herbs ready to cook.", DebugScenarioCategory.SYSTEM, worldLabel = "World 2: Sector 9"),
        scenario("crafting_tinkering_advanced", "Tinkering / Advanced Workshop", "Start aboard the Astra with ingredients and tools for every tinkering recipe. Open Tinkering from the menu.", DebugScenarioCategory.SYSTEM, worldLabel = "The Astra"),
        scenario("fishing_beach_pools", "Fishing / Sector 9 Beach", "Open Sector 9 Beach tide pools with starter and tuned rods and lures for minigame testing.", DebugScenarioCategory.SYSTEM, worldLabel = "World 2: Sector 9"),
        scenario("fishing_spire_runoff", "Fishing / Spire Deep Runoff", "Open Spire Sewer Passage with high-tier rods and lures for advanced fish catching.", DebugScenarioCategory.SYSTEM, worldLabel = "World 3: The Spire"),
        scenario("qa_recipe_exact", "Progression QA / Exact Recipe", "Jed's Bench with exactly the Cryo-Inductor recipe requirements. No spare resources.", DebugScenarioCategory.SYSTEM),
        scenario("qa_recipe_short", "Progression QA / Missing Scrap", "Jed's Bench without recipe scrap. Verify crafting refuses without consuming other ingredients; test recovery routes.", DebugScenarioCategory.SYSTEM),
        scenario("qa_meal_reload", "Progression QA / Meal Save", "Sewer landing with injured crew and a three-encounter QA meal (+5 accuracy, +10 focus). Save, reload, then fight.", DebugScenarioCategory.SYSTEM, worldLabel = "World 3: The Spire"),
        scenario("qa_w2_gate_closed", "Progression QA / W2 Locked", "Canopy Walk without a Thermal Cutter. Check the east gate and return route.", DebugScenarioCategory.SYSTEM, worldLabel = "World 2: Sector 9"),
        scenario("qa_w2_gate_open", "Progression QA / W2 Unlocked", "Same Canopy Walk setup with a Thermal Cutter. Cross east, save/reload, and return west.", DebugScenarioCategory.SYSTEM, worldLabel = "World 2: Sector 9"),
        scenario("qa_w3_routes_before", "Progression QA / W3 Before Intel", "The Static before completing The Plan. Walk Transit Plaza, Underrail, and Sewers; check Upper City access.", DebugScenarioCategory.SYSTEM, worldLabel = "World 3: The Spire"),
        scenario("qa_w3_routes_after", "Progression QA / W3 After Intel", "The Static after The Plan. Walk the same routes toward Upper City and compare access.", DebugScenarioCategory.SYSTEM, worldLabel = "World 3: The Spire"),
        scenario("weather_lab", "Weather Lab", "Open the dedicated weather-effects test room.", DebugScenarioCategory.SYSTEM, worldLabel = "World 1: The Mines"),
        scenario("enemy_party", "Enemy Party Combat", "Test the launch-checkpoint enemy party encounter.", DebugScenarioCategory.SYSTEM, worldLabel = "World 1: The Mines"),
        scenario("dynamic_patrol", "Dynamic Patrol", "Test a live Deep Mine patrol route.", DebugScenarioCategory.SYSTEM, worldLabel = "World 1: The Mines"),
        scenario("arcade_deep_mine", "Arcade / Deep Mine", "Open the restored Deep Mine cabinet aboard the Astra for gameplay and reward testing.", DebugScenarioCategory.SYSTEM, worldLabel = "The Astra"),
        scenario("hub_qa_w1_rest", "Hub QA / W1 Rest", "Open Nova's bunk for rest/cook hub testing.", DebugScenarioCategory.SYSTEM, worldLabel = "World 1: The Mines"),
        scenario("hub_qa_w2_cookfire", "Hub QA / W2 Cookfire", "Open the Sector 9 falls cookfire for rest/cook hub testing.", DebugScenarioCategory.SYSTEM, worldLabel = "World 2: Sector 9"),
        scenario("hub_qa_w3_tuning", "Hub QA / W3 Tuning", "Open the Prism Gallery tuning puzzle.", DebugScenarioCategory.SYSTEM, worldLabel = "World 3: The Spire"),
        scenario("hub_qa_w4_tuning", "Hub QA / W4 Forge", "Open the Foundry Forge environmental puzzle slice.", DebugScenarioCategory.SYSTEM, worldLabel = "World 4: The Foundry"),
        scenario("hub_qa_w6_source", "Hub QA / W6 Source", "Open the Source campfire with story and rest actions separated.", DebugScenarioCategory.SYSTEM, worldLabel = "World 6: The Source")
    )

    fun find(id: String): DebugScenario? = com.example.starborn.debug.DebugTestRegistry.find(id)
        ?: scenarios.firstOrNull { it.id == id }

    private fun scenario(
        id: String,
        title: String,
        description: String,
        category: DebugScenarioCategory,
        destination: DebugScenarioDestination = DebugScenarioDestination.EXPLORATION,
        worldLabel: String = "World 1: The Mines"
    ) = DebugScenario(id, title, description, category, destination, worldLabel)

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
