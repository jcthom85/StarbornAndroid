package com.example.starborn.navigation

import com.example.starborn.domain.leveling.LevelUpSummary
import java.io.Serializable

data class CombatResultPayload(
    val outcome: Outcome,
    val enemyIds: List<String> = emptyList(),
    val rewardXp: Int = 0,
    val rewardAp: Int = 0,
    val rewardCredits: Int = 0,
    val rewardItems: Map<String, Int> = emptyMap(),
    val levelUps: List<LevelUpSummary> = emptyList(),
    val isPlaceholder: Boolean = false
) : Serializable {
    enum class Outcome : Serializable {
        VICTORY,
        DEFEAT,
        RETREAT
    }

    companion object {
        val EMPTY = CombatResultPayload(
            outcome = Outcome.VICTORY,
            enemyIds = emptyList(),
            rewardXp = 0,
            rewardAp = 0,
            rewardCredits = 0,
            rewardItems = emptyMap(),
            levelUps = emptyList(),
            isPlaceholder = true
        )
    }
}
