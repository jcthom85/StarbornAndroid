package com.example.starborn.domain.combat

/** Translate historical victories only in the three rooms whose formations changed. */
object World2EncounterCompatibility {
    fun creditedIds(roomId: String, ids: List<String>): List<String> = when (roomId) {
        "sector9_stream_pools" -> ids.map { if (it == "siren_skimmer") "mist_glider" else it }
        "sector9_stream_cave_depths" -> ids.map { if (it == "echo_borer") "moss_borer" else it }
        "sector9_hall_acoustics_lab" ->
            if (ids == listOf("spore_spitter")) listOf("sentinel_orb", "spore_spitter") else ids
        else -> ids
    }
}
