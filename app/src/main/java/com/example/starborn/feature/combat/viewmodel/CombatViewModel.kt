package com.example.starborn.feature.combat.viewmodel

import com.example.starborn.domain.model.forEncounterRoom
import com.example.starborn.feature.combat.viewmodel.helpers.*
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.local.Theme
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioCueType
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.combat.CombatAction
import com.example.starborn.domain.combat.CombatActionProcessor
import com.example.starborn.domain.combat.CombatAiWeights
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.CombatFormulas
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatPlaytestBridge
import com.example.starborn.domain.combat.CombatReward
import com.example.starborn.domain.combat.CombatSetup
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.ElementalAffinityRules
import com.example.starborn.domain.combat.ElementalStackRules
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.Combatant
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.combat.CombatLogEntry
import com.example.starborn.domain.combat.CombatWeapon
import com.example.starborn.domain.combat.LootDrop
import com.example.starborn.domain.combat.ResistanceProfile
import com.example.starborn.domain.combat.StatBlock
import com.example.starborn.domain.combat.StatusEffect
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.combat.WeaponAttack
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.inventory.normalizeLootItemId
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.leveling.ProgressionData
import com.example.starborn.domain.leveling.SkillUnlockSummary
import com.example.starborn.domain.model.BuffEffect
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.SkillTreeNode
import com.example.starborn.domain.model.StatusDefinition
import com.example.starborn.domain.model.Drop
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.domain.telemetry.NoOpPlaytestTelemetry
import com.example.starborn.domain.telemetry.PlaytestTelemetry
import com.example.starborn.domain.theme.defaultWeatherForEnvironment
import java.util.Locale
import com.example.starborn.domain.combat.CombatRandom
import com.example.starborn.domain.combat.DefaultCombatRandom
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class CombatViewModel(
    worldAssets: WorldAssetDataSource,
    combatEngine: CombatEngine,
    statusRegistry: StatusRegistry,
    sessionStore: GameSessionStore,
    inventoryService: InventoryService,
    itemCatalog: ItemCatalog,
    levelingManager: LevelingManager,
    progressionData: ProgressionData,
    audioRouter: AudioRouter,
    themeRepository: ThemeRepository,
    environmentThemeManager: EnvironmentThemeManager,
    encounterCoordinator: EncounterCoordinator,
    enemyIds: List<String>,
    tutorialsEnabled: Boolean = true,
    telemetry: PlaytestTelemetry = NoOpPlaytestTelemetry,
    elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    random: CombatRandom = DefaultCombatRandom
) : ViewModel() {
    val environmentalAssets = worldAssets
    val environmentalRoom by lazy { runtime.encounterRoomId?.let { id -> worldAssets.loadRooms().firstOrNull { it.id==id } } }
    val environmentalSession = sessionStore.state
    val runtime = CombatController(
        worldAssets = worldAssets,
        combatEngine = combatEngine,
        statusRegistry = statusRegistry,
        sessionStore = sessionStore,
        inventoryService = inventoryService,
        itemCatalog = itemCatalog,
        levelingManager = levelingManager,
        progressionData = progressionData,
        audioRouter = audioRouter,
        themeRepository = themeRepository,
        environmentThemeManager = environmentThemeManager,
        encounterCoordinator = encounterCoordinator,
        enemyIds = enemyIds,
        tutorialsEnabled = tutorialsEnabled,
        telemetry = telemetry,
        elapsedRealtime = elapsedRealtime,
        random = random,
        parentScope = viewModelScope
    )

    val player: Player? get() = runtime.player
    val playerParty: List<Player> get() = runtime.playerParty
    val enemies: List<Enemy> get() = runtime.enemies
    val fxEvents: SharedFlow<CombatFxEvent> get() = runtime.fxEvents
    val environmentId: String? get() = runtime.environmentId
    val weatherId: String? get() = runtime.weatherId
    val theme: Theme? get() = runtime.theme
    val roomBackground: String? get() = runtime.roomBackground
    val locationTitle: String? get() = runtime.locationTitle
    val encounterTitle: String get() = runtime.encounterTitle
    val encounterSourcePartyId: String? get() = runtime.encounterSourcePartyId
    val encounterRoomId: String? get() = runtime.encounterRoomId
    val timedPrompt: StateFlow<CombatController.TimedPromptState?> get() = runtime.timedPrompt
    val awaitingAction: StateFlow<String?> get() = runtime.awaitingAction
    val combatBanner: StateFlow<CombatBannerMessage?> get() = runtime.combatBanner
    val combatTutorial: StateFlow<CombatTutorialState?> get() = runtime.combatTutorial
    val isOpMode: StateFlow<Boolean> get() = runtime.isOpMode
    val state: StateFlow<CombatState?> get() = runtime.state
    val combatState: CombatState? get() = runtime.combatState
    val encounterEnemyIds: List<String> get() = runtime.encounterEnemyIds
    val enemyCombatantIds: List<String> get() = runtime.enemyCombatantIds
    val selectedEnemies: StateFlow<Set<String>> get() = runtime.selectedEnemies
    val lungeActorId: StateFlow<String?> get() = runtime.lungeActorId
    val lungeToken: StateFlow<Long> get() = runtime.lungeToken
    val lungeStyle: StateFlow<AttackLungeStyle> get() = runtime.lungeStyle
    val missLungeActorId: StateFlow<String?> get() = runtime.missLungeActorId
    val missLungeToken: StateFlow<Long> get() = runtime.missLungeToken
    val inventory: StateFlow<List<InventoryEntry>> get() = runtime.inventory
    val atbMeters: StateFlow<Map<String, Float>> get() = runtime.atbMeters

    fun setBackgroundPaused(paused: Boolean) = runtime.setBackgroundPaused(paused)
    fun playerAttack(targetIdOverride: String? = null) = runtime.playerAttack(targetIdOverride)
    fun useSkill(skill: Skill, explicitTargets: List<String>? = null) = runtime.useSkill(skill, explicitTargets)
    fun onLungeFinished(token: Long) = runtime.onLungeFinished(token)
    fun onMissLungeFinished(token: Long) = runtime.onMissLungeFinished(token)
    fun useItem(entry: InventoryEntry, targetId: String? = null) = runtime.useItem(entry, targetId)
    fun snackLabel(actorId: String) = runtime.snackLabel(actorId)
    fun snackTargetRequirement(actorId: String) = runtime.snackTargetRequirement(actorId)
    fun snackCooldownRemaining(actorId: String) = runtime.snackCooldownRemaining(actorId)
    fun canUseSnack(actorId: String) = runtime.canUseSnack(actorId)
    fun useSnack(targetIdOverride: String? = null) = runtime.useSnack(targetIdOverride)
    fun attemptRetreat() = runtime.attemptRetreat()
    fun focusEnemyTarget(enemyId: String) = runtime.focusEnemyTarget(enemyId)
    fun selectReadyPlayer(actorId: String) = runtime.selectReadyPlayer(actorId)
    fun dismissActionMenu(actorId: String) = runtime.dismissActionMenu(actorId)
    fun onCombatTutorialContinue() = runtime.onCombatTutorialContinue()
    fun skipCombatTutorial() = runtime.skipCombatTutorial()
    fun onCombatTutorialCommand(command: String) = runtime.onCombatTutorialCommand(command)
    fun onCombatTutorialSkillSelected(skillId: String) = runtime.onCombatTutorialSkillSelected(skillId)
    fun onCombatTutorialSkillDialogDismissed() = runtime.onCombatTutorialSkillDialogDismissed()
    fun onCombatTutorialTargetCancelled() = runtime.onCombatTutorialTargetCancelled()
    fun isCombatTutorialCommandEnabled(command: String) = runtime.isCombatTutorialCommandEnabled(command)
    fun isCombatTutorialTargetEnabled(targetId: String) = runtime.isCombatTutorialTargetEnabled(targetId)
    fun toggleEnemyTarget(enemyId: String) = runtime.toggleEnemyTarget(enemyId)
    fun checkCombatEnd() = runtime.checkCombatEnd()
    fun skillsForPlayer(playerId: String) = runtime.skillsForPlayer(playerId)
    fun activePlayerSkills() = runtime.activePlayerSkills()
    fun canUseSkill(actorId: String, skill: Skill) = runtime.canUseSkill(actorId, skill)
    fun skillUnavailableReason(actorId: String, skill: Skill) = runtime.skillUnavailableReason(actorId, skill)
    fun skillCooldownRemaining(actorId: String, skillId: String) = runtime.skillCooldownRemaining(actorId, skillId)
    fun momentumFor(actorId: String) = runtime.momentumFor(actorId)
    fun isOverchargeReady(actorId: String) = runtime.isOverchargeReady(actorId)
    fun weaponInfusionFor(actorId: String) = runtime.weaponInfusionFor(actorId)
    fun consumeLevelUpSummaries() = runtime.consumeLevelUpSummaries()
    fun targetRequirementFor(skill: Skill) = runtime.targetRequirementFor(skill)
    fun itemDisplayName(itemId: String) = runtime.itemDisplayName(itemId)
    fun registerTimedPromptTap() = runtime.registerTimedPromptTap()
    fun onScreenReady() = runtime.onScreenReady()
    fun playVictoryMusic() = runtime.playVictoryMusic()
    fun toggleOpMode() = runtime.toggleOpMode()
    fun healPartyToFull() = runtime.healPartyToFull()
    fun instaWinBattle() = runtime.instaWinBattle()

    override fun onCleared() { runtime.close(); super.onCleared() }
}
