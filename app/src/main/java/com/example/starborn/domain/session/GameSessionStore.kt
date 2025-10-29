package com.example.starborn.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameSessionStore {

    private val _state = MutableStateFlow(GameSessionState())
    val state: StateFlow<GameSessionState> = _state.asStateFlow()

    fun setWorld(worldId: String?) {
        _state.update { it.copy(worldId = worldId) }
    }

    fun setHub(hubId: String?) {
        _state.update { it.copy(hubId = hubId) }
    }

    fun setRoom(roomId: String?) {
        _state.update { it.copy(roomId = roomId) }
    }

    fun setPlayer(playerId: String?) {
        _state.update { it.copy(playerId = playerId) }
    }

    fun startQuest(questId: String) {
        _state.update { it.copy(activeQuests = it.activeQuests + questId) }
    }

    fun completeQuest(questId: String) {
        _state.update {
            it.copy(
                activeQuests = it.activeQuests - questId,
                completedQuests = it.completedQuests + questId
            )
        }
    }

    fun setMilestone(milestoneId: String) {
        _state.update { it.copy(completedMilestones = it.completedMilestones + milestoneId) }
    }

    fun clearMilestone(milestoneId: String) {
        _state.update { it.copy(completedMilestones = it.completedMilestones - milestoneId) }
    }

    fun learnSchematic(schematicId: String) {
        if (schematicId.isBlank()) return
        _state.update { current ->
            if (schematicId in current.learnedSchematics) current
            else current.copy(learnedSchematics = current.learnedSchematics + schematicId)
        }
    }

    fun forgetSchematic(schematicId: String) {
        if (schematicId.isBlank()) return
        _state.update { it.copy(learnedSchematics = it.learnedSchematics - schematicId) }
    }
}
