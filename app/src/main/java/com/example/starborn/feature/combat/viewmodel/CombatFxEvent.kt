package com.example.starborn.feature.combat.viewmodel

sealed interface CombatFxEvent {
    data class Impact(
        val sourceId: String,
        val targetId: String,
        val amount: Int,
        val element: String? = null,
        val critical: Boolean = false
    ) : CombatFxEvent

    data class Heal(
        val sourceId: String,
        val targetId: String,
        val amount: Int
    ) : CombatFxEvent

    data class StatusApplied(
        val targetId: String,
        val statusId: String,
        val stacks: Int
    ) : CombatFxEvent

    data class Knockout(
        val targetId: String
    ) : CombatFxEvent

    data class TurnQueued(
        val actorId: String
    ) : CombatFxEvent

    data class CombatOutcomeFx(
        val outcome: OutcomeType
    ) : CombatFxEvent {
        enum class OutcomeType {
            VICTORY,
            DEFEAT,
            RETREAT
        }
    }

    data class SupportCue(
        val actorId: String,
        val skillName: String,
        val targetIds: List<String>
    ) : CombatFxEvent

    data class Audio(
        val commands: List<com.example.starborn.domain.audio.AudioCommand>
    ) : CombatFxEvent
}
