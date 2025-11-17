package com.example.starborn.domain.combat

import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.model.BuffEffect
import com.example.starborn.domain.model.Skill
import kotlin.math.roundToInt
import java.util.LinkedHashMap
import com.example.starborn.domain.model.StatusDefinition
import kotlin.random.Random

class CombatActionProcessor(
    private val engine: CombatEngine,
    private val statusRegistry: StatusRegistry,
    private val skillLookup: (String) -> Skill?,
    private val consumeItem: ((String) -> ItemUseResult?)? = null
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
        return when (action) {
            is CombatAction.BasicAttack -> finalizeAction(
                processBasicAttack(queued, action),
                rewardProvider
            )
            is CombatAction.SkillUse -> finalizeAction(
                processSkillUse(queued, action),
                rewardProvider
            )
            is CombatAction.ItemUse -> finalizeAction(
                processItemUse(queued, action),
                rewardProvider
            )
            is CombatAction.Defend -> finalizeAction(
                processDefend(queued, action),
                rewardProvider
            )
            is CombatAction.Flee -> finalizeAction(
                processFlee(queued),
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
        val target = state.combatants[action.targetId] ?: return state
        val hit = rollPhysicalHit(attacker, target)
        if (hit is HitRoll.Miss) {
            return state.logMiss(action.actorId, action.targetId)
        }
        val outgoing = adjustOutgoingDamage(attacker, hit.damage)
        val rawDamage = engine.calculateDamage(attacker, target, outgoing, PHYSICAL_ELEMENT)
        val critAdjusted = if (hit.critical) {
            (rawDamage * CombatFormulas.CRIT_DAMAGE_MULT).roundToInt().coerceAtLeast(1)
        } else rawDamage
        val finalDamage = adjustIncomingDamage(target, critAdjusted)
        return if (finalDamage <= 0) state.logMiss(action.actorId, action.targetId)
        else engine.applyDamage(
            state = state,
            attackerId = action.actorId,
            targetId = action.targetId,
            amount = finalDamage,
            element = PHYSICAL_ELEMENT
        )
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
                isHealSkill(skill) -> SkillMode.Heal(skill.basePower.coerceAtLeast(1))
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
            if (result.rp > 0) {
                working = engine.restoreResource(
                    state = working,
                    targetId = targetId,
                    amount = result.rp
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
        target: CombatantState
    ): HitRoll {
        val hitChance = (attacker.accuracyRating() - target.evasionRating())
            .coerceIn(MIN_HIT_CHANCE, MAX_HIT_CHANCE)
        val roll = Random.nextDouble(0.0, 100.0)
        if (roll > hitChance) return HitRoll.Miss
        val baseDamage = basePhysicalDamage(attacker, target)
        val critRoll = Random.nextDouble(0.0, 100.0)
        val critical = critRoll <= attacker.critChance()
        return HitRoll.Hit(baseDamage, critical)
    }

    private fun basePhysicalDamage(
        attacker: CombatantState,
        target: CombatantState
    ): Int {
        val attack = CombatFormulas.attackPower(attacker.effectiveStat("strength"))
        val defense = CombatFormulas.defensePower(target.effectiveStat("vitality"))
        val variance = Random.nextInt(PHYSICAL_VARIANCE_MIN, PHYSICAL_VARIANCE_MAX + 1)
        return (attack + variance - defense).coerceAtLeast(1)
    }

    private fun CombatantState.accuracyRating(): Double {
        val buffs = totalBuffValue("accuracy")
        return (CombatFormulas.accuracy(combatant.stats.focus) + buffs).coerceIn(0.0, 100.0)
    }

    private fun CombatantState.evasionRating(): Double {
        val buffs = totalBuffValue("evasion")
        return (CombatFormulas.evasion(combatant.stats.agility) + buffs).coerceIn(0.0, 99.5)
    }

    private fun CombatantState.critChance(): Double {
        val buffs = totalBuffValue("crit_rate")
        return (CombatFormulas.critChance(combatant.stats.focus) + buffs).coerceIn(0.0, 100.0)
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
            "agi", "agility", "speed" -> attacker.effectiveStat("agility")
            "focus", "psi", "int" -> attacker.effectiveStat("focus")
            "vit", "vitality", "def" -> attacker.effectiveStat("vitality")
            "luck", "lck" -> attacker.effectiveStat("luck")
            else -> attacker.effectiveStat("strength")
        }
        val multiplier = skill.basePower.coerceAtLeast(0) / 100.0
        return (base * multiplier).roundToInt().coerceAtLeast(if (skill.basePower > 0) 1 else 0)
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
        var flatReduction = target.totalBuffValue("defense")
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
            "defense" -> combatant.stats.vitality
            "evasion" -> combatant.stats.agility
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
            "vitality" to setOf("vit", "def", "defense"),
            "agility" to setOf("agi"),
            "focus" to setOf("fcs", "int"),
            "luck" to setOf("lck"),
            "speed" to setOf("spd"),
            "evasion" to setOf("eva")
        )
    }
}
