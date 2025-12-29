package com.example.starborn.domain.combat

import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.model.BuffEffect
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Skill
import kotlin.math.roundToInt
import java.util.LinkedHashMap
import com.example.starborn.domain.model.StatusDefinition
import kotlin.random.Random

class CombatActionProcessor(
    private val engine: CombatEngine,
    private val statusRegistry: StatusRegistry,
    private val skillLookup: (String) -> Skill?,
    private val consumeItem: ((String) -> ItemUseResult?)? = null,
    private val itemLookup: ((String) -> Item?)? = null
) {

    fun execute(
        state: CombatState,
        action: CombatAction,
        rewardProvider: () -> CombatReward
    ): CombatState {
        val actorState = state.combatants[action.actorId]
        val skipInfo = actorState?.let { skipReason(it) }
        if (skipInfo != null) {
            val skipped = state.copy(
                log = state.log + CombatLogEntry.TurnSkipped(
                    turn = state.round,
                    actorId = action.actorId,
                    reason = skipInfo.reason
                )
            )
            val afterTick = engine.tickEndOfTurn(skipped)
            val withOutcome = engine.resolveOutcome(afterTick, rewardProvider)
            return if (withOutcome.outcome != null) withOutcome else engine.advance(withOutcome)
        }
        val queued = engine.queueAction(state, action)
        val sanitized = if (action is CombatAction.BasicAttack) queued else queued.clearWeaponCharge(action.actorId)
        return when (action) {
            is CombatAction.BasicAttack -> finalizeAction(
                processBasicAttack(sanitized, action),
                rewardProvider
            )
            is CombatAction.SupportAbility -> finalizeAction(
                processSupportAbility(sanitized, action),
                rewardProvider
            )
            is CombatAction.SkillUse -> finalizeAction(
                processSkillUse(sanitized, action),
                rewardProvider
            )
            is CombatAction.ItemUse -> finalizeAction(
                processItemUse(sanitized, action),
                rewardProvider
            )
            is CombatAction.SnackUse -> finalizeAction(
                processSnackUse(sanitized, action),
                rewardProvider
            )
            is CombatAction.Defend -> finalizeAction(
                processDefend(sanitized, action),
                rewardProvider
            )
            is CombatAction.Flee -> finalizeAction(
                processFlee(sanitized),
                rewardProvider,
                advance = false
            )
        }
    }

    private fun processBasicAttack(
        state: CombatState,
        action: CombatAction.BasicAttack
    ): CombatState {
        val attacker = state.combatants[action.actorId] ?: return state
        val weapon = attacker.combatant.weapon
        return when (val attack = weapon?.attack) {
            null -> processBasicAttackUnarmed(state, action, attacker)
            is WeaponAttack.SingleTarget -> {
                state.clearWeaponCharge(action.actorId).applyWeaponAttack(
                    attackerId = action.actorId,
                    preferredTargetId = action.targetId,
                    multiplier = attack.powerMultiplier,
                    element = attack.element ?: PHYSICAL_ELEMENT
                )
            }
            is WeaponAttack.AllEnemies -> {
                state.clearWeaponCharge(action.actorId).applyWeaponAttackAllEnemies(
                    attackerId = action.actorId,
                    multiplier = attack.powerMultiplier,
                    element = attack.element ?: PHYSICAL_ELEMENT
                )
            }
            is WeaponAttack.ChargedSplash -> {
                val resolvedWeapon = weapon ?: return processBasicAttackUnarmed(state, action, attacker)
                processChargedSplashAttack(
                    state = state,
                    action = action,
                    weapon = resolvedWeapon,
                    attack = attack
                )
            }
        }
    }

    private fun processSupportAbility(
        state: CombatState,
        action: CombatAction.SupportAbility
    ): CombatState {
        val actor = state.combatants[action.actorId] ?: return state
        val actorKey = action.actorId.substringBefore('#').trim().lowercase()
        return when (actorKey) {
            "nova" -> {
                val targetId = resolveValidEnemyTarget(
                    state = state,
                    attacker = actor,
                    preferredTargetId = action.targetId,
                    allowFallback = true
                ) ?: return state
                var working = state
                working = engine.applyBuffs(
                    state = working,
                    targetId = targetId,
                    buffs = listOf(
                        BuffEffect(stat = "accuracy", value = NOVA_CHEAP_SHOT_ACCURACY_PENALTY, duration = SUPPORT_TURN_DURATION)
                    ),
                    sourceId = action.actorId
                )
                working = engine.applyBuffs(
                    state = working,
                    targetId = action.actorId,
                    buffs = listOf(
                        BuffEffect(stat = "evasion", value = NOVA_CHEAP_SHOT_EVASION_BONUS, duration = SUPPORT_TURN_DURATION)
                    ),
                    sourceId = action.actorId
                )
                working
            }
            "zeke" -> {
                val targetId = resolveValidAllyTarget(
                    state = state,
                    attacker = actor,
                    preferredTargetId = action.targetId,
                    allowFallback = true
                ) ?: return state
                engine.applyBuffs(
                    state = state,
                    targetId = targetId,
                    buffs = listOf(
                        BuffEffect(stat = "defense", value = ZEKE_SYNERGY_BARRIER, duration = SUPPORT_TURN_DURATION)
                    ),
                    sourceId = action.actorId
                )
            }
            "orion" -> {
                val targetId = resolveValidAllyTarget(
                    state = state,
                    attacker = actor,
                    preferredTargetId = action.targetId,
                    allowFallback = true
                ) ?: return state
                val target = state.combatants[targetId] ?: return state
                val maxHp = target.combatant.stats.maxHp.coerceAtLeast(1)
                val heal = (maxHp * ORION_STASIS_HEAL_PERCENT).roundToInt().coerceAtLeast(1)
                var working = engine.applyHeal(
                    state = state,
                    sourceId = action.actorId,
                    targetId = targetId,
                    amount = heal
                )
                working = engine.applyBuffs(
                    state = working,
                    targetId = targetId,
                    buffs = listOf(
                        BuffEffect(stat = "defense", value = ORION_WARD_BARRIER, duration = SUPPORT_TURN_DURATION)
                    ),
                    sourceId = action.actorId
                )
                working
            }
            "gh0st" -> {
                val targetId = resolveValidEnemyTarget(
                    state = state,
                    attacker = actor,
                    preferredTargetId = action.targetId,
                    allowFallback = true
                ) ?: return state
                var working = state
                working = applyTargetLock(
                    state = working,
                    targetId = targetId,
                    duration = GHOST_TARGET_LOCK_DURATION,
                    stacks = GHOST_TARGET_LOCK_HITS
                )
                working = engine.applyBuffs(
                    state = working,
                    targetId = action.actorId,
                    buffs = listOf(
                        BuffEffect(stat = "defense", value = GHOST_TARGET_LOCK_DR_BONUS, duration = SUPPORT_TURN_DURATION)
                    ),
                    sourceId = action.actorId
                )
                working
            }
            else -> processDefend(state, CombatAction.Defend(action.actorId))
        }
    }

    private fun processBasicAttackUnarmed(
        state: CombatState,
        action: CombatAction.BasicAttack,
        attacker: CombatantState
    ): CombatState {
        val targetId = resolveValidEnemyTarget(
            state = state,
            attacker = attacker,
            preferredTargetId = action.targetId,
            allowFallback = true
        ) ?: return state
        val target = state.combatants[targetId] ?: return state
        val critBonus = targetLockCritBonus(target)
        val hit = rollPhysicalHit(attacker, target, critBonus = critBonus)
        if (hit is HitRoll.Miss) {
            return state.logMiss(action.actorId, targetId)
        }
        val outgoing = adjustOutgoingDamage(attacker, hit.damage)
        val rawDamage = engine.calculateDamage(attacker, target, outgoing, PHYSICAL_ELEMENT)
        val critAdjusted = if (hit.critical) {
            (rawDamage * CombatFormulas.CRIT_DAMAGE_MULT).roundToInt().coerceAtLeast(1)
        } else rawDamage
        val finalDamage = adjustIncomingDamage(target, critAdjusted)
        val appliedDamage = finalDamage.coerceAtLeast(0)
        return engine.applyDamage(
            state = state,
            attackerId = action.actorId,
            targetId = targetId,
            amount = appliedDamage,
            element = PHYSICAL_ELEMENT,
            critical = hit.critical
        ).consumeTargetLockIfPresent(targetId)
    }

    private fun processChargedSplashAttack(
        state: CombatState,
        action: CombatAction.BasicAttack,
        weapon: CombatWeapon,
        attack: WeaponAttack.ChargedSplash
    ): CombatState {
        val attacker = state.combatants[action.actorId] ?: return state
        val chargeTurns = attack.chargeTurns.coerceAtLeast(0)
        val existingCharge = attacker.weaponCharge
        val sameWeaponCharging = existingCharge?.weaponItemId == weapon.itemId
        if (!sameWeaponCharging) {
            if (chargeTurns <= 0) {
                return state.clearWeaponCharge(action.actorId).applyChargedSplashDamage(
                    attackerId = action.actorId,
                    preferredTargetId = action.targetId,
                    attack = attack
                )
            }
            val updatedAttacker = attacker.copy(
                weaponCharge = WeaponChargeState(
                    weaponItemId = weapon.itemId,
                    remainingTurns = chargeTurns
                )
            )
            return state.copy(
                combatants = state.combatants + (action.actorId to updatedAttacker),
                log = state.log + CombatLogEntry.TurnSkipped(
                    turn = state.round,
                    actorId = action.actorId,
                    reason = "is charging ${weapon.name}"
                )
            )
        }

        val remaining = (existingCharge?.remainingTurns ?: 0) - 1
        return if (remaining > 0) {
            val updatedAttacker = attacker.copy(
                weaponCharge = existingCharge!!.copy(remainingTurns = remaining)
            )
            state.copy(
                combatants = state.combatants + (action.actorId to updatedAttacker),
                log = state.log + CombatLogEntry.TurnSkipped(
                    turn = state.round,
                    actorId = action.actorId,
                    reason = "continues charging ${weapon.name}"
                )
            )
        } else {
            state.clearWeaponCharge(action.actorId).applyChargedSplashDamage(
                attackerId = action.actorId,
                preferredTargetId = action.targetId,
                attack = attack
            )
        }
    }

    private fun CombatState.applyChargedSplashDamage(
        attackerId: String,
        preferredTargetId: String,
        attack: WeaponAttack.ChargedSplash
    ): CombatState {
        val attacker = combatants[attackerId] ?: return this
        val targetId = resolveValidEnemyTarget(
            state = this,
            attacker = attacker,
            preferredTargetId = preferredTargetId,
            allowFallback = true
        ) ?: return this
        val element = attack.element ?: PHYSICAL_ELEMENT
        val primaryMultiplier = attack.powerMultiplier
        val splashMultiplier = attack.splashMultiplier

        var working = applyWeaponAttack(
            attackerId = attackerId,
            preferredTargetId = targetId,
            multiplier = primaryMultiplier,
            element = element,
            forceHit = true,
            allowCrit = false
        )

        if (splashMultiplier <= 0.0) return working
        val refreshedAttacker = working.combatants[attackerId] ?: return working
        val splashTargets = opposingLivingTargets(working, refreshedAttacker).filterNot { it == targetId }
        if (splashTargets.isEmpty()) return working
        val combinedMultiplier = primaryMultiplier * splashMultiplier
        splashTargets.forEach { otherId ->
            working = working.applyWeaponAttack(
                attackerId = attackerId,
                preferredTargetId = otherId,
                multiplier = combinedMultiplier,
                element = element,
                forceHit = true,
                allowCrit = false,
                fallbackToOtherTarget = false
            )
        }
        return working
    }

    private fun CombatState.applyWeaponAttackAllEnemies(
        attackerId: String,
        multiplier: Double,
        element: String
    ): CombatState {
        val attacker = combatants[attackerId] ?: return this
        val targets = opposingLivingTargets(this, attacker)
        if (targets.isEmpty()) return this
        var working = this
        targets.forEach { targetId ->
            working = working.applyWeaponAttack(
                attackerId = attackerId,
                preferredTargetId = targetId,
                multiplier = multiplier,
                element = element,
                fallbackToOtherTarget = false
            )
        }
        return working
    }

    private fun CombatState.applyWeaponAttack(
        attackerId: String,
        preferredTargetId: String,
        multiplier: Double,
        element: String,
        forceHit: Boolean = false,
        allowCrit: Boolean = true,
        fallbackToOtherTarget: Boolean = true
    ): CombatState {
        val attacker = combatants[attackerId] ?: return this
        val targetId = resolveValidEnemyTarget(
            state = this,
            attacker = attacker,
            preferredTargetId = preferredTargetId,
            allowFallback = fallbackToOtherTarget
        ) ?: return this
        val target = combatants[targetId] ?: return this
        if (!attacker.isAlive || !target.isAlive) return this

        val shouldApplyTargetLock = target.hasStatus(TARGET_LOCK_STATUS_ID)
        val critBonus = if (shouldApplyTargetLock) GHOST_TARGET_LOCK_CRIT_BONUS else 0.0
        val hit = if (forceHit) {
            HitRoll.Hit(
                damage = basePhysicalDamage(attacker, target),
                critical = false
            )
        } else {
            rollPhysicalHit(attacker, target, critBonus = critBonus)
        }
        if (hit is HitRoll.Miss) return logMiss(attackerId, targetId)

        val scaledDamage = scaleDamage(hit.damage, multiplier)
        val outgoing = adjustOutgoingDamage(attacker, scaledDamage)
        val rawDamage = engine.calculateDamage(attacker, target, outgoing, element)
        val critAdjusted = if (allowCrit && hit.critical) {
            (rawDamage * CombatFormulas.CRIT_DAMAGE_MULT).roundToInt().coerceAtLeast(1)
        } else rawDamage
        val finalDamage = adjustIncomingDamage(target, critAdjusted)
        val appliedDamage = finalDamage.coerceAtLeast(0)
        val damaged = engine.applyDamage(
            state = this,
            attackerId = attackerId,
            targetId = targetId,
            amount = appliedDamage,
            element = element,
            critical = allowCrit && hit.critical
        )
        val statusId = attacker.combatant.weapon?.statusOnHit
            ?.trim()
            ?.lowercase()
        val statusChance = attacker.combatant.weapon?.statusChance ?: 0.0
        val withStatus = if (!statusId.isNullOrBlank() && Random.nextDouble(0.0, 100.0) <= statusChance) {
            engine.applyStatus(
                state = damaged,
                targetId = targetId,
                statusId = statusId,
                duration = 0,
                sourceId = attackerId
            )
        } else {
            damaged
        }
        return if (shouldApplyTargetLock) withStatus.consumeTargetLockIfPresent(targetId) else withStatus
    }

    private fun CombatState.clearWeaponCharge(actorId: String): CombatState {
        val actor = combatants[actorId] ?: return this
        if (actor.weaponCharge == null) return this
        return copy(
            combatants = combatants + (actorId to actor.copy(weaponCharge = null))
        )
    }

    private fun opposingLivingTargets(
        state: CombatState,
        attacker: CombatantState
    ): List<String> {
        val opposingSide = opposingSide(attacker.combatant.side)
        return state.turnOrder.asSequence()
            .map { it.combatantId }
            .mapNotNull(state.combatants::get)
            .filter { it.combatant.side == opposingSide && it.isAlive }
            .map { it.combatant.id }
            .toList()
    }

    private fun resolveValidEnemyTarget(
        state: CombatState,
        attacker: CombatantState,
        preferredTargetId: String,
        allowFallback: Boolean
    ): String? {
        val preferred = state.combatants[preferredTargetId]
        if (preferred?.isAlive == true && preferred.combatant.side == opposingSide(attacker.combatant.side)) {
            return preferredTargetId
        }
        return if (allowFallback) opposingLivingTargets(state, attacker).firstOrNull() else null
    }

    private fun resolveValidAllyTarget(
        state: CombatState,
        attacker: CombatantState,
        preferredTargetId: String,
        allowFallback: Boolean
    ): String? {
        val preferred = state.combatants[preferredTargetId]
        if (preferred?.isAlive == true && preferred.combatant.side == attacker.combatant.side) {
            return preferredTargetId
        }
        if (!allowFallback) return null
        if (attacker.isAlive) return attacker.combatant.id
        return state.turnOrder.asSequence()
            .map { it.combatantId }
            .mapNotNull(state.combatants::get)
            .filter { it.combatant.side == attacker.combatant.side && it.isAlive }
            .map { it.combatant.id }
            .firstOrNull()
    }

    private fun targetLockCritBonus(target: CombatantState): Double =
        if (target.hasStatus(TARGET_LOCK_STATUS_ID)) GHOST_TARGET_LOCK_CRIT_BONUS else 0.0

    private fun CombatantState.hasStatus(statusId: String): Boolean =
        statusEffects.any { it.id.equals(statusId, ignoreCase = true) && it.stacks > 0 }

    private fun CombatState.consumeTargetLockIfPresent(targetId: String): CombatState {
        val target = combatants[targetId] ?: return this
        val statuses = target.statusEffects.toMutableList()
        val index = statuses.indexOfFirst { it.id.equals(TARGET_LOCK_STATUS_ID, ignoreCase = true) }
        if (index < 0) return this
        val existing = statuses[index]
        val remainingStacks = (existing.stacks - 1).coerceAtLeast(0)
        if (remainingStacks <= 0) {
            statuses.removeAt(index)
        } else {
            statuses[index] = existing.copy(stacks = remainingStacks)
        }
        return copy(
            combatants = combatants + (targetId to target.copy(statusEffects = statuses))
        )
    }

    private fun applyTargetLock(
        state: CombatState,
        targetId: String,
        duration: Int,
        stacks: Int
    ): CombatState {
        val target = state.combatants[targetId] ?: return state
        val resolvedDuration = duration.coerceAtLeast(1)
        val resolvedStacks = stacks.coerceAtLeast(1)
        val updatedStatuses = target.statusEffects.toMutableList()
        val index = updatedStatuses.indexOfFirst { it.id.equals(TARGET_LOCK_STATUS_ID, ignoreCase = true) }
        val newStatus = StatusEffect(
            id = TARGET_LOCK_STATUS_ID,
            remainingTurns = resolvedDuration,
            stacks = resolvedStacks
        )
        if (index >= 0) {
            updatedStatuses[index] = newStatus
        } else {
            updatedStatuses.add(newStatus)
        }
        val updatedState = state.copy(
            combatants = state.combatants + (targetId to target.copy(statusEffects = updatedStatuses))
        )
        val logEntry = CombatLogEntry.StatusApplied(
            turn = state.round,
            targetId = targetId,
            statusId = TARGET_LOCK_STATUS_ID,
            stacks = resolvedStacks
        )
        return updatedState.copy(log = updatedState.log + logEntry)
    }

    private fun scaleDamage(baseDamage: Int, multiplier: Double): Int {
        if (baseDamage <= 0) return 0
        val clamped = multiplier.coerceAtLeast(0.0)
        if (clamped == 0.0) return 0
        return (baseDamage * clamped).roundToInt().coerceAtLeast(1)
    }

    private fun processSkillUse(
        state: CombatState,
        action: CombatAction.SkillUse
    ): CombatState {
        val attackerId = action.actorId
        val attackerState = state.combatants[attackerId] ?: return state
        val skill = skillLookup(action.skillId)

        var working = state
        val special = applySkillSpecialCases(working, skill, attackerId)
        working = special.state

        if (!special.handled) {
            val element = resolveElement(skill)
            val explicitTargets = action.targetIds
            val mode = when {
                skill == null -> SkillMode.Damage(attackerState.effectiveStat("strength").coerceAtLeast(1), PHYSICAL_ELEMENT)
                isHealSkill(skill) -> SkillMode.Heal(resolveSkillHeal(attackerState, skill))
                else -> SkillMode.Damage(
                    baseDamage = resolveSkillDamage(attackerState, skill),
                    element = element ?: PHYSICAL_ELEMENT
                )
            }

            working = when (mode) {
                is SkillMode.Heal -> applyHealing(
                    working,
                    attackerId,
                    explicitTargets,
                    mode
                )
                is SkillMode.Damage -> {
                    if (mode.baseDamage <= 0) {
                        working
                    } else {
                        applyDamage(
                            working,
                            attackerId,
                            explicitTargets,
                            mode
                        )
                    }
                }
            }

            if (skill != null) {
                val statusDescriptors = statusesForSkill(
                    skill = skill,
                    attackerId = attackerId,
                    state = working,
                    attackerState = attackerState,
                    explicitTargets = explicitTargets
                )
                statusDescriptors.forEach { descriptor ->
                    working = applyStatusEffects(
                        working,
                        descriptor.sourceId,
                        descriptor.targets,
                        descriptor.definition
                    )
                }
            }
        }

        return working
    }

    private fun processItemUse(
        state: CombatState,
        action: CombatAction.ItemUse
    ): CombatState {
        val itemResult = consumeItem?.invoke(action.itemId) ?: return state
        return when (itemResult) {
            is ItemUseResult.None -> state
            is ItemUseResult.Restore -> applyItemRestore(state, action, itemResult)
            is ItemUseResult.Damage -> applyItemDamage(state, action, itemResult)
            is ItemUseResult.Buff -> applyItemBuff(state, action, itemResult)
            is ItemUseResult.LearnSchematic -> state // handled outside combat
        }
    }

    private fun processSnackUse(
        state: CombatState,
        action: CombatAction.SnackUse
    ): CombatState {
        val actor = state.combatants[action.actorId] ?: return state
        val item = itemLookup?.invoke(action.snackItemId) ?: return state
        val effect = item.effect ?: return state
        val hasDamage = (effect.damage ?: 0) > 0
        val hasRestore = (effect.restoreHp ?: 0) > 0
        val hasBuff = effect.singleBuff != null || !effect.buffs.isNullOrEmpty()

        val declaredTarget = effect.target?.trim()?.lowercase()
        val resolvedTargetId = when (declaredTarget) {
            "self" -> action.actorId
            "enemy" -> resolveValidEnemyTarget(
                state = state,
                attacker = actor,
                preferredTargetId = action.targetId.orEmpty(),
                allowFallback = true
            )
            "ally" -> resolveValidAllyTarget(
                state = state,
                attacker = actor,
                preferredTargetId = action.targetId ?: action.actorId,
                allowFallback = true
            )
            "any" -> {
                val preferred = action.targetId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { candidate ->
                        state.combatants[candidate]?.takeIf { it.isAlive }?.combatant?.id
                    }
                preferred
                    ?: resolveValidEnemyTarget(
                        state = state,
                        attacker = actor,
                        preferredTargetId = action.targetId.orEmpty(),
                        allowFallback = true
                    )
                    ?: resolveValidAllyTarget(
                        state = state,
                        attacker = actor,
                        preferredTargetId = action.targetId ?: action.actorId,
                        allowFallback = true
                    )
            }
            else -> when {
                hasDamage -> resolveValidEnemyTarget(
                    state = state,
                    attacker = actor,
                    preferredTargetId = action.targetId.orEmpty(),
                    allowFallback = true
                )
                hasRestore || hasBuff -> action.actorId
                else -> action.actorId
            }
        }

        val proxy = CombatAction.ItemUse(
            actorId = action.actorId,
            itemId = item.id,
            targetId = resolvedTargetId
        )
        val result = when {
            hasRestore -> ItemUseResult.Restore(
                item = item,
                hp = effect.restoreHp ?: 0
            )
            hasDamage -> ItemUseResult.Damage(
                item = item,
                amount = effect.damage ?: 0
            )
            hasBuff -> {
                val buffs = buildList {
                    effect.singleBuff?.let { add(it) }
                    effect.buffs?.let { addAll(it) }
                }
                ItemUseResult.Buff(item = item, buffs = buffs)
            }
            else -> ItemUseResult.None(item)
        }
        return when (result) {
            is ItemUseResult.None -> state
            is ItemUseResult.Restore -> applyItemRestore(state, proxy, result)
            is ItemUseResult.Damage -> applyItemDamage(state, proxy, result)
            is ItemUseResult.Buff -> applyItemBuff(state, proxy, result)
            is ItemUseResult.LearnSchematic -> state
        }
    }

    private fun processDefend(
        state: CombatState,
        action: CombatAction.Defend
    ): CombatState {
        val buff = BuffEffect(stat = "defense", value = DEFEND_BONUS, duration = DEFEND_DURATION)
        var working = engine.applyBuffs(state, action.actorId, listOf(buff))
        working = engine.applyStatus(
            working,
            targetId = action.actorId,
            statusId = DEFEND_STATUS_ID,
            duration = DEFEND_DURATION,
            stacks = 1
        )
        return working
    }

    private fun processFlee(state: CombatState): CombatState {
        val outcome = CombatOutcome.Retreat
        val logEntry = CombatLogEntry.Outcome(turn = state.round, result = outcome)
        return state.copy(
            outcome = outcome,
            log = state.log + logEntry
        )
    }

    private fun applyHealing(
        state: CombatState,
        sourceId: String,
        explicitTargets: List<String>,
        healMode: SkillMode.Heal
    ): CombatState {
        if (healMode.amount <= 0) return state
        val source = state.combatants[sourceId]
        var working = state
        val targets = actionTargets(working, source, explicitTargets, Targeting.SELF)
        targets.forEach { targetId ->
            working = engine.applyHeal(
                state = working,
                sourceId = sourceId,
                targetId = targetId,
                amount = healMode.amount
            )
        }
        return working
    }

    private fun applyDamage(
        state: CombatState,
        attackerId: String,
        explicitTargets: List<String>,
        damageMode: SkillMode.Damage
    ): CombatState {
        var working = state
        val attacker = working.combatants[attackerId]
        val targets = actionTargets(working, attacker, explicitTargets, Targeting.ENEMY)
        targets.forEach { targetId ->
            val currentAttacker = working.combatants[attackerId] ?: return@forEach
            val targetState = working.combatants[targetId] ?: return@forEach
            val outgoing = adjustOutgoingDamage(currentAttacker, damageMode.baseDamage)
            val rawDamage = engine.calculateDamage(
                attackerState = currentAttacker,
                targetState = targetState,
                baseDamage = outgoing,
                element = damageMode.element
            )
            val damage = adjustIncomingDamage(targetState, rawDamage)
            if (damage > 0) {
                working = engine.applyDamage(
                    state = working,
                    attackerId = attackerId,
                    targetId = targetId,
                    amount = damage,
                    element = damageMode.element
                )
            }
        }
        return working
    }

    private fun applyStatusEffects(
        state: CombatState,
        sourceId: String,
        explicitTargets: List<String>,
        descriptor: StatusDescriptor
    ): CombatState {
        var working = state
        val source = working.combatants[sourceId]
        val targets = actionTargets(working, source, explicitTargets, descriptor.targeting.toTargeting())
        targets.forEach { targetId ->
            working = engine.applyStatus(
                state = working,
                targetId = targetId,
                statusId = descriptor.id,
                duration = descriptor.duration,
                stacks = descriptor.stacks
            )
        }
        return working
    }

    private fun applyItemRestore(
        state: CombatState,
        action: CombatAction.ItemUse,
        result: ItemUseResult.Restore
    ): CombatState {
        var working = state
        val targets = resolveTargetsForItem(state, action, Targeting.SELF)
        targets.forEach { targetId ->
            if (result.hp > 0) {
                working = engine.applyHeal(
                    state = working,
                    sourceId = action.actorId,
                    targetId = targetId,
                    amount = result.hp
                )
            }
        }
        return working
    }

    private fun applyItemDamage(
        state: CombatState,
        action: CombatAction.ItemUse,
        result: ItemUseResult.Damage
    ): CombatState {
        val damageMode = SkillMode.Damage(
            baseDamage = result.amount.coerceAtLeast(1),
            element = PHYSICAL_ELEMENT
        )
        return applyDamage(
            state = state,
            attackerId = action.actorId,
            explicitTargets = resolveTargetsForItem(state, action, Targeting.ENEMY),
            damageMode = damageMode
        )
    }

    private fun applyItemBuff(
        state: CombatState,
        action: CombatAction.ItemUse,
        result: ItemUseResult.Buff
    ): CombatState {
        val targets = resolveTargetsForItem(state, action, Targeting.SELF)
        var working = state
        targets.forEach { targetId ->
            working = engine.applyBuffs(
                state = working,
                targetId = targetId,
                buffs = result.buffs,
                sourceId = action.actorId
            )
        }
        return working
    }

    private fun resolveTargetsForItem(
        state: CombatState,
        action: CombatAction.ItemUse,
        fallback: Targeting
    ): List<String> {
        val actorState = state.combatants[action.actorId]
        val explicit = listOfNotNull(action.targetId).takeUnless { it.isEmpty() } ?: emptyList()
        return actionTargets(state, actorState, explicit, fallback)
    }

    private fun actionTargets(
        state: CombatState,
        attacker: CombatantState?,
        explicitTargets: List<String>,
        targeting: Targeting
    ): List<String> {
        if (explicitTargets.isNotEmpty()) return explicitTargets
        attacker ?: return emptyList()
        return when (targeting) {
            Targeting.ENEMY -> state.turnOrder.asSequence()
                .mapNotNull { state.combatants[it.combatantId] }
                .filter { it.combatant.side == opposingSide(attacker.combatant.side) && it.isAlive }
                .map { it.combatant.id }
                .toList()
            Targeting.ALLY -> state.turnOrder.asSequence()
                .mapNotNull { state.combatants[it.combatantId] }
                .filter { it.combatant.side == attacker.combatant.side && it.isAlive }
                .map { it.combatant.id }
                .toList()
            Targeting.SELF -> listOf(attacker.combatant.id)
        }
    }

    private fun CombatState.logMiss(
        attackerId: String,
        targetId: String
    ): CombatState = copy(
        log = log + CombatLogEntry.Damage(
            turn = round,
            sourceId = attackerId,
            targetId = targetId,
            amount = 0,
            element = "miss",
            critical = false
        )
    )

    private fun rollPhysicalHit(
        attacker: CombatantState,
        target: CombatantState,
        critBonus: Double = 0.0
    ): HitRoll {
        val hitChance = (attacker.accuracyRating() - target.evasionRating())
            .coerceIn(MIN_HIT_CHANCE, MAX_HIT_CHANCE)
        val roll = Random.nextDouble(0.0, 100.0)
        if (roll > hitChance) return HitRoll.Miss
        val baseDamage = basePhysicalDamage(attacker, target)
        val critRoll = Random.nextDouble(0.0, 100.0)
        val critChance = (attacker.critChance() + critBonus).coerceIn(0.0, 100.0)
        val critical = critRoll <= critChance
        return HitRoll.Hit(baseDamage, critical)
    }

    private fun basePhysicalDamage(
        attacker: CombatantState,
        target: CombatantState
    ): Int {
        val attack = CombatFormulas.attackPower(attacker.effectiveStat("strength"))
        val defense = CombatFormulas.defensePower(target.effectiveStat("vitality"))
        val variance = Random.nextInt(PHYSICAL_VARIANCE_MIN, PHYSICAL_VARIANCE_MAX + 1)
        val weaponDamage = rollWeaponDamage(attacker)
        return (attack + weaponDamage + variance - defense).coerceAtLeast(1)
    }

    private fun rollWeaponDamage(attacker: CombatantState): Int {
        val weapon = attacker.combatant.weapon ?: return 0
        val min = weapon.minDamage.coerceAtLeast(0)
        val max = weapon.maxDamage.coerceAtLeast(min)
        if (max <= 0) return 0
        return Random.nextInt(min, max + 1)
    }

    private fun CombatantState.accuracyRating(): Double {
        val buffs = totalBuffValue("accuracy")
        val base = CombatFormulas.accuracy(effectiveStat("focus"))
        return (base + combatant.stats.accuracyBonus + buffs).coerceIn(0.0, 100.0)
    }

    private fun CombatantState.evasionRating(): Double {
        val buffs = totalBuffValue("evasion")
        val base = CombatFormulas.evasion(effectiveStat("agility"))
        return (base + combatant.stats.evasionBonus + buffs).coerceIn(0.0, 99.5)
    }

    private fun CombatantState.critChance(): Double {
        val buffs = totalBuffValue("crit_rate")
        val base = CombatFormulas.critChance(
            focus = effectiveStat("focus"),
            luck = effectiveStat("luck")
        )
        return (base + combatant.stats.critBonus + buffs).coerceIn(0.0, 100.0)
    }

    private fun opposingSide(side: CombatSide): CombatSide =
        when (side) {
            CombatSide.PLAYER, CombatSide.ALLY -> CombatSide.ENEMY
            CombatSide.ENEMY -> CombatSide.PLAYER
        }

    private fun applySkillSpecialCases(
        state: CombatState,
        skill: Skill?,
        attackerId: String
    ): SpecialCaseResult {
        if (skill == null) return SpecialCaseResult(state, handled = false)
        return when (skill.id) {
            "nova_smoke_bomb" -> {
                val buff = BuffEffect(stat = "evasion", value = 15, duration = 2)
                val updated = engine.applyBuffs(state, attackerId, listOf(buff))
                SpecialCaseResult(updated, handled = true)
            }
            else -> SpecialCaseResult(state, handled = false)
        }
    }

    private fun resolveElement(skill: Skill?): String? {
        val tags = skill?.combatTags ?: return null
        return tags.firstOrNull { ELEMENT_TAGS.contains(it.lowercase()) }?.lowercase()
    }

    private fun resolveSkillDamage(
        attacker: CombatantState,
        skill: Skill
    ): Int {
        val base = when (skill.scaling?.lowercase()) {
            "atk", "attack", "str", "strength" -> attacker.effectiveStat("strength")
            "agi", "agility" -> attacker.effectiveStat("agility")
            "speed", "spd" -> attacker.effectiveStat("speed")
            "focus", "psi", "int" -> attacker.effectiveStat("focus")
            "vit", "vitality", "def" -> attacker.effectiveStat("vitality")
            "luck", "lck" -> attacker.effectiveStat("luck")
            else -> attacker.effectiveStat("strength")
        }
        val multiplier = skill.basePower.coerceAtLeast(0) / 100.0
        val potency = CombatFormulas.skillPotencyMultiplier(attacker.effectiveStat("focus"))
        return (base * multiplier * potency).roundToInt().coerceAtLeast(if (skill.basePower > 0) 1 else 0)
    }

    private fun resolveSkillHeal(attacker: CombatantState, skill: Skill): Int {
        val base = skill.basePower.coerceAtLeast(0)
        if (base <= 0) return 0
        val potency = CombatFormulas.skillPotencyMultiplier(attacker.effectiveStat("focus"))
        return (base * potency).roundToInt().coerceAtLeast(1)
    }

    private fun statusesForSkill(
        skill: Skill,
        attackerId: String,
        state: CombatState,
        attackerState: CombatantState,
        explicitTargets: List<String>
    ): List<StatusApplication> {
        val definitions = LinkedHashMap<String, StatusDefinition>()
        skill.statusApplications.orEmpty().forEach { id ->
            statusRegistry.definition(id)?.let { definitions.putIfAbsent(it.id, it) }
        }
        skill.combatTags.orEmpty().forEach { tag ->
            statusRegistry.definition(tag)?.let { definitions.putIfAbsent(it.id, it) }
        }
        if (definitions.isEmpty()) return emptyList()

        val enemyTargets = explicitTargets.ifEmpty {
            actionTargets(state, attackerState, emptyList(), Targeting.ENEMY)
        }
        val allyTargets = actionTargets(state, attackerState, emptyList(), Targeting.ALLY)
        val selfTarget = listOf(attackerId)

        return definitions.values.map { definition ->
            val targeting = definition.target.toStatusTargeting()
            val targets = when (targeting) {
                StatusTargeting.ENEMY -> enemyTargets.ifEmpty { enemyTargets }
                StatusTargeting.ALLY -> allyTargets.ifEmpty { selfTarget }
                StatusTargeting.SELF -> selfTarget
            }
            StatusApplication(
                sourceId = attackerId,
                targets = targets,
                definition = StatusDescriptor(
                    id = definition.id,
                    duration = definition.defaultDuration ?: DEFAULT_STATUS_DURATION,
                    stacks = 1,
                    targeting = targeting
                )
            )
        }
    }

    private data class StatusApplication(
        val sourceId: String,
        val targets: List<String>,
        val definition: StatusDescriptor
    )

    private fun String?.toStatusTargeting(): StatusTargeting = when (this?.lowercase()) {
        "ally", "allies" -> StatusTargeting.ALLY
        "self" -> StatusTargeting.SELF
        else -> StatusTargeting.ENEMY
    }

    private fun isHealSkill(skill: Skill): Boolean {
        if (skill.type.equals("heal", ignoreCase = true)) return true
        return skill.combatTags?.any { it.equals("heal", ignoreCase = true) || it.equals("support", ignoreCase = true) } == true
    }

    private fun finalizeAction(
        processed: CombatState,
        rewardProvider: () -> CombatReward,
        advance: Boolean = true
    ): CombatState {
        val afterTick = engine.tickEndOfTurn(processed)
        val withOutcome = engine.resolveOutcome(afterTick, rewardProvider)
        if (!advance || withOutcome.outcome != null) return withOutcome
        return engine.advance(withOutcome)
    }

    private fun skipReason(combatant: CombatantState): SkipInfo? {
        combatant.statusEffects.forEach { status ->
            val definition = statusRegistry.definition(status.id)
            if (!definition?.skipReason.isNullOrBlank()) {
                return SkipInfo(definition!!.skipReason!!)
            }
        }
        return null
    }

    private fun adjustOutgoingDamage(attacker: CombatantState, baseDamage: Int): Int {
        if (attacker.statusEffects.isEmpty()) return baseDamage
        var multiplier = 1.0
        attacker.statusEffects.forEach { status ->
            statusRegistry.definition(status.id)?.outgoingMultiplier?.let { multiplier *= it }
        }
        val adjusted = (baseDamage * multiplier).roundToInt()
        return if (baseDamage <= 0) adjusted.coerceAtLeast(0) else adjusted.coerceAtLeast(1)
    }

    private fun adjustIncomingDamage(target: CombatantState, rawDamage: Int): Int {
        if (rawDamage <= 0) return 0
        var result = rawDamage.toDouble()
        var flatReduction = target.combatant.stats.flatDamageReduction + target.totalBuffValue("flat_defense")
        target.statusEffects.forEach { status ->
            statusRegistry.definition(status.id)?.let { definition ->
                definition.incomingMultiplier?.let { result *= it }
                definition.flatDefenseBonus?.let { flatReduction += it }
            }
        }
        val adjusted = result.roundToInt() - flatReduction
        return adjusted.coerceAtLeast(0)
    }

    private fun CombatantState.totalBuffValue(stat: String): Int {
        val canonical = canonicalStatName(stat)
        return buffs.filter { matchesStat(it.effect.stat, canonical) }.sumOf { it.effect.value }
    }

    private fun CombatantState.effectiveStat(stat: String): Int {
        val canonical = canonicalStatName(stat)
        val base = when (canonical) {
            "strength" -> combatant.stats.strength
            "vitality" -> combatant.stats.vitality
            "agility" -> combatant.stats.agility
            "focus" -> combatant.stats.focus
            "luck" -> combatant.stats.luck
            "speed" -> combatant.stats.speed
            else -> 0
        }
        val bonus = totalBuffValue(canonical)
        return (base + bonus).coerceAtLeast(0)
    }

    private fun canonicalStatName(stat: String): String {
        val lower = stat.lowercase()
        STAT_ALIASES.entries.forEach { (canonical, aliases) ->
            if (lower == canonical || lower in aliases) return canonical
        }
        return lower
    }

    private fun matchesStat(candidate: String, canonical: String): Boolean {
        val normalized = canonicalStatName(candidate)
        return normalized == canonical
    }

    private data class SkipInfo(val reason: String)

    private data class SpecialCaseResult(
        val state: CombatState,
        val handled: Boolean
    )

    private sealed interface SkillMode {
        data class Damage(val baseDamage: Int, val element: String) : SkillMode
        data class Heal(val amount: Int) : SkillMode
    }

    private enum class Targeting {
        ENEMY,
        ALLY,
        SELF
    }

    private data class StatusDescriptor(
        val id: String,
        val duration: Int,
        val stacks: Int = 1,
        val targeting: StatusTargeting = StatusTargeting.ENEMY
    )

    private enum class StatusTargeting {
        ENEMY,
        ALLY,
        SELF;

        fun toTargeting(): Targeting = when (this) {
            ENEMY -> Targeting.ENEMY
            ALLY -> Targeting.ALLY
            SELF -> Targeting.SELF
        }
    }

    private sealed interface HitRoll {
        val damage: Int
        val critical: Boolean

        data class Hit(
            override val damage: Int,
            override val critical: Boolean
        ) : HitRoll

        data object Miss : HitRoll {
            override val damage: Int = 0
            override val critical: Boolean = false
        }
    }

    companion object {
        private const val PHYSICAL_ELEMENT = "physical"
        private const val DEFEND_BONUS = 20
        private const val DEFEND_DURATION = 2
        private const val DEFEND_STATUS_ID = "defend"
        private const val SUPPORT_TURN_DURATION = 2
        private const val NOVA_CHEAP_SHOT_ACCURACY_PENALTY = -12
        private const val NOVA_CHEAP_SHOT_EVASION_BONUS = 5
        private const val ZEKE_SYNERGY_BARRIER = 6
        private const val ORION_WARD_BARRIER = 5
        private const val ORION_STASIS_HEAL_PERCENT = 0.08
        private const val TARGET_LOCK_STATUS_ID = "target_lock"
        private const val GHOST_TARGET_LOCK_HITS = 2
        private const val GHOST_TARGET_LOCK_DURATION = 4
        private const val GHOST_TARGET_LOCK_CRIT_BONUS = 25.0
        private const val GHOST_TARGET_LOCK_DR_BONUS = 4
        private const val DEFAULT_STATUS_DURATION = 2
        private const val MIN_HIT_CHANCE = 5.0
        private const val MAX_HIT_CHANCE = 99.0
        private const val PHYSICAL_VARIANCE_MIN = -2
        private const val PHYSICAL_VARIANCE_MAX = 2

        private val ELEMENT_TAGS = setOf(
            "fire",
            "ice",
            "lightning",
            "poison",
            "radiation",
            "psychic",
            "psionic",
            "void",
            "physical"
        )

        private val STAT_ALIASES: Map<String, Set<String>> = mapOf(
            "strength" to setOf("str", "atk", "attack"),
            "vitality" to setOf("vit"),
            "agility" to setOf("agi"),
            "focus" to setOf("fcs", "foc", "int", "psi"),
            "luck" to setOf("lck"),
            "speed" to setOf("spd"),
            "accuracy" to setOf("acc"),
            "evasion" to setOf("eva"),
            "crit_rate" to setOf("crit", "crit_chance"),
            "flat_defense" to setOf("def", "defense", "armor", "damage_reduction", "dr")
        )
    }
}
