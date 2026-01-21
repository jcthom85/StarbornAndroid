package com.example.starborn.domain.combat

import com.example.starborn.domain.model.BuffEffect
import kotlin.math.max
import kotlin.math.roundToInt

class CombatEngine(
    private val timelineCalculator: TurnOrderCalculator = DefaultTurnOrderCalculator(),
    private val statusRegistry: StatusRegistry = StatusRegistry()
) {

    fun beginEncounter(setup: CombatSetup): CombatState {
        val turnOrder = timelineCalculator.calculate(setup)
        val combatants = setup.allCombatants.associate { combatant ->
            combatant.id to CombatantState(
                combatant = combatant,
                hp = combatant.stats.maxHp,
                stability = combatant.stats.stability
            )
        }
        return CombatState(
            turnOrder = turnOrder,
            activeTurnIndex = 0,
            combatants = combatants
        )
    }

    fun queueAction(state: CombatState, action: CombatAction): CombatState {
        return state.copy(
            log = state.log + CombatLogEntry.ActionQueued(
                turn = state.round,
                actorId = action.actorId,
                action = action
            )
        )
    }

    fun advance(state: CombatState): CombatState {
        val nextIndex = (state.activeTurnIndex + 1).let { if (it >= state.turnOrder.size) 0 else it }
        val nextRound = if (nextIndex == 0) state.round + 1 else state.round
        return state.copy(
            activeTurnIndex = nextIndex,
            round = nextRound
        )
    }

    fun calculateDamage(
        attackerState: CombatantState,
        targetState: CombatantState,
        baseDamage: Int,
        element: String?
    ): Int {
        if (baseDamage <= 0) return 0
        val resistance = resolveResistance(targetState, element)
        val modifier = 1.0 - (resistance / 100.0)
        val scaled = baseDamage * modifier
        return when {
            scaled <= 0.0 -> 0
            else -> scaled.roundToInt().coerceAtLeast(1)
        }
    }

    fun applyDamage(
        state: CombatState,
        attackerId: String,
        targetId: String,
        amount: Int,
        element: String?,
        applyElementStacks: Boolean = true,
        critical: Boolean = false
    ): CombatState {
        val targetState = state.combatants[targetId] ?: return state
        val resistance = resolveResistance(targetState, element)
        val isWeakness = resistance < 0

        val clamped = if (amount < 0) 0 else amount
        val newHp = (targetState.hp - clamped).coerceAtLeast(0)

        val stabilityDamage = if (isWeakness) clamped * 2 else clamped
        var newStability = targetState.stability - stabilityDamage
        var staggerApplied = false

        if (newStability <= 0) {
            newStability = targetState.combatant.stats.stability
            staggerApplied = true
        }

        val updated = targetState.copy(hp = newHp, stability = newStability)
        val damageEntry = CombatLogEntry.Damage(
            turn = state.round,
            sourceId = attackerId,
            targetId = targetId,
            amount = clamped,
            element = element,
            critical = critical,
            isWeakness = isWeakness
        )
        val normalizedElement = ElementalStackRules.normalize(element)
        var working = state.copy(
            combatants = state.combatants + (targetId to updated),
            log = state.log + damageEntry
        )

        if (staggerApplied) {
            working = applyStatus(working, targetId, "stagger", duration = 1, stacks = 1)
        }

        val shouldStack = applyElementStacks && clamped > 0 && ElementalStackRules.isStackable(normalizedElement)
        return if (shouldStack && normalizedElement != null) {
            applyElementStack(
                state = working,
                attackerId = attackerId,
                targetId = targetId,
                element = normalizedElement
            )
        } else {
            working
        }
    }

    fun applyHeal(
        state: CombatState,
        sourceId: String,
        targetId: String,
        amount: Int
    ): CombatState {
        if (amount <= 0) return state
        val targetState = state.combatants[targetId] ?: return state
        val maxHp = targetState.combatant.stats.maxHp
        val newHp = (targetState.hp + amount).coerceAtMost(maxHp)
        val healed = newHp - targetState.hp
        if (healed <= 0) return state
        val updated = targetState.copy(hp = newHp)
        val logEntry = CombatLogEntry.Heal(
            turn = state.round,
            sourceId = sourceId,
            targetId = targetId,
            amount = healed
        )
        return state.copy(
            combatants = state.combatants + (targetId to updated),
            log = state.log + logEntry
        )
    }

    fun applyBuffs(
        state: CombatState,
        targetId: String,
        buffs: List<BuffEffect>,
        sourceId: String? = null
    ): CombatState {
        if (buffs.isEmpty()) return state
        val targetState = state.combatants[targetId] ?: return state
        val updatedBuffs = targetState.buffs.toMutableList()
        buffs.forEach { effect ->
            val duration = max(effect.duration ?: DEFAULT_BUFF_DURATION, 1)
            val newBuff = ActiveBuff(effect = effect, remainingTurns = duration)
            val index = updatedBuffs.indexOfFirst { it.effect.stat.equals(effect.stat, ignoreCase = true) }
            if (index >= 0) {
                val existing = updatedBuffs[index]
                val merged = existing.copy(
                    effect = effect,
                    remainingTurns = max(existing.remainingTurns, duration)
                )
                updatedBuffs[index] = merged
            } else {
                updatedBuffs.add(newBuff)
            }
        }
        val updatedState = state.copy(
            combatants = state.combatants + (targetId to targetState.copy(buffs = updatedBuffs))
        )
        val logEntries = buffs.map { effect ->
            CombatLogEntry.StatusApplied(
                turn = state.round,
                targetId = targetId,
                statusId = "buff_${effect.stat}",
                stacks = effect.value
            )
        }
        return updatedState.copy(log = updatedState.log + logEntries)
    }

    fun applyStatus(
        state: CombatState,
        targetId: String,
        statusId: String,
        duration: Int,
        stacks: Int = 1,
        sourceId: String? = null
    ): CombatState {
        val resolvedDuration = when {
            duration > 0 -> duration
            else -> statusRegistry.definition(statusId)?.defaultDuration ?: DEFAULT_STATUS_DURATION
        }
        if (resolvedDuration <= 0) return state
        val targetState = state.combatants[targetId] ?: return state
        val updatedStatuses = targetState.statusEffects.toMutableList()
        val index = updatedStatuses.indexOfFirst { it.id.equals(statusId, ignoreCase = true) }
        val newStatus = if (index >= 0) {
            val existing = updatedStatuses[index]
            existing.copy(
                remainingTurns = max(existing.remainingTurns, resolvedDuration),
                stacks = existing.stacks + stacks
            )
        } else {
            StatusEffect(
                id = statusId,
                remainingTurns = resolvedDuration,
                stacks = stacks.coerceAtLeast(1)
            )
        }
        if (index >= 0) {
            updatedStatuses[index] = newStatus
        } else {
            updatedStatuses.add(newStatus)
        }
        val updatedState = state.copy(
            combatants = state.combatants + (targetId to targetState.copy(statusEffects = updatedStatuses))
        )
        val logEntry = CombatLogEntry.StatusApplied(
            turn = state.round,
            targetId = targetId,
            statusId = statusId,
            stacks = newStatus.stacks
        )
        return updatedState.copy(log = updatedState.log + logEntry)
    }

    fun tickEndOfTurn(state: CombatState): CombatState {
        var working = state
        val expiredLogs = mutableListOf<CombatLogEntry>()
        val combatantIds = working.combatants.keys.toList()
        for (combatantId in combatantIds) {
            val snapshot = working.combatants[combatantId] ?: continue
            snapshot.statusEffects.forEach { status ->
                working = applyStatusTick(working, combatantId, status)
            }

            val current = working.combatants[combatantId] ?: continue

            val updatedStatuses = mutableListOf<StatusEffect>()
            current.statusEffects.forEach { status ->
                val definition = statusRegistry.definition(status.id)
                val ticked = definition?.tick
                val remaining = status.remainingTurns - 1
                if (remaining > 0) {
                    updatedStatuses.add(status.copy(remainingTurns = remaining))
                } else {
                    expiredLogs.add(
                        CombatLogEntry.StatusExpired(
                            turn = working.round,
                            targetId = combatantId,
                            statusId = status.id
                        )
                    )
                }
            }

            val updatedBuffs = mutableListOf<ActiveBuff>()
            current.buffs.forEach { buff ->
                val remaining = buff.remainingTurns - 1
                if (remaining > 0) {
                    updatedBuffs.add(buff.copy(remainingTurns = remaining))
                } else {
                    expiredLogs.add(
                        CombatLogEntry.StatusExpired(
                            turn = working.round,
                            targetId = combatantId,
                            statusId = "buff_${buff.effect.stat}"
                        )
                    )
                }
            }

            val replacement = current.copy(
                statusEffects = updatedStatuses,
                buffs = updatedBuffs
            )
            working = working.copy(
                combatants = working.combatants + (combatantId to replacement)
            )
        }

        return if (expiredLogs.isEmpty()) working else working.copy(log = working.log + expiredLogs)
    }

    private fun applyStatusTick(
        state: CombatState,
        combatantId: String,
        status: StatusEffect
    ): CombatState {
        val current = state.combatants[combatantId] ?: return state
        val definition = statusRegistry.definition(status.id) ?: return state
        val tick = definition.tick ?: return state
        val maxHp = current.combatant.stats.maxHp.coerceAtLeast(1)
        val amount = when {
            tick.amount != null -> tick.amount
            tick.perMaxHpDivisor != null && tick.perMaxHpDivisor > 0 ->
                max(tick.min ?: 1, maxHp / tick.perMaxHpDivisor)
            else -> 0
        }
        if (amount <= 0) return state
        return when (tick.mode.lowercase()) {
            "damage" -> applyDamage(
                state = state,
                attackerId = STATUS_SOURCE_PREFIX + status.id,
                targetId = combatantId,
                amount = amount,
                element = tick.element,
                applyElementStacks = false
            )
            "heal" -> applyHeal(
                state = state,
                sourceId = STATUS_SOURCE_PREFIX + status.id,
                targetId = combatantId,
                amount = amount
            )
            else -> state
        }
    }

    fun resolveOutcome(
        state: CombatState,
        rewardProvider: () -> CombatReward? = { null }
    ): CombatState {
        if (state.outcome != null) return state

        val playerAlive = state.combatants.values.any { it.combatant.side == CombatSide.PLAYER && it.isAlive }
        val alliesAlive = state.combatants.values.any { it.combatant.side == CombatSide.ALLY && it.isAlive }
        val hasFriendly = playerAlive || alliesAlive
        val enemiesAlive = state.combatants.values.any { it.combatant.side == CombatSide.ENEMY && it.isAlive }

        val outcome = when {
            !hasFriendly -> CombatOutcome.Defeat()
            !enemiesAlive -> CombatOutcome.Victory(rewardProvider() ?: CombatReward())
            else -> null
        } ?: return state

        return state.copy(
            outcome = outcome,
            log = state.log + CombatLogEntry.Outcome(turn = state.round, result = outcome)
        )
    }

    private fun applyElementStack(
        state: CombatState,
        attackerId: String,
        targetId: String,
        element: String
    ): CombatState {
        val targetState = state.combatants[targetId] ?: return state
        val currentStacks = targetState.elementStacks[element] ?: 0
        val newStacks = (currentStacks + 1).coerceAtLeast(1)
        val updated = targetState.copy(elementStacks = targetState.elementStacks + (element to newStacks))
        var working = state.copy(
            combatants = state.combatants + (targetId to updated)
        )
        working = working.copy(
            log = working.log + CombatLogEntry.ElementStack(
                turn = working.round,
                targetId = targetId,
                element = element,
                stacks = newStacks
            )
        )
        return if (newStacks >= ElementalStackRules.STACK_THRESHOLD) {
            val burstApplied = triggerElementBurst(working, targetId, element)
            clearElementStack(burstApplied, targetId, element)
        } else {
            working
        }
    }

    private fun triggerElementBurst(state: CombatState, targetId: String, element: String): CombatState {
        val targetState = state.combatants[targetId] ?: return state
        var working = state.copy(
            log = state.log + CombatLogEntry.ElementBurst(
                turn = state.round,
                targetId = targetId,
                element = element
            )
        )
        val maxHp = targetState.combatant.stats.maxHp.coerceAtLeast(1)
        working = when (element) {
            "burn" -> applyBurnBurst(working, targetId, maxHp)
            "freeze" -> applyFreezeBurst(working, targetId)
            "shock" -> applyShockBurst(working, targetId, maxHp)
            "acid" -> applyAcidBurst(working, targetId)
            else -> working
        }
        return working
    }

    private fun applyBurnBurst(state: CombatState, originId: String, originMaxHp: Int): CombatState {
        val origin = state.combatants[originId] ?: return state
        val damage = burstDamage(originMaxHp, BURN_BURST_DIVISOR)
        val allies = state.combatants.values.filter {
            it.combatant.side == origin.combatant.side && it.isAlive
        }
        var working = state
        allies.forEach { ally ->
            working = applyDamage(
                state = working,
                attackerId = originId,
                targetId = ally.combatant.id,
                amount = damage,
                element = "burn",
                applyElementStacks = false
            )
        }
        return working
    }

    private fun applyFreezeBurst(state: CombatState, targetId: String): CombatState =
        applyStatus(
            state = state,
            targetId = targetId,
            statusId = "brittle",
            duration = FREEZE_BURST_DURATION,
            stacks = 1
        )

    private fun applyShockBurst(state: CombatState, targetId: String, originMaxHp: Int): CombatState {
        val damage = burstDamage(originMaxHp, SHOCK_BURST_DIVISOR)
        var working = applyDamage(
            state = state,
            attackerId = targetId,
            targetId = targetId,
            amount = damage,
            element = "shock",
            applyElementStacks = false
        )
        return applyStatus(
            state = working,
            targetId = targetId,
            statusId = "short",
            duration = SHOCK_BURST_DURATION,
            stacks = 1
        )
    }

    private fun applyAcidBurst(state: CombatState, targetId: String): CombatState =
        applyStatus(
            state = state,
            targetId = targetId,
            statusId = "erosion",
            duration = ACID_BURST_DURATION,
            stacks = 2
        )



    private fun clearElementStack(state: CombatState, targetId: String, element: String): CombatState {
        val targetState = state.combatants[targetId] ?: return state
        if (!targetState.elementStacks.containsKey(element)) return state
        val updated = targetState.copy(elementStacks = targetState.elementStacks - element)
        return state.copy(combatants = state.combatants + (targetId to updated))
    }

    private fun burstDamage(maxHp: Int, divisor: Int): Int = max(1, maxHp / divisor)

    private fun resolveResistance(
        targetState: CombatantState,
        element: String?
    ): Double {
        val profile = targetState.combatant.resistances
        val key = element?.lowercase()
        val base = when (key) {
            "burn", "fire" -> profile.burn
            "freeze", "ice" -> profile.freeze
            "shock", "lightning" -> profile.shock
            "acid", "poison", "corrosion" -> profile.acid
            "source", "harmonic", "psychic", "psionic" -> profile.source
            else -> profile.physical
        } ?: 0
        val general = CombatFormulas.generalResistance(effectiveFocus(targetState))
        return (base + general).toDouble()
    }

    private fun effectiveFocus(targetState: CombatantState): Int {
        if (targetState.buffs.isEmpty()) return targetState.combatant.stats.focus
        val bonus = targetState.buffs.sumOf { buff ->
            when (buff.effect.stat.lowercase()) {
                "focus", "foc", "fcs", "int", "psi" -> buff.effect.value
                else -> 0
            }
        }
        return (targetState.combatant.stats.focus + bonus).coerceAtLeast(0)
    }

    companion object {
        private const val DEFAULT_BUFF_DURATION = 2
        private const val DEFAULT_STATUS_DURATION = 2
        private const val STATUS_SOURCE_PREFIX = "status_"
        private const val BURN_BURST_DIVISOR = 10
        private const val SHOCK_BURST_DIVISOR = 10
        private const val FREEZE_BURST_DURATION = 2
        private const val SHOCK_BURST_DURATION = 1
        private const val ACID_BURST_DURATION = 4
    }
}

data class CombatSetup(
    val playerParty: List<Combatant>,
    val enemyParty: List<Combatant>
) {
    val allCombatants: List<Combatant> = playerParty + enemyParty
}

fun interface TurnOrderCalculator {
    fun calculate(setup: CombatSetup): List<TurnSlot>
}

class DefaultTurnOrderCalculator : TurnOrderCalculator {
    override fun calculate(setup: CombatSetup): List<TurnSlot> {
        return setup.allCombatants
            .map { combatant ->
                val baseInitiative = combatant.stats.speed + combatant.initiativeModifier
                TurnSlot(combatant.id, baseInitiative)
            }
            .sortedByDescending { it.initiative }
    }
}
