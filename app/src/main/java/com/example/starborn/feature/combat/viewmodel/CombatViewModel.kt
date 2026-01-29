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
import com.example.starborn.domain.combat.CombatAiWeights
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.CombatFormulas
import com.example.starborn.domain.combat.CombatOutcome
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

enum class AttackLungeStyle {
    MELEE,
    RANGED,
    CAST,
    BUFF,
    ITEM,
    SNACK,
    MISS
}

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
    private val aiWeights = CombatAiWeights() // Default configuration
    private val enemyCooldowns: MutableMap<String, Int> = mutableMapOf()
    private val enemyBrains: MutableMap<String, EnemyBrain> = mutableMapOf()
    private val enemyDefinitions: MutableMap<String, Enemy> = mutableMapOf()
    private val enemySkillHistory: MutableMap<String, ArrayDeque<String>> = mutableMapOf()
    private val enemySkillUsageCounts: MutableMap<String, Int> = mutableMapOf()
    private val playerCooldowns: MutableMap<String, Int> = mutableMapOf()
    private val skillUsageCounts: MutableMap<String, Int> = mutableMapOf()
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
            if (actorId in playerIdList) {
                tickPlayerCooldowns(actorId)
                tickSnackCooldown(actorId)
                playUiCue("turn_start")
            }
        } else {
            atbMenuPaused = false
            if (!isAtbPaused()) {
                tryProcessEnemyTurns()
            }
        }
    }

    private fun tickPlayerCooldowns(actorId: String) {
        val prefix = "$actorId:"
        val keysToUpdate = playerCooldowns.keys.filter { it.startsWith(prefix) }
        keysToUpdate.forEach { key ->
            val current = playerCooldowns[key] ?: return@forEach
            if (current > 0) {
                playerCooldowns[key] = current - 1
            }
        }
    }
    private val combatTextVerbosity = CombatTextVerbosity.STANDARD
    private var bannerSession: BannerSession? = null
    private val _combatBanner = MutableStateFlow<CombatBannerMessage?>(null)
    val combatBanner: StateFlow<CombatBannerMessage?> = _combatBanner.asStateFlow()

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
    private val _lungeStyle = MutableStateFlow(AttackLungeStyle.MELEE)
    val lungeStyle: StateFlow<AttackLungeStyle> = _lungeStyle.asStateFlow()
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
    private var lastShieldBlockCueAtMs: Long = 0L
    private var lastShieldBreakCueAtMs: Long = 0L

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

        playerParty = resolvedParty.toList()
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
            enemyDefinitions[instanceId] = enemy
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
        initializeEnemyBrains()

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
            val key = "$attackerId:${skill.id}"
            if (skill.cooldown > 0) {
                playerCooldowns[key] = skill.cooldown + 1 // +1 because tick happens at start of turn
            }
            if (skill.usesPerBattle != null) {
                skillUsageCounts[key] = skillUsageCounts.getOrDefault(key, 0) + 1
            }
            val animStyle = resolveSkillAnimationStyle(skill)
            triggerAttackLunge(attackerId, animStyle)
            clearAwaitingAction(attackerId)
            concludeActorTurn(attackerId)
        }
    }

    private fun resolveSkillAnimationStyle(skill: Skill): AttackLungeStyle {
        val tags = skill.combatTags.orEmpty().map { it.lowercase(Locale.getDefault()) }
        return when {
            tags.contains("heal") || tags.contains("support") || tags.contains("buff") -> AttackLungeStyle.BUFF
            tags.contains("shock") || tags.contains("burn") || tags.contains("freeze") || tags.contains("acid") || tags.contains("source") -> AttackLungeStyle.CAST
            skill.scaling?.lowercase(Locale.getDefault()) == "tech" -> AttackLungeStyle.RANGED
            tags.contains("physical") || tags.contains("dmg") -> AttackLungeStyle.MELEE
            else -> AttackLungeStyle.CAST
        }
    }

    private fun maybeTriggerAttackLunge(
        action: CombatAction,
        style: AttackLungeStyle = AttackLungeStyle.MELEE
    ) {
        if (action is CombatAction.BasicAttack) {
            triggerAttackLunge(action.actorId, style)
        }
    }

    private fun triggerMissLunge(targetId: String) {
        pauseAtbForAnimation()
        _missLungeToken.value = _missLungeToken.value + 1
        _missLungeActorId.value = targetId
    }

    private fun executePlayerAttack(attackerId: String, targetId: String) {
        var executed = false
        var attackMissed = false
        val action = CombatAction.BasicAttack(attackerId, targetId)
        updateState { current ->
            val attackerState = current.combatants[attackerId] ?: return@updateState current
            val targetState = current.combatants[targetId] ?: return@updateState current
            val previousLogSize = current.log.size
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            val newEntries = resolved.log.drop(previousLogSize)
            val damageEntries = newEntries
                .filterIsInstance<CombatLogEntry.Damage>()
                .filter { it.sourceId == attackerId }
            attackMissed = damageEntries.isNotEmpty() &&
                damageEntries.all { it.amount == 0 && it.element == "miss" }
            executed = true
            resolved.applyOutcomeResults(current)
        }
        if (executed) {
            val style = if (attackMissed) AttackLungeStyle.MISS else AttackLungeStyle.MELEE
            triggerAttackLunge(attackerId, style)
            clearAwaitingAction(attackerId)
            concludeActorTurn(attackerId)
        } else {
            removeFromReadyQueue(attackerId)
            updateActiveActorFromQueue()
        }
    }

    private fun triggerAttackLunge(
        actorId: String,
        style: AttackLungeStyle = AttackLungeStyle.MELEE
    ) {
        pauseAtbForAnimation()
        _lungeStyle.value = style
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
            triggerAttackLunge(attackerId, AttackLungeStyle.ITEM)
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
            val cooldown = snack.effect?.cooldown?.takeIf { it > 0 } ?: 5
            snackCooldowns[attackerId] = cooldown + 1
            triggerAttackLunge(attackerId, AttackLungeStyle.SNACK)
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

    fun canUseSkill(actorId: String, skill: Skill): Boolean {
        val state = _state.value ?: return false
        
        // 0. Check Jammed
        val actorState = state.combatants[actorId] ?: return false
        val isJammed = actorState.statusEffects.any { status ->
            statusRegistry.definition(status.id)?.blockSkills == true
        }
        if (isJammed) return false

        val key = "$actorId:${skill.id}"
        
        // 1. Check Cooldown
        if (playerCooldowns.getOrDefault(key, 0) > 0) return false
        
        // 2. Check Usage Limits
        if (skill.usesPerBattle != null) {
            val used = skillUsageCounts.getOrDefault(key, 0)
            if (used >= skill.usesPerBattle) return false
        }
        
        // 3. Check Conditions (Weapon Requirements, HP thresholds, etc.)
        if (!checkSkillConditions(actorId, skill, state)) return false
        
        return true
    }

    fun skillCooldownRemaining(actorId: String, skillId: String): Int =
        playerCooldowns.getOrDefault("$actorId:$skillId", 0)

    private fun checkSkillConditions(actorId: String, skill: Skill, state: CombatState): Boolean {
        if (skill.conditions.isNullOrEmpty()) return true
        
        val actorState = state.combatants[actorId] ?: return false
        // TODO: Expand this with actual condition parsing logic
        // Examples: "hp_below_50", "sword_equipped", "target_stunned"
        return true 
    }

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

    private fun initializeEnemyBrains() {
        enemyBrains.clear()
        enemySkillHistory.clear()
        enemySkillUsageCounts.clear()
        enemyCooldowns.clear()
        encounterEnemySlots.forEachIndexed { index, slot ->
            val combatant = enemyCombatants.getOrNull(index) ?: return@forEachIndexed
            val skills = combatant.skills.mapNotNull { skillById[it] }
            val behavior = parseBehavior(slot.enemy.combatBehavior)
                ?: parseBehavior(slot.enemy.behavior)
                ?: inferBehavior(skills)
            val role = parseRole(slot.enemy.combatRole) ?: inferRole(skills)
            enemyBrains[combatant.id] = EnemyBrain(behavior = behavior, role = role)
            enemySkillHistory[combatant.id] = ArrayDeque()
        }
    }

    private fun selectEnemyAction(state: CombatState, enemyState: CombatantState): CombatAction {
        val enemyId = enemyState.combatant.id
        val brain = enemyBrains[enemyId] ?: EnemyBrain()
        val candidates = mutableListOf<ScoredAction>()

        enemyState.combatant.skills.forEach { skillId ->
            val skill = skillById[skillId] ?: return@forEach
            if (!canEnemyUseSkill(enemyId, skill)) return@forEach
            val targetSets = resolveTargetSetsForAi(skill, state, enemyId)
            if (targetSets.isEmpty()) return@forEach
            targetSets.forEach { targets ->
                val score = scoreSkill(skill, enemyState, targets, state, brain)
                if (score > 0) {
                    candidates += ScoredAction(
                        score = score,
                        action = CombatAction.SkillUse(enemyId, skillId, targets)
                    )
                }
            }
        }

        val basicTargets = resolveOpponentIds(state, enemyState).ifEmpty {
            listOfNotNull(state.firstAlivePlayer()?.combatant?.id)
        }
        if (basicTargets.isNotEmpty()) {
            val bestBasic = basicTargets.maxByOrNull { targetId ->
                scoreBasicAttack(enemyState, targetId, state, brain)
            }
            if (bestBasic != null) {
                val basicScore = scoreBasicAttack(enemyState, bestBasic, state, brain)
                candidates += ScoredAction(
                    score = basicScore,
                    action = CombatAction.BasicAttack(enemyId, bestBasic)
                )
            }
        }

        val defendScore = scoreDefend(enemyState, state, brain)
        if (defendScore > 0) {
            candidates += ScoredAction(score = defendScore, action = CombatAction.Defend(enemyId))
        }

        val selected = candidates.maxByOrNull { it.score }?.let { best ->
            val threshold = best.score - 3.0
            val top = candidates.filter { it.score >= threshold }
            top[Random.nextInt(top.size)]
        }

        val fallbackTarget = basicTargets.firstOrNull() ?: enemyId
        val chosen = selected?.action ?: if (fallbackTarget == enemyId) {
            CombatAction.Defend(enemyId)
        } else {
            CombatAction.BasicAttack(enemyId, fallbackTarget)
        }

        when (chosen) {
            is CombatAction.BasicAttack -> lastEnemyTargetId = chosen.targetId
            is CombatAction.SkillUse -> {
                if (chosen.targetIds.size == 1) {
                    lastEnemyTargetId = chosen.targetIds.first()
                }
            }
            else -> Unit
        }

        return chosen
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
    private fun recordEnemySkillUse(action: CombatAction) {
        if (action !is CombatAction.SkillUse) return
        val key = "${action.actorId}:${action.skillId}"
        enemySkillUsageCounts[key] = enemySkillUsageCounts.getOrDefault(key, 0) + 1
        val history = enemySkillHistory.getOrPut(action.actorId) { ArrayDeque() }
        history.addLast(action.skillId)
        while (history.size > ENEMY_SKILL_MEMORY) {
            history.removeFirst()
        }
    }

    private fun canEnemyUseSkill(enemyId: String, skill: Skill): Boolean {
        if (enemyCooldowns.getOrDefault(skill.id, 0) > 0) return false
        val uses = skill.usesPerBattle ?: return true
        val used = enemySkillUsageCounts.getOrDefault("$enemyId:${skill.id}", 0)
        return used < uses
    }

    private fun resolveTargetSetsForAi(
        skill: Skill,
        state: CombatState,
        attackerId: String
    ): List<List<String>> {
        val attackerState = state.combatants[attackerId]
        val targeting = determineSkillTargeting(skill)
        val allies = resolveFriendlyIds(state, attackerState)
        val opponents = resolveOpponentIds(state, attackerState)
        return when (targeting) {
            SkillTargeting.SELF -> listOf(listOf(attackerId))
            SkillTargeting.ALL_ALLIES -> if (allies.isNotEmpty()) listOf(allies) else listOf(listOf(attackerId))
            SkillTargeting.ALL_ENEMIES -> if (opponents.isNotEmpty()) listOf(opponents) else emptyList()
            SkillTargeting.SINGLE_ENEMY -> opponents.map { listOf(it) }
        }
    }

    private fun resolveFriendlyIds(state: CombatState, attackerState: CombatantState?): List<String> {
        val side = attackerState?.combatant?.side ?: CombatSide.PLAYER
        val sides = if (side == CombatSide.ENEMY) {
            setOf(CombatSide.ENEMY)
        } else {
            setOf(CombatSide.PLAYER, CombatSide.ALLY)
        }
        return resolveAliveIdsForSides(state, sides)
    }

    private fun resolveOpponentIds(state: CombatState, attackerState: CombatantState?): List<String> {
        val side = attackerState?.combatant?.side ?: CombatSide.PLAYER
        val sides = if (side == CombatSide.ENEMY) {
            setOf(CombatSide.PLAYER, CombatSide.ALLY)
        } else {
            setOf(CombatSide.ENEMY)
        }
        return resolveAliveIdsForSides(state, sides)
    }

    private fun resolveAliveIdsForSides(state: CombatState, sides: Set<CombatSide>): List<String> {
        return state.turnOrder.asSequence()
            .mapNotNull { state.combatants[it.combatantId] }
            .filter { it.isAlive && it.combatant.side in sides }
            .map { it.combatant.id }
            .toList()
    }

    private fun scoreSkill(
        skill: Skill,
        enemyState: CombatantState,
        targetIds: List<String>,
        state: CombatState,
        brain: EnemyBrain
    ): Double {
        val tags = skill.combatTags.orEmpty().map { it.lowercase(Locale.getDefault()) }
        val isSupport = isSupportSkill(skill)
        val hasDamage = skill.basePower > 0 && !isSupport
        val isHeal = tags.contains("heal") || skill.type.equals("heal", true)
        val isSummon = tags.contains("summon")
        val isGuardBreak = tags.contains("guard_break") || tags.contains("stagger")
        val isAoe = tags.any { it == "aoe" || it == "burst" || it == "multi" }
        val statusDefs = skillStatusDefinitions(skill)
        val hasStatuses = statusDefs.isNotEmpty()

        val weights = behaviorWeights(brain.behavior).combine(roleWeights(brain.role))

        var damageScore = 0.0
        if (hasDamage) {
            val element = resolveSkillElement(skill)
            targetIds.forEach { targetId ->
                val targetState = state.combatants[targetId] ?: return@forEach
                damageScore += scoreDamageTarget(skill.basePower, element, targetState)
            }
            if (isAoe) damageScore *= 0.85
        }

        var healScore = 0.0
        if (isSupport && (isHeal || skill.basePower > 0)) {
            targetIds.forEach { targetId ->
                val targetState = state.combatants[targetId] ?: return@forEach
                healScore += scoreHealTarget(skill, targetState)
            }
        }

        var statusScore = 0.0
        if (hasStatuses) {
            targetIds.forEach { targetId ->
                val targetState = state.combatants[targetId]
                statusScore += scoreStatusTargets(statusDefs, targetState, isSupport)
            }
        }

        var guardBreakScore = 0.0
        if (isGuardBreak) {
            targetIds.forEach { targetId ->
                val targetState = state.combatants[targetId] ?: return@forEach
                guardBreakScore += scoreGuardBreakTarget(targetState)
            }
        }

        var summonScore = 0.0
        if (isSummon) {
            val allyCount = resolveFriendlyIds(state, enemyState).size
            summonScore = aiWeights.summonBase + (if (state.round <= 2) aiWeights.summonEarlyRoundBonus else aiWeights.summonLateRoundBonus)
            if (allyCount < 3) summonScore += aiWeights.summonLowAllyBonus
        }

        val debuffIntent = !isSupport && hasStatuses
        val buffIntent = isSupport && hasStatuses

        var total = 0.0
        if (hasDamage) total += damageScore * weights.damage
        if (isSupport && (isHeal || skill.basePower > 0)) total += healScore * weights.heal
        if (buffIntent) total += statusScore * weights.buff
        if (debuffIntent) total += statusScore * weights.debuff
        if (isGuardBreak) total += guardBreakScore * weights.guardBreak
        if (isSummon) total += summonScore * weights.summon

        total -= skill.cooldown.coerceAtLeast(0) * aiWeights.cooldownPenaltyPerTurn
        total -= diversityPenalty(enemyState.combatant.id, skill.id)
        return total
    }

    private fun scoreBasicAttack(
        enemyState: CombatantState,
        targetId: String,
        state: CombatState,
        brain: EnemyBrain
    ): Double {
        val targetState = state.combatants[targetId] ?: return 0.0
        val base = enemyState.combatant.stats.strength * 1.1 + aiWeights.basicAttackBaseConstant
        val damageScore = scoreDamageTarget(base.roundToInt(), "physical", targetState)
        val weights = behaviorWeights(brain.behavior).combine(roleWeights(brain.role))
        val repeatPenalty = if (targetId == lastEnemyTargetId) aiWeights.basicAttackRepeatPenalty else 0.0
        return damageScore * weights.damage - repeatPenalty
    }

    private fun scoreDefend(
        enemyState: CombatantState,
        state: CombatState,
        brain: EnemyBrain
    ): Double {
        val maxHp = enemyState.combatant.stats.maxHp.coerceAtLeast(1)
        val hpRatio = enemyState.hp.toDouble() / maxHp
        
        // Anticipation Logic: Elites/Bosses sense charging attacks
        val enemyDef = enemyDefinitions[enemyState.combatant.id]
        val isSmart = enemyDef?.tier.equals("elite", ignoreCase = true) || 
                      enemyDef?.tier.equals("boss", ignoreCase = true)
        
        if (isSmart) {
            val playerCharging = state.combatants.values.any { 
                (it.combatant.side == CombatSide.PLAYER || it.combatant.side == CombatSide.ALLY) && 
                it.isAlive && 
                it.weaponCharge != null 
            }
            if (playerCharging) {
                return aiWeights.anticipationDefendBonus
            }
        }

        if (hpRatio >= 0.75) return 0.0
        var score = 0.0
        if (hpRatio < 0.6) score += aiWeights.defendHighHpBonus
        if (hpRatio < 0.4) score += aiWeights.defendMedHpBonus
        if (hpRatio < 0.25) score += aiWeights.defendLowHpBonus
        val isGuarded = enemyState.statusEffects.any { isBlockingStatus(it.id) }
        if (isGuarded) score += aiWeights.defendExistingGuardPenalty
        val weights = behaviorWeights(brain.behavior).combine(roleWeights(brain.role))
        return score * weights.defend
    }

    private fun scoreDamageTarget(
        basePower: Int,
        element: String?,
        targetState: CombatantState
    ): Double {
        if (basePower <= 0) return 0.0
        val maxHp = targetState.combatant.stats.maxHp.coerceAtLeast(1)
        val hpRatio = targetState.hp.toDouble() / maxHp
        val multiplier = affinityMultiplier(targetState, element)
        val weaknessBonus = when {
            multiplier >= 1.5 -> aiWeights.weaknessHighBonus
            multiplier > 1.0 -> aiWeights.weaknessModerateBonus
            multiplier <= 0.5 -> aiWeights.resistancePenalty
            else -> 0.0
        }
        val finishBonus = when {
            hpRatio <= 0.25 -> aiWeights.executeHighBonus
            hpRatio <= 0.45 -> aiWeights.executeModerateBonus
            else -> 0.0
        }
        return basePower * multiplier + weaknessBonus + finishBonus
    }

    private fun scoreHealTarget(skill: Skill, targetState: CombatantState): Double {
        val maxHp = targetState.combatant.stats.maxHp.coerceAtLeast(1)
        val missing = (maxHp - targetState.hp).coerceAtLeast(0)
        if (missing == 0) return 0.0
        val ratio = missing.toDouble() / maxHp
        val base = if (skill.basePower > 0) skill.basePower.toDouble() else aiWeights.healBaseScore
        return base * ratio + ratio * aiWeights.healMissingHpMultiplier
    }

    private fun scoreStatusTargets(
        statusDefs: List<StatusDefinition>,
        targetState: CombatantState?,
        isSupport: Boolean
    ): Double {
        if (statusDefs.isEmpty()) return 0.0
        val targetStatuses = targetState?.statusEffects.orEmpty()
        var total = 0.0
        statusDefs.forEach { def ->
            val normalized = def.id.lowercase(Locale.getDefault())
            val already = targetStatuses.any { it.id.equals(def.id, true) }
            val base = when (normalized) {
                "stun", "stagger" -> aiWeights.statusStun
                "blind" -> aiWeights.statusBlind
                "jammed" -> aiWeights.statusJammed
                "meltdown", "erosion", "bleed" -> aiWeights.statusDoT
                "weak", "brittle", "exposed", "short" -> aiWeights.statusDebuff
                "shield", "guard", "regen" -> aiWeights.statusBuffDefense
                "invulnerable" -> aiWeights.statusInvulnerable
                else -> aiWeights.statusDefault
            }
            val scaled = if (already) base * aiWeights.statusAlreadyAppliedMultiplier else base
            total += scaled
        }
        val sideBias = if (isSupport) 1.0 else 1.1
        return total * sideBias
    }

    private fun scoreGuardBreakTarget(targetState: CombatantState): Double {
        val isBlocking = targetState.statusEffects.any { isBlockingStatus(it.id) }
        if (isBlocking) return aiWeights.guardBreakBlocking
        val maxStability = targetState.combatant.stats.stability.coerceAtLeast(1)
        val ratio = targetState.stability.toDouble() / maxStability
        return when {
            ratio >= 0.7 -> aiWeights.guardBreakHighStability
            ratio >= 0.5 -> aiWeights.guardBreakMedStability
            else -> 0.0
        }
    }

    private fun diversityPenalty(enemyId: String, skillId: String): Double {
        val history = enemySkillHistory[enemyId] ?: return 0.0
        if (history.isEmpty()) return 0.0
        val repeatCount = history.count { it == skillId }
        val lastRepeat = if (history.lastOrNull() == skillId) 1 else 0
        return repeatCount * aiWeights.diversityRepeatPenalty + lastRepeat * aiWeights.diversityConsecutivePenalty
    }

    private fun resolveSkillElement(skill: Skill): String? {
        return skill.combatTags.orEmpty()
            .map { it.lowercase(Locale.getDefault()) }
            .firstOrNull { it in ELEMENT_TAGS }
    }

    private fun shouldTelegraph(skill: Skill): Boolean {
        val tags = skill.combatTags.orEmpty().map { it.lowercase(Locale.getDefault()) }
        if (skill.basePower >= 140) return true
        if (tags.any { it == "aoe" || it == "burst" || it == "summon" }) return true
        if (tags.any { it == "stun" || it == "stagger" || it == "guard_break" }) return true
        val statusIds = skillStatusDefinitions(skill).map { it.id.lowercase(Locale.getDefault()) }
        return statusIds.any { it in HIGH_IMPACT_STATUS_IDS }
    }

    private fun affinityMultiplier(targetState: CombatantState, element: String?): Double {
        val key = element?.lowercase(Locale.getDefault())
        val profile = targetState.combatant.resistances
        val value = when (key) {
            "burn", "fire" -> profile.burn
            "freeze", "ice" -> profile.freeze
            "shock", "lightning" -> profile.shock
            "acid", "poison", "corrosion" -> profile.acid
            "source", "harmonic", "psychic", "psionic" -> profile.source
            else -> profile.physical
        }
        val tier = ElementalAffinityRules.tierForValue(value)
        return tier.multiplier
    }

    private fun isBlockingStatus(statusId: String): Boolean {
        val normalized = statusId.trim().lowercase(Locale.getDefault())
        return normalized in BLOCKING_STATUS_IDS
    }

    private fun behaviorWeights(behavior: CombatBehavior): IntentWeights {
        return when (behavior) {
            CombatBehavior.AGGRESSIVE -> IntentWeights(
                damage = 1.25, heal = 0.65, buff = 0.75, debuff = 1.05, guardBreak = 1.1, summon = 0.9, defend = 0.7
            )
            CombatBehavior.DEFENSIVE -> IntentWeights(
                damage = 0.85, heal = 1.35, buff = 1.3, debuff = 0.9, guardBreak = 0.95, summon = 1.0, defend = 1.25
            )
            CombatBehavior.TRICKSTER -> IntentWeights(
                damage = 0.95, heal = 0.9, buff = 0.9, debuff = 1.4, guardBreak = 1.15, summon = 1.0, defend = 0.9
            )
            CombatBehavior.SUMMONER -> IntentWeights(
                damage = 0.8, heal = 1.1, buff = 1.0, debuff = 0.9, guardBreak = 0.9, summon = 1.5, defend = 1.0
            )
            CombatBehavior.BALANCED -> IntentWeights(
                damage = 1.0, heal = 1.0, buff = 1.0, debuff = 1.0, guardBreak = 1.0, summon = 1.0, defend = 1.0
            )
        }
    }

    private fun roleWeights(role: CombatRole): IntentWeights {
        return when (role) {
            CombatRole.STRIKER -> IntentWeights(
                damage = 1.3, heal = 0.65, buff = 0.8, debuff = 1.0, guardBreak = 1.15, summon = 0.9, defend = 0.8
            )
            CombatRole.TANK -> IntentWeights(
                damage = 0.85, heal = 1.05, buff = 1.2, debuff = 0.9, guardBreak = 1.05, summon = 0.9, defend = 1.3
            )
            CombatRole.SUPPORT -> IntentWeights(
                damage = 0.75, heal = 1.45, buff = 1.35, debuff = 1.0, guardBreak = 0.85, summon = 1.0, defend = 1.05
            )
            CombatRole.CONTROLLER -> IntentWeights(
                damage = 0.85, heal = 0.85, buff = 0.95, debuff = 1.45, guardBreak = 1.2, summon = 0.95, defend = 0.9
            )
            CombatRole.SUMMONER -> IntentWeights(
                damage = 0.8, heal = 1.0, buff = 1.0, debuff = 0.9, guardBreak = 0.9, summon = 1.4, defend = 1.0
            )
        }
    }

    private fun parseBehavior(raw: String?): CombatBehavior? {
        val normalized = raw?.trim()?.lowercase(Locale.getDefault()) ?: return null
        return when (normalized) {
            "aggressive", "berserk", "offense" -> CombatBehavior.AGGRESSIVE
            "defensive", "defense", "guarded" -> CombatBehavior.DEFENSIVE
            "trickster", "controller", "debuffer" -> CombatBehavior.TRICKSTER
            "summoner", "summon" -> CombatBehavior.SUMMONER
            "balanced", "default" -> CombatBehavior.BALANCED
            else -> null
        }
    }

    private fun parseRole(raw: String?): CombatRole? {
        val normalized = raw?.trim()?.lowercase(Locale.getDefault()) ?: return null
        return when (normalized) {
            "striker", "damage", "dps" -> CombatRole.STRIKER
            "tank", "defender", "guard" -> CombatRole.TANK
            "support", "healer" -> CombatRole.SUPPORT
            "controller", "debuffer", "trickster" -> CombatRole.CONTROLLER
            "summoner", "summon" -> CombatRole.SUMMONER
            else -> null
        }
    }

    private fun inferBehavior(skills: List<Skill>): CombatBehavior {
        if (skills.isEmpty()) return CombatBehavior.BALANCED
        val tags = skills.flatMap { it.combatTags.orEmpty() }.map { it.lowercase(Locale.getDefault()) }
        if (tags.contains("summon")) return CombatBehavior.SUMMONER
        val healCount = skills.count { isSupportSkill(it) && it.combatTags.orEmpty().any { tag -> tag.equals("heal", true) } }
        val debuffCount = skills.count { it.combatTags.orEmpty().any { tag -> tag.equals("debuff", true) } }
        val damageCount = skills.count { it.basePower > 0 && !isSupportSkill(it) }
        return when {
            healCount >= 2 -> CombatBehavior.DEFENSIVE
            debuffCount >= 2 -> CombatBehavior.TRICKSTER
            damageCount >= (skills.size + 1) / 2 -> CombatBehavior.AGGRESSIVE
            else -> CombatBehavior.BALANCED
        }
    }

    private fun inferRole(skills: List<Skill>): CombatRole {
        if (skills.isEmpty()) return CombatRole.STRIKER
        val tags = skills.flatMap { it.combatTags.orEmpty() }.map { it.lowercase(Locale.getDefault()) }
        val statusDefs = skills.flatMap { skillStatusDefinitions(it) }
        val summonScore = if (tags.contains("summon")) 3 else 0
        val supportScore = skills.count { isSupportSkill(it) } + statusDefs.count { it.id == "shield" || it.id == "guard" || it.id == "regen" }
        val debuffScore = tags.count { it == "debuff" } + statusDefs.count { it.target?.equals("enemy", true) == true }
        val guardScore = statusDefs.count { it.id == "shield" || it.id == "guard" }
        val damageScore = skills.count { it.basePower >= 110 && !isSupportSkill(it) } + tags.count { it == "dmg" }
        val scores = mapOf(
            CombatRole.SUMMONER to summonScore,
            CombatRole.SUPPORT to supportScore,
            CombatRole.CONTROLLER to debuffScore,
            CombatRole.TANK to guardScore,
            CombatRole.STRIKER to damageScore
        )
        return scores.maxByOrNull { it.value }?.key ?: CombatRole.STRIKER
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
                    setCombatBanner(entry, updated)
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
                    val fxElement = resolveImpactElement(entry, updated)
                    if (fxElement == "blocked" && entry.sourceId in playerIdList) {
                        maybePlayShieldBlockCue()
                    }
                    if (shouldEmitShieldBreak(entry, currentAction, previous, updated)) {
                        combatFxEvents.tryEmit(CombatFxEvent.ShieldBreak(targetId = entry.targetId))
                        maybePlayShieldBreakCue()
                    }
                    emitImpact(entry, fxElement, showAttackFx, targetDefeated, 0L)
                    setCombatBanner(entry, updated)
                }
                is CombatLogEntry.WeaknessReward -> {
                    if (entry.actorId in playerIdList) {
                        tickPlayerCooldowns(entry.actorId)
                    }
                }
                is CombatLogEntry.Heal -> {
                    combatFxEvents.tryEmit(
                        CombatFxEvent.Heal(
                            sourceId = entry.sourceId,
                            targetId = entry.targetId,
                            amount = entry.amount
                        )
                    )
                    setCombatBanner(entry, updated)
                }
                is CombatLogEntry.StatusApplied -> {
                    emitStatusApplied(entry, 0L)
                    setCombatBanner(entry, updated)
                }
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
                    setCombatBanner(entry, updated)
                    if (updated.outcome != null) {
                        readyQueue.clear()
                        enemyTurnJob?.cancel()
                        enemyTurnJob = null
                        cancelTimedPrompt()
                        setAwaitingAction(null)
                    }
                }
                is CombatLogEntry.StatusExpired,
                is CombatLogEntry.TurnSkipped -> setCombatBanner(entry, updated)
                else -> Unit
            }
        }
        val newlyBroken = updated.combatants.mapNotNull { (id, updatedState) ->
            val before = previous.combatants[id]?.breakTurns ?: 0
            if (before <= 0 && updatedState.breakTurns > 0) id else null
        }
        if (newlyBroken.isNotEmpty()) {
            _atbMeters.update { existing ->
                if (existing.isEmpty()) existing else existing + newlyBroken.associateWith { 0f }
            }
            newlyBroken.forEach { removeFromReadyQueue(it) }
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

    private fun emitImpact(
        entry: CombatLogEntry.Damage,
        elementOverride: String?,
        showAttackFx: Boolean,
        targetDefeated: Boolean,
        delayMs: Long
    ) {
        if (delayMs <= 0L) {
            combatFxEvents.tryEmit(
                CombatFxEvent.Impact(
                    sourceId = entry.sourceId,
                    targetId = entry.targetId,
                    amount = entry.amount,
                    element = elementOverride ?: entry.element,
                    critical = entry.critical,
                    showAttackFx = showAttackFx,
                    targetDefeated = targetDefeated
                )
            )
            if (targetDefeated) {
                combatFxEvents.tryEmit(CombatFxEvent.Knockout(entry.targetId))
            }
            return
        }
        viewModelScope.launch {
            delay(delayMs)
            combatFxEvents.tryEmit(
                CombatFxEvent.Impact(
                    sourceId = entry.sourceId,
                    targetId = entry.targetId,
                    amount = entry.amount,
                    element = elementOverride ?: entry.element,
                    critical = entry.critical,
                    showAttackFx = showAttackFx,
                    targetDefeated = targetDefeated
                )
            )
            if (targetDefeated) {
                combatFxEvents.tryEmit(CombatFxEvent.Knockout(entry.targetId))
            }
        }
    }

    private fun resolveImpactElement(entry: CombatLogEntry.Damage, state: CombatState): String? {
        val normalized = entry.element?.trim()?.lowercase(Locale.getDefault())
        if (entry.amount != 0) return entry.element
        if (normalized == "miss") return entry.element
        val target = state.combatants[entry.targetId] ?: return entry.element
        val blocked = target.statusEffects.any { effect ->
            when (effect.id.trim().lowercase(Locale.getDefault())) {
                "invulnerable", "shield", "guard" -> effect.stacks > 0
                else -> false
            }
        }
        return if (blocked) "blocked" else entry.element
    }

    private fun maybePlayShieldBlockCue() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastShieldBlockCueAtMs < 280L) return
        lastShieldBlockCueAtMs = now
        playBattleCue("shield_block")
    }

    private fun maybePlayShieldBreakCue() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastShieldBreakCueAtMs < 280L) return
        lastShieldBreakCueAtMs = now
        playBattleCue("shield_break")
    }

    private fun shouldEmitShieldBreak(
        entry: CombatLogEntry.Damage,
        currentAction: CombatAction?,
        previous: CombatState,
        updated: CombatState
    ): Boolean {
        if (entry.sourceId !in playerIdList) return false
        val skillUse = currentAction as? CombatAction.SkillUse ?: return false
        if (skillUse.actorId != entry.sourceId) return false
        val skill = skillById[skillUse.skillId] ?: return false
        val guardBreak = skill.combatTags.orEmpty().any { tag -> tag.equals("guard_break", ignoreCase = true) }
        if (!guardBreak) return false
        val before = previous.combatants[entry.targetId] ?: return false
        val after = updated.combatants[entry.targetId] ?: return false
        val beforeInvulnerable = before.statusEffects.any { it.id.equals("invulnerable", ignoreCase = true) && it.stacks > 0 }
        val afterInvulnerable = after.statusEffects.any { it.id.equals("invulnerable", ignoreCase = true) && it.stacks > 0 }
        return beforeInvulnerable && !afterInvulnerable
    }

    private fun emitStatusApplied(entry: CombatLogEntry.StatusApplied, delayMs: Long) {
        if (delayMs <= 0L) {
            combatFxEvents.tryEmit(
                CombatFxEvent.StatusApplied(
                    targetId = entry.targetId,
                    statusId = entry.statusId,
                    stacks = entry.stacks
                )
            )
            return
        }
        viewModelScope.launch {
            delay(delayMs)
            combatFxEvents.tryEmit(
                CombatFxEvent.StatusApplied(
                    targetId = entry.targetId,
                    statusId = entry.statusId,
                    stacks = entry.stacks
                )
            )
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
        val recoveredBreaks = mutableListOf<String>()
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
            val breakTurns = combatantState.breakTurns.coerceAtLeast(0)
            val breakSlowdown = if (breakTurns > 0) breakTurns.toFloat() else 1f
            val gain = deltaSeconds * speed / ATB_SPEED_SCALE / breakSlowdown
            val current = meters.getOrElse(id) { 0f }
            val next = (current + gain).coerceIn(0f, 1f)
            meters[id] = next
            if (next >= 0.999f) {
                if (breakTurns > 0) {
                    recoveredBreaks += id
                }
                readyQueue.add(id)
                meters[id] = 1f
                updateActiveActorFromQueue()
            }
        }
        _atbMeters.value = meters
        if (recoveredBreaks.isNotEmpty()) {
            updateState { state ->
                val updated = state.combatants.mapValues { (id, combatant) ->
                    if (id in recoveredBreaks && combatant.breakTurns > 0) {
                        combatant.copy(breakTurns = 0)
                    } else {
                        combatant
                    }
                }
                state.copy(combatants = updated)
            }
        }
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

    private data class BannerSession(
        val id: String,
        val actorId: String,
        val primary: String,
        val accent: CombatBannerAccent,
        val icon: CombatBannerIcon?
    )

    private fun setCombatBanner(
        entry: CombatLogEntry,
        state: CombatState,
        currentAction: CombatAction? = null
    ) {
        when (combatTextVerbosity) {
            CombatTextVerbosity.VERBOSE -> {
                val message = describeEntry(entry, state).orEmpty().trim()
                if (message.isNotBlank()) {
                    bannerSession = null
                    _combatBanner.value = CombatBannerMessage(
                        id = UUID.randomUUID().toString(),
                        primary = message,
                        accent = CombatBannerAccent.DEFAULT,
                        icon = null
                    )
                }
                return
            }
            else -> Unit
        }

        val update = when (entry) {
            is CombatLogEntry.ActionQueued -> bannerForActionQueued(entry, state)
            is CombatLogEntry.Damage -> bannerForDamage(entry, state)
            is CombatLogEntry.Heal -> bannerForHeal(entry, state, currentAction)
            is CombatLogEntry.StatusApplied -> bannerForStatusApplied(entry, state)
            is CombatLogEntry.StatusExpired -> bannerForStatusExpired(entry, state)
            is CombatLogEntry.TurnSkipped -> bannerForTurnSkipped(entry, state)
            is CombatLogEntry.Outcome -> bannerForOutcome(entry)
            is CombatLogEntry.WeaknessReward -> null

        }
        if (update != null) {
            _combatBanner.value = update
        }
    }

    private fun bannerForActionQueued(
        entry: CombatLogEntry.ActionQueued,
        state: CombatState
    ): CombatBannerMessage? {
        if (entry.action is CombatAction.BasicAttack) {
            bannerSession = null
            return null
        }
        val actorId = entry.actorId
        val actionDescriptor = actionDescriptor(entry.action)
        val primary = actionDescriptor.label
        val session = BannerSession(
            id = UUID.randomUUID().toString(),
            actorId = actorId,
            primary = primary,
            accent = actionDescriptor.accent ?: accentForActor(actorId, state),
            icon = actionDescriptor.icon
        )
        bannerSession = session
        if (!shouldShowActionBanner(actorId = actorId, action = entry.action, state = state)) {
            return null
        }
        return CombatBannerMessage(
            id = session.id,
            primary = session.primary,
            accent = session.accent,
            icon = session.icon
        )
    }

    private fun bannerForDamage(
        entry: CombatLogEntry.Damage,
        state: CombatState
    ): CombatBannerMessage? {
        val isStatusTick = entry.sourceId.startsWith(STATUS_SOURCE_PREFIX)
        val isMiss = entry.element?.equals("miss", ignoreCase = true) == true
        if (isMiss) return null
        if (!isStatusTick) return null
        val targetIsPlayer = entry.targetId in playerIdList
        if (combatTextVerbosity != CombatTextVerbosity.VERBOSE && !targetIsPlayer) return null
        val statusId = entry.sourceId.removePrefix(STATUS_SOURCE_PREFIX)
        val statusName = statusDisplayName(statusId)
        val secondary = entry.amount.takeIf { it > 0 }?.let { "-$it" }
        return CombatBannerMessage(
            id = UUID.randomUUID().toString(),
            primary = statusName,
            secondary = secondary,
            accent = accentForElement(entry.element),
            icon = CombatBannerIcon.STATUS,
            importance = CombatBannerImportance.NORMAL
        )
    }

    private fun bannerForHeal(
        entry: CombatLogEntry.Heal,
        state: CombatState,
        currentAction: CombatAction?
    ): CombatBannerMessage? {
        if (combatTextVerbosity == CombatTextVerbosity.MINIMAL) return null
        val isStatusTick = entry.sourceId.startsWith(STATUS_SOURCE_PREFIX)
        if (isStatusTick) {
            val statusId = entry.sourceId.removePrefix(STATUS_SOURCE_PREFIX)
            val statusName = statusDisplayName(statusId)
            return CombatBannerMessage(
                id = UUID.randomUUID().toString(),
                primary = statusName,
                secondary = "+${entry.amount} HP",
                accent = CombatBannerAccent.HEAL,
                icon = CombatBannerIcon.HEAL,
                importance = CombatBannerImportance.NORMAL
            )
        }

        val session = bannerSession?.takeIf { it.actorId == entry.sourceId }
        val actionFallback = currentAction?.takeIf { it.actorId == entry.sourceId }
        val primary = session?.primary ?: run {
            actionFallback?.let { actionDescriptor(it).label } ?: "Heal"
        }
        val messageId = session?.id ?: UUID.randomUUID().toString()
        val icon = session?.icon ?: actionFallback?.let { actionDescriptor(it).icon } ?: CombatBannerIcon.HEAL
        return CombatBannerMessage(
            id = messageId,
            primary = primary,
            secondary = "+${entry.amount} HP",
            accent = CombatBannerAccent.HEAL,
            icon = icon,
            importance = CombatBannerImportance.IMPORTANT
        )
    }

    private fun bannerForStatusApplied(
        entry: CombatLogEntry.StatusApplied,
        state: CombatState
    ): CombatBannerMessage? {
        val targetIsPlayer = entry.targetId in playerIdList
        if (combatTextVerbosity != CombatTextVerbosity.VERBOSE && !targetIsPlayer) return null

        val statusName = statusDisplayName(entry.statusId)
        val stacksSuffix = if (entry.stacks > 1) " x${entry.stacks}" else ""
        val importance = if (targetIsPlayer) CombatBannerImportance.IMPORTANT else CombatBannerImportance.NORMAL
        val session = bannerSession
        val primary = session?.primary ?: "$statusName$stacksSuffix"
        val secondary = if (session != null) "$statusName$stacksSuffix" else null
        return CombatBannerMessage(
            id = session?.id ?: UUID.randomUUID().toString(),
            primary = primary,
            secondary = secondary,
            accent = accentForStatus(entry.statusId),
            icon = CombatBannerIcon.STATUS,
            importance = importance
        )
    }

    private fun bannerForStatusExpired(
        entry: CombatLogEntry.StatusExpired,
        state: CombatState
    ): CombatBannerMessage? {
        // We no longer show text for expired statuses; the icon simply disappears from the UI.
        return null
    }



        private fun bannerForTurnSkipped(
            entry: CombatLogEntry.TurnSkipped,
            state: CombatState
        ): CombatBannerMessage? {
            val chargingWeapon = chargingWeaponName(entry.reason)
            if (chargingWeapon == null && combatTextVerbosity != CombatTextVerbosity.VERBOSE && entry.actorId !in playerIdList) {
                return null
            }
            val reason = chargingWeapon?.let { "$it..." } ?: skipReasonLabel(entry.reason)
            bannerSession = null
            return CombatBannerMessage(
                id = UUID.randomUUID().toString(),
                primary = reason,
                accent = CombatBannerAccent.DEFAULT,            icon = if (chargingWeapon != null) CombatBannerIcon.ATTACK else CombatBannerIcon.STATUS,
            importance = if (chargingWeapon != null) CombatBannerImportance.IMPORTANT else CombatBannerImportance.IMPORTANT
        )
    }

    private fun bannerForOutcome(entry: CombatLogEntry.Outcome): CombatBannerMessage? {
        bannerSession = null
        return null
    }

    private data class ActionDescriptor(
        val label: String,
        val icon: CombatBannerIcon,
        val accent: CombatBannerAccent? = null
    )

    private fun actionDescriptor(action: CombatAction): ActionDescriptor =
        when (action) {
            is CombatAction.BasicAttack -> ActionDescriptor(label = "Strike", icon = CombatBannerIcon.ATTACK)
            is CombatAction.SkillUse -> ActionDescriptor(
                label = skillById[action.skillId]?.name ?: "Skill",
                icon = CombatBannerIcon.SKILL
            )
            is CombatAction.ItemUse -> ActionDescriptor(
                label = itemDisplayName(action.itemId),
                icon = CombatBannerIcon.ITEM
            )
            is CombatAction.SnackUse -> ActionDescriptor(
                label = itemDisplayName(action.snackItemId),
                icon = CombatBannerIcon.SNACK
            )
            is CombatAction.SupportAbility -> ActionDescriptor(label = "Support", icon = CombatBannerIcon.SKILL)
            is CombatAction.Defend -> ActionDescriptor(label = "Guard", icon = CombatBannerIcon.GUARD)
            is CombatAction.Flee -> ActionDescriptor(label = "Retreat", icon = CombatBannerIcon.RETREAT)
        }

    private fun shouldShowActionBanner(
        actorId: String,
        action: CombatAction,
        state: CombatState
    ): Boolean {
        val side = state.combatants[actorId]?.combatant?.side
        val isEnemy = side == CombatSide.ENEMY
        return when (combatTextVerbosity) {
            CombatTextVerbosity.MINIMAL -> !isEnemy
            CombatTextVerbosity.STANDARD -> !isEnemy || action !is CombatAction.BasicAttack
            CombatTextVerbosity.VERBOSE -> true
        }
    }

    private fun combatantName(state: CombatState, combatantId: String): String =
        state.combatants[combatantId]?.combatant?.name ?: combatantId

    private fun accentForActor(actorId: String, state: CombatState): CombatBannerAccent {
        val normalized = actorId.lowercase(Locale.getDefault()).removePrefix("player_")
        return when {
            normalized.contains("nova") -> CombatBannerAccent.NOVA
            normalized.contains("zeke") -> CombatBannerAccent.ZEKE
            normalized.contains("orion") -> CombatBannerAccent.ORION
            normalized.contains("gh0st") || normalized.contains("ghost") -> CombatBannerAccent.GHOST
            state.combatants[actorId]?.combatant?.side == CombatSide.ENEMY -> CombatBannerAccent.ENEMY
            else -> CombatBannerAccent.DEFAULT
        }
    }

    private fun statusDisplayName(statusId: String): String =
        statusRegistry.definition(statusId)?.displayName?.takeIf { it.isNotBlank() }
            ?: statusRegistry.definition(statusId)?.name?.takeIf { it.isNotBlank() }
            ?: statusId.replace('_', ' ').trim().replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }

    private fun accentForStatus(statusId: String): CombatBannerAccent {
        val element = statusRegistry.definition(statusId)?.tick?.element
        return accentForElement(element)
    }

    private fun accentForElement(element: String?): CombatBannerAccent {
        val normalized = element?.trim()?.lowercase(Locale.getDefault())
        return when (normalized) {
            "miss" -> CombatBannerAccent.MISS
            "heal" -> CombatBannerAccent.HEAL
            "fire", "burn" -> CombatBannerAccent.BURN
            "ice", "freeze" -> CombatBannerAccent.FREEZE
            "lightning", "shock" -> CombatBannerAccent.SHOCK
            "poison", "acid", "corrosion" -> CombatBannerAccent.ACID
            "source", "harmonic", "psychic", "psionic", "void" -> CombatBannerAccent.SOURCE
            "physical" -> CombatBannerAccent.PHYSICAL
            else -> CombatBannerAccent.DEFAULT
        }
    }

    private fun skipReasonLabel(reason: String): String {
        val cleaned = reason.trim()
        val lowered = cleaned.lowercase(Locale.getDefault())
        val withoutIs = if (lowered.startsWith("is ")) cleaned.drop(3) else cleaned
        return withoutIs.replace('_', ' ').trim().replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }

    private fun chargingWeaponName(reason: String): String? {
        val match = Regex("""\bcharging\s+(.+)$""", RegexOption.IGNORE_CASE)
            .find(reason.trim())
        val weapon = match?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return weapon.takeIf { it.isNotBlank() }
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
            val skillUse = action as? CombatAction.SkillUse
            val telegraphSkill = skillUse?.let { skillById[it.skillId] }
            if (telegraphSkill != null && shouldTelegraph(telegraphSkill) && skillUse != null) {
                combatFxEvents.tryEmit(
                    CombatFxEvent.Telegraph(
                        actorId = enemyId,
                        skillName = telegraphSkill.name,
                        targetIds = skillUse.targetIds
                    )
                )
                delay(420)
            }
            maybeTriggerAttackLunge(action)
            // Guard minigame disabled: enemy attacks always resolve immediately.
            suppressMissLungeTargets = emptySet()
            var acted = false
            updateState { current ->
                val enemyState = current.combatants[enemyId] ?: return@updateState current
                if (!enemyState.isAlive) return@updateState current
                tickEnemyCooldowns()
                val resolved = actionProcessor.execute(current, action, ::victoryReward)
                registerEnemyActionCooldown(action)
                acted = true
                resolved.applyOutcomeResults(current)
            }
            if (acted) {
                recordEnemySkillUse(action)
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
                stability = 100,
                accuracyBonus = bonuses.accuracyBonus,
                evasionBonus = bonuses.evasionBonus,
                critBonus = bonuses.critBonus,
                flatDamageReduction = bonuses.flatDamageReduction
            ),
            resistances = ResistanceProfile(),
            skills = skills,
            weapon = resolveCombatWeapon(id, equippedItems, equippedWeapons, unlockedWeapons),
            brokenTurns = 1
        )
        }

    private fun Enemy.toCombatant(combatantId: String = id): Combatant =
        run {
            val baseProfile = ElementalAffinityRules.fromTags(tags)
            val resolvedProfile = ElementalAffinityRules.applyOverrides(baseProfile, resistances)
            val maxHp = CombatFormulas.maxHp(hp, vitality)
            val resolvedStability = stability ?: CombatFormulas.stabilityForTier(maxHp, tier)
            Combatant(
                id = combatantId,
                name = name,
                side = CombatSide.ENEMY,
                stats = StatBlock(
                    maxHp = maxHp,
                    strength = strength,
                    vitality = vitality,
                    agility = agility,
                    focus = focus,
                    luck = luck,
                    speed = CombatFormulas.speed(speed, agility).roundToInt(),
                    stability = resolvedStability
                ),
                resistances = resolvedProfile,
                skills = abilities,
                brokenTurns = (brokenTurns ?: 1).coerceAtLeast(1)
            )
        }

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
        val attackerSide = attackerState?.combatant?.side ?: CombatSide.PLAYER
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
            SkillTargeting.ALL_ENEMIES -> {
                if (attackerSide == CombatSide.ENEMY) {
                    playerIdList.filter { state.combatants[it]?.isAlive == true }
                } else {
                    enemyIdList.filter { state.combatants[it]?.isAlive == true }
                }
            }
            SkillTargeting.SINGLE_ENEMY -> {
                if (attackerSide == CombatSide.ENEMY) {
                    resolvePlayerTargets(state)
                } else {
                    resolveEnemyTargets(state)
                }
            }
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



    private data class EnemyBrain(
        val behavior: CombatBehavior = CombatBehavior.BALANCED,
        val role: CombatRole = CombatRole.STRIKER
    )

    private data class IntentWeights(
        val damage: Double,
        val heal: Double,
        val buff: Double,
        val debuff: Double,
        val guardBreak: Double,
        val summon: Double,
        val defend: Double
    ) {
        fun combine(other: IntentWeights): IntentWeights {
            return IntentWeights(
                damage = damage * other.damage,
                heal = heal * other.heal,
                buff = buff * other.buff,
                debuff = debuff * other.debuff,
                guardBreak = guardBreak * other.guardBreak,
                summon = summon * other.summon,
                defend = defend * other.defend
            )
        }
    }

    private data class ScoredAction(
        val score: Double,
        val action: CombatAction
    )

    private enum class CombatBehavior {
        AGGRESSIVE,
        DEFENSIVE,
        TRICKSTER,
        SUMMONER,
        BALANCED
    }

    private enum class CombatRole {
        STRIKER,
        TANK,
        SUPPORT,
        CONTROLLER,
        SUMMONER
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

            is CombatLogEntry.TurnSkipped -> {
                val actorName = state.combatants[entry.actorId]?.combatant?.name ?: entry.actorId
                "$actorName ${entry.reason}"
            }
            is CombatLogEntry.ActionQueued -> {
                val actorName = state.combatants[entry.actorId]?.combatant?.name ?: entry.actorId
                "$actorName lines up an action"
            }
            is CombatLogEntry.WeaknessReward -> null
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
        private const val ENEMY_SKILL_MEMORY = 3

        private const val TIMED_GUARD_WINDOW_MS = 700L
        private const val TIMED_GUARD_DEF_BONUS = 10
        private const val ZEKE_SUPPORT_ATB_BONUS = 0.25f
        private const val SNACK_COOLDOWN_TURNS = 5

        private val BLOCKING_STATUS_IDS = setOf("invulnerable", "shield", "guard", "defend")
        private val ELEMENT_TAGS = setOf("burn", "freeze", "shock", "acid", "source", "physical")
        private val HIGH_IMPACT_STATUS_IDS = setOf(
            "stun",
            "stagger",
            "blind",
            "jammed",
            "meltdown",
            "erosion",
            "short"
        )
    }
}


enum class TargetRequirement {
    NONE,
    ENEMY,
    ALLY,
    ANY
}
