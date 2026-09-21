package com.example.starborn.domain.session

import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.HubNode

data class AstraDestination(
    val title: String,
    val desc: String,
    val worldId: String,
    val hubId: String,
    val roomId: String,
    val nodeId: String,
    val unlockQuest: String? = null
)

/** Shared navigation rules for the bridge, boarding, and the cargo ramp. */
object AstraTravel {
    const val HUB_ID = "hub_astra"
    const val WORLD_ID = "world_astra"
    // Retain the original ID so existing saves still recognize the ship's node.
    const val NODE_ID = "astra_bridge_node"
    const val ENTRY_ROOM_ID = "astra_cargo_bay"

    val destinations = listOf(
        AstraDestination("World 1: The Mines", "Mining colony perimeter, workshops, and scrap shafts.", "world_1", "hub_1_homestead", "pit_L1_landing", "pit"),
        AstraDestination("World 2: Sector 9", "Coastal shallows, ancient ruins, and jungle canopies.", "world_2", "hub_3_sector9", "sector9_crash_site", "sector9_landing", "w1_mq05"),
        AstraDestination("World 3: The Spire", "City towers, monorail lines, and sewer routes.", "world_3", "hub_5_lower_city", "spire_sewers_landing", "spire_sewers", "w2_mq05"),
        AstraDestination("World 4: The Foundry", "Industrial smelters and magma conduits.", "world_4", "hub_7_slag_pits", "foundry_slag_landing", "foundry_obsidian_shelf", "w3_mq15"),
        AstraDestination("World 5: The Orbital Ring", "Executive facilities and orbital corridors.", "world_5", "hub_9_orbital_ring", "orbital_executive_dock", "orbital_executive_dock", "w4_mq20"),
        AstraDestination("World 6: The Source", "Waveform fire and memory bridges.", "world_6", "hub_11_event_horizon", "source_campfire", "source_campfire_node", "w5_mq25")
    )

    fun availableDestinations(state: GameSessionState): List<AstraDestination> {
        val furthest = destinations.indexOfLast { destination ->
            destination.unlockQuest == null || destination.unlockQuest in state.completedQuests ||
                "ms_${destination.unlockQuest}_complete" in state.completedMilestones
        }
        return destinations.take(furthest + 1)
    }

    /** Recover old/debug saves without a docking record; never return to the ship itself. */
    fun dockingLocation(state: GameSessionState, hubs: List<Hub>, nodes: List<HubNode>): AstraDestination {
        val savedRoom = state.astraReturnRoomId
        val owner = nodes.firstOrNull { it.hubId != HUB_ID && savedRoom in it.rooms }
        val hub = owner?.let { node -> hubs.firstOrNull { it.id == node.hubId } }
        if (owner != null && hub != null && savedRoom != null) {
            return AstraDestination(hub.title, "Return to the current docking location.", hub.worldId, hub.id, savedRoom, owner.id)
        }
        return availableDestinations(state).last()
    }
}
