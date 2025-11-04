package com.example.starborn.feature.combat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.combat.ActiveBuff
import com.example.starborn.domain.combat.StatusEffect
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Player
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.CombatFxEvent
import com.example.starborn.ui.dialogs.SkillsDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.navigation.CombatResultPayload
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.ui.vfx.ThemeBandOverlay
import com.example.starborn.ui.vfx.VignetteOverlay
import com.example.starborn.ui.vfx.WeatherOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private const val STATUS_SOURCE_PREFIX = "status_"

private data class DamageFxUi(
    val id: String,
    val targetId: String,
    val amount: Int,
    val critical: Boolean
)

private data class HealFxUi(
    val id: String,
    val targetId: String,
    val amount: Int
)

private data class StatusFxUi(
    val id: String,
    val targetId: String,
    val statusId: String,
    val stacks: Int
)

private data class KnockoutFxUi(
    val id: String,
    val targetId: String
)

private data class SupportFxUi(
    val id: String,
    val actorId: String,
    val skillName: String,
    val targetIds: List<String>
)

@Composable
fun CombatScreen(
    navController: NavController,
    viewModel: CombatViewModel,
    audioCuePlayer: AudioCuePlayer
) {
    val playerParty = viewModel.playerParty
    val enemies = viewModel.enemies
    val combatState by viewModel.state.collectAsState(initial = viewModel.combatState)
    val inventoryEntries by viewModel.inventory.collectAsStateWithLifecycle()
    val damageFx = remember { mutableStateListOf<DamageFxUi>() }
    val healFx = remember { mutableStateListOf<HealFxUi>() }
    val statusFx = remember { mutableStateListOf<StatusFxUi>() }
    val knockoutFx = remember { mutableStateListOf<KnockoutFxUi>() }
    val supportFx = remember { mutableStateListOf<SupportFxUi>() }
    var outcomeFx by remember { mutableStateOf<CombatFxEvent.CombatOutcomeFx.OutcomeType?>(null) }
    var pendingOutcome by remember { mutableStateOf<CombatOutcome?>(null) }
    val selectedEnemyIds by viewModel.selectedEnemies.collectAsStateWithLifecycle()

    val outcome = combatState?.outcome
    if (outcome != null && pendingOutcome == null) {
        pendingOutcome = outcome
    }

    LaunchedEffect(Unit) {
        viewModel.fxEvents.collect { event ->
            when (event) {
                is CombatFxEvent.Impact -> {
                    val fx = DamageFxUi(
                        id = UUID.randomUUID().toString(),
                        targetId = event.targetId,
                        amount = event.amount,
                        critical = event.critical
                    )
                    damageFx += fx
                    launch {
                        delay(720)
                        damageFx.remove(fx)
                    }
                }
                is CombatFxEvent.Heal -> {
                    val fx = HealFxUi(
                        id = UUID.randomUUID().toString(),
                        targetId = event.targetId,
                        amount = event.amount
                    )
                    healFx += fx
                    launch {
                        delay(720)
                        healFx.remove(fx)
                    }
                }
                is CombatFxEvent.StatusApplied -> {
                    val fx = StatusFxUi(
                        id = UUID.randomUUID().toString(),
                        targetId = event.targetId,
                        statusId = event.statusId,
                        stacks = event.stacks
                    )
                    statusFx += fx
                    launch {
                        delay(880)
                        statusFx.remove(fx)
                    }
                }
                is CombatFxEvent.Knockout -> {
                    val fx = KnockoutFxUi(
                        id = UUID.randomUUID().toString(),
                        targetId = event.targetId
                    )
                    knockoutFx += fx
                    launch {
                        delay(680)
                        knockoutFx.remove(fx)
                    }
                }
                is CombatFxEvent.CombatOutcomeFx -> outcomeFx = event.outcome
                is CombatFxEvent.SupportCue -> {
                    val fx = SupportFxUi(
                        id = UUID.randomUUID().toString(),
                        actorId = event.actorId,
                        skillName = event.skillName,
                        targetIds = event.targetIds
                    )
                    supportFx += fx
                    launch {
                        delay(900)
                        supportFx.remove(fx)
                    }
                }
                is CombatFxEvent.TurnQueued -> { /* no-op for now */ }
                is CombatFxEvent.Audio -> audioCuePlayer.execute(event.commands)
            }
        }
    }

    LaunchedEffect(pendingOutcome) {
        val resolved = pendingOutcome ?: return@LaunchedEffect
        val handle = navController.previousBackStackEntry?.savedStateHandle
        if (resolved is CombatOutcome.Victory) {
            handle?.set("combat_victory", ArrayList(viewModel.encounterEnemyIds))
        }
        delay(
            when (resolved) {
                is CombatOutcome.Victory -> 900L
                is CombatOutcome.Defeat -> 750L
                CombatOutcome.Retreat -> 600L
            }
        )
        val payload = when (resolved) {
            is CombatOutcome.Victory -> {
                val rewards = resolved.rewards
                CombatResultPayload(
                    outcome = CombatResultPayload.Outcome.VICTORY,
                    enemyIds = viewModel.encounterEnemyIds,
                    rewardXp = rewards.xp,
                    rewardAp = rewards.ap,
                    rewardCredits = rewards.credits,
                    rewardItems = rewards.drops.associate { it.itemId to it.quantity },
                    levelUps = viewModel.consumeLevelUpSummaries()
                )
            }
            is CombatOutcome.Defeat -> CombatResultPayload(
                outcome = CombatResultPayload.Outcome.DEFEAT,
                enemyIds = viewModel.encounterEnemyIds
            )
            CombatOutcome.Retreat -> CombatResultPayload(
                outcome = CombatResultPayload.Outcome.RETREAT,
                enemyIds = viewModel.encounterEnemyIds
            )
        }
        handle?.set("combat_result", payload)
        navController.popBackStack()
    }

    if (pendingOutcome != null && outcomeFx == null && outcome != null) {
        outcomeFx = when (outcome) {
            is CombatOutcome.Victory -> CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY
            is CombatOutcome.Defeat -> CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT
            CombatOutcome.Retreat -> CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT
        }
    }

    val state = combatState
    val focusEnemy = selectedEnemyIds.firstOrNull()?.let { id ->
        enemies.firstOrNull { it.id == id }
    } ?: enemies.firstOrNull()

    if (focusEnemy != null && state != null) {
        val showSkillsDialog = remember { mutableStateOf(false) }
        val showItemsDialog = remember { mutableStateOf(false) }
        val activeCombatant = state.activeCombatant
        val activePlayer = activeCombatant?.takeIf {
            it.combatant.side == CombatSide.PLAYER || it.combatant.side == CombatSide.ALLY
        }?.let { combatantState ->
            playerParty.firstOrNull { it.id == combatantState.combatant.id }
        }
        val displayPlayer = activePlayer ?: playerParty.firstOrNull()
        val playerState = displayPlayer?.let { state.combatants[it.id] }
        val enemyState = state.combatants[focusEnemy.id]
        val activeId = activeCombatant?.combatant?.id
        val playerTurn = activePlayer != null
        val actingState = activePlayer?.let { state.combatants[it.id] }
        val combatLocked = pendingOutcome != null
        if (combatLocked) {
            showSkillsDialog.value = false
            showItemsDialog.value = false
        }
        val playerCannotAct = if (!playerTurn || combatLocked) {
            true
        } else actingState?.statusEffects.orEmpty().any { effect ->
            val id = effect.id.lowercase()
            id == "shock" || id == "freeze" || id == "stun"
        }
        val activePlayerSkills = activePlayer?.let { viewModel.skillsForPlayer(it.id) }.orEmpty()

        if (showSkillsDialog.value && activePlayer != null && !combatLocked) {
            SkillsDialog(
                player = activePlayer,
                skills = activePlayerSkills,
                onDismiss = { showSkillsDialog.value = false },
                onSkillSelected = { skill ->
                    viewModel.useSkill(skill)
                    showSkillsDialog.value = false
                }
            )
        }

        if (showItemsDialog.value && playerTurn && !playerCannotAct && !combatLocked) {
            CombatItemsDialog(
                items = inventoryEntries,
                onUseItem = { entry ->
                    viewModel.useItem(entry)
                    showItemsDialog.value = false
                },
                onDismiss = { showItemsDialog.value = false }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            WeatherOverlay(
                weatherId = viewModel.weatherId,
                modifier = Modifier.fillMaxSize()
            )
            VignetteOverlay(
                visible = true,
                intensity = 0.62f,
                feather = 0.24f,
                tint = Color.Black,
                modifier = Modifier.fillMaxSize()
            )
            ThemeBandOverlay(
                env = viewModel.environmentId ?: "combat",
                weather = viewModel.weatherId,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth(0.9f)
                    .height(72.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TurnTimelineBar(state = state)
                ActiveTurnPrompt(
                    active = state.activeCombatant,
                    activePlayerName = activePlayer?.name,
                    focusedEnemyName = focusEnemy.name
                )
            PartyRoster(
                party = playerParty,
                combatState = state,
                activeId = activeId,
                damageFx = damageFx,
                healFx = healFx,
                statusFx = statusFx,
                knockoutFx = knockoutFx,
                supportFx = supportFx
            )
            EnemyRoster(
                enemies = enemies,
                combatState = state,
                activeId = activeId,
                selectedEnemyIds = selectedEnemyIds,
                isTelegraphActive = playerTurn && !playerCannotAct && pendingOutcome == null,
                damageFx = damageFx,
                healFx = healFx,
                statusFx = statusFx,
                knockoutFx = knockoutFx,
                onEnemyClick = { if (pendingOutcome == null) viewModel.focusEnemyTarget(it) },
                onEnemyLongPress = { if (pendingOutcome == null) viewModel.toggleEnemyTarget(it) }
            )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val imageId = LocalContext.current.resources.getIdentifier(
                        focusEnemy.portrait.substringAfterLast("/").substringBeforeLast("."),
                        "drawable",
                        LocalContext.current.packageName
                    )
                    Image(
                        painter = painterResource(id = imageId),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp)
                    )
                    Text(text = focusEnemy.name)
                    val enemyMaxHp = focusEnemy.hp.coerceAtLeast(1)
                    val enemyHp = enemyState?.hp ?: enemyMaxHp
                    LinearProgressIndicator(
                        progress = {
                            enemyHp.toFloat() / enemyMaxHp.toFloat()
                        }
                    )
                    Text(text = "$enemyHp/$enemyMaxHp")
                    StatusBadges(
                        statuses = enemyState?.statusEffects.orEmpty(),
                        buffs = enemyState?.buffs.orEmpty()
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hero = displayPlayer
                    if (hero != null) {
                        val heroImageId = LocalContext.current.resources.getIdentifier(
                            hero.miniIconPath.substringAfterLast("/").substringBeforeLast("."),
                            "drawable",
                            LocalContext.current.packageName
                        )
                        if (heroImageId != 0) {
                            Image(
                                painter = painterResource(id = heroImageId),
                                contentDescription = null,
                                modifier = Modifier.size(128.dp)
                            )
                        }
                        Text(text = hero.name)
                        val playerMaxHp = hero.hp.coerceAtLeast(1)
                        val playerHp = playerState?.hp ?: playerMaxHp
                        LinearProgressIndicator(
                            progress = {
                                playerHp.toFloat() / playerMaxHp.toFloat()
                            }
                        )
                        Text(text = "$playerHp/$playerMaxHp")
                        StatusBadges(
                            statuses = playerState?.statusEffects.orEmpty(),
                            buffs = playerState?.buffs.orEmpty()
                        )
                    } else {
                        Text(
                            text = "No allies available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = playerTurn && !playerCannotAct && !combatLocked,
                        onClick = { viewModel.playerAttack() }
                    ) {
                        Text(text = "Attack")
                    }
                    Button(
                        enabled = playerTurn && !playerCannotAct && !combatLocked && activePlayerSkills.isNotEmpty(),
                        onClick = { showSkillsDialog.value = true }
                    ) {
                        Text(text = "Skills")
                    }
                    Button(
                        enabled = playerTurn && !playerCannotAct && !combatLocked && inventoryEntries.isNotEmpty(),
                        onClick = { showItemsDialog.value = true }
                    ) {
                        Text(text = "Items")
                    }
                    Button(
                        enabled = playerTurn && !combatLocked,
                        onClick = { viewModel.defend() }
                    ) {
                        Text(text = "Defend")
                    }
                    Button(
                        enabled = playerTurn && !combatLocked,
                        onClick = { viewModel.attemptRetreat() }
                    ) {
                        Text(text = "Retreat")
                    }
                }

                CombatLogFeed(
                    entries = state.log.map { entry ->
                        when (entry) {
                            is com.example.starborn.domain.combat.CombatLogEntry.Damage -> {
                                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                                if (entry.sourceId.startsWith(STATUS_SOURCE_PREFIX)) {
                                    val statusLabel = entry.sourceId.removePrefix(STATUS_SOURCE_PREFIX).replace('_', ' ')
                                    "$targetName suffers ${entry.amount} ${statusLabel.trim()} damage"
                                } else {
                                    val sourceName = state.combatants[entry.sourceId]?.combatant?.name ?: entry.sourceId
                                    "$sourceName dealt ${entry.amount} to $targetName"
                                }
                            }
                            is com.example.starborn.domain.combat.CombatLogEntry.Heal -> {
                                val sourceName = state.combatants[entry.sourceId]?.combatant?.name ?: entry.sourceId
                                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                                "$sourceName healed $targetName for ${entry.amount}"
                            }
                            is com.example.starborn.domain.combat.CombatLogEntry.StatusApplied -> {
                                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                                "${entry.statusId} applied to $targetName"
                            }
                            is com.example.starborn.domain.combat.CombatLogEntry.StatusExpired -> {
                                val targetName = state.combatants[entry.targetId]?.combatant?.name ?: entry.targetId
                                "${entry.statusId} faded from $targetName"
                            }
                            is com.example.starborn.domain.combat.CombatLogEntry.TurnSkipped -> {
                                val actorName = state.combatants[entry.actorId]?.combatant?.name ?: entry.actorId
                                "$actorName ${entry.reason}"
                            }
                            is com.example.starborn.domain.combat.CombatLogEntry.Outcome -> entry.result.toString()
                            is com.example.starborn.domain.combat.CombatLogEntry.ActionQueued -> {
                                val actorName = state.combatants[entry.actorId]?.combatant?.name ?: entry.actorId
                                "$actorName readies an action"
                            }
                        }
                    }
                )
            }
            outcomeFx?.let { OutcomeOverlay(it) }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Preparing encounter...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun TurnTimelineBar(state: CombatState) {
    val entries = state.turnOrder.take(8)
    if (entries.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Text(
            text = "Round ${state.round}",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            entries.forEachIndexed { index, slot ->
                val combatantState = state.combatants[slot.combatantId] ?: return@forEachIndexed
                val isActive = index == state.activeTurnIndex
                val color = when (combatantState.combatant.side) {
                    CombatSide.PLAYER, CombatSide.ALLY -> MaterialTheme.colorScheme.primary
                    CombatSide.ENEMY -> MaterialTheme.colorScheme.error
                }
                val pulse by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0f,
                    animationSpec = tween(durationMillis = 250),
                    label = "timelinePulse"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.widthIn(min = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp + 12.dp * pulse)
                            .width(18.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.3f + 0.5f * pulse))
                    )
                    Text(
                        text = combatantState.combatant.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = if (isActive) 1f else 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTurnPrompt(
    active: CombatantState?,
    activePlayerName: String?,
    focusedEnemyName: String?
) {
    if (active == null) return
    val message = when (active.combatant.side) {
        CombatSide.PLAYER, CombatSide.ALLY -> {
            val actorName = activePlayerName ?: active.combatant.name
            if (!focusedEnemyName.isNullOrBlank()) {
                "$actorName is lining up $focusedEnemyName"
            } else {
                "$actorName awaits your command"
            }
        }
        CombatSide.ENEMY -> "${active.combatant.name} is preparing an action"
    }
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun PartyRoster(
    party: List<Player>,
    combatState: CombatState,
    activeId: String?,
    damageFx: List<DamageFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    supportFx: List<SupportFxUi>
) {
    if (party.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        party.forEach { member ->
            val memberState = combatState.combatants[member.id]
            val maxHp = member.hp.coerceAtLeast(1)
            val currentHp = memberState?.hp ?: maxHp
            val isActive = member.id == activeId
            val shape = MaterialTheme.shapes.small
            val supportHighlights = supportFx.filter { member.id in it.targetIds }
            Surface(
                shape = shape,
                tonalElevation = if (isActive) 4.dp else 0.dp,
                border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 72.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = {
                                currentHp.toFloat() / maxHp.toFloat()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "$currentHp/$maxHp",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    CombatFxOverlay(
                        damageFx = damageFx.filter { it.targetId == member.id },
                        healFx = healFx.filter { it.targetId == member.id },
                        statusFx = statusFx.filter { it.targetId == member.id },
                        showKnockout = knockoutFx.any { it.targetId == member.id },
                        shape = shape,
                        supportFx = supportHighlights
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnemyRoster(
    enemies: List<Enemy>,
    combatState: CombatState,
    activeId: String?,
    selectedEnemyIds: Set<String>,
    isTelegraphActive: Boolean,
    damageFx: List<DamageFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    onEnemyClick: (String) -> Unit,
    onEnemyLongPress: (String) -> Unit
) {
    if (enemies.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(enemies) { enemy ->
            val enemyState = combatState.combatants[enemy.id]
            val maxHp = enemy.hp.coerceAtLeast(1)
            val currentHp = enemyState?.hp ?: maxHp
            val isActive = activeId == enemy.id
            val isSelected = enemy.id in selectedEnemyIds
            val flash = rememberDamageFlash(enemy.id, combatState.log)
            val baseColor = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
            val surfaceColor = baseColor.copy(alpha = 0.25f + 0.4f * flash)
            val shape = MaterialTheme.shapes.medium
            Surface(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { onEnemyClick(enemy.id) },
                        onLongClick = { onEnemyLongPress(enemy.id) }
                    ),
                color = surfaceColor,
                contentColor = Color.White,
                shape = shape,
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                tonalElevation = if (isActive) 4.dp else 0.dp
            ) {
                Box {
                    if (isSelected && isTelegraphActive) {
                        TelegraphGlow(modifier = Modifier.fillMaxSize(), shape = shape)
                    }
                    if (flash > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f * flash))
                        )
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = enemy.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { currentHp.toFloat() / maxHp.toFloat() },
                            modifier = Modifier
                                .width(96.dp)
                                .heightIn(min = 6.dp)
                        )
                        Text(
                            text = "$currentHp/$maxHp",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        StatusBadges(
                            statuses = enemyState?.statusEffects.orEmpty(),
                            buffs = enemyState?.buffs.orEmpty()
                        )
                    }
                    CombatFxOverlay(
                        damageFx = damageFx.filter { it.targetId == enemy.id },
                        healFx = healFx.filter { it.targetId == enemy.id },
                        statusFx = statusFx.filter { it.targetId == enemy.id },
                        showKnockout = knockoutFx.any { it.targetId == enemy.id },
                        shape = shape
                    )
                }
            }
        }
    }
}

@Composable
private fun TelegraphGlow(modifier: Modifier, shape: Shape) {
    val pulse = rememberInfiniteTransition(label = "telegraphPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "telegraphAmount"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(2.dp, Color.Cyan.copy(alpha = 0.4f + 0.4f * pulse.value)), shape)
            .background(Color.Cyan.copy(alpha = 0.12f * pulse.value))
    )
}

@Composable
private fun rememberDamageFlash(
    targetId: String,
    log: List<com.example.starborn.domain.combat.CombatLogEntry>
): Float {
    val lastDamage = log.lastOrNull { entry ->
        entry is com.example.starborn.domain.combat.CombatLogEntry.Damage && entry.targetId == targetId
    }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(lastDamage) {
        if (lastDamage != null) {
            anim.snapTo(1f)
            anim.animateTo(0f, animationSpec = tween(durationMillis = 350))
        }
    }
    return anim.value
}

@Composable
private fun StatusBadges(statuses: List<StatusEffect>, buffs: List<ActiveBuff>) {
    if (statuses.isEmpty() && buffs.isEmpty()) return
    val entries = buildList {
        statuses.forEach { statusEffect ->
            add(
                StatusChip(
                    label = statusEffect.id.uppercase(),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    turns = statusEffect.remainingTurns
                )
            )
        }
        buffs.forEach { buff ->
            val sign = if (buff.effect.value >= 0) "+" else ""
            add(
                StatusChip(
                    label = "${sign}${buff.effect.value} ${buff.effect.stat}",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                    turns = buff.remainingTurns
                )
            )
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(entries) { chip ->
            Surface(
                color = chip.tint,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${chip.label} (${chip.turns})",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private data class StatusChip(
    val label: String,
    val tint: Color,
    val turns: Int
)

@Composable
private fun CombatFxOverlay(
    damageFx: List<DamageFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    showKnockout: Boolean,
    shape: Shape,
    supportFx: List<SupportFxUi> = emptyList()
) {
    if (damageFx.isEmpty() && healFx.isEmpty() && statusFx.isEmpty() && !showKnockout && supportFx.isEmpty()) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
    ) {
        val supportHighlight = supportFx.firstOrNull()
        if (supportHighlight != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF00ACC1).copy(alpha = 0.22f))
            )
            Text(
                text = "Support: ${supportHighlight.skillName}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }
        if (showKnockout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
            Text(
                text = "KO",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        damageFx.forEach { fx ->
            DamageNumberBubble(
                fx = fx,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        healFx.forEach { fx ->
            HealNumberBubble(
                fx = fx,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        statusFx.forEach { fx ->
            StatusPulse(
                fx = fx,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun DamageNumberBubble(
    fx: DamageFxUi,
    modifier: Modifier = Modifier
) {
    val verticalOffset = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    LaunchedEffect(fx.id) {
        verticalOffset.snapTo(0f)
        alphaAnim.snapTo(1f)
        verticalOffset.animateTo(-64f, tween(durationMillis = 620, easing = LinearEasing))
        alphaAnim.animateTo(0f, tween(durationMillis = 620, easing = LinearEasing))
    }
    Text(
        text = if (fx.critical) "!${fx.amount}" else "-${fx.amount}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (fx.critical) FontWeight.Bold else FontWeight.SemiBold,
        color = if (fx.critical) Color.Yellow else Color(0xFFFF7043),
        modifier = modifier.graphicsLayer {
            translationY = verticalOffset.value
            alpha = alphaAnim.value
        }
    )
}

@Composable
private fun HealNumberBubble(
    fx: HealFxUi,
    modifier: Modifier = Modifier
) {
    val verticalOffset = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    LaunchedEffect(fx.id) {
        verticalOffset.snapTo(0f)
        alphaAnim.snapTo(1f)
        verticalOffset.animateTo(-52f, tween(durationMillis = 560, easing = LinearEasing))
        alphaAnim.animateTo(0f, tween(durationMillis = 560, easing = LinearEasing))
    }
    Text(
        text = "+${fx.amount}",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF81C784),
        modifier = modifier.graphicsLayer {
            translationY = verticalOffset.value
            alpha = alphaAnim.value
        }
    )
}

@Composable
private fun StatusPulse(
    fx: StatusFxUi,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(1f) }
    LaunchedEffect(fx.id) {
        alphaAnim.snapTo(1f)
        alphaAnim.animateTo(0f, tween(durationMillis = 600, easing = LinearEasing))
    }
    Surface(
        modifier = modifier.graphicsLayer { alpha = alphaAnim.value },
        color = Color(0xFF4DD0E1).copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = fx.statusId.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OutcomeOverlay(outcomeType: CombatFxEvent.CombatOutcomeFx.OutcomeType) {
    val message = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> "Victory!"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> "Defeated..."
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> "Retreat Successful"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.6f))
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
private fun CombatItemsDialog(
    items: List<InventoryEntry>,
    onUseItem: (InventoryEntry) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Use Item") },
        text = {
            if (items.isEmpty()) {
                Text("No usable items available.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = entry.item.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "x${entry.quantity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { onUseItem(entry) },
                                enabled = entry.quantity > 0
                            ) {
                                Text("Use")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CombatLogFeed(entries: List<String>) {
    if (entries.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Combat Log", color = Color.White, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            items(entries.takeLast(10).reversed()) { line ->
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
