package com.example.starborn.feature.exploration.viewmodel

import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.World

data class ExplorationUiState(
    val isLoading: Boolean = true,
    val currentWorld: World? = null,
    val currentHub: Hub? = null,
    val currentRoom: Room? = null,
    val player: Player? = null,
    val availableConnections: Map<String, String> = emptyMap(),
    val npcs: List<String> = emptyList(),
    val actions: List<RoomAction> = emptyList(),
    val enemies: List<String> = emptyList(),
    val blockedDirections: Set<String> = emptySet(),
    val roomState: Map<String, Boolean> = emptyMap(),
    val groundItems: Map<String, Int> = emptyMap(),
    val activeDialogue: com.example.starborn.domain.model.DialogueLine? = null,
    val tutorialPrompt: TutorialPrompt? = null,
    val milestonePrompt: MilestonePrompt? = null,
    val narrationPrompt: NarrationPrompt? = null,
    val activeQuests: Set<String> = emptySet(),
    val completedQuests: Set<String> = emptySet(),
    val completedMilestones: Set<String> = emptySet(),
    val questLogActive: List<QuestSummaryUi> = emptyList(),
    val questLogCompleted: List<QuestSummaryUi> = emptyList(),
    val isQuestLogVisible: Boolean = false,
    val statusMessage: String? = null
)

data class TutorialPrompt(
    val sceneId: String?,
    val context: String?,
    val message: String
)

data class QuestSummaryUi(
    val id: String,
    val title: String,
    val summary: String,
    val stageTitle: String?,
    val stageDescription: String?,
    val objectives: List<String>,
    val completed: Boolean
)

data class MilestonePrompt(
    val milestoneId: String,
    val message: String
)

data class NarrationPrompt(
    val message: String,
    val tapToDismiss: Boolean
)
