package com.example.starborn.feature.exploration.presentation

import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionState

data class AstraFacilityEntry(val title: String, val body: String, val isNew: Boolean = false)
data class AstraFacilityUi(val title: String, val entries: List<AstraFacilityEntry>)

/** Read-only ship facilities. Check-ins use existing persistent room flags. */
fun astraFacility(id: String, state: GameSessionState, item: (String) -> Item?): AstraFacilityUi {
    fun done(quest: String) = quest in state.completedQuests || "ms_${quest}_complete" in state.completedMilestones
    val chapter = when {
        done("w5_mq25") -> 5
        done("w4_mq20") -> 4
        done("w3_mq15") -> 3
        done("w2_mq05") -> 2
        else -> 1
    }
    fun owned(predicate: (String, Item) -> Boolean) = state.inventory.entries.filter { it.value > 0 }
        .mapNotNull { (key, count) -> item(key)?.takeIf { predicate(key, it) }?.let { value ->
            AstraFacilityEntry("${value.name} x$count", value.description.orEmpty())
        } }.sortedBy { it.title }
    return when (id) {
        "cargo" -> AstraFacilityUi("Cargo Manifest", listOf(AstraFacilityEntry("Shared supplies",
            "Your shared supplies are listed below. Manage equipment and supplies through Items.")) +
            owned { _, _ -> true }.ifEmpty { listOf(AstraFacilityEntry("No cargo recorded", "Collected supplies will appear here.")) })
        "relics" -> AstraFacilityUi("Relic Array", listOf(AstraFacilityEntry("Shielded artifact archive",
            "Recovered artifacts are cataloged here. Source abilities are earned through the crew's story discoveries.")) +
            owned { key, value -> key == "tuning_fork" || key.startsWith("relic_") || value.rarity == "mythic" }
                .ifEmpty { listOf(AstraFacilityEntry("No recovered artifacts", "The shielded cradle awaits your discoveries.")) })
        "chime" -> {
            val notes = mutableListOf(AstraFacilityEntry("Signal archive", "Recovered signal notes, available for reference between voyages."))
            if ("ms_w2_chime_recovered" in state.completedMilestones || (state.inventory["ghost_signal_cell"] ?: 0) > 0)
                notes += AstraFacilityEntry("Orion's Chime", "The acoustic pins authenticate compatible launch telemetry and navigation arrays. The ship carries a history older than this crew's voyage.")
            if (done("w2_mq05")) notes += AstraFacilityEntry("A ship of our own", "The Astra is ready to carry the crew beyond Sector 9. Navigation now opens the route to the Spire.")
            if (done("w3_mq15")) notes += AstraFacilityEntry("Through the Shield", "The crew found its opening through Dominion control. The Foundry route is available.")
            if ("ms_w4_engine_stolen" in state.completedMilestones) notes += AstraFacilityEntry("Deep-Core engine", "The recovered engine changes what this old survey vessel can survive. The crew's work is written into the hull.")
            if ("ms_w5_elara_found" in state.completedMilestones) notes += AstraFacilityEntry("Elara", "The crew has reached Elara. Her part in this journey can no longer be treated as a distant signal.")
            if ("ms_w6_elara_voice_isolated" in state.completedMilestones) notes += AstraFacilityEntry("A voice of her own", "Elara's voice has been isolated from the commands surrounding it.")
            if (notes.size == 1) notes += AstraFacilityEntry("No recovered notes yet", "Recover signals during your travels to add notes here.")
            AstraFacilityUi("Chime Archive", notes)
        }
        else -> {
            val personal = id == "quarters"
            val key = "${if (personal) "personal" else "table"}_checkin_$chapter"
            val room = if (personal) "astra_quarters" else "astra_common_room"
            val fresh = state.roomStates[room]?.get(key) != true
            val nova = if (personal) when (chapter) {
                1 -> "Nova: I keep waiting for someone to tell us this ship isn't ours. Until then, I'm keeping the bed made."
                2 -> "Nova: We got everyone aboard. For tonight, that's enough."
                3 -> "Nova: I can still hear the Spire when the engines stop. I think I'll leave the corridor light on."
                4 -> "Nova: More power doesn't make this home. The people who keep coming back do."
                else -> "Nova: I used to count the exits. Now I count who's coming through the door."
            } else when (chapter) {
                1 -> "Nova: Set a place for whoever comes aboard next. We don't need permission to do that."
                2 -> "Nova: One ship, too many opinions, and a kettle that works. I'll take it."
                3 -> "Nova: We can argue about the next route after everyone eats."
                4 -> "Nova: The engine's louder. Nobody has to shout across the table, though."
                else -> "Nova: We made it this far together. Whatever comes next, that still counts."
            }
            val entries = mutableListOf(AstraFacilityEntry(if (personal) "Between missions" else "Around the table", nova, fresh))
            if ("zeke" in state.partyMembers) entries += AstraFacilityEntry("Zeke", if (chapter >= 4) "Zeke: The new engine has stopped making the noise I was worried about. It has started making three different noises. Progress." else if (personal)
                "Zeke: I've been labeling the loose cables. You'd be amazed how many were labeled 'probably fine'." else
                "Zeke: I fixed the kettle's switch. That is my entire contribution to dinner, and I expect it to be recognized.")
            if ("orion" in state.partyMembers) entries += AstraFacilityEntry("Orion", if (chapter >= 3) "Orion: After the Spire, I appreciate being able to open a door without asking what it costs." else if (personal)
                "Orion: A little quiet helps me hear which thoughts are actually mine." else
                "Orion: Leave the cups out. A ship should look as though someone intends to stay.")
            if ("gh0st" in state.partyMembers) entries += AstraFacilityEntry("Gh0st", if (chapter >= 5) "Gh0st: I am keeping a place for conversations we have not had yet. There is no deadline." else if (personal)
                "Gh0st: I do not require sleep. I am beginning to understand the value of a place where nothing is required." else
                "Gh0st: I have reserved a seat. This is not a tactical formation. I checked.")
            if (personal) entries += AstraFacilityEntry("Personal keepsakes",
                "Nova's handmade paper guardian carries Tyson's note: 'To Nova and the crew - Built by Tyson.' Ian's astronaut slate charts a mission to explore the stars and never leave the crew behind. Oliver's thermal cushion has a shift log approved with a paw print.")
            AstraFacilityUi(if (personal) "Crew Downtime" else "Crew Check-in", entries)
        }
    }
}
