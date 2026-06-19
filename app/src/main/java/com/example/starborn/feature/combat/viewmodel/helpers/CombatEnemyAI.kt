package com.example.starborn.feature.combat.viewmodel.helpers

import com.example.starborn.domain.combat.CombatAction
import com.example.starborn.domain.combat.CombatAiWeights
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.combat.ElementalAffinityRules
import com.example.starborn.domain.combat.ResistanceProfile
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.feature.combat.viewmodel.SkillTargeting
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.StatusDefinition
import com.example.starborn.domain.combat.StatusEffect
import java.util.Locale
import kotlin.random.Random
import kotlin.math.roundToInt
import kotlin.collections.ArrayDeque

data class EnemyBrain(
    val behavior: CombatBehavior = CombatBehavior.BALANCED,
    val role: CombatRole = CombatRole.STRIKER
)

enum class CombatBehavior {
    AGGRESSIVE,
    DEFENSIVE,
    TRICKSTER,
    SUMMONER,
    BALANCED
}

enum class CombatRole {
    STRIKER,
    TANK,
    SUPPORT,
    CONTROLLER,
    SUMMONER
}

data class IntentWeights(
    val damage: Double,
    val heal: Double,
    val buff: Double,
    val debuff: Double,
    val guardBreak: Double,
    val summon: Double,
    val defend: Double
) {
    fun combine(other: IntentWeights): IntentWeights {
        return IntentWeights(
            damage = damage * other.damage,
            heal = heal * other.heal,
            buff = buff * other.buff,
            debuff = debuff * other.debuff,
            guardBreak = guardBreak * other.guardBreak,
            summon = summon * other.summon,
            defend = defend * other.defend
        )
    }
}

data class ScoredAction(
    val score: Double,
    val action: CombatAction
)

class CombatEnemyAI(
    private val skillById: Map<String, Skill>,
    private val enemyDefinitions: Map<String, Enemy>,
    private val statusRegistry: StatusRegistry,
    private val aiWeights: CombatAiWeights,
    private val isSupportSkill: (Skill) -> Boolean,
    private val skillStatusDefinitions: (Skill) -> List<StatusDefinition>,
    private val determineSkillTargeting: (Skill) -> SkillTargeting,
    private val checkSkillConditions: (String, Skill, CombatState, List<String>?) -> Boolean,
    private val enemyBrains: MutableMap<String, EnemyBrain>,
    private val enemyActionHistory: MutableMap<String, ArrayDeque<String>>,
    private val enemySkillUsageCounts: MutableMap<String, Int>,
    private val getPlayerIdList: () -> List<String>
) {
    private val ELEMENT_TAGS = setOf("physical", "burn", "fire", "freeze", "ice", "shock", "lightning", "acid", "poison", "corrosion", "source", "harmonic", "psychic", "psionic")
    private val BLOCKING_STATUS_IDS = setOf("invulnerable", "shield", "guard", "defend")

    private fun isBlockingStatus(statusId: String): Boolean {
        val normalized = statusId.trim().lowercase(Locale.getDefault())
        return normalized in BLOCKING_STATUS_IDS
    }

    fun canEnemyUseSkill(enemyId: String, skill: Skill, state: CombatState): Boolean {
        val enemyState = state.combatants[enemyId] ?: return false
        val isJammed = enemyState.statusEffects.any { status ->
            statusRegistry.definition(status.id)?.blockSkills == true
        }
        if (isJammed) return false
        if (enemyState.activeCooldowns.getOrDefault(skill.id, 0) > 0) return false
        skill.usesPerBattle?.let { uses ->
            val used = enemySkillUsageCounts.getOrDefault("$enemyId:${skill.id}", 0)
            if (used >= uses) return false
        }
        return checkSkillConditions(enemyId, skill, state, null)
    }

    private fun resolveTargetSetsForAi(
        skill: Skill,
        state: CombatState,
        attackerId: String
    ): List<List<String>> {
        val attackerState = state.combatants[attackerId]
        val targeting = determineSkillTargeting(skill)
        val allies = resolveFriendlyIds(state, attackerState)
        val opponents = resolveOpponentIds(state, attackerState)
        return when (targeting) {
            SkillTargeting.SELF -> listOf(listOf(attackerId))
            SkillTargeting.ALL_ALLIES -> if (allies.isNotEmpty()) listOf(allies) else listOf(listOf(attackerId))
            SkillTargeting.ALL_ENEMIES -> if (opponents.isNotEmpty()) listOf(opponents) else emptyList()
            SkillTargeting.SINGLE_ENEMY -> opponents.map { listOf(it) }
        }
    }

    private fun resolveFriendlyIds(state: CombatState, attackerState: CombatantState?): List<String> {
        val side = attackerState?.combatant?.side ?: CombatSide.PLAYER
        val sides = if (side == CombatSide.ENEMY) {
            setOf(CombatSide.ENEMY)
        } else {
            setOf(CombatSide.PLAYER, CombatSide.ALLY)
        }
        return resolveAliveIdsForSides(state, sides)
    }

    private fun resolveOpponentIds(state: CombatState, attackerState: CombatantState?): List<String> {
        val side = attackerState?.combatant?.side ?: CombatSide.PLAYER
        val sides = if (side == CombatSide.ENEMY) {
            setOf(CombatSide.PLAYER, CombatSide.ALLY)
        } else {
            setOf(CombatSide.ENEMY)
        }
        return resolveAliveIdsForSides(state, sides)
    }

    private fun resolveAliveIdsForSides(state: CombatState, sides: Set<CombatSide>): List<String> {
        return state.turnOrder.asSequence()
            .mapNotNull { state.combatants[it.combatantId] }
            .filter { it.isAlive && it.combatant.side in sides }
            .map { it.combatant.id }
            .toList()
    }

    fun parseBehavior(raw: String?): CombatBehavior? {
        val normalized = raw?.trim()?.lowercase(Locale.getDefault()) ?: return null
        return when (normalized) {
            "aggressive", "berserk", "offense" -> CombatBehavior.AGGRESSIVE
            "defensive", "defense", "guarded" -> CombatBehavior.DEFENSIVE
            "trickster", "controller", "debuffer" -> CombatBehavior.TRICKSTER
            "summoner", "summon" -> CombatBehavior.SUMMONER
            "balanced", "default" -> CombatBehavior.BALANCED
            else -> null
        }
    }

    fun parseRole(raw: String?): CombatRole? {
        val normalized = raw?.trim()?.lowercase(Locale.getDefault()) ?: return null
        return when (normalized) {
            "striker", "damage", "dps" -> CombatRole.STRIKER
            "tank", "defender", "guard" -> CombatRole.TANK
            "support", "healer" -> CombatRole.SUPPORT
            "controller", "debuffer", "trickster" -> CombatRole.CONTROLLER
            "summoner", "summon" -> CombatRole.SUMMONER
            else -> null
        }
    }

    fun inferBehavior(skills: List<Skill>): CombatBehavior {
        if (skills.isEmpty()) return CombatBehavior.BALANCED
        val tags = skills.flatMap { it.combatTags.orEmpty() }.map { it.lowercase(Locale.getDefault()) }
        if (tags.contains("summon")) return CombatBehavior.SUMMONER
        val healCount = skills.count { isSupportSkill(it) && it.combatTags.orEmpty().any { tag -> tag.equals("heal", true) } }
        val debuffCount = skills.count { it.combatTags.orEmpty().any { tag -> tag.equals("debuff", true) } }
        val damageCount = skills.count { it.basePower > 0 && !isSupportSkill(it) }
        return when {
            healCount >= 2 -> CombatBehavior.DEFENSIVE
            debuffCount >= 2 -> CombatBehavior.TRICKSTER
            damageCount >= (skills.size + 1) / 2 -> CombatBehavior.AGGRESSIVE
            else -> CombatBehavior.BALANCED
        }
    }

    fun inferRole(skills: List<Skill>): CombatRole {
        if (skills.isEmpty()) return CombatRole.STRIKER
        val tags = skills.flatMap { it.combatTags.orEmpty() }.map { it.lowercase(Locale.getDefault()) }
        val statusDefs = skills.flatMap { skillStatusDefinitions(it) }
        val summonScore = if (tags.contains("summon")) 3 else 0
        val supportScore = skills.count { isSupportSkill(it) } + statusDefs.count { it.id == "shield" || it.id == "guard" || it.id == "regen" }
        val debuffScore = tags.count { it == "debuff" } + statusDefs.count { it.target?.equals("enemy", true) == true }
        val guardScore = statusDefs.count { it.id == "shield" || it.id == "guard" }
        val damageScore = skills.count { it.basePower >= 110 && !isSupportSkill(it) } + tags.count { it == "dmg" }
        val scores = mapOf(
            CombatRole.SUMMONER to summonScore,
            CombatRole.SUPPORT to supportScore,
            CombatRole.CONTROLLER to debuffScore,
            CombatRole.TANK to guardScore,
            CombatRole.STRIKER to damageScore
        )
        return scores.maxByOrNull { it.value }?.key ?: CombatRole.STRIKER
    }

    private fun behaviorWeights(behavior: CombatBehavior): IntentWeights {
        return when (behavior) {
            CombatBehavior.AGGRESSIVE -> IntentWeights(
                damage = 1.25, heal = 0.65, buff = 0.75, debuff = 1.05, guardBreak = 1.1, summon = 0.9, defend = 0.7
            )
            CombatBehavior.DEFENSIVE -> IntentWeights(
                damage = 0.85, heal = 1.35, buff = 1.3, debuff = 0.9, guardBreak = 0.95, summon = 1.0, defend = 1.25
            )
            CombatBehavior.TRICKSTER -> IntentWeights(
                damage = 0.95, heal = 0.9, buff = 0.9, debuff = 1.4, guardBreak = 1.15, summon = 1.0, defend = 0.9
            )
            CombatBehavior.SUMMONER -> IntentWeights(
                damage = 0.8, heal = 1.1, buff = 1.0, debuff = 0.9, guardBreak = 0.9, summon = 1.5, defend = 1.0
            )
            CombatBehavior.BALANCED -> IntentWeights(
                damage = 1.0, heal = 1.0, buff = 1.0, debuff = 1.0, guardBreak = 1.0, summon = 1.0, defend = 1.0
            )
        }
    }

    private fun roleWeights(role: CombatRole): IntentWeights {
        return when (role) {
            CombatRole.STRIKER -> IntentWeights(
                damage = 1.3, heal = 0.65, buff = 0.8, debuff = 1.0, guardBreak = 1.15, summon = 0.9, defend = 0.8
            )
            CombatRole.TANK -> IntentWeights(
                damage = 0.85, heal = 1.05, buff = 1.2, debuff = 0.9, guardBreak = 1.05, summon = 0.9, defend = 1.3
            )
            CombatRole.SUPPORT -> IntentWeights(
                damage = 0.75, heal = 1.45, buff = 1.35, debuff = 1.0, guardBreak = 0.85, summon = 1.0, defend = 1.05
            )
            CombatRole.CONTROLLER -> IntentWeights(
                damage = 0.85, heal = 0.85, buff = 0.95, debuff = 1.45, guardBreak = 1.2, summon = 0.95, defend = 0.9
            )
            CombatRole.SUMMONER -> IntentWeights(
                damage = 0.8, heal = 1.0, buff = 1.0, debuff = 0.9, guardBreak = 0.9, summon = 1.4, defend = 1.0
            )
        }
    }

    private fun scoreSkill(
        skill: Skill,
        enemyState: CombatantState,
        targetIds: List<String>,
        state: CombatState,
        brain: EnemyBrain
    ): Double {
        val tags = skill.combatTags.orEmpty().map { it.lowercase(Locale.getDefault()) }
        val isSupport = isSupportSkill(skill)
        val hasDamage = skill.basePower > 0 && !isSupport
        val isHeal = tags.contains("heal") || skill.type.equals("heal", true)
        val isSummon = tags.contains("summon")
        val isGuardBreak = tags.contains("guard_break") || tags.contains("stagger")
        val isAoe = tags.any { it == "aoe" || it == "burst" || it == "multi" }
        val statusDefs = skillStatusDefinitions(skill)
        val hasStatuses = statusDefs.isNotEmpty()

        val weights = behaviorWeights(brain.behavior).combine(roleWeights(brain.role))

        var damageScore = 0.0
        if (hasDamage) {
            val element = resolveSkillElement(skill)
            targetIds.forEach { targetId ->
                val targetState = state.combatants[targetId] ?: return@forEach
                damageScore += scoreDamageTarget(skill.basePower, element, targetState)
            }
            if (isAoe) damageScore *= 0.85
        }

        var healScore = 0.0
        if (isSupport && (isHeal || skill.basePower > 0)) {
            targetIds.forEach { targetId ->
                val targetState = state.combatants[targetId] ?: return@forEach
                healScore += scoreHealTarget(skill, targetState)
            }
        }

        var statusScore = 0.0
        if (hasStatuses) {
            targetIds.forEach { targetId ->
                val targetState = state.combatants[targetId]
                statusScore += scoreStatusTargets(statusDefs, targetState, isSupport)
            }
        }

        var guardBreakScore = 0.0
        if (isGuardBreak) {
            targetIds.forEach { targetId ->
                val targetState = state.combatants[targetId] ?: return@forEach
                guardBreakScore += scoreGuardBreakTarget(targetState)
            }
        }

        var summonScore = 0.0
        if (isSummon) {
            val allyCount = resolveFriendlyIds(state, enemyState).size
            summonScore = aiWeights.summonBase + (if (state.round <= 2) aiWeights.summonEarlyRoundBonus else aiWeights.summonLateRoundBonus)
            if (allyCount < 3) summonScore += aiWeights.summonLowAllyBonus
        }

        val debuffIntent = !isSupport && hasStatuses
        val buffIntent = isSupport && hasStatuses

        var total = 0.0
        if (hasDamage) total += damageScore * weights.damage
        if (isSupport && (isHeal || skill.basePower > 0)) total += healScore * weights.heal
        if (buffIntent) total += statusScore * weights.buff
        if (debuffIntent) total += statusScore * weights.debuff
        if (isGuardBreak) total += guardBreakScore * weights.guardBreak
        if (isSummon) total += summonScore * weights.summon

        total -= skill.cooldown.coerceAtLeast(0) * aiWeights.cooldownPenaltyPerTurn
        total -= diversityPenalty(enemyState.combatant.id, skill.id)
        return total
    }

    private fun scoreBasicAttack(
        enemyState: CombatantState,
        targetId: String,
        state: CombatState,
        brain: EnemyBrain,
        lastEnemyTargetId: String?
    ): Double {
        val targetState = state.combatants[targetId] ?: return 0.0
        val base = enemyState.combatant.stats.strength * 1.1 + aiWeights.basicAttackBaseConstant
        val damageScore = scoreDamageTarget(base.roundToInt(), "physical", targetState)
        val weights = behaviorWeights(brain.behavior).combine(roleWeights(brain.role))
        val repeatPenalty = if (targetId == lastEnemyTargetId) aiWeights.basicAttackRepeatPenalty else 0.0
        return damageScore * weights.damage - repeatPenalty
    }

    private fun scoreDefend(
        enemyState: CombatantState,
        state: CombatState,
        brain: EnemyBrain
    ): Double {
        val maxHp = enemyState.combatant.stats.maxHp.coerceAtLeast(1)
        val hpRatio = enemyState.hp.toDouble() / maxHp

        val enemyDef = enemyDefinitions[enemyState.combatant.id]
        val isSmart = enemyDef?.tier.equals("elite", ignoreCase = true) ||
            enemyDef?.tier.equals("boss", ignoreCase = true)

        if (isSmart) {
            val playerCharging = state.combatants.values.any {
                (it.combatant.side == CombatSide.PLAYER || it.combatant.side == CombatSide.ALLY) &&
                    it.isAlive &&
                    it.weaponCharge != null
            }
            if (playerCharging) {
                return aiWeights.anticipationDefendBonus
            }
        }

        if (hpRatio >= 0.75) return 0.0
        var score = 0.0
        if (hpRatio < 0.6) score += aiWeights.defendHighHpBonus
        if (hpRatio < 0.4) score += aiWeights.defendMedHpBonus
        if (hpRatio < 0.25) score += aiWeights.defendLowHpBonus
        val isGuarded = enemyState.statusEffects.any { isBlockingStatus(it.id) }
        if (isGuarded) score += aiWeights.defendExistingGuardPenalty
        val weights = behaviorWeights(brain.behavior).combine(roleWeights(brain.role))
        return score * weights.defend
    }

    private fun scoreDamageTarget(
        basePower: Int,
        element: String?,
        targetState: CombatantState
    ): Double {
        if (basePower <= 0) return 0.0
        val maxHp = targetState.combatant.stats.maxHp.coerceAtLeast(1)
        val hpRatio = targetState.hp.toDouble() / maxHp
        val multiplier = affinityMultiplier(targetState, element)
        val weaknessBonus = when {
            multiplier >= 1.5 -> aiWeights.weaknessHighBonus
            multiplier > 1.0 -> aiWeights.weaknessModerateBonus
            multiplier <= 0.5 -> aiWeights.resistancePenalty
            else -> 0.0
        }
        val finishBonus = when {
            hpRatio <= 0.25 -> aiWeights.executeHighBonus
            hpRatio <= 0.45 -> aiWeights.executeModerateBonus
            else -> 0.0
        }
        return basePower * multiplier + weaknessBonus + finishBonus
    }

    private fun scoreHealTarget(skill: Skill, targetState: CombatantState): Double {
        val maxHp = targetState.combatant.stats.maxHp.coerceAtLeast(1)
        val missing = (maxHp - targetState.hp).coerceAtLeast(0)
        if (missing == 0) return 0.0
        val ratio = missing.toDouble() / maxHp
        val base = if (skill.basePower > 0) skill.basePower.toDouble() else aiWeights.healBaseScore
        return base * ratio + ratio * aiWeights.healMissingHpMultiplier
    }

    private fun scoreStatusTargets(
        statusDefs: List<StatusDefinition>,
        targetState: CombatantState?,
        isSupport: Boolean
    ): Double {
        if (statusDefs.isEmpty()) return 0.0
        val targetStatuses = targetState?.statusEffects.orEmpty()
        var total = 0.0
        statusDefs.forEach { def ->
            val normalized = def.id.lowercase(Locale.getDefault())
            val already = targetStatuses.any { it.id.equals(def.id, true) }
            val base = when (normalized) {
                "stun", "stagger" -> aiWeights.statusStun
                "blind" -> aiWeights.statusBlind
                "jammed" -> aiWeights.statusJammed
                "meltdown", "erosion", "bleed" -> aiWeights.statusDoT
                "weak", "brittle", "exposed", "short" -> aiWeights.statusDebuff
                "shield", "guard", "regen" -> aiWeights.statusBuffDefense
                "invulnerable" -> aiWeights.statusInvulnerable
                else -> aiWeights.statusDefault
            }
            val scaled = if (already) base * aiWeights.statusAlreadyAppliedMultiplier else base
            total += scaled
        }
        val sideBias = if (isSupport) 1.0 else 1.1
        return total * sideBias
    }

    private fun scoreGuardBreakTarget(targetState: CombatantState): Double {
        val isBlocking = targetState.statusEffects.any { isBlockingStatus(it.id) }
        if (isBlocking) return aiWeights.guardBreakBlocking
        val maxStability = targetState.combatant.stats.stability.coerceAtLeast(1)
        val ratio = targetState.stability.toDouble() / maxStability
        return when {
            ratio >= 0.7 -> aiWeights.guardBreakHighStability
            ratio >= 0.5 -> aiWeights.guardBreakMedStability
            else -> 0.0
        }
    }

    fun diversityPenalty(enemyId: String, skillId: String): Double {
        val history = enemyActionHistory[enemyId] ?: return 0.0
        if (history.isEmpty()) return 0.0
        val repeatCount = history.count { it == skillId }
        val lastRepeat = if (history.lastOrNull() == skillId) 1 else 0
        return repeatCount * aiWeights.diversityRepeatPenalty + lastRepeat * aiWeights.diversityConsecutivePenalty
    }

    private fun resolveSkillElement(skill: Skill): String? {
        return skill.combatTags.orEmpty()
            .map { it.lowercase(Locale.getDefault()) }
            .firstOrNull { it in ELEMENT_TAGS }
    }

    private fun affinityMultiplier(targetState: CombatantState, element: String?): Double {
        val key = element?.lowercase(Locale.getDefault())
        val profile = targetState.combatant.resistances
        val value = when (key) {
            "burn", "fire" -> profile.burn
            "freeze", "ice" -> profile.freeze
            "shock", "lightning" -> profile.shock
            "acid", "poison", "corrosion" -> profile.acid
            "source", "harmonic", "psychic", "psionic" -> profile.source
            else -> profile.physical
        }
        val tier = ElementalAffinityRules.tierForValue(value)
        return tier.multiplier
    }

    fun selectEnemyAction(
        state: CombatState,
        enemyState: CombatantState,
        lastEnemyTargetId: String?,
        setLastEnemyTargetId: (String?) -> Unit
    ): CombatAction {
        val enemyId = enemyState.combatant.id
        val brain = enemyBrains[enemyId] ?: EnemyBrain()
        val candidates = mutableListOf<ScoredAction>()

        enemyState.combatant.skills.forEach { skillId ->
            val skill = skillById[skillId] ?: return@forEach
            if (!canEnemyUseSkill(enemyId, skill, state)) return@forEach
            val targetSets = resolveTargetSetsForAi(skill, state, enemyId)
                .filter { targets -> checkSkillConditions(enemyId, skill, state, targets) }
            if (targetSets.isEmpty()) return@forEach
            targetSets.forEach { targets ->
                val score = scoreSkill(skill, enemyState, targets, state, brain)
                if (score > 0) {
                    candidates += ScoredAction(
                        score = score,
                        action = CombatAction.SkillUse(enemyId, skillId, targets)
                    )
                }
            }
        }

        val basicTargets = resolveOpponentIds(state, enemyState).ifEmpty {
            listOfNotNull(state.firstAlivePlayer(getPlayerIdList())?.combatant?.id)
        }
        if (basicTargets.isNotEmpty()) {
            val bestBasic = basicTargets.maxByOrNull { targetId ->
                scoreBasicAttack(enemyState, targetId, state, brain, lastEnemyTargetId)
            }
            if (bestBasic != null) {
                val basicScore = scoreBasicAttack(enemyState, bestBasic, state, brain, lastEnemyTargetId)
                candidates += ScoredAction(
                    score = basicScore,
                    action = CombatAction.BasicAttack(enemyId, bestBasic)
                )
            }
        }

        val defendScore = scoreDefend(enemyState, state, brain)
        if (defendScore > 0) {
            candidates += ScoredAction(score = defendScore, action = CombatAction.Defend(enemyId))
        }

        val selected = candidates.maxByOrNull { it.score }?.let { best ->
            val threshold = best.score - 3.0
            val top = candidates.filter { it.score >= threshold }
            top[Random.nextInt(top.size)]
        }

        val fallbackTarget = basicTargets.firstOrNull() ?: enemyId
        val chosen = selected?.action ?: if (fallbackTarget == enemyId) {
            CombatAction.Defend(enemyId)
        } else {
            CombatAction.BasicAttack(enemyId, fallbackTarget)
        }

        when (chosen) {
            is CombatAction.BasicAttack -> setLastEnemyTargetId(chosen.targetId)
            is CombatAction.SkillUse -> {
                if (chosen.targetIds.size == 1) {
                    setLastEnemyTargetId(chosen.targetIds.first())
                }
            }
            else -> Unit
        }

        return chosen
    }

    private fun CombatState.firstAlivePlayer(playerIdList: List<String>): CombatantState? =
        playerIdList.asSequence()
            .mapNotNull { combatants[it] }
            .firstOrNull { it.isAlive }
}
