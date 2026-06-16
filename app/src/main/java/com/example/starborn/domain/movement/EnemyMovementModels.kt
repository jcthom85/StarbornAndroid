package com.example.starborn.domain.movement

import com.squareup.moshi.Json

data class EnemyMovementCatalog(
    val zones: List<EnemyMovementZone> = emptyList(),
    val parties: List<EnemyMovementParty> = emptyList()
)

data class EnemyMovementZone(
    val id: String,
    val rooms: List<String>,
    @Json(name = "protected_rooms")
    val protectedRooms: List<String> = emptyList()
)

data class EnemyMovementParty(
    val id: String,
    @Json(name = "zone_id")
    val zoneId: String,
    val enemies: List<String>,
    @Json(name = "start_room")
    val startRoom: String,
    val route: List<String>,
    val behavior: String = "stationary",
    val aggression: String = "passive",
    @Json(name = "move_interval_seconds")
    val moveIntervalSeconds: Int = 25,
    @Json(name = "engage_delay_seconds")
    val engageDelaySeconds: Int = 10,
    @Json(name = "requires_active_quest")
    val requiresActiveQuest: String? = null,
    val signals: EnemyMovementSignals = EnemyMovementSignals()
)

data class EnemyMovementSignals(
    @Json(name = "enter_room")
    val enterRoom: String = "A hostile patrol enters the room.",
    @Json(name = "leave_room")
    val leaveRoom: String = "The patrol moves on.",
    val adjacent: String = "Heavy steps echo nearby."
)

data class EnemyPartyRuntimeState(
    val roomId: String,
    val routeIndex: Int = 0,
    val routeDirection: Int = 1,
    val moveRemainingMs: Long,
    val aggressionRemainingMs: Long? = null,
    val retreatGraceRemainingMs: Long = 0,
    val defeated: Boolean = false
)

data class EnemyMovementEvent(
    val partyId: String,
    val type: Type,
    val roomId: String,
    val fromRoomId: String? = null,
    val direction: String? = null,
    val message: String
) {
    enum class Type { ENTERED, LEFT, ADJACENT }
}

data class EnemyAggressionState(
    val partyId: String,
    val enemyIds: List<String>,
    val aggression: String,
    val remainingMs: Long,
    val totalMs: Long
)

data class EnemyMovementTick(
    val states: Map<String, EnemyPartyRuntimeState>,
    val events: List<EnemyMovementEvent> = emptyList(),
    val aggression: EnemyAggressionState? = null,
    val autoEngagePartyId: String? = null
)
