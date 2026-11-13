package com.example.starborn.domain.combat

import com.example.starborn.domain.model.Enemy

/** Combat state, not a wall-clock coroutine: preparation survives UI recomposition and pauses. */
data class EnemyPreparation(
    val actorId: String,
    val sourceId: String,
    val skillId: String,
    val targetIds: List<String>,
    val label: String
)

data class AuthoredEncounterState(
    val preparations: Map<String, EnemyPreparation> = emptyMap(),
    val cyclePositions: Map<String, Int> = emptyMap(),
    val phases: Map<String, String> = emptyMap(),
    val phaseOpenings: Set<String> = emptySet(),
    val brokenLinks: Set<String> = emptySet(),
    val formationSlots: Map<String, Int> = emptyMap(),
    val formationCapacity: Int = 0,
    val recovery: Set<String> = emptySet(),
    val reservesUsed: Map<String, Int> = emptyMap(),
    val remainingUses: Map<String, Map<String, Int>> = emptyMap(),
    val reserveOwners: Map<String, String> = emptyMap()
)

object AuthoredEncounterRules {
    /** The latest occupant owns a reused vacancy; fallen actors never pull survivors inward. */
    fun formationOccupants(state: CombatState): List<String?> {
        if (state.authored.formationCapacity == 0) return state.combatants.values
            .filter { it.combatant.side == CombatSide.ENEMY }.map { it.combatant.id }
        return List(state.authored.formationCapacity) { slot ->
            state.authored.formationSlots.entries.lastOrNull { it.value == slot }?.key
        }
    }

    fun linkedOwner(id: String, definitions: Map<String, Enemy>): String? =
        definitions[id]?.authored?.linkedTo?.let { canonical ->
            val candidates = definitions.filterValues { it.id == canonical }
            candidates.keys.singleOrNull() ?: candidates.keys.firstOrNull {
                it.substringAfter('#', "") == id.substringAfter('#', "")
            }
        }

    fun sourceFor(actor: String, skill: String, definitions: Map<String, Enemy>): String? {
        val canonical = definitions[actor]?.authored?.linkedActions?.get(skill) ?: return actor
        return definitions.entries.singleOrNull {
            it.value.id == canonical && (linkedOwner(it.key, definitions) == actor ||
                definitions[actor]?.authored?.coordination?.partner == canonical)
        }?.key
    }

    fun canPrepare(id: String, state: CombatState, registry: StatusRegistry): Boolean {
        val actor = state.combatants[id] ?: return false
        return actor.isAlive && actor.breakTurns == 0 && actor.statusEffects.none {
            val status = registry.definition(it.id)
            status?.blockSkills == true || !status?.skipReason.isNullOrBlank()
        }
    }

    /** Reconcile immediately after damage/status changes, before another queued attack can fire. */
    fun reconcile(state: CombatState, definitions: Map<String, Enemy>, registry: StatusRegistry, previous: CombatState? = null): CombatState {
        var runtime = state.authored
        var combatants = state.combatants
        val messages = mutableListOf<CombatLogEntry>()
        definitions.forEach { (id, definition) ->
            val link = definition.authored?.coordination ?: return@forEach
            val actor = combatants[id] ?: return@forEach
            val partner = definitions.entries.singleOrNull { it.value.id == link.partner }?.key
                ?: return@forEach
            if (state.outcome == null && actor.isAlive && id !in runtime.brokenLinks &&
                combatants[partner]?.isAlive == false) {
                runtime = runtime.copy(brokenLinks = runtime.brokenLinks + id,
                    phases = runtime.phases + (id to link.name),
                    phaseOpenings = runtime.phaseOpenings + id,
                    cyclePositions = runtime.cyclePositions + (id to 0),
                    preparations = runtime.preparations - id)
                combatants = combatants + (id to actor.copy(
                    statusEffects = actor.statusEffects.filterNot { it.id in link.removeStatuses }))
                messages += CombatLogEntry.TurnSkipped(state.round, id, link.name)
            }
        }
        definitions.forEach { (id, definition) ->
            val phase = definition.authored?.phase ?: return@forEach
            val actor = combatants[id] ?: return@forEach
            if (state.outcome == null && actor.isAlive && id !in runtime.phases && id !in runtime.brokenLinks &&
                actor.hp.toDouble() / actor.combatant.stats.maxHp < phase.hpBelow) {
                runtime = runtime.copy(phases = runtime.phases + (id to phase.name),
                    phaseOpenings = runtime.phaseOpenings + id,
                    cyclePositions = runtime.cyclePositions + (id to 0),
                    preparations = runtime.preparations - id)
                combatants = combatants + (id to actor.copy(
                    statusEffects = actor.statusEffects.filterNot { it.id in phase.removeStatuses }))
                messages += CombatLogEntry.TurnSkipped(state.round, id, phase.name)
            }
        }
        runtime.preparations.values.forEach { pending ->
            val cutPreparation = pending.skillId in definitions[pending.actorId]?.authored?.interruptOnDamage.orEmpty() &&
                previous?.combatants?.get(pending.sourceId)?.hp?.let { oldHp ->
                    state.combatants[pending.sourceId]?.hp?.let { it < oldHp }
                } == true
            if (cutPreparation || state.outcome != null || !canPrepare(pending.actorId, state, registry) ||
                !canPrepare(pending.sourceId, state, registry) ||
                pending.targetIds.none { state.combatants[it]?.isAlive == true }) {
                runtime = runtime.copy(preparations = runtime.preparations - pending.actorId,
                    recovery = runtime.recovery + pending.actorId)
                if (state.outcome == null && state.combatants[pending.actorId]?.isAlive == true)
                    messages += CombatLogEntry.TurnSkipped(state.round, pending.actorId, "${pending.label} interrupted")
            }
        }
        return if (runtime == state.authored && combatants == state.combatants && messages.isEmpty()) state
        else state.copy(authored = runtime, combatants = combatants, log = state.log + messages)
    }

    fun nextReserve(owner: String, state: CombatState, definitions: Map<String, Enemy>): String? {
        val plan = definitions[owner]?.authored?.reinforcements ?: return null
        val alive = state.combatants.count { (id, actor) ->
            id != owner && actor.isAlive && actor.combatant.side == CombatSide.ENEMY &&
                linkedOwner(id, definitions) == null
        }
        if (alive >= plan.maxAlive.coerceIn(0, 4)) return null
        return plan.pool.getOrNull(state.authored.reservesUsed[owner] ?: 0)
    }
}
