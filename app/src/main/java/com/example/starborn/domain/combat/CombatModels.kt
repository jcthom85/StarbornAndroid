package com.example.starborn.domain.combat

import com.example.starborn.domain.model.BuffEffect

data class Combatant(
    val id: String,
    val name: String,
    val side: CombatSide,
    val stats: StatBlock,
    val resistances: ResistanceProfile = ResistanceProfile(),
    val skills: List<String> = emptyList(),
    val initiativeModifier: Int = 0,
    val weapon: CombatWeapon? = null
)

enum class CombatSide {
    PLAYER,
    ALLY,
    ENEMY
}

data class StatBlock(
    val maxHp: Int,
    val strength: Int,
    val vitality: Int,
    val agility: Int,
    val focus: Int,
    val luck: Int,
    val speed: Int,
    val stability: Int = 100,
    val accuracyBonus: Double = 0.0,
    val evasionBonus: Double = 0.0,
    val critBonus: Double = 0.0,
    val flatDamageReduction: Int = 0
)

data class ResistanceProfile(
    val burn: Int = 0,
    val freeze: Int = 0,
    val shock: Int = 0,
    val acid: Int = 0,
    val source: Int = 0,
    val physical: Int = 0
)

data class CombatantState(
    val combatant: Combatant,
    val hp: Int,
    val stability: Int,
    val buffs: List<ActiveBuff> = emptyList(),
    val statusEffects: List<StatusEffect> = emptyList(),
    val elementStacks: Map<String, Int> = emptyMap(),
    val weaponCharge: WeaponChargeState? = null
) {
    val isAlive: Boolean get() = hp > 0
}

data class ActiveBuff(
    val effect: BuffEffect,
    val remainingTurns: Int
)

data class StatusEffect(
    val id: String,
    val remainingTurns: Int,
    val stacks: Int = 1
)

data class TurnSlot(
    val combatantId: String,
    val initiative: Int
)

data class CombatState(
    val turnOrder: List<TurnSlot>,
    val activeTurnIndex: Int,
    val combatants: Map<String, CombatantState>,
    val round: Int = 1,
    val log: List<CombatLogEntry> = emptyList(),
    val outcome: CombatOutcome? = null
) {
    val activeCombatant: CombatantState?
        get() = turnOrder.getOrNull(activeTurnIndex)?.combatantId?.let(combatants::get)
}

sealed interface CombatOutcome {
    data class Victory(val rewards: CombatReward) : CombatOutcome
    data class Defeat(val cause: String? = null) : CombatOutcome
    data object Retreat : CombatOutcome
}

data class CombatReward(
    val xp: Int = 0,
    val ap: Int = 0,
    val credits: Int = 0,
    val drops: List<LootDrop> = emptyList()
)

data class LootDrop(
    val itemId: String,
    val quantity: Int
)

sealed interface CombatAction {
    val actorId: String

    data class BasicAttack(
        override val actorId: String,
        val targetId: String
    ) : CombatAction

    data class SupportAbility(
        override val actorId: String,
        val targetId: String
    ) : CombatAction

    data class SkillUse(
        override val actorId: String,
        val skillId: String,
        val targetIds: List<String>
    ) : CombatAction

    data class ItemUse(
        override val actorId: String,
        val itemId: String,
        val targetId: String? = null
    ) : CombatAction

    data class SnackUse(
        override val actorId: String,
        val snackItemId: String,
        val targetId: String? = null
    ) : CombatAction

    data class Defend(override val actorId: String) : CombatAction
    data class Flee(override val actorId: String) : CombatAction
}

sealed interface CombatLogEntry {
    val turn: Int

    data class Damage(
        override val turn: Int,
        val sourceId: String,
        val targetId: String,
        val amount: Int,
        val element: String? = null,
        val critical: Boolean = false,
        val isWeakness: Boolean = false
    ) : CombatLogEntry

    data class Heal(
        override val turn: Int,
        val sourceId: String,
        val targetId: String,
        val amount: Int
    ) : CombatLogEntry

    data class StatusApplied(
        override val turn: Int,
        val targetId: String,
        val statusId: String,
        val stacks: Int
    ) : CombatLogEntry

    data class ElementStack(
        override val turn: Int,
        val targetId: String,
        val element: String,
        val stacks: Int
    ) : CombatLogEntry

    data class ElementBurst(
        override val turn: Int,
        val targetId: String,
        val element: String
    ) : CombatLogEntry

    data class StatusExpired(
        override val turn: Int,
        val targetId: String,
        val statusId: String
    ) : CombatLogEntry

    data class ActionQueued(
        override val turn: Int,
        val actorId: String,
        val action: CombatAction
    ) : CombatLogEntry

    data class Outcome(
        override val turn: Int,
        val result: CombatOutcome
    ) : CombatLogEntry

    data class TurnSkipped(
        override val turn: Int,
        val actorId: String,
        val reason: String
    ) : CombatLogEntry
}
