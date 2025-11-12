package com.example.starborn.feature.combat.ui

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.ActiveBuff
import com.example.starborn.domain.combat.StatusEffect
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.CombatViewModel.TimedPromptState
import com.example.starborn.feature.combat.viewmodel.CombatFxEvent
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import com.example.starborn.ui.dialogs.SkillsDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.navigation.CombatResultPayload
import com.example.starborn.ui.background.rememberAssetPainter
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.ui.vfx.WeatherOverlay
import com.example.starborn.ui.vfx.GlowProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random
import kotlin.math.sin

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

private enum class TargetFilter {
    ENEMY,
    ALLY,
    ANY
}

private enum class VictoryDialogStage {
    SPOILS,
    LEVEL_UPS
}

private sealed interface PendingTargetRequest {
    val instruction: String
    fun accepts(target: TargetFilter): Boolean

    data object Attack : PendingTargetRequest {
        override val instruction: String = "Choose an enemy to attack"
        override fun accepts(target: TargetFilter): Boolean = target == TargetFilter.ENEMY
    }

    data class SkillRequest(
        val skill: Skill,
        val filter: TargetFilter,
        override val instruction: String
    ) : PendingTargetRequest {
        override fun accepts(target: TargetFilter): Boolean =
            filter == TargetFilter.ANY || filter == target
    }

    data class ItemRequest(
        val entry: InventoryEntry,
        val filter: TargetFilter,
        override val instruction: String
    ) : PendingTargetRequest {
        override fun accepts(target: TargetFilter): Boolean =
            filter == TargetFilter.ANY || filter == target
    }
}

private val HUD_BAR_WIDTH = 156.dp

@Composable
fun CombatScreen(
    navController: NavController,
    viewModel: CombatViewModel,
    audioCuePlayer: AudioCuePlayer,
    suppressFlashes: Boolean,
    suppressScreenshake: Boolean,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val playerParty = viewModel.playerParty
    val enemies = viewModel.enemies
    val combatState by viewModel.state.collectAsState(initial = viewModel.combatState)
    val inventoryEntries by viewModel.inventory.collectAsStateWithLifecycle()
    val resonance by viewModel.resonance.collectAsStateWithLifecycle()
    val atbMeters by viewModel.atbMeters.collectAsStateWithLifecycle()
    val timedPromptState by viewModel.timedPrompt.collectAsStateWithLifecycle()
    val awaitingActionId by viewModel.awaitingAction.collectAsStateWithLifecycle()
    val combatMessage by viewModel.combatMessage.collectAsStateWithLifecycle()
    val damageFx = remember { mutableStateListOf<DamageFxUi>() }
    val healFx = remember { mutableStateListOf<HealFxUi>() }
    val statusFx = remember { mutableStateListOf<StatusFxUi>() }
    val knockoutFx = remember { mutableStateListOf<KnockoutFxUi>() }
    val supportFx = remember { mutableStateListOf<SupportFxUi>() }
    var outcomeFx by remember { mutableStateOf<CombatFxEvent.CombatOutcomeFx.OutcomeType?>(null) }
    var pendingOutcome by remember { mutableStateOf<CombatOutcome?>(null) }
    var pendingVictoryPayload by remember { mutableStateOf<CombatResultPayload?>(null) }
    var victoryDialogStage by remember { mutableStateOf<VictoryDialogStage?>(null) }
    val selectedEnemyIds by viewModel.selectedEnemies.collectAsStateWithLifecycle()
    var pendingTargetRequest by remember { mutableStateOf<PendingTargetRequest?>(null) }
    var pendingInstruction by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val shakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

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
                    if (!suppressScreenshake) {
                        launch {
                            val amplitude = with(density) { if (event.critical) 16.dp.toPx() else 10.dp.toPx() }
                            val offset = Offset(
                                x = if (Random.nextBoolean()) amplitude else -amplitude,
                                y = if (Random.nextBoolean()) amplitude * 0.6f else -amplitude * 0.6f
                            )
                            shakeOffset.stop()
                            shakeOffset.snapTo(offset)
                            shakeOffset.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = tween(durationMillis = 180)
                            )
                        }
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
        val waitMillis = when (resolved) {
            is CombatOutcome.Victory -> 900L
            is CombatOutcome.Defeat -> 750L
            CombatOutcome.Retreat -> 600L
        }
        delay(waitMillis)
        when (resolved) {
            is CombatOutcome.Victory -> {
                val rewards = resolved.rewards
                pendingVictoryPayload = CombatResultPayload(
                    outcome = CombatResultPayload.Outcome.VICTORY,
                    enemyIds = viewModel.encounterEnemyIds,
                    rewardXp = rewards.xp,
                    rewardAp = rewards.ap,
                    rewardCredits = rewards.credits,
                    rewardItems = rewards.drops.associate { it.itemId to it.quantity },
                    levelUps = viewModel.consumeLevelUpSummaries()
                )
                victoryDialogStage = VictoryDialogStage.SPOILS
            }
            is CombatOutcome.Defeat -> {
                val payload = CombatResultPayload(
                    outcome = CombatResultPayload.Outcome.DEFEAT,
                    enemyIds = viewModel.encounterEnemyIds
                )
                handle?.set("combat_result", payload)
                pendingOutcome = null
                navController.popBackStack()
            }
            CombatOutcome.Retreat -> {
                val payload = CombatResultPayload(
                    outcome = CombatResultPayload.Outcome.RETREAT,
                    enemyIds = viewModel.encounterEnemyIds
                )
                handle?.set("combat_result", payload)
                pendingOutcome = null
                navController.popBackStack()
            }
        }
    }

    if (pendingOutcome != null && outcomeFx == null && outcome != null) {
        outcomeFx = when (outcome) {
            is CombatOutcome.Victory -> CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY
            is CombatOutcome.Defeat -> CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT
            CombatOutcome.Retreat -> CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT
        }
    }

    fun completeVictory(payload: CombatResultPayload) {
        val handle = navController.previousBackStackEntry?.savedStateHandle
        handle?.set("combat_result", payload)
        pendingVictoryPayload = null
        victoryDialogStage = null
        pendingOutcome = null
        navController.popBackStack()
    }

    fun advanceVictoryDialog() {
        val payload = pendingVictoryPayload ?: return
        if (victoryDialogStage == VictoryDialogStage.SPOILS && payload.levelUps.isNotEmpty()) {
            victoryDialogStage = VictoryDialogStage.LEVEL_UPS
        } else {
            completeVictory(payload)
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
        val enemyState = state.combatants[focusEnemy.id]
        val activeId = activeCombatant?.combatant?.id
        val combatLocked = pendingOutcome != null || timedPromptState != null || pendingVictoryPayload != null
        val menuActor = awaitingActionId?.let { id -> playerParty.firstOrNull { it.id == id } }
        val menuActorState = awaitingActionId?.let { id -> state.combatants[id] }
        val menuActorCannotAct = menuActorState?.statusEffects.orEmpty().any { effect ->
            val id = effect.id.lowercase()
            id == "shock" || id == "freeze" || id == "stun"
        }
        if (combatLocked || menuActor == null) {
            showSkillsDialog.value = false
            showItemsDialog.value = false
        }
        val menuActorSkills = menuActor?.let { viewModel.skillsForPlayer(it.id) }.orEmpty()
        val victoryEmotes = pendingOutcome is CombatOutcome.Victory

        if (awaitingActionId == null && pendingTargetRequest != null) {
            pendingTargetRequest = null
            pendingInstruction = null
        }

        fun clearPendingRequest() {
            pendingTargetRequest = null
            pendingInstruction = null
        }

        fun requestTarget(request: PendingTargetRequest) {
            pendingTargetRequest = request
            pendingInstruction = request.instruction
        }

        fun executePendingAction(request: PendingTargetRequest, targetId: String) {
            when (request) {
                PendingTargetRequest.Attack -> {
                    viewModel.focusEnemyTarget(targetId)
                    viewModel.playerAttack(targetId)
                }
                is PendingTargetRequest.SkillRequest -> {
                    viewModel.useSkill(request.skill, listOf(targetId))
                }
                is PendingTargetRequest.ItemRequest -> {
                    viewModel.useItem(request.entry, targetId)
                }
            }
            clearPendingRequest()
        }

        fun handleEnemyTap(targetId: String) {
            val pending = pendingTargetRequest
            if (pending == null) {
                if (!combatLocked) viewModel.focusEnemyTarget(targetId)
            } else if (pending.accepts(TargetFilter.ENEMY)) {
                executePendingAction(pending, targetId)
            } else {
                pendingInstruction = pending.instruction
            }
        }

        fun handleAllyTap(targetId: String) {
            val pending = pendingTargetRequest ?: return
            if (pending.accepts(TargetFilter.ALLY)) {
                executePendingAction(pending, targetId)
            } else {
                pendingInstruction = pending.instruction
            }
        }

        fun handleSkillSelection(skill: Skill) {
            when (viewModel.targetRequirementFor(skill)) {
                TargetRequirement.ENEMY -> requestTarget(
                    PendingTargetRequest.SkillRequest(
                        skill = skill,
                        filter = TargetFilter.ENEMY,
                        instruction = "Choose an enemy for ${skill.name}"
                    )
                )
                TargetRequirement.ALLY -> requestTarget(
                    PendingTargetRequest.SkillRequest(
                        skill = skill,
                        filter = TargetFilter.ALLY,
                        instruction = "Choose an ally for ${skill.name}"
                    )
                )
                TargetRequirement.ANY -> requestTarget(
                    PendingTargetRequest.SkillRequest(
                        skill = skill,
                        filter = TargetFilter.ANY,
                        instruction = "Choose a target for ${skill.name}"
                    )
                )
                TargetRequirement.NONE -> viewModel.useSkill(skill)
            }
        }

        fun handleItemSelection(entry: InventoryEntry) {
            when (entry.targetFilter()) {
                TargetFilter.ENEMY -> requestTarget(
                    PendingTargetRequest.ItemRequest(
                        entry = entry,
                        filter = TargetFilter.ENEMY,
                        instruction = "Choose an enemy for ${entry.item.name}"
                    )
                )
                TargetFilter.ALLY -> requestTarget(
                    PendingTargetRequest.ItemRequest(
                        entry = entry,
                        filter = TargetFilter.ALLY,
                        instruction = "Choose an ally for ${entry.item.name}"
                    )
                )
                TargetFilter.ANY -> requestTarget(
                    PendingTargetRequest.ItemRequest(
                        entry = entry,
                        filter = TargetFilter.ANY,
                        instruction = "Choose a target for ${entry.item.name}"
                    )
                )
                null -> viewModel.useItem(entry)
            }
        }

        if (showSkillsDialog.value && menuActor != null && !combatLocked && !menuActorCannotAct) {
            SkillsDialog(
                player = menuActor,
                skills = menuActorSkills,
                onDismiss = { showSkillsDialog.value = false },
                onSkillSelected = { skill ->
                    showSkillsDialog.value = false
                    handleSkillSelection(skill)
                }
            )
        }

        if (showItemsDialog.value && !combatLocked && !menuActorCannotAct && menuActor != null) {
            CombatItemsDialog(
                items = inventoryEntries,
                onItemSelected = { entry ->
                    showItemsDialog.value = false
                    handleItemSelection(entry)
                },
                onDismiss = { showItemsDialog.value = false }
            )
        }

        val backgroundPainter = rememberRoomBackgroundPainter(viewModel.roomBackground)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF05070B))
                .clipToBounds()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = shakeOffset.value.x
                        translationY = shakeOffset.value.y
                    }
            ) {
                Image(
                    painter = backgroundPainter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                WeatherOverlay(
                    weatherId = viewModel.weatherId,
                    suppressFlashes = suppressFlashes,
                    modifier = Modifier.fillMaxSize(),
                    tintColor = MaterialTheme.colorScheme.primary,
                    darkness = 0f
                )
                if (highContrastMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.18f))
                    )
                }
            val allowAllySelection = pendingTargetRequest?.accepts(TargetFilter.ALLY) == true
            val actionMenuAvailable = menuActor != null &&
                !combatLocked &&
                !menuActorCannotAct &&
                pendingTargetRequest == null
            val actorForMenu = menuActor
            val hasTargets = enemies.any { state.combatants[it.id]?.isAlive == true }
            val actionMenuContent: (@Composable () -> Unit)? =
                if (actionMenuAvailable && actorForMenu != null) {
                    {
                        ActionMenu(
                            actor = actorForMenu,
                            canAttack = hasTargets,
                            hasSkills = menuActorSkills.isNotEmpty(),
                            hasItems = inventoryEntries.isNotEmpty(),
                            onAttack = {
                                requestTarget(PendingTargetRequest.Attack)
                            },
                            onSkills = {
                                showSkillsDialog.value = true
                                pendingTargetRequest = null
                                pendingInstruction = null
                            },
                            onItems = {
                                showItemsDialog.value = true
                                pendingTargetRequest = null
                                pendingInstruction = null
                            },
                            onDefend = {
                                pendingTargetRequest = null
                                pendingInstruction = null
                                viewModel.defend()
                            },
                            onRetreat = {
                                pendingTargetRequest = null
                                pendingInstruction = null
                                viewModel.attemptRetreat()
                            },
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets
                        )
                    }
                } else null

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    EnemyRoster(
                        enemies = enemies,
                        combatState = state,
                        activeId = activeId,
                        selectedEnemyIds = selectedEnemyIds,
                        damageFx = damageFx,
                        healFx = healFx,
                        statusFx = statusFx,
                        knockoutFx = knockoutFx,
                        onEnemyTap = { handleEnemyTap(it) },
                        onEnemyLongPress = {
                            if (pendingTargetRequest == null && !combatLocked) {
                                viewModel.toggleEnemyTarget(it)
                            }
                        },
                        atbMeters = atbMeters
                    )
                    CombatLogPanel(
                        flavorLine = combatMessage,
                        instruction = pendingInstruction,
                        showCancel = pendingTargetRequest != null,
                        onCancel = if (pendingTargetRequest != null) {
                            { clearPendingRequest() }
                        } else null,
                        actionMenu = actionMenuContent
                    )
                    PartyRoster(
                        party = playerParty,
                        combatState = state,
                        activeId = activeId,
                        damageFx = damageFx,
                        healFx = healFx,
                        statusFx = statusFx,
                        knockoutFx = knockoutFx,
                        supportFx = supportFx,
                        onMemberTap = if (allowAllySelection) { memberId ->
                            handleAllyTap(memberId)
                        } else null,
                        victoryEmotes = victoryEmotes,
                        atbMeters = atbMeters
                    )
                }
                if (playerParty.isNotEmpty()) {
                    ResonanceMeter(
                        current = resonance,
                        max = viewModel.resonanceMax,
                        min = viewModel.resonanceMinBound,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            outcomeFx?.let { OutcomeOverlay(it, playerParty) }
            timedPromptState?.let { prompt ->
                TimedPromptOverlay(
                    prompt = prompt,
                    onTap = { viewModel.registerTimedPromptTap() }
                )
            }
            val victoryPayload = pendingVictoryPayload
            if (victoryPayload != null) {
                victoryDialogStage?.let { stage ->
                    VictoryDialog(
                        stage = stage,
                        payload = victoryPayload,
                        itemNameResolver = viewModel::itemDisplayName,
                        onContinue = { advanceVictoryDialog() }
                    )
                }
            }
        }
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
private fun PartyRoster(
    party: List<Player>,
    combatState: CombatState,
    activeId: String?,
    damageFx: List<DamageFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    supportFx: List<SupportFxUi>,
    onMemberTap: ((String) -> Unit)? = null,
    victoryEmotes: Boolean = false,
    atbMeters: Map<String, Float> = emptyMap()
) {
    if (party.isEmpty()) return
    val rows = party.chunked(2)
    val density = LocalDensity.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { rowMembers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                rowMembers.forEach { member ->
                    val memberState = combatState.combatants[member.id]
                    val maxHp = memberState?.combatant?.stats?.maxHp ?: member.hp.coerceAtLeast(1)
                    val currentHp = memberState?.hp ?: maxHp
                    val isActive = member.id == activeId
                    val supportHighlights = supportFx.filter { member.id in it.targetIds }
                    val cardShape = RoundedCornerShape(22.dp)
                    val isAlive = memberState?.isAlive != false
                    val portraitPath = when {
                        !isAlive -> "images/characters/emotes/${member.id}_down.png"
                        victoryEmotes -> "images/characters/emotes/${member.id}_cool.png"
                        else -> member.miniIconPath
                    }
                    val portraitPainter = rememberAssetPainter(portraitPath, R.drawable.main_menu_background)
                    val atbProgress = atbMeters[member.id] ?: 0f

                    val baseModifier = Modifier.widthIn(min = 150.dp)
                    val interactiveModifier = if (onMemberTap != null && isAlive) {
                        baseModifier.clickable { onMemberTap(member.id) }
                    } else {
                        baseModifier
                    }
                    val attackImpulse = rememberAttackImpulse(member.id, combatState.log)
                    val attackShiftPx = with(density) { 14.dp.toPx() * attackImpulse }
                    Box(
                        modifier = interactiveModifier
                            .graphicsLayer {
                                translationY = -attackShiftPx
                            }
                            .padding(horizontal = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(if (isActive) 96.dp else 92.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .border(
                                                width = 2.dp,
                                                color = Color.White.copy(alpha = 0.55f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1C1F24)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = portraitPainter,
                                        contentDescription = member.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Text(
                                text = member.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            AtbBar(
                                progress = atbProgress,
                                modifier = Modifier.width(HUD_BAR_WIDTH)
                            )
                            StatBar(
                                current = currentHp,
                                max = maxHp,
                                color = Color(0xFFFF5252),
                                background = Color.Black.copy(alpha = 0.5f),
                                height = 12.dp,
                                modifier = Modifier.width(HUD_BAR_WIDTH)
                            )
                            Text(
                                text = "$currentHp/$maxHp",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            StatusBadges(
                                statuses = memberState?.statusEffects.orEmpty(),
                                buffs = memberState?.buffs.orEmpty()
                            )
                        }
                        CombatFxOverlay(
                            damageFx = damageFx.filter { it.targetId == member.id },
                            healFx = healFx.filter { it.targetId == member.id },
                            statusFx = statusFx.filter { it.targetId == member.id },
                            showKnockout = knockoutFx.any { it.targetId == member.id },
                            shape = cardShape,
                            supportFx = supportHighlights
                        )
                    }
                }
                if (rowMembers.size == 1 && party.size > 1) {
                    Spacer(modifier = Modifier.width(150.dp))
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
    damageFx: List<DamageFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    onEnemyTap: (String) -> Unit,
    onEnemyLongPress: (String) -> Unit,
    atbMeters: Map<String, Float> = emptyMap()
) {
    if (enemies.isEmpty()) return
    val density = LocalDensity.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        enemies.forEach { enemy ->
            val enemyState = combatState.combatants[enemy.id]
            val maxHp = enemyState?.combatant?.stats?.maxHp ?: enemy.hp.coerceAtLeast(1)
            val currentHp = enemyState?.hp ?: maxHp
            val isActive = activeId == enemy.id
            val isSelected = enemy.id in selectedEnemyIds
            val flash = rememberDamageFlash(enemy.id, combatState.log)
            val selectionGlow = remember { Animatable(0f) }
            LaunchedEffect(isSelected) {
                if (isSelected) {
                    selectionGlow.snapTo(0.6f)
                    selectionGlow.animateTo(0f, tween(durationMillis = 500))
                } else {
                    selectionGlow.snapTo(0f)
                }
            }
            val spritePath = enemy.sprite.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: enemy.portrait?.takeIf { it.isNotBlank() }
                ?: "images/enemies/${enemy.id}_combat.png"
            val painter = rememberAssetPainter(
                spritePath,
                R.drawable.main_menu_background
            )
            val shape = RoundedCornerShape(28.dp)
            val atbProgress = atbMeters[enemy.id] ?: 0f

            val attackImpulse = rememberAttackImpulse(enemy.id, combatState.log)
            val attackShiftPx = with(density) { 14.dp.toPx() * attackImpulse }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .graphicsLayer {
                        translationY = attackShiftPx
                    }
                    .widthIn(min = 150.dp)
                    .combinedClickable(
                        onClick = { onEnemyTap(enemy.id) },
                        onLongClick = { onEnemyLongPress(enemy.id) }
                    )
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AtbBar(
                        progress = atbProgress,
                        modifier = Modifier.width(HUD_BAR_WIDTH),
                        color = Color(0xFF2D9CFF)
                    )
                    StatBar(
                        current = currentHp,
                        max = maxHp,
                        color = Color(0xFFFF5252),
                        background = Color.Black.copy(alpha = 0.5f),
                        height = 12.dp,
                        modifier = Modifier.width(HUD_BAR_WIDTH)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            if (selectionGlow.value > 0f) {
                                scaleX = 1f + selectionGlow.value * 0.08f
                                scaleY = 1f + selectionGlow.value * 0.08f
                            }
                        }
                ) {
                    val selectionAlpha = if (isSelected) 0.18f + selectionGlow.value * 0.2f else if (isActive) 0.08f else 0f
                    if (flash > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(shape)
                                .background(Color.Black.copy(alpha = 0.18f * flash))
                        )
                    }
                    if (selectionAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = selectionAlpha),
                                    shape = shape
                                )
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .width(110.dp)
                            .height(28.dp)
                    ) {
                        drawOval(color = Color.Black.copy(alpha = 0.45f))
                    }
                    Image(
                        painter = painter,
                        contentDescription = enemy.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 12.dp)
                            .size(130.dp)
                    )
                    CombatFxOverlay(
                        damageFx = damageFx.filter { it.targetId == enemy.id },
                        healFx = healFx.filter { it.targetId == enemy.id },
                        statusFx = statusFx.filter { it.targetId == enemy.id },
                        showKnockout = knockoutFx.any { it.targetId == enemy.id },
                        shape = shape
                    )
                }
                Text(
                    text = enemy.name.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                StatusBadges(
                    statuses = enemyState?.statusEffects.orEmpty(),
                    buffs = enemyState?.buffs.orEmpty()
                )
            }
        }
    }
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
private fun rememberAttackImpulse(
    sourceId: String,
    log: List<com.example.starborn.domain.combat.CombatLogEntry>
): Float {
    val lastAttack = log.lastOrNull { entry ->
        entry is com.example.starborn.domain.combat.CombatLogEntry.Damage && entry.sourceId == sourceId
    }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(lastAttack) {
        if (lastAttack != null) {
            anim.snapTo(1f)
            anim.animateTo(0f, tween(durationMillis = 260, easing = EaseOutBack))
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

@Composable
private fun StatBar(
    current: Int,
    max: Int,
    color: Color,
    background: Color,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val ratio = if (max <= 0) 0f else current.coerceAtLeast(0).coerceAtMost(max).toFloat() / max.toFloat()
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
    ) {
        val clampedRatio = ratio.coerceIn(0f, 1f)
        if (clampedRatio > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedRatio)
                    .align(Alignment.CenterStart)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val corner = CornerRadius(size.height, size.height)
                    val bodyBrush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.6f),
                            color.copy(alpha = 0.85f),
                            color
                        )
                    )
                    drawRoundRect(
                        brush = bodyBrush,
                        size = size,
                        cornerRadius = corner
                    )
                    val tipWidth = size.width.coerceAtMost(size.height * 1.5f)
                    if (tipWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0f)
                                )
                            ),
                            topLeft = Offset(size.width - tipWidth, 0f),
                            size = Size(tipWidth, size.height),
                            cornerRadius = corner
                        )
                    }
                }
            }
        }
    }
}

private data class StatusChip(
    val label: String,
    val tint: Color,
    val turns: Int
)

private fun InventoryEntry.targetFilter(): TargetFilter? {
    val effect = item.effect ?: return null
    val declared = effect.target?.lowercase()
    return when {
        declared == "enemy" -> TargetFilter.ENEMY
        declared == "ally" -> TargetFilter.ALLY
        declared == "any" -> TargetFilter.ANY
        effect.damage?.let { it > 0 } == true -> TargetFilter.ENEMY
        effect.restoreHp?.let { it > 0 } == true -> TargetFilter.ALLY
        effect.restoreRp?.let { it > 0 } == true -> TargetFilter.ALLY
        effect.singleBuff != null || !effect.buffs.isNullOrEmpty() -> TargetFilter.ALLY
        else -> null
    }
}

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
    val scaleAnim = remember { Animatable(1f) }
    val driftX = remember(fx.id) { (Random.nextFloat() - 0.5f) * 48f }
    val tilt = remember(fx.id) {
        (Random.nextFloat() - 0.5f) * if (fx.critical) 18f else 10f
    }
    LaunchedEffect(fx.id) {
        verticalOffset.snapTo(0f)
        alphaAnim.snapTo(1f)
        scaleAnim.snapTo(if (fx.critical) 1.25f else 1.1f)
        launch {
            verticalOffset.animateTo(
                targetValue = if (fx.critical) -96f else -72f,
                animationSpec = tween(durationMillis = 680, easing = LinearEasing)
            )
        }
        launch {
            alphaAnim.animateTo(0f, tween(durationMillis = 680, easing = LinearEasing))
        }
        launch {
            scaleAnim.animateTo(1f, tween(durationMillis = 420, easing = EaseOutBack))
        }
    }
    val headline = if (fx.critical) "!${fx.amount}" else "-${fx.amount}"
    val topColor = if (fx.critical) Color(0xFFFFF59D) else Color(0xFFFFB74D)
    val bottomColor = if (fx.critical) Color(0xFFFFD740) else Color(0xFFFF7043)
    Text(
        text = headline,
        style = MaterialTheme.typography.titleLarge.copy(
            shadow = Shadow(
                color = bottomColor.copy(alpha = 0.75f),
                offset = Offset(0f, 6f),
                blurRadius = 18f
            )
        ),
        fontWeight = if (fx.critical) FontWeight.Black else FontWeight.SemiBold,
        color = topColor,
        modifier = modifier.graphicsLayer {
            translationY = verticalOffset.value
            translationX = driftX
            alpha = alphaAnim.value
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            rotationZ = tilt
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
private fun ResonanceMeter(
    current: Int,
    max: Int,
    min: Int = 0,
    modifier: Modifier = Modifier
) {
    val span = (max - min).coerceAtLeast(1)
    val clampedValue = current.coerceIn(min, max)
    val progress = (clampedValue - min) / span.toFloat()
    val accent = MaterialTheme.colorScheme.secondary
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Team Resonance",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "$clampedValue / $max",
                style = MaterialTheme.typography.labelLarge
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
        ) {
            GlowProgressBar(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                trackColor = Color.Transparent,
                glowColor = accent.copy(alpha = 0.95f),
                height = 12.dp,
                glowWidth = 0.12f
            )
            ResonancePulseField(progress = progress, tint = accent)
        }
    }
}

@Composable
private fun ResonancePulseField(
    progress: Float,
    tint: Color,
    pulseCount: Int = 6
) {
    if (progress <= 0f) return
    val transition = rememberInfiniteTransition(label = "resonance_pulse")
    val pulseShift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "resonance_shift"
    )
    val seeds = remember(pulseCount) {
        List(pulseCount) { index ->
            ((index * 0.173f) % 1f).coerceIn(0.05f, 0.95f)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val activeWidth = size.width * progress
        val height = size.height
        seeds.forEachIndexed { idx, seed ->
            val offsetSeed = (seed + pulseShift.value + idx * 0.07f) % 1f
            if (offsetSeed <= progress) {
                val x = activeWidth * offsetSeed
                val baseY = height / 2f
                val wobble = kotlin.math.sin((pulseShift.value + idx) * 6f) * (height * 0.35f)
                val radius = (height * 0.2f) + (idx % 3) * (height * 0.05f)
                drawCircle(
                    color = tint.copy(alpha = 0.35f),
                    radius = radius,
                    center = Offset(x, baseY + wobble.toFloat())
                )
            }
        }
    }
}

@Composable
private fun OutcomeOverlay(
    outcomeType: CombatFxEvent.CombatOutcomeFx.OutcomeType,
    party: List<Player>
) {
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
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                if (outcomeType != CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        party.forEach { player ->
                            val emotePath = when (outcomeType) {
                                CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY ->
                                    "images/characters/emotes/${player.id}_cool.png"
                                CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT ->
                                    "images/characters/emotes/${player.id}_down.png"
                                else -> null
                            }
                            if (emotePath != null) {
                                Image(
                                    painter = rememberAssetPainter(emotePath, R.drawable.main_menu_background),
                                    contentDescription = player.name,
                                    modifier = Modifier.size(72.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimedPromptOverlay(
    prompt: TimedPromptState,
    onTap: () -> Unit
) {
    var progress by remember(prompt.id) { mutableStateOf(1f) }
    LaunchedEffect(prompt.id) {
        val duration = prompt.durationMillis.coerceAtLeast(1L).toFloat()
        val start = prompt.startedAt
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - start
            val fraction = (elapsed / duration).coerceIn(0f, 1f)
            progress = 1f - fraction
            if (fraction >= 1f) break
            delay(16)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .pointerInput(prompt.id) {
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = prompt.message,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
                Text(
                    text = "Tap!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun VictoryDialog(
    stage: VictoryDialogStage,
    payload: CombatResultPayload,
    itemNameResolver: (String) -> String,
    onContinue: () -> Unit
) {
    val buttonLabel = if (stage == VictoryDialogStage.SPOILS && payload.levelUps.isNotEmpty()) {
        "Next"
    } else {
        "Continue"
    }
    val title = when (stage) {
        VictoryDialogStage.SPOILS -> "Spoils Recovered"
        VictoryDialogStage.LEVEL_UPS -> "Level Up!"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F1118).copy(alpha = 0.96f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                when (stage) {
                    VictoryDialogStage.SPOILS -> VictorySpoilsContent(payload, itemNameResolver)
                    VictoryDialogStage.LEVEL_UPS -> VictoryLevelUpContent(payload.levelUps)
                }
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
private fun VictorySpoilsContent(
    payload: CombatResultPayload,
    itemNameResolver: (String) -> String
) {
    val resourceEntries = buildList {
        if (payload.rewardXp > 0) add("Experience" to "+${payload.rewardXp} XP")
        if (payload.rewardAp > 0) add("Ability Points" to "+${payload.rewardAp} AP")
        if (payload.rewardCredits > 0) add("Credits" to "+${payload.rewardCredits}")
    }
    val itemEntries = payload.rewardItems.entries
        .filter { it.value > 0 }
        .sortedBy { itemNameResolver(it.key) }

    if (resourceEntries.isEmpty() && itemEntries.isEmpty()) {
        Text(
            text = "No spoils collected.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (resourceEntries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                resourceEntries.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        Text(text = value, color = Color(0xFF80E8F5), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (itemEntries.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Loot",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(itemEntries, key = { it.key }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = itemNameResolver(entry.key),
                                color = Color.White
                            )
                            Text(
                                text = "×${entry.value}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VictoryLevelUpContent(levelUps: List<LevelUpSummary>) {
    if (levelUps.isEmpty()) {
        Text(
            text = "No one advanced this battle.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(levelUps, key = { summary -> summary.characterId }) { summary ->
            LevelUpCard(summary)
        }
    }
}

@Composable
private fun LevelUpCard(summary: LevelUpSummary) {
    Surface(
        color = Color(0xFF1A1F2B).copy(alpha = 0.9f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = summary.characterName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Reached Lv. ${summary.newLevel} (+${summary.levelsGained})",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF80E8F5)
            )
            if (summary.statChanges.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    summary.statChanges.forEach { delta ->
                        Text(
                            text = "• ${delta.label}: ${delta.value}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            if (summary.unlockedSkills.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "New Skills",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFE082)
                    )
                    summary.unlockedSkills.forEach { skill ->
                        Text(
                            text = "• ${skill.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatLogPanel(
    flavorLine: String?,
    instruction: String?,
    showCancel: Boolean,
    onCancel: (() -> Unit)?,
    actionMenu: (@Composable () -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedFlavorText(flavorLine)
        instruction?.let {
            Text(
                text = it,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (showCancel && onCancel != null) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                Text(text = "Cancel target")
            }
        }
        actionMenu?.let {
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            it()
        }
    }
}

@Composable
private fun ActionMenu(
    actor: Player,
    canAttack: Boolean,
    hasSkills: Boolean,
    hasItems: Boolean,
    onAttack: () -> Unit,
    onSkills: () -> Unit,
    onItems: () -> Unit,
    onDefend: () -> Unit,
    onRetreat: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val minHeight = if (largeTouchTargets) 52.dp else 0.dp
        Text(
            text = "${actor.name} is ready.",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAttack,
                enabled = canAttack,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = minHeight)
            ) {
                Text("Attack")
            }
            Button(
                onClick = onSkills,
                enabled = hasSkills,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = minHeight)
            ) {
                Text("Skills")
            }
            Button(
                onClick = onItems,
                enabled = hasItems,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = minHeight)
            ) {
                Text("Items")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDefend,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = minHeight)
            ) {
                Text("Defend")
            }
            Button(
                onClick = onRetreat,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = minHeight)
            ) {
                Text("Retreat")
            }
        }
        if (highContrastMode) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.28f))
        }
    }
}

@Composable
private fun CombatItemsDialog(
    items: List<InventoryEntry>,
    onItemSelected: (InventoryEntry) -> Unit,
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
                                onClick = { onItemSelected(entry) },
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
private fun AnimatedFlavorText(line: String?) {
    if (line.isNullOrBlank()) return
    val offsetY = remember { Animatable(-40f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember(line) { (Random.nextInt(-10, 11)).toFloat() }
    LaunchedEffect(line) {
        offsetY.snapTo(-40f)
        alpha.snapTo(0f)
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 420, easing = EaseOutBack)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 180)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, delayMillis = 1600)
        )
    }
    Text(
        text = line,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = offsetY.value
                this.rotationZ = rotation
                this.alpha = alpha.value
            },
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AtbBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2D9CFF)
) {
    val clamped = progress.coerceIn(0f, 1f)
    val tipTransition = rememberInfiniteTransition(label = "atb_tip_transition")
    val tipGlow by tipTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atb_tip_glow"
    )
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
    ) {
        GlowProgressBar(
            progress = clamped,
            modifier = Modifier
                .fillMaxSize(),
            trackColor = Color.Black.copy(alpha = 0.45f),
            glowColor = color.copy(alpha = 0.95f),
            height = 8.dp,
            glowWidth = 0.18f
        )
        if (clamped > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tipX = size.width * clamped
                drawRoundRect(
                    color = color.copy(alpha = 0.2f),
                    topLeft = Offset.Zero,
                    size = Size(size.width * clamped, size.height),
                    cornerRadius = CornerRadius(size.height, size.height)
                )
                drawCircle(
                    color = color.copy(alpha = 0.25f + 0.45f * tipGlow),
                    radius = size.height * 0.75f,
                    center = Offset(tipX, size.height / 2f)
                )
            }
        }
    }
}
