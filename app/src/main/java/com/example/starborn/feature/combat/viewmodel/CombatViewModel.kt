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
import com.example.starborn.domain.combat.LootDrop
import com.example.starborn.domain.combat.ResistanceProfile
import com.example.starborn.domain.combat.StatBlock
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.inventory.normalizeLootItemId
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.leveling.ProgressionData
import com.example.starborn.domain.leveling.SkillUnlockSummary
import com.example.starborn.domain.model.BuffEffect
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.SkillTreeNode
import com.example.starborn.domain.model.StatusDefinition
import com.example.starborn.domain.model.Drop
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random
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
    private val playerCombatants: List<Combatant>
    private val enemyCombatants: List<Combatant>
    private val playerIdList: List<String>
    private val enemyIdList: List<String>
    private val encounterEnemyIdList: List<String>
    private val readyQueue = mutableListOf<String>()
    private val skillById: Map<String, Skill>
    private val skillNodesById: Map<String, SkillTreeNode>
    private val actionProcessor: CombatActionProcessor
    private val enemyCooldowns: MutableMap<String, Int> = mutableMapOf()
    private val combatFxEvents = MutableSharedFlow<CombatFxEvent>(extraBufferCapacity = 16)
    val fxEvents: SharedFlow<CombatFxEvent> = combatFxEvents.asSharedFlow()
    private val pendingLevelUps = mutableListOf<LevelUpSummary>()
    val environmentId: String?
    val weatherId: String?
    val theme: Theme?
    val roomBackground: String?
    private val resonanceMin: Int
    val resonanceMax: Int
    private val _resonance = MutableStateFlow(0)
    val resonance: StateFlow<Int> = _resonance.asStateFlow()
    val resonanceMinBound: Int get() = resonanceMin
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

    init {
        itemCatalog.load()
        val players = worldAssets.loadCharacters()
        val allEnemies = worldAssets.loadEnemies()
        val allSkills = worldAssets.loadSkills()
        val rooms = worldAssets.loadRooms()

        val sessionSnapshot = sessionStore.state.value
        val partyIds = sessionSnapshot.partyMembers
        val resolvedParty = (if (partyIds.isEmpty()) players.take(1) else partyIds.mapNotNull { id ->
            players.find { it.id == id }
        }).ifEmpty { players.take(1) }

        playerParty = resolvedParty
        player = playerParty.firstOrNull()
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

        playerCombatants = playerParty.map { it.toCombatant() }
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

        readyQueue.clear()
        initializeAtbMeters()

        skillById = allSkills.associateBy { it.id }
        skillNodesById = worldAssets.loadSkillNodes()
        actionProcessor = CombatActionProcessor(
            engine = combatEngine,
            statusRegistry = statusRegistry,
            skillLookup = { id -> skillById[id] },
            consumeItem = { itemId -> inventoryService.useItem(itemId) }
        )

        resonanceMin = sessionSnapshot.resonanceMin
        resonanceMax = sessionSnapshot.resonanceMax
        _resonance.value = sessionStore.resetResonanceToStart()

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
            val cost = resolveSkillCost(skill.id)
            if (!spendResonance(cost)) {
                playUiCue("error")
                return@updateState current.appendResonanceFailure(attackerId)
            }
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

    fun defend() {
        val attackerId = _awaitingAction.value ?: return
        var executed = false
        updateState { current ->
            val attackerState = current.combatants[attackerId] ?: return@updateState current
            if (!attackerState.isAlive) {
                clearAwaitingAction(attackerId)
                return@updateState current
            }
            playUiCue("confirm")
            val action = CombatAction.Defend(attackerId)
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            executed = true
            resolved.applyOutcomeResults(current)
        }
        if (executed) {
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
        playerSkillsById[playerId].orEmpty()

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
        newEntries.forEach { entry ->
            when (entry) {
                is CombatLogEntry.ActionQueued -> {
                    resetAtbMeter(entry.actorId)
                    combatFxEvents.tryEmit(CombatFxEvent.TurnQueued(entry.actorId))
                    if (entry.actorId in playerIdList) {
                        _combatMessage.value = null
                    }
                }
                is CombatLogEntry.Damage -> {
                    if (entry.amount == 0 && entry.element == "miss") {
                        if (entry.targetId !in suppressMissLungeTargets) {
                            triggerMissLunge(entry.targetId)
                        }
                    }
                    combatFxEvents.tryEmit(
                        CombatFxEvent.Impact(
                            sourceId = entry.sourceId,
                            targetId = entry.targetId,
                            amount = entry.amount,
                            element = entry.element,
                            critical = entry.critical
                        )
                    )
                    if (entry.amount > 0 && shouldAwardResonance(entry, updated)) {
                        awardResonanceFromDamage(entry.amount)
                    }
                    val targetState = updated.combatants[entry.targetId]
                    if (targetState?.isAlive == false) {
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
            val speed = combatantState.combatant.stats.speed.coerceAtLeast(1)
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
        removeFromReadyQueue(actorId, updateActive = false)
        resetAtbMeter(actorId)
        updateActiveActorFromQueue()
        tryProcessEnemyTurns()
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
        val head = readyQueue.firstOrNull() ?: return
        if (head !in enemyIdList) return
        enemyTurnJob = viewModelScope.launch {
            executeEnemyTurn(head)
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

    private fun Player.toCombatant(): Combatant =
        Combatant(
            id = id,
            name = name,
            side = CombatSide.PLAYER,
            stats = StatBlock(
                maxHp = CombatFormulas.maxHp(hp, vitality),
                maxRp = CombatFormulas.maxHp(hp, vitality),
                strength = strength,
                vitality = vitality,
                agility = agility,
                focus = focus,
                luck = luck,
                speed = agility
            ),
            resistances = ResistanceProfile(),
            skills = skills
        )

    private fun Enemy.toCombatant(combatantId: String = id): Combatant =
        Combatant(
            id = combatantId,
            name = name,
            side = CombatSide.ENEMY,
            stats = StatBlock(
                maxHp = CombatFormulas.maxHp(hp, vitality),
                maxRp = vitality.coerceAtLeast(1),
                strength = strength,
                vitality = vitality,
                agility = agility,
                focus = focus,
                luck = luck,
                speed = speed
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
        if (previous.outcome == null && outcome is CombatOutcome.Victory) {
            val rewards = (outcome as CombatOutcome.Victory).rewards
            val levelUps = applyVictoryRewards(rewards)
            if (levelUps.isNotEmpty()) {
                pendingLevelUps.addAll(levelUps)
            }
            persistPartyVitals(this)
        } else if (outcome != null) {
            persistPartyVitals(this)
        }
        return this
    }

    private fun persistPartyVitals(state: CombatState) {
        if (playerParty.isEmpty()) return
        val snapshot = buildMap {
            playerParty.forEach { member ->
                state.combatants[member.id]?.let { combatantState ->
                    put(member.id, combatantState.hp to combatantState.rp)
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
            val skillName = skillById[skillId]?.name ?: skillId
            unlocks += SkillUnlockSummary(id = skillId, name = skillName)
        }
        return unlocks
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
        return if (allowMultiple) aliveIds else listOf(aliveIds.first())
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

    private fun spendResonance(cost: Int): Boolean {
        if (cost <= 0) return true
        val current = sessionStore.state.value.resonance
        if (current < cost) return false
        val updated = sessionStore.changeResonance(-cost)
        _resonance.value = updated
        return true
    }

    private fun awardResonanceFromDamage(amount: Int) {
        if (amount <= 0) return
        val gain = max(1, amount / 10)
        val updated = sessionStore.changeResonance(gain)
        _resonance.value = updated
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

    private fun resolveSkillCost(skillId: String): Int =
        skillNodesById[skillId]?.effect?.rpCost?.coerceAtLeast(0) ?: 0

    private fun CombatState.appendResonanceFailure(actorId: String): CombatState =
        copy(
            log = log + CombatLogEntry.TurnSkipped(
                turn = round,
                actorId = actorId,
                reason = "can't focus enough resonance"
            )
        )

    private fun shouldAwardResonance(
        entry: CombatLogEntry.Damage,
        state: CombatState
    ): Boolean {
        if (entry.sourceId.startsWith(STATUS_SOURCE_PREFIX)) return false
        val source = state.combatants[entry.sourceId] ?: return false
        val target = state.combatants[entry.targetId] ?: return false
        val friendly = source.combatant.side == CombatSide.PLAYER || source.combatant.side == CombatSide.ALLY
        val hostile = target.combatant.side == CombatSide.ENEMY
        return friendly && hostile
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
                    if (entry.amount <= 0 || entry.element == "miss") {
                        "$sourceName whiffs past $targetName"
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

    companion object {
        private const val STATUS_SOURCE_PREFIX = "status_"
        private const val ATB_SPEED_SCALE = 90f
        private const val TIMED_GUARD_WINDOW_MS = 700L
        private const val TIMED_GUARD_DEF_BONUS = 10
    }
}


enum class TargetRequirement {
    NONE,
    ENEMY,
    ALLY,
    ANY
}
