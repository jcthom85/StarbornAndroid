package com.example.starborn.feature.combat.viewmodel

import androidx.lifecycle.ViewModel
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.audio.AudioCommand
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.combat.CombatAction
import com.example.starborn.domain.combat.CombatActionProcessor
import com.example.starborn.domain.combat.CombatEngine
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatReward
import com.example.starborn.domain.combat.CombatSetup
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.Combatant
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.combat.CombatLogEntry
import com.example.starborn.domain.combat.LootDrop
import com.example.starborn.domain.combat.ResistanceProfile
import com.example.starborn.domain.combat.StatBlock
import com.example.starborn.domain.combat.StatusRegistry
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.leveling.ProgressionData
import com.example.starborn.domain.leveling.SkillUnlockSummary
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.StatusDefinition
import com.example.starborn.domain.session.GameSessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class CombatViewModel(
    worldAssets: WorldAssetDataSource,
    private val combatEngine: CombatEngine,
    private val statusRegistry: StatusRegistry,
    private val sessionStore: GameSessionStore,
    private val inventoryService: InventoryService,
    private val levelingManager: LevelingManager,
    private val progressionData: ProgressionData,
    private val audioRouter: AudioRouter,
    enemyIds: List<String>
) : ViewModel() {

    val player: Player?
    val playerParty: List<Player>
    val enemies: List<Enemy>

    private val playerSkillsById: Map<String, List<Skill>>
    private val playerCombatants: List<Combatant>
    private val enemyCombatants: List<Combatant>
    private val playerIdList: List<String>
    private val enemyIdList: List<String>
    private val skillById: Map<String, Skill>
    private val actionProcessor: CombatActionProcessor
    private val enemyCooldowns: MutableMap<String, Int> = mutableMapOf()
    private val combatFxEvents = MutableSharedFlow<CombatFxEvent>(extraBufferCapacity = 16)
    val fxEvents: SharedFlow<CombatFxEvent> = combatFxEvents.asSharedFlow()
    private val pendingLevelUps = mutableListOf<LevelUpSummary>()

    private val _state = MutableStateFlow<CombatState?>(null)
    val state: StateFlow<CombatState?> = _state.asStateFlow()
    val combatState: CombatState? get() = _state.value
    val encounterEnemyIds: List<String> get() = enemyIdList
    private val _selectedEnemies = MutableStateFlow<Set<String>>(emptySet())
    val selectedEnemies: StateFlow<Set<String>> = _selectedEnemies.asStateFlow()
    val inventory: StateFlow<List<InventoryEntry>> = inventoryService.state

    init {
        val players = worldAssets.loadCharacters()
        val allEnemies = worldAssets.loadEnemies()
        val allSkills = worldAssets.loadSkills()

        val partyIds = sessionStore.state.value.partyMembers
        val resolvedParty = (if (partyIds.isEmpty()) players.take(1) else partyIds.mapNotNull { id ->
            players.find { it.id == id }
        }).ifEmpty { players.take(1) }

        playerParty = resolvedParty
        player = playerParty.firstOrNull()
        enemies = enemyIds.mapNotNull { id -> allEnemies.find { it.id == id } }

        playerSkillsById = playerParty.associate { member ->
            member.id to allSkills.filter { it.character == member.id }
        }

        playerCombatants = playerParty.map { it.toCombatant() }
        enemyCombatants = enemies.map { it.toCombatant() }
        playerIdList = playerCombatants.map { it.id }
        enemyIdList = enemyCombatants.map { it.id }

        skillById = allSkills.associateBy { it.id }
        actionProcessor = CombatActionProcessor(
            engine = combatEngine,
            statusRegistry = statusRegistry,
            skillLookup = { id -> skillById[id] },
            consumeItem = { itemId -> inventoryService.useItem(itemId) }
        )

        if (playerCombatants.isNotEmpty() && enemyCombatants.isNotEmpty()) {
            _state.value = combatEngine.beginEncounter(
                CombatSetup(
                    playerParty = playerCombatants,
                    enemyParty = enemyCombatants
                )
            ).autoResolveEnemyTurns()
            playBattleCue("start")
        }
        refreshSelection(_state.value)
    }

    fun playerAttack() {
        updateState { current ->
            val attackerId = resolveActivePlayerId(current) ?: return@updateState current
            val targetId = resolveEnemyTargets(current).firstOrNull() ?: return@updateState current
            playUiCue("confirm")
            val action = CombatAction.BasicAttack(attackerId, targetId)
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            val withRewards = resolved.applyOutcomeResults(current)
            withRewards.autoResolveEnemyTurns()
        }
    }

    fun useSkill(skill: Skill) {
        updateState { current ->
            val attackerId = resolveActivePlayerId(current) ?: return@updateState current
            if (skillsForPlayer(attackerId).none { it.id == skill.id }) return@updateState current
            val targets = resolveSkillTargets(skill, current, attackerId)
                .ifEmpty { resolveEnemyTargets(current) }
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
            withRewards.autoResolveEnemyTurns()
        }
    }

    fun useItem(entry: InventoryEntry) {
        updateState { current ->
            val attackerId = resolveActivePlayerId(current) ?: return@updateState current
            val targetId = when {
                entry.item.effect?.damage?.let { it > 0 } == true -> resolveEnemyTargets(current).firstOrNull()
                else -> null
            }
            playUiCue("confirm")
            val action = CombatAction.ItemUse(attackerId, entry.item.id, targetId)
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            val withRewards = resolved.applyOutcomeResults(current)
            withRewards.autoResolveEnemyTurns()
        }
    }

    fun defend() {
        updateState { current ->
            val attackerId = resolveActivePlayerId(current) ?: return@updateState current
            playUiCue("confirm")
            val action = CombatAction.Defend(attackerId)
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            resolved.applyOutcomeResults(current).autoResolveEnemyTurns()
        }
    }

    fun attemptRetreat() {
        updateState { current ->
            val attackerId = resolveActivePlayerId(current) ?: return@updateState current
            playUiCue("confirm")
            val action = CombatAction.Flee(attackerId)
            val resolved = actionProcessor.execute(current, action, ::victoryReward)
            resolved.applyOutcomeResults(current)
        }
    }

    fun focusEnemyTarget(enemyId: String) {
        val state = _state.value ?: return
        if (enemyId !in enemyIdList) return
        if (state.combatants[enemyId]?.isAlive != true) return
        playUiCue("click")
        setSelection(setOf(enemyId))
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

    private fun CombatState.autoResolveEnemyTurns(): CombatState {
        var working = this
        while (working.outcome == null) {
            val active = working.activeCombatant ?: break
            if (active.combatant.side != CombatSide.ENEMY) break
            tickEnemyCooldowns()
            val beforeAction = working
            val action = selectEnemyAction(beforeAction, active)
            val resolved = actionProcessor.execute(beforeAction, action, ::victoryReward)
            registerEnemyActionCooldown(action)
            working = resolved.applyOutcomeResults(beforeAction)
        }
        return working
    }

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
    }

    private fun processNewLogEntries(previous: CombatState, updated: CombatState) {
        val previousSize = previous.log.size
        if (updated.log.size <= previousSize) return
        val newEntries = updated.log.drop(previousSize)
        newEntries.forEach { entry ->
            when (entry) {
                is CombatLogEntry.ActionQueued -> {
                    combatFxEvents.tryEmit(CombatFxEvent.TurnQueued(entry.actorId))
                }
                is CombatLogEntry.Damage -> {
                    combatFxEvents.tryEmit(
                        CombatFxEvent.Impact(
                            sourceId = entry.sourceId,
                            targetId = entry.targetId,
                            amount = entry.amount,
                            element = entry.element,
                            critical = entry.critical
                        )
                    )
                    val targetState = updated.combatants[entry.targetId]
                    if (targetState?.isAlive == false) {
                        combatFxEvents.tryEmit(CombatFxEvent.Knockout(entry.targetId))
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
                }
                is CombatLogEntry.StatusApplied -> {
                    combatFxEvents.tryEmit(
                        CombatFxEvent.StatusApplied(
                            targetId = entry.targetId,
                            statusId = entry.statusId,
                            stacks = entry.stacks
                        )
                    )
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
                }
                else -> Unit
            }
        }
    }

    private fun Player.toCombatant(): Combatant =
        Combatant(
            id = id,
            name = name,
            side = CombatSide.PLAYER,
            stats = StatBlock(
                maxHp = hp,
                maxRp = hp,
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

    private fun Enemy.toCombatant(): Combatant =
        Combatant(
            id = id,
            name = name,
            side = CombatSide.ENEMY,
            stats = StatBlock(
                maxHp = hp,
                maxRp = vitality,
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
        enemies.forEach { enemy ->
            xpTotal += enemy.xpReward
            apTotal += enemy.apReward
            creditsTotal += enemy.creditReward
            enemy.drops.forEach { drop ->
                val qty = (drop.quantity ?: drop.qtyMax ?: drop.qtyMin ?: 1).coerceAtLeast(1)
                dropAccumulator[drop.id] = dropAccumulator.getOrDefault(drop.id, 0) + qty
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
            if (drop.quantity > 0) {
                inventoryService.addItem(drop.itemId, drop.quantity)
            }
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
}
