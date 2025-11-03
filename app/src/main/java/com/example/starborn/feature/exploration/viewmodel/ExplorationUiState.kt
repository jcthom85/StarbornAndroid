package com.example.starborn.feature.exploration.viewmodel

import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.World
import com.example.starborn.domain.prompt.UIPrompt
import com.example.starborn.domain.milestone.MilestoneEvent

data class ExplorationUiState(
    val isLoading: Boolean = true,
    val currentWorld: World? = null,
    val currentHub: Hub? = null,
    val currentRoom: Room? = null,
    val availableConnections: Map<String, String?> = emptyMap(),
    val npcs: List<String> = emptyList(),
    val actions: List<RoomAction> = emptyList(),
    val actionHints: Map<String, ActionHintUi> = emptyMap(),
    val enemies: List<String> = emptyList(),
    val blockedDirections: Set<String> = emptySet(),
    val roomState: Map<String, Boolean> = emptyMap(),
    val groundItems: Map<String, Int> = emptyMap(),
    val activeDialogue: DialogueUi? = null,
    val dialogueChoices: List<DialogueChoiceUi> = emptyList(),
    val statusMessage: String? = null,
    val blockedPrompt: BlockedPrompt? = null,
    val activeQuests: Set<String> = emptySet(),
    val completedQuests: Set<String> = emptySet(),
    val failedQuests: Set<String> = emptySet(),
    val trackedQuestId: String? = null,
    val questLogEntries: List<QuestLogEntryUi> = emptyList(),
    val questLogActive: List<QuestSummaryUi> = emptyList(),
    val questLogCompleted: List<QuestSummaryUi> = emptyList(),
    val completedMilestones: Set<String> = emptySet(),
    val milestoneBands: List<MilestoneBandUi> = emptyList(),
    val narrationPrompt: NarrationPrompt? = null,
    val partyStatus: PartyStatusUi = PartyStatusUi(),
    val progressionSummary: ProgressionSummaryUi = ProgressionSummaryUi(),
    val combatOutcome: CombatOutcomeUi? = null,
    val levelUpPrompt: LevelUpPrompt? = null,
    val cinematic: CinematicUiState? = null,
    val minimap: MinimapUiState? = null,
    val isMinimapLegendVisible: Boolean = false,
    val isQuestLogVisible: Boolean = false,
    val shopGreeting: ShopGreetingUi? = null,
    val pendingShopId: String? = null,
    val prompt: UIPrompt? = null,
    val milestoneHistory: List<MilestoneEvent> = emptyList(),
    val radialMenu: RadialMenuUi? = null,
    val isMilestoneGalleryVisible: Boolean = false
)

data class NarrationPrompt(
    val message: String,
    val tapToDismiss: Boolean
)

data class DialogueChoiceUi(
    val id: String,
    val label: String
)

data class LevelUpPrompt(
    val characterName: String,
    val characterId: String,
    val newLevel: Int,
    val levelsGained: Int,
    val unlockedSkills: List<SkillUnlockUi>,
    val portraitPath: String?,
    val statChanges: List<StatChangeUi>
)

data class CombatOutcomeUi(
    val outcome: com.example.starborn.navigation.CombatResultPayload.Outcome,
    val enemyIds: List<String>,
    val message: String
)

data class QuestSummaryUi(
    val id: String,
    val title: String,
    val summary: String,
    val stageTitle: String?,
    val stageDescription: String?,
    val objectives: List<String>,
    val completed: Boolean,
    val stageIndex: Int,
    val totalStages: Int
)

data class QuestLogEntryUi(
    val questId: String,
    val message: String,
    val stageId: String?,
    val stageTitle: String?,
    val timestamp: Long
)

data class CinematicUiState(
    val sceneId: String,
    val title: String?,
    val stepIndex: Int,
    val stepCount: Int,
    val step: CinematicStepUi
)

data class CinematicStepUi(
    val type: com.example.starborn.domain.cinematic.CinematicStepType,
    val speaker: String?,
    val text: String
)

data class DialogueUi(
    val line: com.example.starborn.domain.model.DialogueLine,
    val portrait: String?,
    val voiceCue: String?
)

data class MinimapUiState(
    val cells: List<MinimapCellUi>
)

data class MinimapCellUi(
    val roomId: String,
    val offsetX: Int,
    val offsetY: Int,
    val visited: Boolean,
    val discovered: Boolean,
    val isCurrent: Boolean,
    val hasEnemies: Boolean,
    val blockedDirections: Set<String>,
    val connections: Set<String>,
    val pathHints: Set<String> = emptySet(),
    val services: Set<MinimapService> = emptySet()
)

enum class MinimapService {
    SHOP,
    COOKING,
    FIRST_AID,
    TINKERING
}

data class MilestoneBandUi(
    val milestoneId: String,
    val message: String
)

data class BlockedPrompt(
    val direction: String,
    val message: String,
    val sceneId: String? = null,
    val requiresItemLabel: String? = null
)

data class ActionHintUi(
    val locked: Boolean,
    val message: String?
)

data class PartyStatusUi(
    val members: List<PartyMemberStatusUi> = emptyList()
)

data class PartyMemberStatusUi(
    val id: String,
    val name: String,
    val level: Int,
    val xpProgress: Float,
    val xpLabel: String,
    val hpLabel: String?,
    val rpLabel: String?,
    val portraitPath: String?,
    val unlockedSkills: List<String>
)

data class ProgressionSummaryUi(
    val playerLevel: Int = 1,
    val xpLabel: String = "0 XP",
    val xpProgress: Float = 0f,
    val xpToNextLabel: String? = null,
    val actionPointLabel: String = "0 AP",
    val creditsLabel: String = "0 credits",
    val hpLabel: String? = null,
    val rpLabel: String? = null
)

data class SkillUnlockUi(
    val id: String,
    val name: String,
    val description: String?
)

data class StatChangeUi(
    val label: String,
    val value: String
)

data class ShopGreetingUi(
    val shopId: String,
    val shopName: String,
    val portraitPath: String?,
    val lines: List<ShopDialogueLineUi>,
    val choices: List<ShopDialogueChoiceUi>
)

data class ShopDialogueLineUi(
    val id: String,
    val speaker: String?,
    val text: String,
    val voiceCue: String?
)

data class ShopDialogueChoiceUi(
    val id: String,
    val label: String,
    val action: ShopDialogueAction,
    val enabled: Boolean = true
)

enum class ShopDialogueAction {
    ENTER_SHOP,
    SMALLTALK,
    LEAVE
}

data class RadialMenuUi(
    val title: String,
    val items: List<RadialMenuItemUi>
)

data class RadialMenuItemUi(
    val id: String,
    val label: String,
    val description: String? = null
)
