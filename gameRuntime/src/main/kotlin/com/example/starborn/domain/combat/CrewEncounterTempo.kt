package com.example.starborn.domain.combat

import com.example.starborn.domain.leveling.CrewBuildPrototype
import com.example.starborn.domain.model.Enemy

/** Base Speed only; Agility still contributes through the shared formula and keeps its other effects. */
data class CrewEncounterTempo(
    val orbitalFighter: Int = 8,
    val sourceShadow: Int = 8,
    val distortedSentinel: Int = 8,
    val glitchHound: Int = 10
) {
    init { require(listOf(orbitalFighter, sourceShadow, distortedSentinel, glitchHound).all { it >= 0 }) }

    fun apply(enemy: Enemy, roomId: String?, prototypeVersion: Int): Enemy {
        if (prototypeVersion != CrewBuildPrototype.VERSION) return enemy
        val speed = when (roomId) {
            "orbital_executive_dock" -> orbitalFighter.takeIf { enemy.id == "orbital_fighter" }
            "source_memory_fragments" -> when (enemy.id) {
                "source_shadow" -> sourceShadow
                "distorted_sentinel" -> distortedSentinel
                "glitch_hound" -> glitchHound
                else -> null
            }
            else -> null
        } ?: return enemy
        return enemy.copy(speed = speed)
    }
}
