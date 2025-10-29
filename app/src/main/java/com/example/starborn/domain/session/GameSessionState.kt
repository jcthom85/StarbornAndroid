package com.example.starborn.domain.session

data class GameSessionState(
    val worldId: String? = null,
    val hubId: String? = null,
    val roomId: String? = null,
    val playerId: String? = null,
    val activeQuests: Set<String> = emptySet(),
    val completedQuests: Set<String> = emptySet(),
    val completedMilestones: Set<String> = emptySet(),
    val learnedSchematics: Set<String> = emptySet()
)
