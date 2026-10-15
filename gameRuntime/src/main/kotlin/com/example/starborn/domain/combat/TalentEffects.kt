package com.example.starborn.domain.combat

import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import com.example.starborn.domain.leveling.CrewBuildPrototype as Crew
import kotlin.math.roundToInt

/** Executable talent contracts. HP bonuses never leak into independently resolved stability. */
object TalentEffects {
    fun hpMultiplier(attacker: CombatantState, target: CombatantState, skillId: String? = null, singleTargetSkill: Boolean = false): Double {
        val owned = attacker.combatant.talents
        val maxHp = target.combatant.stats.maxHp.toLong()
        var bonus = 0.0
        if (Nova.SEAM in owned && target.statusEffects.any { it.id == "weak" || it.id == "brittle" }) bonus += .15
        if (Nova.START in owned && target.hp.toLong() * 4 > maxHp * 3) bonus += .15
        if (Crew.SCOPE in owned && skillId == "gh0st_headshot" && target.hp.toLong() * 4 >= maxHp * 3) bonus += .10
        if (Crew.EXECUTE in owned && singleTargetSkill && target.hp.toLong() * 10 <= maxHp * 3) bonus += .15
        if (Crew.COOL_HEAD in owned && skillId == "gh0st_headshot" && attacker.talentTriggers.getOrDefault(Crew.COOL_HEAD, 0) > 0) bonus += .15
        if (attacker.combatant.side == CombatSide.PLAYER && target.statusEffects.any {
                it.id == Crew.OPENING_STATUS && it.sourceId != null && it.sourceId != attacker.combatant.id }) bonus += .10
        return 1.0 + bonus.coerceAtMost(.25)
    }

    fun stabilityMultiplier(attacker: CombatantState, skillId: String?, target: CombatantState? = null, physical: Boolean = false): Double {
        val owned = attacker.combatant.talents
        var bonus = 0.0
        if (skillId == "nova_arc_tether" && Nova.GROUNDED in owned) bonus += .20
        if (skillId == Nova.BREAKER_SKILL && Nova.BREAKER in owned) bonus += .50
        if (skillId == "zeke_shatter_blow" && Crew.FAULT_LINE in owned) bonus += .20
        if (skillId == "orion_prism_lance" && Crew.PRISM_LENS in owned) bonus += .20
        if (skillId == null && Crew.SURGICAL_STRIKE in owned) bonus += .20
        if (physical && Crew.CONTROLLED_DEMOLITION in owned && target?.statusEffects?.any { it.id == "weak" } == true) bonus += .15
        return 1.0 + bonus.coerceAtMost(.50)
    }

    fun canEmergencyCover(state: CombatState, actorId: String, skillId: String, targetIds: List<String>?): Boolean {
        if (skillId != Crew.COVER_SKILL) return false
        val actor = state.combatants[actorId] ?: return false
        if (!actor.isAlive || Crew.LAST_LINE !in actor.combatant.talents || actor.talentTriggers.getOrDefault(Crew.LAST_LINE, 0) > 0) return false
        val targets = targetIds?.takeIf { it.size == 1 }?.mapNotNull(state.combatants::get)
            ?: if (targetIds == null) state.combatants.values.toList() else emptyList()
        return targets.any { it.isAlive && it.combatant.side == actor.combatant.side && it.hp.toLong() * 10 < it.combatant.stats.maxHp.toLong() * 3 }
    }

    fun validSupportTarget(state: CombatState, action: CombatAction.SkillUse): Boolean {
        val actor = state.combatants[action.actorId] ?: return false
        val target = action.targetIds.singleOrNull()?.let(state.combatants::get) ?: return false
        return actor.isAlive && target.isAlive && target.combatant.side == actor.combatant.side
    }

    fun recenter(state: CombatState, actorId: String, registry: StatusRegistry): CombatState {
        val actor = state.combatants[actorId] ?: return state
        if (Crew.RECENTER !in actor.combatant.talents || actor.talentTriggers.getOrDefault(Crew.RECENTER, 0) > 0) return state
        val skipping = actor.statusEffects.filter { !registry.definition(it.id)?.skipReason.isNullOrBlank() }
        if (skipping.isEmpty() || skipping.any { it.id !in setOf("stun", "stagger") }) return state
        return state.copy(combatants = state.combatants + (actorId to actor.copy(
            statusEffects = actor.statusEffects.filterNot { it.id in setOf("stun", "stagger") },
            talentTriggers = actor.talentTriggers + (Crew.RECENTER to 1))),
            log = state.log + skipping.map { CombatLogEntry.StatusExpired(state.round, actorId, it.id) })
    }

    private fun putStatus(state: CombatState, targetId: String, id: String, duration: Int, source: String, potency: Int? = null, keepLonger: Boolean = false): CombatState {
        val actor = state.combatants[targetId] ?: return state
        val existing = actor.statusEffects.firstOrNull { it.id == id }
        val turns = if (keepLonger) maxOf(duration, existing?.remainingTurns ?: 0) else duration
        val statuses = actor.statusEffects.filterNot { it.id == id } + StatusEffect(id, turns, 1, source, potency)
        return state.copy(combatants = state.combatants + (targetId to actor.copy(statusEffects = statuses)),
            log = state.log + CombatLogEntry.StatusApplied(state.round, targetId, id, 1))
    }

    /** Called after the normal action cooldown is committed, before its end-of-turn tick. */
    fun supportSkill(before: CombatState, state: CombatState, action: CombatAction.SkillUse, engine: CombatEngine): CombatState? {
        if (action.skillId !in setOf(Crew.COVER_SKILL, Crew.TREATMENT_SKILL, Crew.PATCH_SKILL)) return null
        if (!validSupportTarget(state, action)) return state
        val actor = state.combatants.getValue(action.actorId)
        val targetId = action.targetIds.single()
        val target = state.combatants.getValue(targetId)
        val owned = actor.combatant.talents
        var working = state
        when (action.skillId) {
            Crew.COVER_SKILL -> {
                working = putStatus(working, targetId, "shield", 2, action.actorId, keepLonger = true)
                if (Crew.STEADY_COVER in owned) {
                    val current = working.combatants.getValue(targetId)
                    working = working.copy(combatants = working.combatants + (targetId to current.copy(
                        stability = (current.stability + 20).coerceAtMost(current.combatant.stats.stability))))
                }
                if (before.combatants.getValue(action.actorId).activeCooldowns.getOrDefault(Crew.COVER_SKILL, 0) > 0) {
                    val current = working.combatants.getValue(action.actorId)
                    working = working.copy(combatants = working.combatants + (action.actorId to current.copy(
                        talentTriggers = current.talentTriggers + (Crew.LAST_LINE to 1))))
                }
            }
            Crew.TREATMENT_SKILL -> {
                val emergency = Crew.TRIAGE in owned && target.hp.toLong() * 100 < target.combatant.stats.maxHp.toLong() * 35
                val percent = if (emergency) 25 else 20
                val heal = (target.combatant.stats.maxHp.toLong() * percent / 100).toInt().coerceAtLeast(1)
                working = engine.applyHeal(working, action.actorId, targetId, heal)
                if (Crew.AFTERCARE in owned && target.statusEffects.none { it.id == "regen" }) {
                    working = putStatus(working, targetId, Crew.AFTERCARE_STATUS, 2, action.actorId,
                        potency = (target.combatant.stats.maxHp / 20).coerceAtLeast(1))
                }
            }
            Crew.PATCH_SKILL -> {
                val focus = actor.combatant.stats.focus + actor.buffs.filter { it.effect.stat.lowercase() in setOf("focus", "int", "psi") }.sumOf { it.effect.value }
                working = engine.applyHeal(working, action.actorId, targetId, (18 * CombatFormulas.skillPotencyMultiplier(focus.coerceAtLeast(0))).roundToInt())
                working = putStatus(working, targetId, "shield", 1, action.actorId, keepLonger = true)
            }
        }
        return working
    }

    fun afterSkill(before: CombatState, after: CombatState, action: CombatAction.SkillUse, engine: CombatEngine): CombatState {
        val actor = before.combatants[action.actorId] ?: return after
        var working = after
        val owned = actor.combatant.talents
        action.targetIds.distinct().forEach { targetId ->
            val target = before.combatants[targetId] ?: return@forEach
            if (target.combatant.side == actor.combatant.side || after.combatants[targetId]?.isAlive != true) return@forEach
            if (action.skillId == "zeke_shatter_blow" && Crew.OPENING in owned && after.log.drop(before.log.size).any {
                    it is CombatLogEntry.StatusApplied && it.targetId == targetId && it.statusId == "weak" }) {
                working = putStatus(working, targetId, Crew.OPENING_STATUS, 2, action.actorId)
            }
            if (action.skillId == "orion_prism_lance" && Crew.SAFE_EXPOSURE in owned && target.statusEffects.none { it.id == "weak" }) {
                working = engine.applyStatus(working, targetId, "weak", if (Crew.STEADY_FIELD in owned) 2 else 1, sourceId = action.actorId)
            }
        }
        return working
    }

    fun afterDirectAction(before: CombatState, after: CombatState, action: CombatAction, engine: CombatEngine): CombatState {
        val entries = after.log.drop(before.log.size)
        if (entries.none { it is CombatLogEntry.ActionQueued && it.actorId == action.actorId } ||
            entries.any { it is CombatLogEntry.TurnSkipped && it.actorId == action.actorId }) return after
        var working = after
        val damaged = entries.filterIsInstance<CombatLogEntry.Damage>().filter { it.sourceId == action.actorId && it.amount > 0 }
        // An opening belongs to one whole action, including any multi-hit basic attack.
        damaged.map { it.targetId }.distinct().forEach { targetId ->
            val target = working.combatants[targetId] ?: return@forEach
            val mark = target.statusEffects.firstOrNull { it.id == Crew.OPENING_STATUS && it.sourceId != action.actorId }
            if (mark != null && before.combatants[action.actorId]?.combatant?.side == CombatSide.PLAYER) {
                working = working.copy(combatants = working.combatants + (targetId to target.copy(statusEffects = target.statusEffects - mark)))
            }
        }
        var actor = working.combatants[action.actorId] ?: return working
        val owned = actor.combatant.talents
        if (Crew.COOL_HEAD in owned) {
            val charge = when {
                action is CombatAction.BasicAttack && damaged.isNotEmpty() -> 1
                action is CombatAction.SkillUse && action.skillId == "gh0st_headshot" -> 0
                else -> actor.talentTriggers.getOrDefault(Crew.COOL_HEAD, 0)
            }
            actor = actor.copy(talentTriggers = actor.talentTriggers + (Crew.COOL_HEAD to charge))
            working = working.copy(combatants = working.combatants + (action.actorId to actor))
        }
        if (!actor.isAlive || Nova.MOVING !in owned || action is CombatAction.Defend || action is CombatAction.Flee) return working
        val used = actor.talentTriggers[Nova.MOVING] ?: 0
        if (used >= 2) return working
        val killed = damaged.any { hit -> before.combatants[hit.targetId]?.let { it.isAlive && it.combatant.side == CombatSide.ENEMY } == true && after.combatants[hit.targetId]?.isAlive == false }
        if (!killed) return working
        val shielded = putStatus(working, action.actorId, "shield", 1, action.actorId, keepLonger = true)
        val current = shielded.combatants.getValue(action.actorId)
        return shielded.copy(combatants = shielded.combatants + (action.actorId to current.copy(talentTriggers = current.talentTriggers + (Nova.MOVING to used + 1))))
    }
}
