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

        scenario("scavenger", "Scavenger's Stash", "Test the Trade Row side-quest entry state.", DebugScenarioCategory.CONTENT),
        scenario("heavy_lifting", "Heavy Lifting", "Test the Workshop training side quest.", DebugScenarioCategory.CONTENT),
        scenario("checkpoint", "Transit Checkpoint", "Test the guarded checkpoint sequence.", DebugScenarioCategory.CONTENT),
        scenario("room_items", "Room Items", "Open the Med-Bay storage room with a full inventory.", DebugScenarioCategory.CONTENT),
        scenario("lift_shaft", "Lift Shaft", "Enter the Pit shaft directly for traversal testing.", DebugScenarioCategory.CONTENT),

        scenario("full_inventory", "Full Inventory", "Start at the beginning with all gear, skills, party members, and credits.", DebugScenarioCategory.SYSTEM),
        scenario("weather_lab", "Weather Lab", "Open the dedicated weather-effects test room.", DebugScenarioCategory.SYSTEM),
        scenario("enemy_party", "Enemy Party Combat", "Test the launch-checkpoint enemy party encounter.", DebugScenarioCategory.SYSTEM),
        scenario("dynamic_patrol", "Dynamic Patrol", "Test a live Deep Mine patrol route.", DebugScenarioCategory.SYSTEM),
        scenario("party_sizes", "Enemy Party Sizes", "Compare supported enemy party layouts.", DebugScenarioCategory.SYSTEM),
        scenario("presence_stress", "Presence Stress", "Stress-test room NPC and entity presence.", DebugScenarioCategory.SYSTEM)
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
}
