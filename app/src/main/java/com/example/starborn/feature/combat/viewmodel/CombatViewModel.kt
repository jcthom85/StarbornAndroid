package com.example.starborn.feature.combat.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.local.Theme
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.combat.CombatAction
import com.example.starborn.domain.combat.CombatActionProcessor
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.CombatFormulas
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatReward
import com.example.starborn.domain.combat.CombatSetup
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.ElementalStackRules
import com.example.starborn.domain.combat.EncounterCoordinator
import com.example.starborn.domain.combat.Combatant
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.combat.CombatLogEntry
import com.example.starborn.domain.combat.CombatWeapon
import com.example.starborn.domain.combat.LootDrop
import com.example.starborn.domain.combat.ResistanceProfile
import com.example.starborn.domain.combat.StatBlock
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
import com.example.starborn.domain.theme.EnvironmentThemeManager
import java.util.Locale
import kotlin.random.Random
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
    private val combatEngine: CombatEngine,
    private val statusRegistry: StatusRegistry,
    private val sessionStore: GameSessionStore,
    private val inventoryService: InventoryService,
    private val itemCatalog: ItemCatalog,
    private val levelingManager: LevelingManager,
    private val progressionData: ProgressionData,
    private val audioRouter: AudioRouter,
    private val themeRepository: ThemeRepository,
    private val environmentThemeManager: EnvironmentThemeManager,
    private val encounterCoordinator: EncounterCoordinator,
    enemyIds: List<String>
) : ViewModel() {

    val player: Player?
    val playerParty: List<Player>
    val enemies: List<Enemy>
    private val encounterEnemySlots: List<EncounterEnemySlot>

    private val playerSkillsById: Map<String, List<Skill>>
    private val playerDefaultSkillIds: Map<String, Set<String>>
    private val playerCombatants: List<Combatant>
    private val enemyCombatants: List<Combatant>
    private val playerIdList: List<String>
    private val enemyIdList: List<String>
    private val encounterEnemyIdList: List<String>
    private val bossCoreCombatantIds: Set<String>
    private val readyQueue = mutableListOf<String>()
    private val skillById: Map<String, Skill>
    private val skillNodesById: Map<String, SkillTreeNode>
    private val actionProcessor: CombatActionProcessor
    private val enemyCooldowns: MutableMap<String, Int> = mutableMapOf()
    private val snackCooldowns: MutableMap<String, Int> = mutableMapOf()
    private val combatFxEvents = MutableSharedFlow<CombatFxEvent>(extraBufferCapacity = 16)
    private var lastEnemyTargetId: String? = null
    val fxEvents: SharedFlow<CombatFxEvent> = combatFxEvents.asSharedFlow()
    private val pendingLevelUps = mutableListOf<LevelUpSummary>()
    val environmentId: String?
    val weatherId: String?
    val theme: Theme?
    val roomBackground: String?
    private val timedPromptLock = Any()
    private var timedPromptDeferred: CompletableDeferred<Boolean>? = null
    private val _timedPrompt = MutableStateFlow<TimedPromptState?>(null)
    val timedPrompt: StateFlow<TimedPromptState?> = _timedPrompt.asStateFlow()
    private val _awaitingAction = MutableStateFlow<String?>(null)
    val awaitingAction: StateFlow<String?> = _awaitingAction.asStateFlow()
    private fun setAwaitingAction(actorId: String?) {
        val previous = _awaitingAction.value
        if (previous == actorId) return
        _awaitingAction.value = actorId
        if (actorId != null) {
            atbMenuPaused = true
        } else {
            atbMenuPaused = false
            if (!isAtbPaused()) {
                tryProcessEnemyTurns()
            }
        }
    }
    private val _combatMessage = MutableStateFlow<String?>(null)
    val combatMessage: StateFlow<String?> = _combatMessage.asStateFlow()

    private val _state = MutableStateFlow<CombatState?>(null)
    val state: StateFlow<CombatState?> = _state.asStateFlow()
    val combatState: CombatState? get() = _state.value
    private var enemyTurnJob: Job? = null
    val encounterEnemyIds: List<String> get() = encounterEnemyIdList
    val enemyCombatantIds: List<String> get() = enemyIdList
    private val _selectedEnemies = MutableStateFlow<Set<String>>(emptySet())
    val selectedEnemies: StateFlow<Set<String>> = _selectedEnemies.asStateFlow()
    private val _lungeActorId = MutableStateFlow<String?>(null)
    val lungeActorId: StateFlow<String?> = _lungeActorId.asStateFlow()
    private val _lungeToken = MutableStateFlow(0L)
    val lungeToken: StateFlow<Long> = _lungeToken.asStateFlow()
    private val _missLungeActorId = MutableStateFlow<String?>(null)
    val missLungeActorId: StateFlow<String?> = _missLungeActorId.asStateFlow()
    private val _missLungeToken = MutableStateFlow(0L)
    val missLungeToken: StateFlow<Long> = _missLungeToken.asStateFlow()
    val inventory: StateFlow<List<InventoryEntry>> = inventoryService.state

    private val _atbMeters = MutableStateFlow<Map<String, Float>>(emptyMap())
    val atbMeters: StateFlow<Map<String, Float>> = _atbMeters.asStateFlow()
    private var atbJob: Job? = null
    private var atbMenuPaused: Boolean = false
    private var atbAnimationPauses: Int = 0
    private var suppressMissLungeTargets: Set<String> = emptySet()

    private fun isAtbPaused(): Boolean = atbMenuPaused || atbAnimationPauses > 0

    private fun pauseAtbForAnimation() {
        atbAnimationPauses += 1
    }

    private fun resumeAtbForAnimation() {
        atbAnimationPauses = (atbAnimationPauses - 1).coerceAtLeast(0)
        if (!isAtbPaused()) {
            tryProcessEnemyTurns()
        }
    }

    private fun pauseForAttackAnimation(durationMs: Long = ATTACK_ANIMATION_PAUSE_MS) {
        pauseAtbForAnimation()
        viewModelScope.launch {
            delay(durationMs)
            resumeAtbForAnimation()
        }
    }

    private fun shouldPauseForAttackAnimation(action: CombatAction): Boolean {
        return when (action) {
            is CombatAction.BasicAttack -> true
            is CombatAction.SkillUse -> {
                val skill = skillById[action.skillId]
                skill == null || !isSupportTaggedSkill(skill)
            }
            is CombatAction.ItemUse -> isDamageItem(action.itemId)
            is CombatAction.SnackUse -> isDamageItem(action.snackItemId)
            is CombatAction.SupportAbility,
            is CombatAction.Defend,
            is CombatAction.Flee -> false
        }
    }

    private fun isDamageItem(itemId: String): Boolean {
        val effect = itemCatalog.findItem(itemId)?.effect ?: return false
        return effect.damage?.let { it > 0 } == true
    }

    private fun isSupportTaggedSkill(skill: Skill): Boolean {
        val supportType = skill.type.equals("support", true) || skill.type.equals("heal", true)
        if (supportType) return true
        return skill.combatTags.orEmpty().any { tag ->
            when (tag.trim().lowercase(Locale.getDefault())) {
                "support", "heal", "buff" -> true
                else -> false
            }
        }
    }

    init {
        itemCatalog.load()
        val players = worldAssets.loadCharacters()
        val allEnemies = worldAssets.loadEnemies()
        val allSkills = worldAssets.loadSkills()
        val rooms = worldAssets.loadRooms()

        val sessionSnapshot = sessionStore.state.value
        val equippedItemsSnapshot = sessionSnapshot.equippedItems
        val equippedWeaponsSnapshot = sessionSnapshot.equippedWeapons
        val equippedArmorsSnapshot = sessionSnapshot.equippedArmors
        val unlockedWeaponsSnapshot = sessionSnapshot.unlockedWeapons
        val partyIds = sessionSnapshot.partyMembers
        val resolvedParty = (if (partyIds.isEmpty()) players.take(1) else partyIds.mapNotNull { id ->
            players.find { it.id == id }
        }).ifEmpty { players.take(1) }

        playerParty = resolvedParty
        player = playerParty.firstOrNull()
        playerDefaultSkillIds = playerParty.associate { member -> member.id to member.skills.toSet() }
        val pendingEncounter = encounterCoordinator.consumePendingEncounter()
        val pendingSlots = pendingEncounter?.enemies.orEmpty()
        val slots = enemyIds
            .mapIndexedNotNull { index, id ->
                val canonicalId = id.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
                val enemy = allEnemies.find { it.id == canonicalId } ?: return@mapIndexedNotNull null
                val overrides = pendingSlots.getOrNull(index)?.takeIf { it.enemyId == canonicalId }
                EncounterEnemySlot(
                    canonicalId = canonicalId,
                    enemy = enemy,
                    overrideDrops = overrides?.overrideDrops,
                    extraDrops = overrides?.extraDrops.orEmpty()
                )
            }
        encounterEnemySlots = slots
        enemies = slots.map { it.enemy }
        encounterEnemyIdList = slots.map { it.canonicalId }

        val currentRoomId = sessionSnapshot.roomId
        val currentRoom: Room? = currentRoomId?.let { id -> rooms.firstOrNull { it.id == id } }
        environmentId = currentRoom?.env
        weatherId = currentRoom?.weather
        theme = environmentId?.let { themeRepository.getTheme(it) }
        roomBackground = currentRoom?.backgroundImage
        environmentThemeManager.apply(environmentId, weatherId)

        playerSkillsById = playerParty.associate { member ->
            member.id to allSkills.filter { it.character == member.id }
        }

        playerCombatants = playerParty.map {
            it.toCombatant(
                equippedItemsSnapshot,
                equippedWeaponsSnapshot,
                equippedArmorsSnapshot,
                unlockedWeaponsSnapshot
            )
        }
        val duplicateCounts = encounterEnemyIdList.groupingBy { it }.eachCount()
        val instanceCounters = mutableMapOf<String, Int>()
        enemyCombatants = encounterEnemySlots.map { slot ->
            val canonicalId = slot.canonicalId
            val enemy = slot.enemy
            val nextIndex = instanceCounters.getOrDefault(canonicalId, 0) + 1
            instanceCounters[canonicalId] = nextIndex
            val requireSuffix = (duplicateCounts[canonicalId] ?: 0) > 1
            val instanceId = if (requireSuffix) "${canonicalId}#$nextIndex" else canonicalId
            enemy.toCombatant(instanceId)
        }
        playerIdList = playerCombatants.map { it.id }
        enemyIdList = enemyCombatants.map { it.id }
        bossCoreCombatantIds = encounterEnemySlots.mapIndexedNotNull { index, slot ->
            val role = slot.enemy.composite?.role
            if (role.equals("core", ignoreCase = true)) {
                enemyCombatants.getOrNull(index)?.id
            } else {
                null
            }
        }.toSet()

        readyQueue.clear()
        initializeAtbMeters()

        skillById = allSkills.associateBy { it.id }
        skillNodesById = worldAssets.loadSkillNodes()
        actionProcessor = CombatActionProcessor(
            engine = combatEngine,
            statusRegistry = statusRegistry,
            skillLookup = { id -> skillById[id] },
            consumeItem = { itemId -> inventoryService.useItem(itemId) },
            itemLookup = { itemId -> itemCatalog.findItem(itemId) }
        )

        if (playerCombatants.isNotEmpty() && enemyCombatants.isNotEmpty()) {
            val seeded = combatEngine.beginEncounter(
                CombatSetup(
                    playerParty = playerCombatants,
                    enemyParty = enemyCombatants
                )
            )
            _state.value = seeded
            playBattleCue("start")
            startAtbTicker()
        }
        refreshSelection(_state.value)
    }

    fun playerAttack(targetIdOverride: String? = null) {
        val attackerId = _awaitingAction.value ?: return
        val snapshot = _state.value ?: return
        val attackerState = snapshot.combatants[attackerId]
        if (attackerState?.isAlive != true) {
            clearAwaitingAction(attackerId)
            return
        }
        val targetId = targetIdOverride ?: resolveEnemyTargets(snapshot).firstOrNull() ?: return
        playUiCue("confirm")
        executePlayerAttack(attackerId, targetId)
    }

    fun useSkill(skill: Skill, explicitTargets: List<String>? = null) {
        val attackerId = _awaitingAction.value ?: return
        var executed = false
        updateState { current ->
            val attackerState = current.combatants[attackerId] ?: return@updateState current
            if (!attackerState.isAlive) {
                clearAwaitingAction(attackerId)
                return@updateState current
            }
            if (skillsForPlayer(attackerId).none { it.id == skill.id }) return@updateState current
            val targets = explicitTargets?.takeIf { it.isNotEmpty() }
                ?: resolveSkillTargets(skill, current, attackerId).ifEmpty { resolveEnemyTargets(current) }
            if (targets.isEmpty()) return@updateState current
            playUiCue("confirm")
            val action = CombatAction.SkillUse(attackerId, skill.id, targets)
            val supportTargets = if (isSupportSkill(skill)) {
                targets.filter { it in playerIdList || it == attackerId }.ifEmpty { listOf(attackerId) }.distinct()
            } else {
                emptyList()
            }
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            val withRewards = resolved.applyOutcomeResults(current)
            if (supportTargets.isNotEmpty()) {
                combatFxEvents.tryEmit(
                    CombatFxEvent.SupportCue(
                        actorId = attackerId,
                        skillName = skill.name,
                        targetIds = supportTargets
                    )
                )
            }
            executed = true
            withRewards
        }
        if (executed) {
            clearAwaitingAction(attackerId)
            concludeActorTurn(attackerId)
        }
    }

    private fun maybeTriggerAttackLunge(action: CombatAction) {
        if (action is CombatAction.BasicAttack && action.actorId in playerIdList) {
            triggerAttackLunge(action.actorId)
        }
    }

    private fun triggerMissLunge(targetId: String) {
        pauseAtbForAnimation()
        _missLungeToken.value = _missLungeToken.value + 1
        _missLungeActorId.value = targetId
    }

    private fun executePlayerAttack(attackerId: String, targetId: String) {
        var executed = false
        val action = CombatAction.BasicAttack(attackerId, targetId)
        maybeTriggerAttackLunge(action)
        updateState { current ->
            val attackerState = current.combatants[attackerId] ?: return@updateState current
            val targetState = current.combatants[targetId] ?: return@updateState current
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            executed = true
            resolved.applyOutcomeResults(current)
        }
        if (executed) {
            clearAwaitingAction(attackerId)
            concludeActorTurn(attackerId)
        } else {
            removeFromReadyQueue(attackerId)
            updateActiveActorFromQueue()
        }
    }

    private fun triggerAttackLunge(actorId: String) {
        pauseAtbForAnimation()
        _lungeToken.value = _lungeToken.value + 1
        _lungeActorId.value = actorId
    }

    fun onLungeFinished(token: Long) {
        if (_lungeToken.value == token) {
            _lungeActorId.value = null
            resumeAtbForAnimation()
        }
    }

    fun onMissLungeFinished(token: Long) {
        if (_missLungeToken.value == token) {
            _missLungeActorId.value = null
            resumeAtbForAnimation()
        }
    }

    fun useItem(entry: InventoryEntry, targetId: String? = null) {
        val attackerId = _awaitingAction.value ?: return
        var executed = false
        updateState { current ->
            val attackerState = current.combatants[attackerId] ?: return@updateState current
            if (!attackerState.isAlive) {
                clearAwaitingAction(attackerId)
                return@updateState current
            }
            val resolvedTarget = targetId ?: when {
                entry.item.effect?.damage?.let { it > 0 } == true -> resolveEnemyTargets(current).firstOrNull()
                else -> null
            }
            playUiCue("confirm")
            val action = CombatAction.ItemUse(attackerId, entry.item.id, resolvedTarget)
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            executed = true
            resolved.applyOutcomeResults(current)
        }
        if (executed) {
            clearAwaitingAction(attackerId)
            concludeActorTurn(attackerId)
        }
    }

    fun snackLabel(actorId: String): String {
        val equippedItems = sessionStore.state.value.equippedItems
        val snackId = equippedItemId(actorId, "snack", equippedItems) ?: return "Snack"
        return itemCatalog.findItem(snackId)?.name ?: "Snack"
    }

    fun snackTargetRequirement(actorId: String): TargetRequirement {
        val equippedItems = sessionStore.state.value.equippedItems
        val snackId = equippedItemId(actorId, "snack", equippedItems) ?: return TargetRequirement.NONE
        val effect = itemCatalog.findItem(snackId)?.effect ?: return TargetRequirement.NONE
        return when (effect.target?.trim()?.lowercase(Locale.getDefault())) {
            "enemy" -> TargetRequirement.ENEMY
            "ally" -> TargetRequirement.ALLY
            "any" -> TargetRequirement.ANY
            "self" -> TargetRequirement.NONE
            else -> when {
                effect.damage?.let { it > 0 } == true -> TargetRequirement.ENEMY
                else -> TargetRequirement.NONE
            }
        }
    }

    fun snackCooldownRemaining(actorId: String): Int =
        snackCooldowns[actorId]?.coerceAtLeast(0) ?: 0

    fun canUseSnack(actorId: String): Boolean {
        if (snackCooldownRemaining(actorId) > 0) return false
        val equippedItems = sessionStore.state.value.equippedItems
        val snackId = equippedItemId(actorId, "snack", equippedItems) ?: return false
        val item = itemCatalog.findItem(snackId) ?: return false
        return item.effect != null
    }

    fun useSnack(targetIdOverride: String? = null) {
        val attackerId = _awaitingAction.value ?: return
        if (snackCooldownRemaining(attackerId) > 0) {
            playUiCue("error")
            return
        }
        val equippedItems = sessionStore.state.value.equippedItems
        val snackId = equippedItemId(attackerId, "snack", equippedItems) ?: run {
            playUiCue("error")
            return
        }
        val snack = itemCatalog.findItem(snackId) ?: run {
            playUiCue("error")
            return
        }
        if (snack.effect == null) {
            playUiCue("error")
            return
        }

        val requirement = snackTargetRequirement(attackerId)
        var executed = false
        updateState { current ->
            val attackerState = current.combatants[attackerId] ?: return@updateState current
            if (!attackerState.isAlive) {
                clearAwaitingAction(attackerId)
                return@updateState current
            }
            val validOverride = targetIdOverride?.takeIf { candidate ->
                val snapshot = current.combatants[candidate] ?: return@takeIf false
                val isEnemy = snapshot.combatant.side == CombatSide.ENEMY
                val isAlly = snapshot.combatant.side == CombatSide.PLAYER || snapshot.combatant.side == CombatSide.ALLY
                snapshot.isAlive && when (requirement) {
                    TargetRequirement.ENEMY -> isEnemy
                    TargetRequirement.ALLY -> isAlly
                    TargetRequirement.ANY -> isEnemy || isAlly
                    TargetRequirement.NONE -> false
                }
            }
            val targetId = when (requirement) {
                TargetRequirement.ENEMY -> validOverride ?: resolveEnemyTargets(current).firstOrNull()
                TargetRequirement.ALLY -> validOverride ?: attackerId
                TargetRequirement.ANY -> validOverride ?: resolveEnemyTargets(current).firstOrNull() ?: attackerId
                TargetRequirement.NONE -> null
            }

            playUiCue("confirm")
            val action = CombatAction.SnackUse(
                actorId = attackerId,
                snackItemId = snackId,
                targetId = targetId
            )
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            executed = true
            resolved.applyOutcomeResults(current)
        }
        if (executed) {
            snackCooldowns[attackerId] = SNACK_COOLDOWN_TURNS + 1
            clearAwaitingAction(attackerId)
            concludeActorTurn(attackerId)
        }
    }

    fun supportAbilityLabel(actorId: String): String {
        val key = actorId.substringBefore('#').trim().lowercase(Locale.getDefault())
        return when (key) {
            "nova" -> "Cheap Shot"
            "zeke" -> "Synergy Pitch"
            "orion" -> "Stasis Stitch"
            "gh0st" -> "Target Lock"
            else -> "Support"
        }
    }

    fun supportTargetRequirement(actorId: String): TargetRequirement {
        val key = actorId.substringBefore('#').trim().lowercase(Locale.getDefault())
        return when (key) {
            "nova", "gh0st" -> TargetRequirement.ENEMY
            "zeke", "orion" -> TargetRequirement.ALLY
            else -> TargetRequirement.NONE
        }
    }

    fun useSupportAbility(targetIdOverride: String? = null) {
        val attackerId = _awaitingAction.value ?: return
        val supportName = supportAbilityLabel(attackerId)
        val requirement = supportTargetRequirement(attackerId)
        var executed = false
        var resolvedTargetId: String? = null
        updateState { current ->
            val attackerState = current.combatants[attackerId] ?: return@updateState current
            if (!attackerState.isAlive) {
                clearAwaitingAction(attackerId)
                return@updateState current
            }
            val validOverride = targetIdOverride?.takeIf { candidate ->
                val snapshot = current.combatants[candidate] ?: return@takeIf false
                val isEnemy = snapshot.combatant.side == CombatSide.ENEMY
                val isAlly = snapshot.combatant.side == CombatSide.PLAYER || snapshot.combatant.side == CombatSide.ALLY
                snapshot.isAlive && when (requirement) {
                    TargetRequirement.ENEMY -> isEnemy
                    TargetRequirement.ALLY -> isAlly
                    TargetRequirement.ANY -> isEnemy || isAlly
                    TargetRequirement.NONE -> false
                }
            }
            val targetId = when (requirement) {
                TargetRequirement.ENEMY -> validOverride ?: resolveEnemyTargets(current).firstOrNull()
                TargetRequirement.ALLY -> validOverride ?: attackerId
                TargetRequirement.ANY -> validOverride ?: resolveEnemyTargets(current).firstOrNull() ?: attackerId
                TargetRequirement.NONE -> attackerId
            } ?: return@updateState current

            resolvedTargetId = targetId
            playUiCue("confirm")
            val action = CombatAction.SupportAbility(attackerId, targetId)
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            executed = true
            resolved.applyOutcomeResults(current)
        }
        val resolvedTarget = resolvedTargetId
        if (executed) {
            if (!resolvedTarget.isNullOrBlank()) {
                combatFxEvents.tryEmit(
                    CombatFxEvent.SupportCue(
                        actorId = attackerId,
                        skillName = supportName,
                        targetIds = listOf(resolvedTarget)
                    )
                )
                if (attackerId.substringBefore('#').equals("zeke", ignoreCase = true)) {
                    boostAtb(resolvedTarget, ZEKE_SUPPORT_ATB_BONUS)
                }
            }
            clearAwaitingAction(attackerId)
            concludeActorTurn(attackerId)
        }
    }

    fun attemptRetreat() {
        val attackerId = _awaitingAction.value ?: return
        var executed = false
        updateState { current ->
            val attackerState = current.combatants[attackerId] ?: return@updateState current
            if (!attackerState.isAlive) {
                clearAwaitingAction(attackerId)
                return@updateState current
            }
            playUiCue("confirm")
            val action = CombatAction.Flee(attackerId)
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            executed = true
            resolved.applyOutcomeResults(current)
        }
        if (executed) {
            clearAwaitingAction(attackerId)
            concludeActorTurn(attackerId)
        }
    }

    fun focusEnemyTarget(enemyId: String) {
        val state = _state.value ?: return
        if (enemyId !in enemyIdList) return
        if (state.combatants[enemyId]?.isAlive != true) return
        playUiCue("click")
        setSelection(setOf(enemyId))
    }

    fun selectReadyPlayer(actorId: String) {
        val state = _state.value ?: return
        if (actorId !in playerIdList) return
        if (!readyQueue.contains(actorId)) return
        val actorState = state.combatants[actorId] ?: return
        if (!actorState.isAlive) return
        val index = readyQueue.indexOf(actorId)
        if (index > 0) {
            readyQueue.removeAt(index)
            readyQueue.add(0, actorId)
            updateActiveActorFromQueue(state)
        }
        setAwaitingAction(actorId)
    }

    fun dismissActionMenu(actorId: String) {
        clearAwaitingAction(actorId)
    }

    fun toggleEnemyTarget(enemyId: String) {
        val state = _state.value ?: return
        if (enemyId !in enemyIdList) return
        if (state.combatants[enemyId]?.isAlive != true) return
        val current = _selectedEnemies.value
        val updated = if (enemyId in current) {
            val remaining = current - enemyId
            if (remaining.isEmpty()) setOf(enemyId) else remaining
        } else {
            current + enemyId
        }
        playUiCue("click")
        setSelection(updated)
    }

    fun checkCombatEnd(): Boolean = combatState?.outcome != null

    fun skillsForPlayer(playerId: String): List<Skill> =
        run {
            val skills = playerSkillsById[playerId].orEmpty()
            if (skills.isEmpty()) return@run emptyList()
            val allowed = sessionStore.state.value.unlockedSkills + playerDefaultSkillIds[playerId].orEmpty()
            if (allowed.isEmpty()) {
                skills
            } else {
                skills.filter { it.id in allowed }
            }
        }

    fun activePlayerSkills(): List<Skill> =
        combatState?.let { current ->
            resolveActivePlayerId(current)?.let { skillsForPlayer(it) }
        }.orEmpty()

    fun consumeLevelUpSummaries(): List<LevelUpSummary> {
        if (pendingLevelUps.isEmpty()) return emptyList()
        val copy = pendingLevelUps.toList()
        pendingLevelUps.clear()
        return copy
    }

    private fun CombatState.firstAliveEnemy(): CombatantState? =
        enemyIdList.asSequence()
            .mapNotNull { combatants[it] }
            .firstOrNull { it.isAlive }

    private fun CombatState.firstAlivePlayer(): CombatantState? =
        playerIdList.asSequence()
            .mapNotNull { combatants[it] }
            .firstOrNull { it.isAlive }

    private fun selectEnemyAction(state: CombatState, enemyState: CombatantState): CombatAction {
        val skills = enemyState.combatant.skills
        val candidates = skills.mapNotNull { id ->
            val skill = skillById[id] ?: return@mapNotNull null
            if (enemyCooldowns.getOrDefault(id, 0) > 0) return@mapNotNull null
            id to skill
        }
        val selected = candidates.sortedByDescending { (_, skill) -> skillPriority(skill) }.firstOrNull()
        if (selected != null) {
            val (skillId, skill) = selected
            val targets = resolveSkillTargets(skill, state, enemyState.combatant.id)
                .ifEmpty { resolvePlayerTargets(state) }
            if (targets.isNotEmpty()) {
                return CombatAction.SkillUse(enemyState.combatant.id, skillId, targets)
            }
        }
        val targetId = resolvePlayerTargets(state).firstOrNull()
            ?: state.firstAlivePlayer()?.combatant?.id
            ?: enemyState.combatant.id
        return if (targetId == enemyState.combatant.id) {
            CombatAction.Defend(enemyState.combatant.id)
        } else {
            CombatAction.BasicAttack(enemyState.combatant.id, targetId)
        }
    }

    private fun guardableTargetsFor(action: CombatAction): List<String> =
        when (action) {
            is CombatAction.BasicAttack -> listOf(action.targetId)
            is CombatAction.SkillUse -> action.targetIds
            is CombatAction.ItemUse -> listOfNotNull(action.targetId)
            else -> emptyList()
        }.filter { it in playerIdList }

    private fun guardPromptMessage(targetIds: List<String>, state: CombatState): String {
        val names = targetIds.mapNotNull { id -> state.combatants[id]?.combatant?.name }.distinct()
        return when {
            names.isEmpty() -> "Guard!"
            names.size == 1 -> "Guard ${names.first()}!"
            else -> "Guard the party!"
        }
    }

    private fun tickEnemyCooldowns() {
        if (enemyCooldowns.isEmpty()) return
        enemyCooldowns.keys.toList().forEach { key ->
            val updated = (enemyCooldowns[key] ?: 0) - 1
            enemyCooldowns[key] = updated.coerceAtLeast(0)
        }
    }

    private fun registerEnemyActionCooldown(action: CombatAction) {
        if (action !is CombatAction.SkillUse) return
        val skill = skillById[action.skillId] ?: return
        val cooldown = skill.cooldown.coerceAtLeast(0)
        if (cooldown > 0) {
            enemyCooldowns[action.skillId] = cooldown
        }
    }

    private fun skillPriority(skill: Skill): Int {
        var score = 0
        if (skill.combatTags.orEmpty().any { tag ->
                tag.equals("poison", true) ||
                    tag.equals("burn", true) ||
                    tag.equals("shock", true) ||
                    tag.equals("freeze", true)
            }
        ) {
            score += 50
        }
        score += skill.basePower.coerceAtLeast(0)
        score -= skill.cooldown.coerceAtLeast(0) * 2
        return score
    }

    private fun updateState(block: (CombatState) -> CombatState) {
        val current = _state.value ?: return
        val updated = block(current)
        if (updated === current) return
        _state.value = updated
        processNewLogEntries(current, updated)
        refreshSelection(updated)
        pruneReadyActors(updated)
        updateActiveActorFromQueue(updated)
    }

    private fun processNewLogEntries(previous: CombatState, updated: CombatState) {
        val previousSize = previous.log.size
        if (updated.log.size <= previousSize) return
        val newEntries = updated.log.drop(previousSize)
        var currentAction: CombatAction? = null
        newEntries.forEach { entry ->
            when (entry) {
                is CombatLogEntry.ActionQueued -> {
                    currentAction = entry.action
                    resetAtbMeter(entry.actorId)
                    combatFxEvents.tryEmit(CombatFxEvent.TurnQueued(entry.actorId))
                    if (shouldPauseForAttackAnimation(entry.action)) {
                        pauseForAttackAnimation()
                    }
                    if (entry.actorId in playerIdList) {
                        _combatMessage.value = null
                    }
                }
                is CombatLogEntry.Damage -> {
                    if (entry.amount == 0 && entry.element == "miss") {
                        val targetIsPlayer = entry.targetId in playerIdList
                        if (!targetIsPlayer && entry.targetId !in suppressMissLungeTargets) {
                            // Only trigger the miss lunge for non-player targets; players dodging should stay put.
                            triggerMissLunge(entry.targetId)
                        }
                    }
                    val showAttackFx = shouldShowAttackFx(entry, currentAction, updated)
                    val targetState = updated.combatants[entry.targetId]
                    val targetDefeated = targetState?.isAlive == false
                    combatFxEvents.tryEmit(
                        CombatFxEvent.Impact(
                            sourceId = entry.sourceId,
                            targetId = entry.targetId,
                            amount = entry.amount,
                            element = entry.element,
                            critical = entry.critical,
                            showAttackFx = showAttackFx,
                            targetDefeated = targetDefeated
                        )
                    )
                    if (targetDefeated) {
                        combatFxEvents.tryEmit(CombatFxEvent.Knockout(entry.targetId))
                    }
                    setCombatMessage(entry, updated)
                }
                is CombatLogEntry.Heal -> {
                    combatFxEvents.tryEmit(
                        CombatFxEvent.Heal(
                            sourceId = entry.sourceId,
                            targetId = entry.targetId,
                            amount = entry.amount
                        )
                    )
                    setCombatMessage(entry, updated)
                }
                is CombatLogEntry.StatusApplied -> {
                    combatFxEvents.tryEmit(
                        CombatFxEvent.StatusApplied(
                            targetId = entry.targetId,
                            statusId = entry.statusId,
                            stacks = entry.stacks
                        )
                    )
                    setCombatMessage(entry, updated)
                }
                is CombatLogEntry.ElementStack,
                is CombatLogEntry.ElementBurst -> setCombatMessage(entry, updated)
                is CombatLogEntry.Outcome -> {
                    val outcomeType = when (entry.result) {
                        is CombatOutcome.Victory -> CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY
                        is CombatOutcome.Defeat -> CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT
                        CombatOutcome.Retreat -> CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT
                    }
                    when (outcomeType) {
                        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> playBattleCue("victory")
                        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> playBattleCue("defeat")
                        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> playBattleCue("retreat")
                    }
                    combatFxEvents.tryEmit(
                        CombatFxEvent.CombatOutcomeFx(
                            outcome = outcomeType
                        )
                    )
                    setCombatMessage(entry, updated)
                    if (updated.outcome != null) {
                        readyQueue.clear()
                        enemyTurnJob?.cancel()
                        enemyTurnJob = null
                        cancelTimedPrompt()
                        setAwaitingAction(null)
                    }
                }
                is CombatLogEntry.StatusExpired,
                is CombatLogEntry.TurnSkipped -> setCombatMessage(entry, updated)
                else -> Unit
            }
        }
    }

    private fun shouldShowAttackFx(
        entry: CombatLogEntry.Damage,
        action: CombatAction?,
        state: CombatState
    ): Boolean {
        if (entry.element == "miss") return false
        val basicAttack = action as? CombatAction.BasicAttack ?: return false
        if (entry.sourceId != basicAttack.actorId) return false
        val attackerSide = state.combatants[basicAttack.actorId]?.combatant?.side
        val targetSide = state.combatants[entry.targetId]?.combatant?.side
        val isAttackerFriendly = attackerSide == CombatSide.PLAYER || attackerSide == CombatSide.ALLY
        return isAttackerFriendly && targetSide == CombatSide.ENEMY
    }

    private fun initializeAtbMeters() {
        val initial = (playerCombatants + enemyCombatants).associate { combatant ->
            val seed = if (combatant.side == CombatSide.PLAYER || combatant.side == CombatSide.ALLY) {
                Random.nextDouble(0.4, 0.8)
            } else {
                Random.nextDouble(0.2, 0.6)
            }
            combatant.id to seed.toFloat()
        }
        _atbMeters.value = initial
    }

    private fun startAtbTicker() {
        if (atbJob != null) return
        atbJob = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(50)
                val now = SystemClock.elapsedRealtime()
                val delta = (now - last) / 1000f
                last = now
                advanceAtb(delta)
            }
        }
    }

    private fun advanceAtb(deltaSeconds: Float) {
        val currentState = _state.value ?: return
        if (currentState.outcome != null) return
        if (isAtbPaused()) return
        val meters = _atbMeters.value.toMutableMap()
        currentState.turnOrder.forEach { slot ->
            val id = slot.combatantId
            val combatantState = currentState.combatants[id] ?: return@forEach
            if (!combatantState.isAlive) {
                meters[id] = 0f
                removeFromReadyQueue(id)
                return@forEach
            }
            if (readyQueue.contains(id)) {
                meters[id] = 1f
                return@forEach
            }
            val speed = combatantState.effectiveSpeed()
            val gain = deltaSeconds * speed / ATB_SPEED_SCALE
            val current = meters.getOrElse(id) { 0f }
            val next = (current + gain).coerceIn(0f, 1f)
            meters[id] = next
            if (next >= 0.999f) {
                readyQueue.add(id)
                meters[id] = 1f
                updateActiveActorFromQueue()
            }
        }
        _atbMeters.value = meters
        tryProcessEnemyTurns()
    }

    private fun resetAtbMeter(id: String) {
        _atbMeters.update { existing ->
            if (existing.isEmpty()) existing else existing + (id to 0f)
        }
    }

    private fun CombatantState.effectiveSpeed(): Int {
        if (buffs.isEmpty()) return combatant.stats.speed.coerceAtLeast(1)
        var speedBonus = 0
        var agilityBonus = 0
        buffs.forEach { buff ->
            when (buff.effect.stat.trim().lowercase(Locale.getDefault())) {
                "speed", "spd" -> speedBonus += buff.effect.value
                "agility", "agi" -> agilityBonus += buff.effect.value
            }
        }
        val agilitySpeed = CombatFormulas.speed(0, agilityBonus).roundToInt()
        return (combatant.stats.speed + speedBonus + agilitySpeed).coerceAtLeast(1)
    }

    private fun boostAtb(targetId: String, amount: Float) {
        if (amount <= 0f) return
        val state = _state.value ?: return
        val targetState = state.combatants[targetId] ?: return
        if (!targetState.isAlive) return
        val current = _atbMeters.value[targetId] ?: 0f
        val next = (current + amount).coerceIn(0f, 1f)
        _atbMeters.update { existing ->
            if (existing.isEmpty()) existing else existing + (targetId to next)
        }
        if (next >= 0.999f && !readyQueue.contains(targetId)) {
            readyQueue.add(targetId)
            updateActiveActorFromQueue(stateOverride = state)
        }
    }

    private fun clearAwaitingAction(actorId: String) {
        if (_awaitingAction.value == actorId) {
            setAwaitingAction(null)
        }
    }

    private fun setCombatMessage(entry: CombatLogEntry, state: CombatState) {
        val message = describeEntry(entry, state)
        if (!message.isNullOrBlank()) {
            _combatMessage.value = message
        }
    }

    private fun concludeActorTurn(actorId: String) {
        tickSnackCooldown(actorId)
        removeFromReadyQueue(actorId, updateActive = false)
        resetAtbMeter(actorId)
        updateActiveActorFromQueue()
        tryProcessEnemyTurns()
    }

    private fun tickSnackCooldown(actorId: String) {
        val current = snackCooldowns[actorId] ?: return
        if (current <= 0) return
        val updated = (current - 1).coerceAtLeast(0)
        if (updated <= 0) snackCooldowns.remove(actorId) else snackCooldowns[actorId] = updated
    }

    private fun removeFromReadyQueue(actorId: String, updateActive: Boolean = true) {
        val index = readyQueue.indexOf(actorId)
        if (index >= 0) {
            readyQueue.removeAt(index)
            if (actorId in playerIdList && _awaitingAction.value == actorId) {
                setAwaitingAction(null)
            }
            if (updateActive && index == 0) {
                updateActiveActorFromQueue()
            }
        }
    }

    private fun updateActiveActorFromQueue(stateOverride: CombatState? = null) {
        val state = stateOverride ?: _state.value ?: return
        val head = readyQueue.firstOrNull() ?: return
        val idx = state.turnOrder.indexOfFirst { it.combatantId == head }
        if (idx >= 0 && state.activeTurnIndex != idx) {
            _state.value = state.copy(activeTurnIndex = idx)
        }
    }

    private fun pruneReadyActors(state: CombatState) {
        if (readyQueue.isEmpty()) return
        val iterator = readyQueue.listIterator()
        var removed = false
        while (iterator.hasNext()) {
            val id = iterator.next()
            val alive = state.combatants[id]?.isAlive == true
            if (!alive || state.outcome != null) {
                iterator.remove()
                resetAtbMeter(id)
                if (_awaitingAction.value == id) {
                    setAwaitingAction(null)
                }
                removed = true
            }
        }
        if (state.outcome != null) {
            readyQueue.clear()
            enemyTurnJob?.cancel()
            enemyTurnJob = null
            cancelTimedPrompt()
            setAwaitingAction(null)
        } else if (removed) {
            updateActiveActorFromQueue(state)
        }
    }

    private fun tryProcessEnemyTurns() {
        val state = _state.value ?: return
        if (state.outcome != null) return
        if (enemyTurnJob?.isActive == true) return
        val readyEnemy = readyQueue.firstOrNull { id ->
            id in enemyIdList && state.combatants[id]?.isAlive == true
        } ?: return
        enemyTurnJob = viewModelScope.launch {
            executeEnemyTurn(readyEnemy)
        }
    }

    private suspend fun executeEnemyTurn(enemyId: String) {
        pauseAtbForAnimation()
        try {
            val snapshot = _state.value
            val enemyStateSnapshot = snapshot?.combatants?.get(enemyId)
            if (snapshot == null || enemyStateSnapshot == null || !enemyStateSnapshot.isAlive) {
                removeFromReadyQueue(enemyId)
                updateActiveActorFromQueue()
                enemyTurnJob = null
                tryProcessEnemyTurns()
                return
            }
            val action = selectEnemyAction(snapshot, enemyStateSnapshot)
            val guardTargets = guardableTargetsFor(action).filter { id ->
                snapshot.combatants[id]?.isAlive == true
            }
            maybeTriggerAttackLunge(action)
            val guardMessage = guardTargets.takeIf { it.isNotEmpty() }?.let { guardPromptMessage(it, snapshot) }
            val guardSuccess = if (guardMessage != null) {
                awaitTimedWindow(
                    type = TimedWindowType.Defense(guardTargets),
                    message = guardMessage,
                    durationMs = TIMED_GUARD_WINDOW_MS
                )
            } else {
                false
            }

            var acted = false
            val suppressedMissTargets = if (guardSuccess) guardTargets.toSet() else emptySet()
            suppressMissLungeTargets = suppressedMissTargets
            updateState { current ->
                val enemyState = current.combatants[enemyId] ?: return@updateState current
                if (!enemyState.isAlive) return@updateState current
                tickEnemyCooldowns()
                var working = current
                if (guardSuccess) {
                    guardTargets.forEach { targetId ->
                        if (current.combatants[targetId]?.isAlive == true) {
                            working = combatEngine.applyBuffs(
                                working,
                                targetId,
                                listOf(BuffEffect(stat = "defense", value = TIMED_GUARD_DEF_BONUS, duration = 1))
                            )
                        }
                    }
                }
                val resolved = actionProcessor.execute(working, action, ::victoryReward)
                registerEnemyActionCooldown(action)
                acted = true
                resolved.applyOutcomeResults(current)
            }
            if (acted) {
                concludeActorTurn(enemyId)
            } else {
                removeFromReadyQueue(enemyId)
                updateActiveActorFromQueue()
            }
            suppressMissLungeTargets = emptySet()
            enemyTurnJob = null
            tryProcessEnemyTurns()
        } finally {
            resumeAtbForAnimation()
        }
    }

    private data class EquipmentBonuses(
        val hpBonus: Int = 0,
        val strength: Int = 0,
        val vitality: Int = 0,
        val agility: Int = 0,
        val focus: Int = 0,
        val luck: Int = 0,
        val speed: Int = 0,
        val accuracyBonus: Double = 0.0,
        val evasionBonus: Double = 0.0,
        val critBonus: Double = 0.0,
        val flatDamageReduction: Int = 0
    )

    private fun equipmentBonuses(
        characterId: String,
        equippedItems: Map<String, String>,
        equippedWeapons: Map<String, String>,
        equippedArmors: Map<String, String>
    ): EquipmentBonuses {
        var hpBonus = 0
        var strength = 0
        var vitality = 0
        var agility = 0
        var focus = 0
        var luck = 0
        var speed = 0
        var accuracyBonus = 0.0
        var evasionBonus = 0.0
        var critBonus = 0.0
        var flatDamageReduction = 0

        val baseSlots = listOf("weapon", "armor", "accessory", "snack")
        val weaponId = equippedWeaponId(characterId, equippedWeapons)
        val armorId = equippedArmorId(characterId, equippedArmors)
        val hasWeapon = !weaponId.isNullOrBlank()
        val hasArmor = !armorId.isNullOrBlank()
        val modSlots = listOf("weapon_mod1", "weapon_mod2", "armor_mod1", "armor_mod2")
        (baseSlots + modSlots).forEach { slot ->
            if (slot.startsWith("weapon_") && !hasWeapon) return@forEach
            if (slot.startsWith("armor_") && !hasArmor) return@forEach
            val itemId = when (slot) {
                "weapon" -> weaponId
                "armor" -> armorId
                else -> equippedItemId(characterId, slot, equippedItems)
            } ?: return@forEach
            val item = itemCatalog.findItem(itemId) ?: return@forEach
            val equipment = item.equipment ?: return@forEach
            hpBonus += equipment.hpBonus ?: 0
            flatDamageReduction += equipment.defense ?: 0
            accuracyBonus += equipment.accuracy ?: 0.0
            critBonus += equipment.critRate ?: 0.0
            equipment.statMods?.forEach { (stat, value) ->
                when (canonicalModKey(stat)) {
                    "strength" -> strength += value
                    "vitality" -> vitality += value
                    "agility" -> agility += value
                    "focus" -> focus += value
                    "luck" -> luck += value
                    "speed" -> speed += value
                    "accuracy" -> accuracyBonus += value
                    "evasion" -> evasionBonus += value
                    "crit_rate" -> critBonus += value
                    "flat_defense" -> flatDamageReduction += value
                }
            }
        }

        return EquipmentBonuses(
            hpBonus = hpBonus,
            strength = strength,
            vitality = vitality,
            agility = agility,
            focus = focus,
            luck = luck,
            speed = speed,
            accuracyBonus = accuracyBonus,
            evasionBonus = evasionBonus,
            critBonus = critBonus,
            flatDamageReduction = flatDamageReduction
        )
    }

    private fun canonicalModKey(raw: String): String {
        val normalized = raw.trim().lowercase(Locale.getDefault())
        return when (normalized) {
            "str", "atk", "attack" -> "strength"
            "vit" -> "vitality"
            "agi" -> "agility"
            "foc", "int", "psi" -> "focus"
            "lck" -> "luck"
            "spd" -> "speed"
            "acc" -> "accuracy"
            "eva" -> "evasion"
            "crit", "crit_chance" -> "crit_rate"
            "def", "defense", "armor", "damage_reduction", "dr" -> "flat_defense"
            else -> normalized
        }
    }

    private fun Player.toCombatant(
        equippedItems: Map<String, String>,
        equippedWeapons: Map<String, String>,
        equippedArmors: Map<String, String>,
        unlockedWeapons: Set<String>
    ): Combatant =
        run {
            val bonuses = equipmentBonuses(id, equippedItems, equippedWeapons, equippedArmors)
            val adjustedStrength = (strength + bonuses.strength).coerceAtLeast(0)
            val adjustedVitality = (vitality + bonuses.vitality).coerceAtLeast(0)
            val adjustedAgility = (agility + bonuses.agility).coerceAtLeast(0)
            val adjustedFocus = (focus + bonuses.focus).coerceAtLeast(0)
            val adjustedLuck = (luck + bonuses.luck).coerceAtLeast(0)
            val adjustedSpeed = CombatFormulas.speed(bonuses.speed, adjustedAgility).roundToInt().coerceAtLeast(0)

            Combatant(
            id = id,
            name = name,
            side = CombatSide.PLAYER,
            stats = StatBlock(
                maxHp = CombatFormulas.maxHp(hp, adjustedVitality) + bonuses.hpBonus,
                strength = adjustedStrength,
                vitality = adjustedVitality,
                agility = adjustedAgility,
                focus = adjustedFocus,
                luck = adjustedLuck,
                speed = adjustedSpeed,
                accuracyBonus = bonuses.accuracyBonus,
                evasionBonus = bonuses.evasionBonus,
                critBonus = bonuses.critBonus,
                flatDamageReduction = bonuses.flatDamageReduction
            ),
            resistances = ResistanceProfile(),
            skills = skills,
            weapon = resolveCombatWeapon(id, equippedItems, equippedWeapons, unlockedWeapons)
        )
        }

    private fun Enemy.toCombatant(combatantId: String = id): Combatant =
        Combatant(
            id = combatantId,
            name = name,
            side = CombatSide.ENEMY,
            stats = StatBlock(
                maxHp = CombatFormulas.maxHp(hp, vitality),
                strength = strength,
                vitality = vitality,
                agility = agility,
                focus = focus,
                luck = luck,
                speed = CombatFormulas.speed(speed, agility).roundToInt()
            ),
            resistances = ResistanceProfile(
                fire = resistances.fire ?: 0,
                ice = resistances.ice ?: 0,
                lightning = resistances.lightning ?: 0,
                poison = resistances.poison ?: 0,
                radiation = resistances.radiation ?: 0,
                psychic = resistances.psychic ?: 0,
                void = resistances.void ?: 0,
                physical = resistances.physical ?: 0
            ),
            skills = abilities
        )

    private fun resolveCombatWeapon(
        characterId: String,
        equippedItems: Map<String, String>,
        equippedWeapons: Map<String, String>,
        unlockedWeapons: Set<String>
    ): CombatWeapon? {
        val item = resolveWeaponItemForCharacter(characterId, equippedWeapons, unlockedWeapons) ?: return null
        val equipment = item.equipment ?: return null
        if (!equipment.slot.equals("weapon", ignoreCase = true)) return null

        val styleKey = equipment.attackStyle?.trim()?.lowercase(Locale.getDefault())
        val modEquipments = listOf("weapon_mod1", "weapon_mod2").mapNotNull { slot ->
            equippedItemId(characterId, slot, equippedItems)
                ?.let { itemId -> itemCatalog.findItem(itemId) }
                ?.equipment
        }
        val modElement = modEquipments
            .mapNotNull { it.attackElement?.trim()?.lowercase(Locale.getDefault()) }
            .firstOrNull()
        val element = equipment.attackElement?.trim()?.lowercase(Locale.getDefault()) ?: modElement
        val baseStatus = equipment.statusOnHit?.trim()?.lowercase(Locale.getDefault())
        val baseChance = equipment.statusChance ?: 100.0
        val modStatus = modEquipments.mapNotNull { mod ->
            val status = mod.statusOnHit?.trim()?.lowercase(Locale.getDefault())
            val chance = mod.statusChance ?: 100.0
            status?.let { it to chance }
        }.maxByOrNull { it.second }
        val statusOnHit = baseStatus ?: modStatus?.first
        val statusChance = if (baseStatus != null) baseChance else (modStatus?.second ?: 0.0)
        val power = equipment.attackPowerMultiplier ?: 1.0
        val minDamage = equipment.damageMin?.coerceAtLeast(0) ?: 0
        val maxDamage = equipment.damageMax?.coerceAtLeast(minDamage) ?: minDamage
        val attack = when (styleKey) {
            null, "", "single", "single_target", "default" -> WeaponAttack.SingleTarget(
                powerMultiplier = power,
                element = element
            )
            "all", "aoe", "all_enemies", "spread", "shotgun" -> WeaponAttack.AllEnemies(
                powerMultiplier = power,
                element = element
            )
            "charged_splash", "charge_splash", "rocket", "launcher" -> WeaponAttack.ChargedSplash(
                chargeTurns = equipment.attackChargeTurns ?: 1,
                powerMultiplier = power,
                splashMultiplier = equipment.attackSplashMultiplier ?: 0.5,
                element = element
            )
            else -> WeaponAttack.SingleTarget(
                powerMultiplier = power,
                element = element
            )
        }

        return CombatWeapon(
            itemId = item.id,
            name = item.name,
            weaponType = equipment.weaponType ?: "",
            attack = attack,
            minDamage = minDamage,
            maxDamage = maxDamage,
            statusOnHit = statusOnHit,
            statusChance = statusChance
        )
    }

    private fun resolveWeaponItemForCharacter(
        characterId: String,
        equippedWeapons: Map<String, String>,
        unlockedWeapons: Set<String>
    ): Item? {
        val normalizedId = characterId.trim().lowercase(Locale.getDefault())
        val expectedType = GearRules.allowedWeaponTypeFor(normalizedId)
        fun matches(item: Item): Boolean {
            if (!item.isWeaponItem()) return false
            if (expectedType == null) return true
            val weaponType = item.equipment?.weaponType?.trim()?.lowercase(Locale.getDefault())
                ?: item.type.trim().lowercase(Locale.getDefault())
            return weaponType == expectedType
        }

        val equippedId = equippedWeaponId(characterId, equippedWeapons)
        val equippedItem = equippedId?.let { itemCatalog.findItem(it) }
        if (equippedItem != null && matches(equippedItem)) return equippedItem

        val candidates = unlockedWeapons.mapNotNull { itemCatalog.findItem(it) }
            .filter { matches(it) }
        return candidates.maxByOrNull { weaponScore(it) }
    }

    private fun weaponScore(item: Item): Double {
        val equipment = item.equipment ?: return 0.0
        val min = equipment.damageMin?.coerceAtLeast(0) ?: 0
        val max = equipment.damageMax?.coerceAtLeast(min) ?: min
        val power = equipment.attackPowerMultiplier ?: 1.0
        return ((min + max) / 2.0) * power
    }

    private fun equippedWeaponId(
        characterId: String,
        equippedWeapons: Map<String, String>
    ): String? {
        val normalizedId = characterId.trim().lowercase(Locale.getDefault())
        return equippedWeapons[normalizedId]?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun equippedArmorId(
        characterId: String,
        equippedArmors: Map<String, String>
    ): String? {
        val normalizedId = characterId.trim().lowercase(Locale.getDefault())
        return equippedArmors[normalizedId]?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun equippedItemId(
        characterId: String,
        slotId: String,
        equippedItems: Map<String, String>
    ): String? {
        val normalizedId = characterId.trim().lowercase(Locale.getDefault())
        val normalizedSlot = slotId.trim().lowercase(Locale.getDefault())
        val scopedKey = "$normalizedId:$normalizedSlot"
        val scoped = equippedItems.entries.firstOrNull { (key, value) ->
            key.equals(scopedKey, ignoreCase = true) && value.isNotBlank()
        }?.value
        if (!scoped.isNullOrBlank()) return scoped
        return equippedItems.entries.firstOrNull { (key, value) ->
            key.equals(normalizedSlot, ignoreCase = true) && value.isNotBlank()
        }?.value
    }

    private fun victoryReward(): CombatReward {
        if (enemies.isEmpty()) return CombatReward()
        var xpTotal = 0
        var apTotal = 0
        var creditsTotal = 0
        val dropAccumulator = mutableMapOf<String, Int>()
        encounterEnemySlots.forEach { slot ->
            val enemy = slot.enemy
            xpTotal += enemy.xpReward
            apTotal += enemy.apReward ?: 0
            creditsTotal += enemy.creditReward
            val baseDrops = slot.overrideDrops ?: enemy.drops
            val dropsToProcess = if (slot.extraDrops.isEmpty()) baseDrops else baseDrops + slot.extraDrops
            dropsToProcess.forEach { drop ->
                val chance = drop.chance.coerceIn(0.0, 1.0)
                if (chance <= 0.0) return@forEach
                if (chance >= 1.0 || Random.nextDouble() <= chance) {
                    val qty = rollDropQuantity(drop)
                    if (qty <= 0) return@forEach
                    val canonicalId = canonicalLootId(drop.id)
                    dropAccumulator[canonicalId] = dropAccumulator.getOrDefault(canonicalId, 0) + qty
                }
            }
        }
        val drops = dropAccumulator.entries.map { (id, qty) -> LootDrop(id, qty) }
        return CombatReward(
            xp = xpTotal,
            ap = apTotal,
            credits = creditsTotal,
            drops = drops
        )
    }

    private fun canonicalLootId(rawId: String): String {
        if (rawId.isBlank()) return rawId
        val normalized = normalizeLootItemId(rawId).trim()
        if (normalized.isEmpty()) return rawId
        val resolved = itemCatalog.findItem(normalized)
        if (resolved != null) return resolved.id
        val fallback = normalized.lowercase(Locale.getDefault())
        return fallback.replace("\\s+".toRegex(), "_")
    }

    private fun rollDropQuantity(drop: Drop): Int {
        drop.quantity?.let { return it.coerceAtLeast(1) }
        val min = (drop.qtyMin ?: 1).coerceAtLeast(1)
        val max = (drop.qtyMax ?: min).coerceAtLeast(min)
        if (max <= min) return min
        return Random.nextInt(min, max + 1)
    }

    private fun applyVictoryRewards(reward: CombatReward): List<LevelUpSummary> {
        val levelUps = mutableListOf<LevelUpSummary>()
        if (reward.xp > 0) {
            val snapshot = sessionStore.state.value
            val knownSkills = snapshot.unlockedSkills.toMutableSet()
            playerParty.forEach { member ->
                val previousXp = snapshot.partyMemberXp[member.id] ?: member.xp
                val previousLevel = snapshot.partyMemberLevels[member.id] ?: member.level
                val newXp = previousXp + reward.xp
                sessionStore.setPartyMemberXp(member.id, newXp)
                val newLevel = levelingManager.levelForXp(newXp)
                sessionStore.setPartyMemberLevel(member.id, newLevel)
                val statChanges = mutableListOf<com.example.starborn.domain.leveling.StatDeltaSummary>()
                if (newLevel > previousLevel) {
                    statChanges += com.example.starborn.domain.leveling.StatDeltaSummary(
                        label = "Level",
                        value = "+${newLevel - previousLevel}"
                    )
                }
                if (newLevel > previousLevel) {
                    if (reward.xp > 0) {
                        statChanges += com.example.starborn.domain.leveling.StatDeltaSummary(
                            label = "XP",
                            value = "+${reward.xp}"
                        )
                    }
                    if (reward.ap > 0) {
                        statChanges += com.example.starborn.domain.leveling.StatDeltaSummary(
                            label = "AP",
                            value = "+${reward.ap}"
                        )
                    }
                    val unlocked = collectSkillUnlocks(
                        characterId = member.id,
                        fromLevelExclusive = previousLevel,
                        toLevelInclusive = newLevel,
                        knownSkills = knownSkills
                    )
                    levelUps += LevelUpSummary(
                        characterId = member.id,
                        characterName = member.name,
                        levelsGained = newLevel - previousLevel,
                        newLevel = newLevel,
                        unlockedSkills = unlocked,
                        statChanges = statChanges
                    )
                }
            }
        }
        if (reward.ap > 0) sessionStore.addAp(reward.ap)
        if (reward.credits > 0) sessionStore.addCredits(reward.credits)

        reward.drops.forEach { drop ->
            inventoryService.addItem(drop.itemId, drop.quantity)
        }

        if (reward.drops.isNotEmpty()) {
            sessionStore.setInventory(inventoryService.snapshot())
        }

        return levelUps
    }

    private fun CombatState.applyOutcomeResults(previous: CombatState): CombatState {
        val resolved = applyBossCoreOutcome()
        if (previous.outcome == null && resolved.outcome is CombatOutcome.Victory) {
            val rewards = (resolved.outcome as CombatOutcome.Victory).rewards
            val levelUps = applyVictoryRewards(rewards)
            if (levelUps.isNotEmpty()) {
                pendingLevelUps.addAll(levelUps)
            }
            persistPartyVitals(resolved)
        } else if (resolved.outcome != null) {
            persistPartyVitals(resolved)
        }
        return resolved
    }

    private fun CombatState.applyBossCoreOutcome(): CombatState {
        if (outcome != null || bossCoreCombatantIds.isEmpty()) return this
        val coreDown = bossCoreCombatantIds.any { id ->
            combatants[id]?.isAlive == false
        }
        if (!coreDown) return this
        val reward = victoryReward()
        val victory = CombatOutcome.Victory(reward)
        return copy(
            outcome = victory,
            log = log + CombatLogEntry.Outcome(turn = round, result = victory)
        )
    }

    private fun persistPartyVitals(state: CombatState) {
        if (playerParty.isEmpty()) return
        val snapshot = buildMap {
            playerParty.forEach { member ->
                state.combatants[member.id]?.let { combatantState ->
                    put(member.id, combatantState.hp)
                }
            }
        }
        if (snapshot.isNotEmpty()) {
            sessionStore.updatePartyVitals(snapshot)
        }
    }

    private fun collectSkillUnlocks(
        characterId: String,
        fromLevelExclusive: Int,
        toLevelInclusive: Int,
        knownSkills: MutableSet<String>
    ): List<SkillUnlockSummary> {
        if (toLevelInclusive <= fromLevelExclusive) return emptyList()
        val mapping = progressionData.levelUpSkills[characterId].orEmpty()
        val unlocks = mutableListOf<SkillUnlockSummary>()
        for (level in (fromLevelExclusive + 1)..toLevelInclusive) {
            val skillId = mapping[level.toString()] ?: continue
            if (!knownSkills.add(skillId)) continue
            sessionStore.unlockSkill(skillId)
            val skillName = skillById[skillId]?.name
                ?: skillNodesById[skillId]?.name
                ?: humanizeSkillId(skillId, characterId)
            unlocks += SkillUnlockSummary(id = skillId, name = skillName)
        }
        return unlocks
    }

    private fun humanizeSkillId(skillId: String, characterId: String? = null): String {
        val trimmed = characterId
            ?.takeIf { skillId.startsWith("${it}_") }
            ?.let { skillId.removePrefix("${it}_") }
            ?: skillId
        val locale = Locale.getDefault()
        return trimmed
            .split('_', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { c ->
                    if (c.isLowerCase()) c.titlecase(locale) else c.toString()
                }
            }
            .ifBlank { skillId }
    }

    private data class EncounterEnemySlot(
        val canonicalId: String,
        val enemy: Enemy,
        val overrideDrops: List<Drop>? = null,
        val extraDrops: List<Drop> = emptyList()
    )

    private fun isSupportSkill(skill: Skill): Boolean {
        val tags = skill.combatTags.orEmpty()
        if (skill.type.equals("support", true) || skill.type.equals("heal", true)) return true
        if (tags.any { it.equals("support", true) || it.equals("heal", true) || it.equals("buff", true) }) return true
        if (skill.basePower <= 0 && skill.statusApplications.orEmpty().isNotEmpty()) return true
        return false
    }

    private fun resolveEnemyTargets(state: CombatState, allowMultiple: Boolean = false): List<String> {
        val aliveIds = enemyIdList.filter { state.combatants[it]?.isAlive == true }
        if (aliveIds.isEmpty()) return emptyList()
        val aliveSelection = _selectedEnemies.value.filter { state.combatants[it]?.isAlive == true }
        if (allowMultiple && aliveSelection.isNotEmpty()) {
            return aliveSelection
        }
        if (!allowMultiple && aliveSelection.isNotEmpty()) {
            return listOf(aliveSelection.first())
        }
        return if (allowMultiple) aliveIds else listOf(aliveIds.first())
    }

    private fun resolvePlayerTargets(state: CombatState, allowMultiple: Boolean = false): List<String> {
        val aliveIds = playerIdList.filter { state.combatants[it]?.isAlive == true }
        if (aliveIds.isEmpty()) return emptyList()
        if (allowMultiple || aliveIds.size == 1) return aliveIds
        val candidates = aliveIds.filterNot { it == lastEnemyTargetId }.ifEmpty { aliveIds }
        val chosen = candidates[Random.nextInt(candidates.size)]
        lastEnemyTargetId = chosen
        return listOf(chosen)
    }

    private fun resolveActivePlayerId(state: CombatState): String? {
        val active = state.activeCombatant ?: return null
        val side = active.combatant.side
        return if (side == CombatSide.PLAYER || side == CombatSide.ALLY) active.combatant.id else null
    }

    private fun resolveSkillTargets(
        skill: Skill,
        state: CombatState,
        attackerId: String
    ): List<String> {
        val targeting = determineSkillTargeting(skill)
        val attackerState = state.combatants[attackerId]
        return when (targeting) {
            SkillTargeting.SELF -> listOf(attackerId)
            SkillTargeting.ALL_ALLIES -> {
                val allies = state.turnOrder.asSequence()
                    .mapNotNull { state.combatants[it.combatantId] }
                    .filter { it.combatant.side == attackerState?.combatant?.side && it.isAlive }
                    .map { it.combatant.id }
                    .toList()
                allies.ifEmpty { listOf(attackerId) }
            }
            SkillTargeting.ALL_ENEMIES -> enemyIdList.filter { state.combatants[it]?.isAlive == true }
            SkillTargeting.SINGLE_ENEMY -> resolveEnemyTargets(state)
        }
    }

    fun targetRequirementFor(skill: Skill): TargetRequirement {
        return when (determineSkillTargeting(skill)) {
            SkillTargeting.SINGLE_ENEMY -> TargetRequirement.ENEMY
            SkillTargeting.ALL_ALLIES -> TargetRequirement.NONE
            SkillTargeting.ALL_ENEMIES -> TargetRequirement.NONE
            SkillTargeting.SELF -> TargetRequirement.NONE
        }
    }

private fun determineSkillTargeting(skill: Skill): SkillTargeting {
        if (skill.id == "driller_core_repair") return SkillTargeting.SELF
        val statusDefs = skillStatusDefinitions(skill)
        val hasSelf = statusDefs.any { it.target.equals("self", true) }
        val hasAlly = statusDefs.any { it.target.equals("ally", true) }
        val hasEnemy = statusDefs.any { it.target.equals("enemy", true) }
        val dealsDamage = skill.basePower > 0
        val supportTag = skill.combatTags.orEmpty().any { it.equals("support", true) || it.equals("heal", true) }
        val aoeTag = skill.combatTags.orEmpty().any {
            it.equals("aoe", true) || it.equals("burst", true) || it.equals("multi", true)
        }
        return when {
            hasSelf || (hasAlly && !dealsDamage) || (!dealsDamage && supportTag) -> SkillTargeting.SELF
            aoeTag -> SkillTargeting.ALL_ENEMIES
            hasEnemy && supportTag && !dealsDamage -> SkillTargeting.ALL_ENEMIES
            hasAlly && dealsDamage -> SkillTargeting.ALL_ALLIES
            else -> SkillTargeting.SINGLE_ENEMY
        }
    }

    private fun skillStatusDefinitions(skill: Skill): List<StatusDefinition> {
        val seen = mutableSetOf<String>()
        val results = mutableListOf<StatusDefinition>()
        fun addDefinition(id: String?) {
            if (id.isNullOrBlank()) return
            val definition = statusRegistry.definition(id) ?: return
            val key = definition.id.lowercase()
            if (seen.add(key)) {
                results += definition
            }
        }
        skill.statusApplications.orEmpty().forEach(::addDefinition)
        skill.combatTags.orEmpty().forEach(::addDefinition)
        return results
    }

    private fun refreshSelection(state: CombatState?) {
        if (state == null) {
            enemyIdList.firstOrNull()?.let { setSelection(setOf(it)) } ?: setSelection(emptySet())
            return
        }
        val alive = enemyIdList.filter { state.combatants[it]?.isAlive == true }
        if (alive.isEmpty()) {
            setSelection(emptySet())
            return
        }
        val filtered = _selectedEnemies.value.filter { state.combatants[it]?.isAlive == true }.toSet()
        val target = when {
            filtered.isNotEmpty() -> filtered
            else -> setOf(alive.first())
        }
        setSelection(target)
    }

    private fun setSelection(newSelection: Set<String>) {
        if (_selectedEnemies.value != newSelection) {
            _selectedEnemies.value = newSelection
        }
    }

    fun itemDisplayName(itemId: String): String =
        inventoryService.itemDisplayName(itemId)

    fun registerTimedPromptTap() {
        val deferred = synchronized(timedPromptLock) { timedPromptDeferred }
        deferred?.complete(true)
    }

    private suspend fun awaitTimedWindow(
        type: TimedWindowType,
        message: String,
        durationMs: Long
    ): Boolean {
        if (_state.value?.outcome != null) return false
        val prompt = TimedPromptState(
            id = UUID.randomUUID().toString(),
            message = message,
            durationMillis = durationMs,
            type = type
        )
        val deferred = CompletableDeferred<Boolean>()
        synchronized(timedPromptLock) {
            timedPromptDeferred?.complete(false)
            timedPromptDeferred = deferred
            _timedPrompt.value = prompt
        }
        val result = withTimeoutOrNull(durationMs) { deferred.await() } ?: false
        synchronized(timedPromptLock) {
            if (timedPromptDeferred === deferred) {
                timedPromptDeferred = null
                if (_timedPrompt.value?.id == prompt.id) {
                    _timedPrompt.value = null
                }
            }
        }
        return result
    }

    private fun cancelTimedPrompt() {
        val pending: CompletableDeferred<Boolean>?
        synchronized(timedPromptLock) {
            pending = timedPromptDeferred
            timedPromptDeferred = null
            _timedPrompt.value = null
        }
        pending?.complete(false)
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimedPrompt()
    }

    private fun playUiCue(key: String) {
        emitAudio(audioRouter.commandsForUi(key))
    }

    private fun playBattleCue(key: String) {
        emitAudio(audioRouter.commandsForBattle(key))
    }

    private fun emitAudio(commands: List<AudioCommand>) {
        if (commands.isEmpty()) return
        combatFxEvents.tryEmit(CombatFxEvent.Audio(commands))
    }

    private enum class SkillTargeting {
        SELF,
        SINGLE_ENEMY,
        ALL_ENEMIES,
        ALL_ALLIES
    }

    private fun describeEntry(entry: CombatLogEntry, state: CombatState): String? =
        when (entry) {
            is CombatLogEntry.Damage -> {
                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                if (entry.sourceId.startsWith(STATUS_SOURCE_PREFIX)) {
                    val statusLabel = entry.sourceId.removePrefix(STATUS_SOURCE_PREFIX).replace('_', ' ')
                    "$targetName reels from ${statusLabel.trim()} (${entry.amount})"
                } else {
                    val sourceName = state.combatants[entry.sourceId]?.combatant?.name ?: entry.sourceId
                    if (entry.element == "miss") {
                        "$sourceName whiffs past $targetName"
                    } else if (entry.amount <= 0) {
                        "$sourceName hits $targetName but deals no damage"
                    } else {
                        "$sourceName hits $targetName for ${entry.amount}"
                    }
                }
            }
            is CombatLogEntry.Heal -> {
                val sourceName = state.combatants[entry.sourceId]?.combatant?.name ?: entry.sourceId
                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                "$sourceName restores ${entry.amount} HP to $targetName"
            }
            is CombatLogEntry.StatusApplied -> {
                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                "${entry.statusId.uppercase()} wraps around $targetName"
            }
            is CombatLogEntry.StatusExpired -> {
                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                "${entry.statusId.uppercase()} dissipates from $targetName"
            }
            is CombatLogEntry.ElementStack -> {
                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                val display = entry.element.uppercase()
                "$targetName builds $display energy (${entry.stacks}/${ElementalStackRules.STACK_THRESHOLD})"
            }
            is CombatLogEntry.ElementBurst -> {
                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                val display = entry.element.uppercase()
                "$display power erupts from $targetName!"
            }
            is CombatLogEntry.TurnSkipped -> {
                val actorName = state.combatants[entry.actorId]?.combatant?.name ?: entry.actorId
                "$actorName ${entry.reason}"
            }
            is CombatLogEntry.ActionQueued -> {
                val actorName = state.combatants[entry.actorId]?.combatant?.name ?: entry.actorId
                "$actorName lines up an action"
            }
            is CombatLogEntry.Outcome -> when (entry.result) {
                is CombatOutcome.Victory -> "All foes defeated!"
                is CombatOutcome.Defeat -> "Party overwhelmed..."
                CombatOutcome.Retreat -> "Retreat successful."
            }
        }

    data class TimedPromptState(
        val id: String,
        val message: String,
        val durationMillis: Long,
        val type: TimedWindowType,
        val startedAt: Long = SystemClock.elapsedRealtime()
    )

    sealed interface TimedWindowType {
        data class Offense(val actorId: String, val targetId: String) : TimedWindowType
        data class Defense(val targetIds: List<String>) : TimedWindowType
    }

    private fun Item.isWeaponItem(): Boolean {
        val normalizedType = type.trim().lowercase(Locale.getDefault())
        return normalizedType == "weapon" ||
            GearRules.isWeaponType(normalizedType) ||
            equipment?.slot?.equals("weapon", ignoreCase = true) == true
    }

    companion object {
        private const val STATUS_SOURCE_PREFIX = "status_"
        private const val ATB_SPEED_SCALE = 90f
        private const val ATTACK_ANIMATION_PAUSE_MS = 500L
        private const val TIMED_GUARD_WINDOW_MS = 700L
        private const val TIMED_GUARD_DEF_BONUS = 10
        private const val ZEKE_SUPPORT_ATB_BONUS = 0.25f
        private const val SNACK_COOLDOWN_TURNS = 5
    }
}


enum class TargetRequirement {
    NONE,
    ENEMY,
    ALLY,
    ANY
}
