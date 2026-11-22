package com.example.starborn.domain.combat

import com.example.starborn.domain.model.BuffEffect
import kotlin.math.roundToInt

/**
 * Reusable, bounded combat passives for equipped accessories.
 * Passives tie into Starborn's Momentum, Stability Break, Shield/Guard,
 * Status, Healing, Snack, and ATB systems without breaking existing combat caps.
 */
object AccessoryPassives {
    const val ATB_HEADSTART_20 = "atb_headstart_20"
    const val ATB_HEADSTART_EVASION = "atb_headstart_evasion"
    const val OPENING_CRIT_12 = "opening_crit_12"
    const val GUARD_STABILITY_RESTORE = "guard_stability_restore"
    const val BREAK_BONUS_STABILITY = "break_bonus_stability"
    const val BROKEN_PUNISHER = "broken_punisher"
    const val BREAK_MOMENTUM_GAIN = "break_momentum_gain"
    const val GUARD_SHIELD_MOMENTUM = "guard_shield_momentum"
    const val OPENING_SHIELD_2 = "opening_shield_2"
    const val SHIELD_REFLECT_STABILITY = "shield_reflect_stability"
    const val HEAL_CLEANSE_1 = "heal_cleanse_1"
    const val SUPPORT_STABILITY_BOOST = "support_stability_boost"
    const val LOW_HP_REGEN = "low_hp_regen"
    const val PACK_GUARDIAN_CLUTCH = "pack_guardian_clutch"
    const val SNACK_COOLDOWN_MINUS_1 = "snack_cooldown_minus_1"
    const val STATUS_HUNTER_BONUS = "status_hunter_bonus"
    const val DEFENSE_PIERCE_25 = "defense_pierce_25"
    const val LEGENDARY_RETRO_OVERCLOCK = "legendary_retro_overclock"
    const val LEGENDARY_CADENCE_CROWN = "legendary_cadence_crown"
    const val LEGENDARY_STARBORN_SURGE = "legendary_starborn_surge"

    private const val MAX_MOMENTUM = 3
    private val STATUS_HUNTER_IDS = setOf("burn", "shock", "corrosion", "bleed", "weak", "brittle")
    private val CLEANSEABLE_DEBUFFS = setOf(
        "burn", "shock", "corrosion", "bleed", "weak", "brittle",
        "stun", "stagger", "freeze", "slow", "blind", "poison"
    )

    fun initialAtbBonus(passiveId: String?): Float = when (passiveId) {
        ATB_HEADSTART_20 -> 0.20f
        ATB_HEADSTART_EVASION -> 0.25f
        else -> 0f
    }

    fun hpBonus(attacker: CombatantState, target: CombatantState): Double = when (attacker.combatant.accessoryPassiveId) {
        BROKEN_PUNISHER -> if (target.breakTurns > 0) 0.15 else 0.0
        STATUS_HUNTER_BONUS -> if (target.statusEffects.any { it.id in STATUS_HUNTER_IDS }) 0.15 else 0.0
        else -> 0.0
    }

    fun stabilityBonus(attacker: CombatantState): Double = when (attacker.combatant.accessoryPassiveId) {
        BREAK_BONUS_STABILITY -> 0.20
        else -> 0.0
    }

    fun critChanceBonus(attacker: CombatantState, target: CombatantState): Double =
        if (attacker.combatant.accessoryPassiveId == OPENING_CRIT_12 &&
            target.hp.toLong() * 4 >= target.combatant.stats.maxHp.toLong() * 3
        ) 12.0 else 0.0

    fun critDamageMultiplier(attacker: CombatantState): Double =
        if (attacker.combatant.accessoryPassiveId == DEFENSE_PIERCE_25) 0.15 else 0.0

    fun defensePierceFraction(attacker: CombatantState?): Double =
        if (attacker?.combatant?.accessoryPassiveId == DEFENSE_PIERCE_25) 0.25 else 0.0

    fun snackCooldownReduction(actor: CombatantState): Int =
        if (actor.combatant.accessoryPassiveId == SNACK_COOLDOWN_MINUS_1) 1 else 0

    fun onBattleStart(state: CombatState): CombatState {
        var working = state
        state.combatants.values.forEach { entry ->
            val id = entry.combatant.id
            val passive = entry.combatant.accessoryPassiveId ?: return@forEach
            val label = entry.combatant.accessoryName ?: "Accessory"
            when (passive) {
                OPENING_SHIELD_2 -> {
                    working = putStatus(working, id, "shield", 2, id, keepLonger = true)
                    working = logPassive(working, id, label, "Opening Shield (2t)")
                }
                ATB_HEADSTART_EVASION -> {
                    val current = working.combatants[id] ?: return@forEach
                    val updated = current.copy(
                        buffs = current.buffs + ActiveBuff(BuffEffect("evasion", 10, 2), remainingTurns = 2)
                    )
                    working = working.copy(combatants = working.combatants + (id to updated))
                    working = logPassive(working, id, label, "+25% ATB & +10 Evasion")
                }
                LEGENDARY_RETRO_OVERCLOCK -> {
                    val current = working.combatants[id] ?: return@forEach
                    val nextMomentum = (current.momentum + 1).coerceAtMost(MAX_MOMENTUM)
                    working = working.copy(
                        combatants = working.combatants + (id to current.copy(momentum = nextMomentum))
                    )
                    working = logPassive(working, id, label, "Start +1 Momentum")
                }
            }
        }
        return working
    }

    fun onDefend(state: CombatState, actorId: String): CombatState {
        var working = state
        val actor = working.combatants[actorId] ?: return working
        val passive = actor.combatant.accessoryPassiveId ?: return working
        val label = actor.combatant.accessoryName ?: "Accessory"
        when (passive) {
            GUARD_STABILITY_RESTORE -> {
                val restoredStab = (actor.stability + 20).coerceAtMost(actor.combatant.stats.stability)
                val debuff = actor.statusEffects.firstOrNull { it.id in CLEANSEABLE_DEBUFFS }
                val nextStatuses = if (debuff != null) actor.statusEffects - debuff else actor.statusEffects
                working = working.copy(
                    combatants = working.combatants + (actorId to actor.copy(
                        stability = restoredStab,
                        statusEffects = nextStatuses
                    )),
                    log = working.log + listOfNotNull(
                        debuff?.let { CombatLogEntry.StatusExpired(working.round, actorId, it.id, cleansed = true) }
                    )
                )
                val detail = if (debuff != null) "+20 Stability & Cleansed ${debuff.id}" else "+20 Stability"
                working = logPassive(working, actorId, label, detail)
            }
            GUARD_SHIELD_MOMENTUM -> {
                val triggerKey = "acc_${GUARD_SHIELD_MOMENTUM}_r"
                if (actor.talentTriggers[triggerKey] != working.round && actor.momentum < MAX_MOMENTUM) {
                    working = working.copy(
                        combatants = working.combatants + (actorId to actor.copy(
                            momentum = (actor.momentum + 1).coerceAtMost(MAX_MOMENTUM),
                            talentTriggers = actor.talentTriggers + (triggerKey to working.round)
                        ))
                    )
                    working = logPassive(working, actorId, label, "+1 Momentum")
                }
            }
        }
        return working
    }

    fun afterAction(
        before: CombatState,
        after: CombatState,
        action: CombatAction,
        engine: CombatEngine
    ): CombatState {
        val entries = after.log.drop(before.log.size)
        if (entries.none { it is CombatLogEntry.ActionQueued && it.actorId == action.actorId } ||
            entries.any { it is CombatLogEntry.TurnSkipped && it.actorId == action.actorId }
        ) return after

        var working = after
        val actorBefore = before.combatants[action.actorId]
        val actorAfter = working.combatants[action.actorId]
        val actorPassive = actorAfter?.combatant?.accessoryPassiveId
        val actorLabel = actorAfter?.combatant?.accessoryName ?: "Accessory"
        val damageEntries = entries.filterIsInstance<CombatLogEntry.Damage>().filter { it.sourceId == action.actorId && it.amount > 0 }
        val healEntries = entries.filterIsInstance<CombatLogEntry.Heal>().filter { it.sourceId == action.actorId && it.amount > 0 }

        // 1. Actor offensive / support passives
        if (actorAfter != null && actorAfter.isAlive && actorPassive != null) {
            when (actorPassive) {
                BREAK_MOMENTUM_GAIN -> {
                    val causedBreak = damageEntries.map { it.targetId }.distinct().any { tid ->
                        (before.combatants[tid]?.breakTurns ?: 0) == 0 && (working.combatants[tid]?.breakTurns ?: 0) > 0
                    }
                    val key = "acc_${BREAK_MOMENTUM_GAIN}_r"
                    val current = working.combatants[action.actorId]
                    if (causedBreak && current != null && current.talentTriggers[key] != working.round && current.momentum < MAX_MOMENTUM) {
                        working = working.copy(
                            combatants = working.combatants + (action.actorId to current.copy(
                                momentum = (current.momentum + 1).coerceAtMost(MAX_MOMENTUM),
                                talentTriggers = current.talentTriggers + (key to working.round)
                            ))
                        )
                        working = logPassive(working, action.actorId, actorLabel, "Break +1 Momentum")
                    }
                }
                STATUS_HUNTER_BONUS -> {
                    val appliedDebuff = entries.filterIsInstance<CombatLogEntry.StatusApplied>().any { applied ->
                        applied.statusId in CLEANSEABLE_DEBUFFS &&
                            working.combatants[applied.targetId]?.combatant?.side != actorAfter.combatant.side
                    }
                    val current = working.combatants[action.actorId]
                    if (appliedDebuff && current != null && current.stability < current.combatant.stats.stability) {
                        val nextStab = (current.stability + 10).coerceAtMost(current.combatant.stats.stability)
                        working = working.copy(
                            combatants = working.combatants + (action.actorId to current.copy(stability = nextStab))
                        )
                        working = logPassive(working, action.actorId, actorLabel, "+10 Stability")
                    }
                }
                HEAL_CLEANSE_1 -> {
                    val healedTargets = healEntries.map { it.targetId }.distinct()
                    val cleansedNames = mutableListOf<String>()
                    healedTargets.forEach { tid ->
                        val target = working.combatants[tid] ?: return@forEach
                        val debuff = target.statusEffects.firstOrNull { it.id in CLEANSEABLE_DEBUFFS } ?: return@forEach
                        working = working.copy(
                            combatants = working.combatants + (tid to target.copy(statusEffects = target.statusEffects - debuff)),
                            log = working.log + CombatLogEntry.StatusExpired(working.round, tid, debuff.id, cleansed = true)
                        )
                        cleansedNames += debuff.id
                    }
                    if (cleansedNames.isNotEmpty()) {
                        working = logPassive(working, action.actorId, actorLabel, "Cleansed ${cleansedNames.first()}")
                    }
                }
                SUPPORT_STABILITY_BOOST -> {
                    val shieldedTargets = entries.filterIsInstance<CombatLogEntry.StatusApplied>()
                        .filter { it.statusId == "shield" && working.combatants[it.targetId]?.combatant?.side == actorAfter.combatant.side }
                        .map { it.targetId }
                    val supportedTargets = (healEntries.map { it.targetId } + shieldedTargets).distinct()
                    var restoredAny = false
                    supportedTargets.forEach { tid ->
                        val target = working.combatants[tid] ?: return@forEach
                        if (target.isAlive && target.stability < target.combatant.stats.stability) {
                            val nextStab = (target.stability + 20).coerceAtMost(target.combatant.stats.stability)
                            working = working.copy(
                                combatants = working.combatants + (tid to target.copy(stability = nextStab))
                            )
                            restoredAny = true
                        }
                    }
                    if (restoredAny) {
                        working = logPassive(working, action.actorId, actorLabel, "+20 Ally Stability")
                    }
                }
                LEGENDARY_RETRO_OVERCLOCK -> {
                    val key = "acc_${LEGENDARY_RETRO_OVERCLOCK}_crit"
                    val landedCrit = damageEntries.any { it.critical }
                    val current = working.combatants[action.actorId]
                    if (landedCrit && current != null && (current.talentTriggers[key] ?: 0) == 0 && current.momentum < MAX_MOMENTUM) {
                        working = working.copy(
                            combatants = working.combatants + (action.actorId to current.copy(
                                momentum = (current.momentum + 1).coerceAtMost(MAX_MOMENTUM),
                                talentTriggers = current.talentTriggers + (key to 1)
                            ))
                        )
                        working = logPassive(working, action.actorId, actorLabel, "Crit +1 Momentum")
                    }
                }
                LEGENDARY_CADENCE_CROWN -> {
                    val spentThree = action is CombatAction.SkillUse &&
                        (damageEntries.any { it.momentumSpent >= 3 } || healEntries.any { it.momentumSpent >= 3 })
                    val current = working.combatants[action.actorId]
                    if (spentThree && current != null && current.activeCooldowns.isNotEmpty()) {
                        val usedSkillId = (action as CombatAction.SkillUse).skillId
                        val reduced = current.activeCooldowns.mapValues { (sid, turns) ->
                            if (sid != usedSkillId) (turns - 1).coerceAtLeast(0) else turns
                        }.filterValues { it > 0 }
                        if (reduced != current.activeCooldowns) {
                            working = working.copy(
                                combatants = working.combatants + (action.actorId to current.copy(activeCooldowns = reduced))
                            )
                            working = logPassive(working, action.actorId, actorLabel, "Other Cooldowns -1t")
                        }
                    }
                }
                LEGENDARY_STARBORN_SURGE -> {
                    val key = "acc_${LEGENDARY_STARBORN_SURGE}_r"
                    val current = working.combatants[action.actorId]
                    val reachedThree = (actorBefore?.momentum ?: 0) < MAX_MOMENTUM && (current?.momentum ?: 0) >= MAX_MOMENTUM
                    if (reachedThree && current != null && current.talentTriggers[key] != working.round) {
                        val nextStab = (current.stability + 15).coerceAtMost(current.combatant.stats.stability)
                        working = working.copy(
                            combatants = working.combatants + (action.actorId to current.copy(
                                stability = nextStab,
                                talentTriggers = current.talentTriggers + (key to working.round)
                            ))
                        )
                        working = putStatus(working, action.actorId, "shield", 1, action.actorId, keepLonger = true)
                        working = logPassive(working, action.actorId, actorLabel, "Shield (1t) & +15 Stability")
                    }
                }
            }
        }

        // 2. Reactive defensive passives when enemies damage player crew members
        if (actorBefore?.combatant?.side == CombatSide.ENEMY && damageEntries.isNotEmpty()) {
            val damagedAllies = damageEntries.map { it.targetId }.distinct()
            damagedAllies.forEach { victimId ->
                val victimBefore = before.combatants[victimId] ?: return@forEach
                val victimAfter = working.combatants[victimId] ?: return@forEach
                if (!victimAfter.isAlive || victimAfter.combatant.side == CombatSide.ENEMY) return@forEach
                val wasShielded = victimBefore.statusEffects.any { it.id == "shield" || it.id == "defending" }
                val victimPassive = victimAfter.combatant.accessoryPassiveId
                val victimLabel = victimAfter.combatant.accessoryName ?: "Accessory"

                if (wasShielded && victimPassive == GUARD_SHIELD_MOMENTUM) {
                    val key = "acc_${GUARD_SHIELD_MOMENTUM}_r"
                    if (victimAfter.talentTriggers[key] != working.round && victimAfter.momentum < MAX_MOMENTUM) {
                        working = working.copy(
                            combatants = working.combatants + (victimId to victimAfter.copy(
                                momentum = (victimAfter.momentum + 1).coerceAtMost(MAX_MOMENTUM),
                                talentTriggers = victimAfter.talentTriggers + (key to working.round)
                            ))
                        )
                        working = logPassive(working, victimId, victimLabel, "Absorbed +1 Momentum")
                    }
                }
                if (wasShielded && victimPassive == SHIELD_REFLECT_STABILITY) {
                    val attackerNow = working.combatants[action.actorId]
                    if (attackerNow != null && attackerNow.isAlive) {
                        val reflectAmt = (attackerNow.combatant.stats.stability * 0.15).roundToInt().coerceIn(18, 40)
                        val nextStab = (attackerNow.stability - reflectAmt).coerceAtLeast(0)
                        val broke = nextStab == 0 && attackerNow.breakTurns == 0
                        working = working.copy(
                            combatants = working.combatants + (action.actorId to attackerNow.copy(
                                stability = nextStab,
                                breakTurns = if (broke) attackerNow.combatant.brokenTurns.coerceAtLeast(1) else attackerNow.breakTurns
                            ))
                        )
                        working = logPassive(working, victimId, victimLabel, if (broke) "Counter Break!" else "Counter -$reflectAmt Stability")
                    }
                }
            }

            // Pack Guardian clutch: if ANY ally dropped below 35% HP, wearer gains Shield + 1 Momentum once/battle
            val anyAllyInDanger = damagedAllies.any { tid ->
                val prev = before.combatants[tid]
                val curr = working.combatants[tid]
                prev != null && curr != null && curr.isAlive &&
                    curr.combatant.side != CombatSide.ENEMY &&
                    prev.hp.toLong() * 100 >= prev.combatant.stats.maxHp.toLong() * 35 &&
                    curr.hp.toLong() * 100 < curr.combatant.stats.maxHp.toLong() * 35
            }
            if (anyAllyInDanger) {
                working.combatants.values.forEach { member ->
                    if (!member.isAlive || member.combatant.accessoryPassiveId != PACK_GUARDIAN_CLUTCH) return@forEach
                    val key = "acc_${PACK_GUARDIAN_CLUTCH}_once"
                    if ((member.talentTriggers[key] ?: 0) > 0) return@forEach
                    val mid = member.combatant.id
                    val mLabel = member.combatant.accessoryName ?: "Accessory"
                    val updated = member.copy(
                        momentum = (member.momentum + 1).coerceAtMost(MAX_MOMENTUM),
                        talentTriggers = member.talentTriggers + (key to 1)
                    )
                    working = working.copy(combatants = working.combatants + (mid to updated))
                    working = putStatus(working, mid, "shield", 2, mid, keepLonger = true)
                    working = logPassive(working, mid, mLabel, "Clutch Shield & +1 Momentum")
                }
            }
        }

        return working
    }

    fun onTurnEnd(state: CombatState, actorId: String, engine: CombatEngine): CombatState {
        val actor = state.combatants[actorId] ?: return state
        if (!actor.isAlive) return state
        val passive = actor.combatant.accessoryPassiveId ?: return state
        val label = actor.combatant.accessoryName ?: "Accessory"
        if (passive == LOW_HP_REGEN) {
            val lowHp = actor.hp.toLong() * 100 < actor.combatant.stats.maxHp.toLong() * 60
            val fortified = actor.statusEffects.any { it.id == "shield" || it.id == "defending" }
            if ((lowHp || fortified) && actor.hp < actor.combatant.stats.maxHp) {
                val healAmt = (actor.combatant.stats.maxHp * 0.06).roundToInt().coerceAtLeast(1)
                val healed = engine.applyHeal(state, actorId, actorId, healAmt)
                return logPassive(healed, actorId, label, "+$healAmt HP")
            }
        }
        return state
    }

    private fun putStatus(
        state: CombatState,
        targetId: String,
        id: String,
        duration: Int,
        source: String,
        keepLonger: Boolean = false
    ): CombatState {
        val actor = state.combatants[targetId] ?: return state
        val existing = actor.statusEffects.firstOrNull { it.id == id }
        val turns = if (keepLonger) maxOf(duration, existing?.remainingTurns ?: 0) else duration
        val statuses = actor.statusEffects.filterNot { it.id == id } + StatusEffect(id, turns, 1, source)
        return state.copy(
            combatants = state.combatants + (targetId to actor.copy(statusEffects = statuses)),
            log = state.log + CombatLogEntry.StatusApplied(state.round, targetId, id, 1)
        )
    }

    private fun logPassive(state: CombatState, actorId: String, label: String, detail: String): CombatState =
        state.copy(log = state.log + CombatLogEntry.PassiveTriggered(state.round, actorId, label, detail))
}
