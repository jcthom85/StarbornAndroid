package com.example.starborn.domain.movement

import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionState
import java.util.Locale

class EnemyMovementManager(
    private val catalog: EnemyMovementCatalog,
    rooms: List<Room>
) {
    private val roomsById = rooms.associateBy { it.id }
    private val definitionsById = catalog.parties.associateBy { it.id }
    private var states: Map<String, EnemyPartyRuntimeState> = emptyMap()

    fun restore(persisted: Map<String, EnemyPartyRuntimeState>, session: GameSessionState) {
        states = definitionsById.values.associate { definition ->
            definition.id to (persisted[definition.id] ?: initialState(definition))
        }
        deactivateUnavailable(session)
    }

    fun partiesInRoom(roomId: String, session: GameSessionState): List<EnemyMovementParty> =
        definitionsById.values.filter { definition ->
            isActive(definition, session) &&
                states[definition.id]?.let { !it.defeated && it.roomId == roomId } == true
        }

    fun party(partyId: String): EnemyMovementParty? = definitionsById[partyId]

    fun stateSnapshot(): Map<String, EnemyPartyRuntimeState> = states

    fun tick(deltaMs: Long, currentRoomId: String?, session: GameSessionState): EnemyMovementTick {
        if (deltaMs <= 0) return currentTick(currentRoomId, session)
        seedMissing(session)
        val events = mutableListOf<EnemyMovementEvent>()
        val graceProtectedAtTickStart = states
            .filterValues { it.retreatGraceRemainingMs > 0 }
            .keys
        val mutable = states.toMutableMap()

        definitionsById.values.forEach { definition ->
            if (!isActive(definition, session)) return@forEach
            val current = mutable[definition.id] ?: initialState(definition)
            if (current.defeated) return@forEach

            var updated = current.copy(
                retreatGraceRemainingMs = (current.retreatGraceRemainingMs - deltaMs).coerceAtLeast(0)
            )
            if (definition.behavior.equals("patrol", ignoreCase = true)) {
                val remaining = updated.moveRemainingMs - deltaMs
                if (remaining <= 0) {
                    val moved = movePatrol(definition, updated, mutable, session)
                    updated = moved.first
                    moved.second?.let(events::add)
                } else {
                    updated = updated.copy(moveRemainingMs = remaining)
                }
            }
            mutable[definition.id] = updated
        }

        states = mutable
        val aggression = updateAggression(deltaMs, currentRoomId, session, graceProtectedAtTickStart)
        val movementEvents = events.flatMap { event ->
            eventForPlayer(event, currentRoomId, definition = definitionsById[event.partyId])
        }
        return EnemyMovementTick(
            states = states,
            events = movementEvents,
            aggression = aggression.first,
            autoEngagePartyId = aggression.second
        )
    }

    fun onPlayerRoomChanged(currentRoomId: String?, session: GameSessionState): EnemyMovementTick {
        seedMissing(session)
        states = states.mapValues { (partyId, state) ->
            val definition = definitionsById[partyId]
            if (definition != null && state.roomId != currentRoomId) {
                state.copy(aggressionRemainingMs = null)
            } else {
                state
            }
        }
        return currentTick(currentRoomId, session)
    }

    fun markDefeated(partyId: String) {
        val state = states[partyId] ?: return
        states = states + (partyId to state.copy(defeated = true, aggressionRemainingMs = null))
    }

    fun applyRetreatGrace(partyId: String, durationMs: Long = 15_000L) {
        val state = states[partyId] ?: return
        states = states + (
            partyId to state.copy(
                aggressionRemainingMs = null,
                retreatGraceRemainingMs = durationMs.coerceAtLeast(0)
            )
        )
    }

    private fun currentTick(currentRoomId: String?, session: GameSessionState): EnemyMovementTick {
        val aggression = updateAggression(0, currentRoomId, session, emptySet())
        return EnemyMovementTick(states, aggression = aggression.first, autoEngagePartyId = aggression.second)
    }

    private fun updateAggression(
        deltaMs: Long,
        currentRoomId: String?,
        session: GameSessionState,
        graceProtectedAtTickStart: Set<String>
    ): Pair<EnemyAggressionState?, String?> {
        val party = definitionsById.values.firstOrNull { definition ->
            val state = states[definition.id] ?: return@firstOrNull false
            isActive(definition, session) &&
                state.roomId == currentRoomId &&
                state.defeated.not() &&
                state.retreatGraceRemainingMs <= 0 &&
                !definition.aggression.equals("passive", ignoreCase = true)
        } ?: run {
            states = states.mapValues { (_, state) -> state.copy(aggressionRemainingMs = null) }
            return null to null
        }
        val state = states.getValue(party.id)
        val total = party.engageDelaySeconds.coerceAtLeast(1) * 1_000L
        val effectiveDelta = if (party.id in graceProtectedAtTickStart) 0L else deltaMs
        val remaining = (state.aggressionRemainingMs ?: total) - effectiveDelta
        states = states + (party.id to state.copy(aggressionRemainingMs = remaining.coerceAtLeast(0)))
        val ui = EnemyAggressionState(
            partyId = party.id,
            enemyIds = party.enemies,
            aggression = party.aggression,
            remainingMs = remaining.coerceAtLeast(0),
            totalMs = total
        )
        return ui to party.id.takeIf { remaining <= 0 }
    }

    private fun movePatrol(
        definition: EnemyMovementParty,
        state: EnemyPartyRuntimeState,
        currentStates: Map<String, EnemyPartyRuntimeState>,
        session: GameSessionState
    ): Pair<EnemyPartyRuntimeState, EnemyMovementEvent?> {
        if (definition.route.size < 2) {
            return state.copy(moveRemainingMs = intervalMs(definition)) to null
        }
        var direction = state.routeDirection.takeIf { it != 0 } ?: 1
        var nextIndex = state.routeIndex + direction
        if (nextIndex !in definition.route.indices) {
            direction *= -1
            nextIndex = state.routeIndex + direction
        }
        if (nextIndex !in definition.route.indices) {
            return state.copy(moveRemainingMs = intervalMs(definition)) to null
        }
        val destination = definition.route[nextIndex]
        val occupied = definitionsById.values.any { other ->
            other.id != definition.id &&
                isActive(other, session) &&
                currentStates[other.id]?.let { !it.defeated && it.roomId == destination } == true
        }
        if (occupied) {
            return state.copy(moveRemainingMs = intervalMs(definition)) to null
        }
        val directionName = directionBetween(state.roomId, destination)
        return state.copy(
            roomId = destination,
            routeIndex = nextIndex,
            routeDirection = direction,
            moveRemainingMs = intervalMs(definition),
            aggressionRemainingMs = null
        ) to EnemyMovementEvent(
            partyId = definition.id,
            type = EnemyMovementEvent.Type.ENTERED,
            roomId = destination,
            fromRoomId = state.roomId,
            direction = directionName,
            message = definition.signals.enterRoom
        )
    }

    private fun eventForPlayer(
        event: EnemyMovementEvent,
        currentRoomId: String?,
        definition: EnemyMovementParty?
    ): List<EnemyMovementEvent> {
        if (currentRoomId.isNullOrBlank() || definition == null) return emptyList()
        val state = states[event.partyId] ?: return emptyList()
        if (state.roomId == currentRoomId) return listOf(event)
        if (event.fromRoomId == currentRoomId) {
            return listOf(
                event.copy(
                    type = EnemyMovementEvent.Type.LEFT,
                    roomId = currentRoomId,
                    message = definition.signals.leaveRoom
                )
            )
        }
        val currentRoom = roomsById[currentRoomId] ?: return emptyList()
        val adjacentDirection = currentRoom.connections.entries
            .firstOrNull { (_, destination) -> destination == state.roomId }
            ?.key
        return if (adjacentDirection != null) {
            listOf(
                EnemyMovementEvent(
                    partyId = event.partyId,
                    type = EnemyMovementEvent.Type.ADJACENT,
                    roomId = currentRoomId,
                    direction = adjacentDirection,
                    message = definition.signals.adjacent
                )
            )
        } else {
            emptyList()
        }
    }

    private fun directionBetween(fromRoomId: String, toRoomId: String): String? =
        roomsById[fromRoomId]?.connections?.entries?.firstOrNull { it.value == toRoomId }?.key

    private fun seedMissing(session: GameSessionState) {
        val mutable = states.toMutableMap()
        definitionsById.values.filter { isActive(it, session) }.forEach { definition ->
            mutable.putIfAbsent(definition.id, initialState(definition))
        }
        states = mutable
    }

    private fun deactivateUnavailable(session: GameSessionState) {
        states = states.filterKeys { partyId ->
            definitionsById[partyId]?.let { isActive(it, session) } == true
        }
    }

    private fun isActive(definition: EnemyMovementParty, session: GameSessionState): Boolean =
        definition.requiresActiveQuest.isNullOrBlank() ||
            session.activeQuests.any { it.equals(definition.requiresActiveQuest, ignoreCase = true) }

    private fun initialState(definition: EnemyMovementParty): EnemyPartyRuntimeState =
        EnemyPartyRuntimeState(
            roomId = definition.startRoom,
            routeIndex = definition.route.indexOf(definition.startRoom).coerceAtLeast(0),
            moveRemainingMs = intervalMs(definition)
        )

    private fun intervalMs(definition: EnemyMovementParty): Long =
        definition.moveIntervalSeconds.coerceAtLeast(1) * 1_000L
}
