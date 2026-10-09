package com.example.starborn.feature.exploration.viewmodel

import com.example.starborn.domain.model.forEncounterRoom
import com.example.starborn.feature.exploration.viewmodel.helpers.*

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.starborn.domain.session.AstraTravel
import androidx.lifecycle.viewModelScope
import com.example.starborn.core.DefaultDispatcherProvider
import com.example.starborn.core.DispatcherProvider
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.local.Theme
import com.example.starborn.data.local.ThemeStyle
import com.example.starborn.data.local.UserSettings
import com.example.starborn.data.local.UserSettingsStore
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.data.repository.ShopRepository
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.audio.VoiceoverController
import com.example.starborn.domain.cinematic.CinematicScene
import com.example.starborn.domain.cinematic.CinematicCoordinator
import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.domain.combat.CombatFormulas
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.EncounterDescriptor
import com.example.starborn.domain.combat.EncounterEnemyInstance
import com.example.starborn.domain.crafting.CraftingOutcome
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.dialogue.DialogueService
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.dialogue.DialogueSession
import com.example.starborn.domain.event.AudioLayerCommandSpec
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemUseController
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.inventory.normalizeLootItemId
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.milestone.MilestoneEvent
import com.example.starborn.domain.milestone.MilestoneRuntimeManager
import com.example.starborn.domain.movement.EnemyMovementManager
import com.example.starborn.domain.movement.EnemyMovementCatalog
import com.example.starborn.domain.movement.EnemyMovementParty
import com.example.starborn.domain.movement.MAX_ACTIVE_ENEMY_PARTIES_PER_ROOM
import com.example.starborn.domain.movement.EnemyMovementEvent
import com.example.starborn.domain.model.BlockedDirection
import com.example.starborn.domain.model.ContainerAction
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.EventAction
import com.example.starborn.domain.model.EventReward
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.QuestReward
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Hub
import com.example.starborn.domain.model.World
import com.example.starborn.domain.model.Requirement
import com.example.starborn.domain.model.RestStopAction
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomEnemyInstance
import com.example.starborn.domain.model.RoomAction
import com.example.starborn.domain.model.ShopAction
import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.SkillTreeDefinition
import com.example.starborn.domain.model.SkillTreeNode
import com.example.starborn.domain.model.TinkeringAction
import com.example.starborn.domain.model.ToggleAction
import com.example.starborn.domain.model.TravelAction
import com.example.starborn.domain.model.TuningPuzzle
import com.example.starborn.domain.model.TuningPuzzleAction
import com.example.starborn.domain.model.actionKey
import com.example.starborn.domain.prompt.UIPromptManager
import com.example.starborn.domain.prompt.TutorialPrompt
import com.example.starborn.domain.prompt.ItemBatchGrantedPrompt
import com.example.starborn.domain.prompt.ItemGrantedPrompt
import com.example.starborn.domain.quest.QuestJournalEntry
import com.example.starborn.domain.quest.QuestLogEntry
import com.example.starborn.domain.quest.QuestRuntimeManager
import com.example.starborn.domain.quest.QuestRuntimeState
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.GameSaveRepository
import com.example.starborn.domain.session.GameSessionSlotInfo
import com.example.starborn.domain.tutorial.TutorialEntry
import com.example.starborn.domain.tutorial.TutorialRuntimeState
import com.example.starborn.domain.tutorial.TutorialRuntimeManager
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.domain.telemetry.NoOpPlaytestTelemetry
import com.example.starborn.domain.telemetry.PlaytestTelemetry
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import com.example.starborn.feature.arcade.domain.ArcadeIds
import com.example.starborn.feature.arcade.domain.ArcadeService
import com.example.starborn.navigation.CombatResultPayload
import com.example.starborn.feature.mainmenu.SaveSlotSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExplorationViewModel(
    worldAssets: WorldAssetDataSource,
    sessionStore: GameSessionStore,
    dialogueService: DialogueService,
    inventoryService: InventoryService,
    craftingService: CraftingService,
    cinematicCoordinator: CinematicCoordinator,
    questRepository: QuestRepository,
    questRuntimeManager: QuestRuntimeManager,
    milestoneManager: MilestoneRuntimeManager,
    audioRouter: AudioRouter,
    voiceoverController: VoiceoverController,
    shopRepository: ShopRepository,
    themeRepository: ThemeRepository,
    environmentThemeManager: EnvironmentThemeManager,
    levelingManager: LevelingManager,
    tutorialManager: TutorialRuntimeManager,
    promptManager: UIPromptManager,
    fishingService: FishingService,
    saveRepository: GameSaveRepository,
    encounterCoordinator: EncounterCoordinator,
    userSettingsStore: UserSettingsStore,
    eventDefinitions: List<GameEvent>,
    bootstrapCinematics: List<String> = emptyList(),
    bootstrapActions: List<String> = emptyList(),
    dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    telemetry: PlaytestTelemetry = NoOpPlaytestTelemetry,
    dialogueTriggerBinder: (((String) -> Boolean)?) -> Unit = {},
    isBurgQuestSession: Boolean = false
) : ViewModel() {
    val runtime = ExplorationController(
        worldAssets = worldAssets,
        sessionStore = sessionStore,
        dialogueService = dialogueService,
        inventoryService = inventoryService,
        craftingService = craftingService,
        cinematicCoordinator = cinematicCoordinator,
        questRepository = questRepository,
        questRuntimeManager = questRuntimeManager,
        milestoneManager = milestoneManager,
        audioRouter = audioRouter,
        voiceoverController = voiceoverController,
        shopRepository = shopRepository,
        themeRepository = themeRepository,
        environmentThemeManager = environmentThemeManager,
        levelingManager = levelingManager,
        tutorialManager = tutorialManager,
        promptManager = promptManager,
        fishingService = fishingService,
        saveRepository = saveRepository,
        encounterCoordinator = encounterCoordinator,
        userSettingsStore = userSettingsStore,
        eventDefinitions = eventDefinitions,
        bootstrapCinematics = bootstrapCinematics,
        bootstrapActions = bootstrapActions,
        dispatchers = dispatchers,
        telemetry = telemetry,
        dialogueTriggerBinder = dialogueTriggerBinder,
        isBurgQuestSession = isBurgQuestSession,
        parentScope = viewModelScope
    )

    val uiState: StateFlow<ExplorationUiState> get() = runtime.uiState
    val events: SharedFlow<ExplorationEvent> get() = runtime.events

    fun setExplorationVisible(visible: Boolean) = runtime.setExplorationVisible(visible)
    fun setExplorationInteractionBlocked(blocked: Boolean) = runtime.setExplorationInteractionBlocked(blocked)
    fun dismissEventAnnouncement() = runtime.dismissEventAnnouncement()
    fun onCombatVictoryEnemiesCleared(enemyIds: List<String>) = runtime.onCombatVictoryEnemiesCleared(enemyIds)
    fun onCombatVictory(result: CombatResultPayload) = runtime.onCombatVictory(result)
    fun onCombatDefeat(enemyIds: List<String>) = runtime.onCombatDefeat(enemyIds)
    fun onCombatRetreat(enemyIds: List<String>) = runtime.onCombatRetreat(enemyIds)
    fun onCombatRetreat(result: CombatResultPayload) = runtime.onCombatRetreat(result)
    fun showStatusMessage(message: String) = runtime.showStatusMessage(message)
    fun dismissLevelUpPrompt() = runtime.dismissLevelUpPrompt()
    fun onFadeOverlayFinished(commandId: Long) = runtime.onFadeOverlayFinished(commandId)
    fun advanceCinematic() = runtime.advanceCinematic()
    fun skipCinematic() = runtime.skipCinematic()
    fun dismissAstraNavConsole() = runtime.dismissAstraNavConsole()
    fun dismissBurgQuestAstraExitDialog() = runtime.dismissBurgQuestAstraExitDialog()
    fun travelToWorldFromAstra(worldId: String, hubId: String, roomId: String, nodeId: String) = runtime.travelToWorldFromAstra(worldId, hubId, roomId, nodeId)
    fun dismissSimulationDeck() = runtime.dismissSimulationDeck()
    fun disembarkAstra() = runtime.disembarkAstra()
    fun launchSimulationCombat(enemyIds: List<String>) = runtime.launchSimulationCombat(enemyIds)
    fun dismissTapeDeck() = runtime.dismissTapeDeck()
    fun playTapeTrack(tapeId: String, trackCueId: String) = runtime.playTapeTrack(tapeId, trackCueId)
    fun stopTapeTrack() = runtime.stopTapeTrack()
    fun travel(direction: String) = runtime.travel(direction)
    fun onNpcInteraction(npcName: String) = runtime.onNpcInteraction(npcName)
    fun advanceDialogue() = runtime.advanceDialogue()
    fun onDialogueChoiceSelected(optionId: String) = runtime.onDialogueChoiceSelected(optionId)
    fun onDialogueVoiceRequested(cueId: String) = runtime.onDialogueVoiceRequested(cueId)
    fun playQuestPresentationCue(key: String) = runtime.playQuestPresentationCue(key)
    fun onActionSelected(action: RoomAction) = runtime.onActionSelected(action)
    fun onTogglePromptSelection(enable: Boolean) = runtime.onTogglePromptSelection(enable)
    fun dismissTogglePrompt() = runtime.dismissTogglePrompt()
    fun dismissTuningPuzzle() = runtime.dismissTuningPuzzle()
    fun updateTuningSlider(sliderId: String, value: Float) = runtime.updateTuningSlider(sliderId, value)
    fun submitTuningPuzzle() = runtime.submitTuningPuzzle()
    fun clearStatusMessage() = runtime.clearStatusMessage()
    fun dismissBlockedPrompt() = runtime.dismissBlockedPrompt()
    fun onBlockedPromptHint(sceneId: String) = runtime.onBlockedPromptHint(sceneId)
    fun dismissPrompt() = runtime.dismissPrompt()
    fun onTinkerTutorialStep(step: com.example.starborn.feature.crafting.TinkeringTutorialStep) = runtime.onTinkerTutorialStep(step)
    fun debugTriggerTinkeringTutorial() = runtime.debugTriggerTinkeringTutorial()
    fun dismissNarration() = runtime.dismissNarration()
    fun onFishingResult(result: FishingResultPayload) = runtime.onFishingResult(result)
    fun openMapLegend() = runtime.openMapLegend()
    fun closeMapLegend() = runtime.closeMapLegend()
    fun openQuestLog() = runtime.openQuestLog()
    fun closeQuestLog() = runtime.closeQuestLog()
    fun openQuestDetails(questId: String) = runtime.openQuestDetails(questId)
    fun closeQuestDetails() = runtime.closeQuestDetails()
    fun toggleQuestTracking(questId: String) = runtime.toggleQuestTracking(questId)
    fun openMilestoneGallery() = runtime.openMilestoneGallery()
    fun closeMilestoneGallery() = runtime.closeMilestoneGallery()
    fun openMenuOverlay(defaultTab: MenuTab? = null) = runtime.openMenuOverlay(defaultTab)
    fun closeMenuOverlay() = runtime.closeMenuOverlay()
    fun requestReturnToHub() = runtime.requestReturnToHub()
    fun openSkillTree(characterId: String) = runtime.openSkillTree(characterId)
    fun closeSkillTreeOverlay() = runtime.closeSkillTreeOverlay()
    fun openPartyMemberDetails(characterId: String) = runtime.openPartyMemberDetails(characterId)
    fun closePartyMemberDetails() = runtime.closePartyMemberDetails()
    fun unlockSkillNode(nodeId: String) = runtime.unlockSkillNode(nodeId)
    fun onMenuActionInvoked() = runtime.onMenuActionInvoked()
    fun quickSave() = runtime.quickSave()
    fun quickSaveAndReturnToTitle(onReturn: () -> Unit) = runtime.quickSaveAndReturnToTitle(onReturn)
    suspend fun saveGame(slot: Int = 1) = runtime.saveGame(slot)
    fun loadGame(slot: Int = 1) = runtime.loadGame(slot)
    fun deleteGame(slot: Int) = runtime.deleteGame(slot)
    suspend fun fetchSaveSlots() = runtime.fetchSaveSlots()
    fun placeholderSaveSlots() = runtime.placeholderSaveSlots()
    fun selectMenuTab(tab: MenuTab) = runtime.selectMenuTab(tab)
    fun updateMusicVolume(volume: Float) = runtime.updateMusicVolume(volume)
    fun updateSfxVolume(volume: Float) = runtime.updateSfxVolume(volume)
    fun updateVoiceVolume(volume: Float) = runtime.updateVoiceVolume(volume)
    fun updateTutorialsEnabled(enabled: Boolean) = runtime.updateTutorialsEnabled(enabled)
    fun setVignetteEnabled(enabled: Boolean) = runtime.setVignetteEnabled(enabled)
    fun setHighContrastMode(enabled: Boolean) = runtime.setHighContrastMode(enabled)
    fun setModernFieldMenu(enabled: Boolean) = runtime.setModernFieldMenu(enabled)
    fun setLargeTouchTargets(enabled: Boolean) = runtime.setLargeTouchTargets(enabled)
    fun setScreenshakeDisabled(disabled: Boolean) = runtime.setScreenshakeDisabled(disabled)
    fun setFlashesDisabled(disabled: Boolean) = runtime.setFlashesDisabled(disabled)
    fun setHapticsDisabled(disabled: Boolean) = runtime.setHapticsDisabled(disabled)
    fun engageEnemy(enemyId: String) = runtime.engageEnemy(enemyId)
    fun itemDisplayName(itemId: String) = runtime.itemDisplayName(itemId)
    fun roomItemDetailLabel(itemId: String) = runtime.roomItemDetailLabel(itemId)
    fun roomItemIsEquipment(itemId: String) = runtime.roomItemIsEquipment(itemId)
    fun weaponItem(itemId: String) = runtime.weaponItem(itemId)
    fun armorItem(itemId: String) = runtime.armorItem(itemId)
    fun isPreparedMeal(itemId: String) = runtime.isPreparedMeal(itemId)
    fun useInventoryItem(itemId: String, targetId: String? = null, replaceMeal: Boolean = false) = runtime.useInventoryItem(itemId, targetId, replaceMeal)
    fun equipInventoryItem(slotId: String, itemId: String?, characterId: String? = null) = runtime.equipInventoryItem(slotId, itemId, characterId)
    fun equipInventoryMod(slotId: String, itemId: String?, characterId: String? = null) = runtime.equipInventoryMod(slotId, itemId, characterId)
    fun equipWeapon(characterId: String, weaponId: String?) = runtime.equipWeapon(characterId, weaponId)
    fun equipArmor(characterId: String, armorId: String?) = runtime.equipArmor(characterId, armorId)
    fun onTinkeringClosed() = runtime.onTinkeringClosed()
    fun onTinkeringCrafted(itemId: String?) = runtime.onTinkeringCrafted(itemId)
    fun openTinkeringShortcut() = runtime.openTinkeringShortcut()
    fun collectGroundItem(itemId: String) = runtime.collectGroundItem(itemId)
    fun collectAllGroundItems() = runtime.collectAllGroundItems()
    fun enterPendingShop() = runtime.enterPendingShop()
    fun onShopChoiceSelected(choiceId: String) = runtime.onShopChoiceSelected(choiceId)
    fun dismissShopGreeting() = runtime.dismissShopGreeting()
    fun showInspection(message: String) = runtime.showInspection(message)

    override fun onCleared() { runtime.close(); super.onCleared() }
}
