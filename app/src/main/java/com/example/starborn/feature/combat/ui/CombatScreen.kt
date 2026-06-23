package com.example.starborn.feature.combat.ui

import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.lerp
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Whatshot
import com.example.starborn.domain.combat.CombatOutcome
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.ActiveBuff
import com.example.starborn.domain.combat.StatusEffect
import com.example.starborn.domain.combat.ElementalStackRules
import com.example.starborn.domain.leveling.LevelUpSummary
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Skill
import com.example.starborn.data.local.Theme
import com.example.starborn.feature.combat.viewmodel.AttackLungeStyle
import com.example.starborn.feature.combat.viewmodel.CombatBannerAccent
import com.example.starborn.feature.combat.viewmodel.CombatBannerImportance
import com.example.starborn.feature.combat.viewmodel.CombatBannerIcon
import com.example.starborn.feature.combat.viewmodel.CombatBannerMessage
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.CombatViewModel.TimedPromptState
import com.example.starborn.feature.combat.viewmodel.CombatTutorialState
import com.example.starborn.feature.combat.viewmodel.CombatTutorialStep
import com.example.starborn.feature.combat.viewmodel.CombatFxEvent
import com.example.starborn.feature.combat.ui.animations.CombatSide
import com.example.starborn.feature.combat.ui.animations.LungeAxis
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import com.example.starborn.feature.combat.ui.components.*
import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.feature.exploration.ui.CinematicOverlayHost
import com.example.starborn.feature.exploration.ui.CombatTransitionOverlay
import com.example.starborn.feature.exploration.ui.TransitionMode
import com.example.starborn.feature.exploration.viewmodel.CinematicStepUi
import com.example.starborn.feature.exploration.viewmodel.CinematicUiState
import com.example.starborn.ui.dialogs.SkillsDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.domain.audio.AudioCuePlayer
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.navigation.CombatResultPayload
import com.example.starborn.ui.background.rememberAssetPainter
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.ui.vfx.WeatherOverlay
import com.example.starborn.ui.vfx.VignetteOverlay
import com.example.starborn.ui.vfx.GlowProgressBar
import com.example.starborn.ui.theme.themeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

private const val STATUS_SOURCE_PREFIX = "status_"
private const val ATTACK_FX_DURATION_MS = 500L

internal val CombatNameFont = FontFamily(
    Font(R.font.orbitron_medium, weight = FontWeight.Medium)
)
private val TargetRippleColor = Color(0xFF3FE4FF)



private fun CinematicPlaybackState.toUiState(): CinematicUiState {
    val steps = scene.steps
    if (steps.isEmpty()) {
        return CinematicUiState(
            sceneId = scene.id,
            title = scene.title,
            stepIndex = 0,
            stepCount = 0,
            step = CinematicStepUi(
                type = CinematicStepType.NARRATION,
                speaker = null,
                text = ""
            )
        )
    }
    val safeIndex = stepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[safeIndex]
    return CinematicUiState(
        sceneId = scene.id,
        title = scene.title,
        stepIndex = safeIndex,
        stepCount = steps.size,
        step = CinematicStepUi(
            type = step.type,
            speaker = step.speaker,
            text = step.text,
            portrait = null
        )
    )
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
    largeTouchTargets: Boolean,
    showCombatActionText: Boolean,
    cinematicState: StateFlow<CinematicPlaybackState?>? = null,
    onAdvanceCinematic: (() -> Unit)? = null
) {
    BackHandler(enabled = true) {
        // Block system back/edge-swipe from leaving combat.
    }
    val playerParty = remember(viewModel) { viewModel.playerParty.toList() }
    val enemies = viewModel.enemies
    val enemyCombatantIds = viewModel.enemyCombatantIds
    val combatState by viewModel.state.collectAsState(initial = viewModel.combatState)
    val inventoryEntries by viewModel.inventory.collectAsStateWithLifecycle()
    val battleUsableItems = remember(inventoryEntries) {
        inventoryEntries.filter { it.isBattleUsableItem() }
    }
    val atbMeters by viewModel.atbMeters.collectAsStateWithLifecycle()
    val lungeActorId by viewModel.lungeActorId.collectAsStateWithLifecycle(null)
    val lungeToken by viewModel.lungeToken.collectAsStateWithLifecycle(0L)
    val lungeStyle by viewModel.lungeStyle.collectAsStateWithLifecycle(AttackLungeStyle.MELEE)
    val missLungeActorId by viewModel.missLungeActorId.collectAsStateWithLifecycle(null)
    val missLungeToken by viewModel.missLungeToken.collectAsStateWithLifecycle(0L)
    val timedPromptState by viewModel.timedPrompt.collectAsStateWithLifecycle()
    val combatTutorial by viewModel.combatTutorial.collectAsStateWithLifecycle()
    val awaitingActionId by viewModel.awaitingAction.collectAsStateWithLifecycle()
    val combatBanner by viewModel.combatBanner.collectAsStateWithLifecycle()
        val cinematicPlayback = cinematicState
        ?.collectAsStateWithLifecycle(initialValue = null)
        ?.value
    
    // Transition State
    var exitTransitionVisible by remember { mutableStateOf(true) }
    var isExiting by remember { mutableStateOf(false) }
    var exitMainText by remember { mutableStateOf("") }

    val damageFx = remember { mutableStateListOf<DamageFxUi>() }
    val healFx = remember { mutableStateListOf<HealFxUi>() }
    val statusFx = remember { mutableStateListOf<StatusFxUi>() }
    val knockoutFx = remember { mutableStateListOf<KnockoutFxUi>() }
    val supportFx = remember { mutableStateListOf<SupportFxUi>() }
    val telegraphFx = remember { mutableStateListOf<TelegraphFxUi>() }
    val attackHitFx = remember { mutableStateListOf<AttackHitFxUi>() }
    val shieldBreakFx = remember { mutableStateListOf<ShieldBreakFxUi>() }

    val delayedDeathTargets = remember { mutableStateMapOf<String, Long>() }
    var lastPlayerActionStyle by remember { mutableStateOf<AttackFxStyle?>(null) }
    var outcomeFx by remember { mutableStateOf<CombatFxEvent.CombatOutcomeFx.OutcomeType?>(null) }
    var pendingOutcome by remember { mutableStateOf<CombatOutcome?>(null) }
    var pendingVictoryPayload by remember { mutableStateOf<CombatResultPayload?>(null) }
    var victoryDialogStage by remember { mutableStateOf<VictoryDialogStage?>(null) }
    val selectedEnemyIds by viewModel.selectedEnemies.collectAsStateWithLifecycle()
    var pendingTargetRequest by remember { mutableStateOf<PendingTargetRequest?>(null) }
    var pendingInstruction by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val shakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val playerIdSet = remember(playerParty) {
        playerParty.map { it.id.lowercase(Locale.getDefault()) }.toSet()
    }
    val turnActorId = combatState?.activeCombatant?.combatant?.id
    val activeId = awaitingActionId

    val outcome = combatState?.outcome
    if (outcome != null && pendingOutcome == null) {
        pendingOutcome = outcome
    }

    val currentCombatState by rememberUpdatedState(combatState)
    val currentTurnActorId by rememberUpdatedState(turnActorId)
    LaunchedEffect(Unit) {
        launch {
            viewModel.fxEvents.collect { event ->
            when (event) {
                is CombatFxEvent.Impact -> {
                    val fx = DamageFxUi(
                        id = UUID.randomUUID().toString(),
                        targetId = event.targetId,
                        amount = event.amount,
                        element = event.element,
                        critical = event.critical
                    )
                    damageFx += fx
                    launch {
                        delay(720)
                        damageFx.remove(fx)
                    }
                    if (event.showAttackFx) {
                        val resolvedStyle = resolveAttackStyle(
                            sourceId = event.sourceId,
                            combatState = currentCombatState,
                            playerIdSet = playerIdSet
                        )
                        val style = resolvedStyle
                            ?: lastPlayerActionStyle?.takeIf { event.targetId in enemyCombatantIds }
                        if (style != null) {
                            val attackDuration = attackFxDurationFor(style)
                            val hitFx = AttackHitFxUi(
                                id = UUID.randomUUID().toString(),
                                targetId = event.targetId,
                                style = style,
                                critical = event.critical
                            )
                            attackHitFx += hitFx
                            launch {
                                delay(attackDuration)
                                attackHitFx.remove(hitFx)
                            }
                            if (event.targetDefeated) {
                                val expiry = SystemClock.elapsedRealtime() + attackDuration
                                val existing = delayedDeathTargets[event.targetId]
                                if (existing == null || expiry > existing) {
                                    delayedDeathTargets[event.targetId] = expiry
                                    launch {
                                        val remaining = (expiry - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                                        delay(remaining)
                                        if (delayedDeathTargets[event.targetId] == expiry) {
                                            delayedDeathTargets.remove(event.targetId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val normalizedSource = normalizeAttackSourceId(event.sourceId)
                    val allowShake = normalizedSource in playerIdSet && event.critical
                    if (!suppressScreenshake && allowShake) {
                        launch {
                            val amplitude = with(density) { 12.dp.toPx() }
                            val offset = Offset(
                                x = if (Random.nextBoolean()) amplitude else -amplitude,
                                y = if (Random.nextBoolean()) amplitude * 0.5f else -amplitude * 0.5f
                            )
                            shakeOffset.stop()
                            shakeOffset.snapTo(offset)
                            shakeOffset.animateTo(
                                targetValue = Offset.Zero,
                                animationSpec = tween(durationMillis = 140)
                            )
                        }
                    }
                }
                is CombatFxEvent.TurnQueued -> {
                    if (event.actorId in playerIdSet) {
                        val normalized = normalizeAttackSourceId(event.actorId)
                        lastPlayerActionStyle = attackFxStyleFor(normalized)
                            ?: attackFxStyleForName(normalized)
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
                is CombatFxEvent.Telegraph -> {
                    val fx = TelegraphFxUi(
                        id = UUID.randomUUID().toString(),
                        actorId = event.actorId,
                        skillName = event.skillName,
                        targetIds = event.targetIds
                    )
                    telegraphFx += fx
                    launch {
                        delay(800)
                        telegraphFx.remove(fx)
                    }
                }
                is CombatFxEvent.ShieldBreak -> {
                    val fx = ShieldBreakFxUi(
                        id = UUID.randomUUID().toString(),
                        targetId = event.targetId
                    )
                    shieldBreakFx += fx
                    launch {
                        delay(520)
                        shieldBreakFx.remove(fx)
                    }
                }
                is CombatFxEvent.Audio -> audioCuePlayer.execute(event.commands)
            }
        }
        }
        delay(100)
        viewModel.onScreenReady()
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
                    levelUps = viewModel.consumeLevelUpSummaries(),
                    sourcePartyId = viewModel.encounterSourcePartyId,
                    roomId = viewModel.encounterRoomId
                )
                victoryDialogStage = VictoryDialogStage.SPOILS
            }
            is CombatOutcome.Defeat -> {
                val payload = CombatResultPayload(
                    outcome = CombatResultPayload.Outcome.DEFEAT,
                    enemyIds = viewModel.encounterEnemyIds,
                    sourcePartyId = viewModel.encounterSourcePartyId,
                    roomId = viewModel.encounterRoomId
                )
                handle?.set("combat_result", payload)
                pendingOutcome = null
                exitMainText = "The party collapses in defeat..."
                isExiting = true
            }
            CombatOutcome.Retreat -> {
                val payload = CombatResultPayload(
                    outcome = CombatResultPayload.Outcome.RETREAT,
                    enemyIds = viewModel.encounterEnemyIds,
                    sourcePartyId = viewModel.encounterSourcePartyId,
                    roomId = viewModel.encounterRoomId
                )
                handle?.set("combat_result", payload)
                pendingOutcome = null
                exitMainText = ""
                isExiting = true
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
        exitMainText = ""
        isExiting = true
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
        val enemyEntries = enemies.mapIndexed { index, enemy ->
            enemy to enemyCombatantIds.getOrElse(index) { enemy.id }
        }
        val focusEnemyEntry = selectedEnemyIds.firstOrNull()?.let { id ->
            enemyEntries.firstOrNull { it.second == id }
    } ?: enemyEntries.firstOrNull()
    val focusEnemy = focusEnemyEntry?.first
    val focusEnemyCombatantId = focusEnemyEntry?.second

    if (focusEnemy != null && focusEnemyCombatantId != null && state != null) {
        val showSkillsDialog = remember { mutableStateOf(false) }
        val showItemsDialog = remember { mutableStateOf(false) }
        val enemyState = state.combatants[focusEnemyCombatantId]
        val combatLocked = pendingOutcome != null || timedPromptState != null || pendingVictoryPayload != null
        val menuActor = awaitingActionId?.let { id -> playerParty.firstOrNull { it.id == id } }
        val menuActorState = awaitingActionId?.let { id -> state.combatants[id] }
        val menuActorCannotAct = menuActorState?.statusEffects.orEmpty().any { effect ->
            val id = effect.id.lowercase()
            id == "shock" || id == "freeze" || id == "stun"
        }
        val allowAllySelection = pendingTargetRequest?.accepts(TargetFilter.ALLY) == true
        val enemyTargetPrompt = pendingTargetRequest?.accepts(TargetFilter.ENEMY) == true
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
            val tutorial = combatTutorial
            if (tutorial != null &&
                request.accepts(TargetFilter.ENEMY) &&
                !viewModel.isCombatTutorialTargetEnabled(targetId)
            ) {
                pendingInstruction = request.instruction
                return
            }
            when (request) {
                PendingTargetRequest.Attack -> {
                    viewModel.focusEnemyTarget(targetId)
                    viewModel.playerAttack(targetId)
                }
                is PendingTargetRequest.SnackRequest -> {
                    viewModel.useSnack(targetId)
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

        fun handlePartyMemberTap(targetId: String) {
            when {
                allowAllySelection -> handleAllyTap(targetId)
                pendingTargetRequest == null && !combatLocked && targetId == awaitingActionId ->
                    viewModel.dismissActionMenu(targetId)
                pendingTargetRequest == null && !combatLocked -> viewModel.selectReadyPlayer(targetId)
            }
        }

        fun handleSkillSelection(skill: Skill) {
            if (!viewModel.onCombatTutorialSkillSelected(skill.id)) return
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
                        instruction = "Choose a target"
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
                        instruction = "Choose a target"
                    )
                )
                null -> viewModel.useItem(entry)
            }
        }

        if (showSkillsDialog.value && menuActor != null && !combatLocked && !menuActorCannotAct) {
            SkillsDialog(
                player = menuActor,
                skills = menuActorSkills,
                viewModel = viewModel,
                onDismiss = { showSkillsDialog.value = false },
                onSkillSelected = { skill ->
                    showSkillsDialog.value = false
                    handleSkillSelection(skill)
                }
            )
        }

        if (showItemsDialog.value && !combatLocked && !menuActorCannotAct && menuActor != null) {
            CombatItemsDialog(
                items = battleUsableItems,
                theme = viewModel.theme,
                highContrastMode = highContrastMode,
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
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(3.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(Color(0xFF070B14).copy(alpha = 0.42f), BlendMode.Darken)
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
            }
            VignetteOverlay(
                visible = true,
                modifier = Modifier.fillMaxSize(),
                intensity = 0.75f,
                color = Color(0xFF030508)
            )
            val accentColor = themeColor(viewModel.theme?.accent, Color(0xFF7BE4FF))
            val borderColor = themeColor(viewModel.theme?.border, Color(0xFF5CCBE8))
            val panelColor = themeColor(viewModel.theme?.bg, Color(0xFF061018))
            val commandActor = menuActor
            val targetPromptText = when {
                !pendingInstruction.isNullOrBlank() -> pendingInstruction.orEmpty()
                pendingTargetRequest != null || enemyTargetPrompt -> "Choose a target"
                else -> null
            }
            val targetMode = !targetPromptText.isNullOrBlank()
            val commandPaletteVisible = commandActor != null &&
                !combatLocked &&
                !menuActorCannotAct &&
                !targetMode
            val hasTargets = enemyCombatantIds.any { state.combatants[it]?.isAlive == true }
            val snackBaseLabel = commandActor?.let { actor -> viewModel.snackLabel(actor.id) } ?: "Snack"
            val snackCooldown = commandActor?.let { actor -> viewModel.snackCooldownRemaining(actor.id) } ?: 0
            val snackLabel = when {
                snackCooldown > 0 -> "$snackBaseLabel ($snackCooldown)"
                else -> snackBaseLabel
            }
            val snackRequirement = commandActor?.let { actor -> viewModel.snackTargetRequirement(actor.id) } ?: TargetRequirement.NONE
            val snackUsable = commandActor?.let { actor -> viewModel.canUseSnack(actor.id) } == true
            val canSnack = snackUsable && when (snackRequirement) {
                TargetRequirement.ENEMY -> hasTargets
                TargetRequirement.ALLY -> playerParty.any { member -> state.combatants[member.id]?.isAlive == true }
                TargetRequirement.ANY -> hasTargets || playerParty.any { member -> state.combatants[member.id]?.isAlive == true }
                TargetRequirement.NONE -> true
            }
            val commandActorState = commandActor?.let { actor -> state.combatants[actor.id] }
            val commandMaxHp = commandActorState?.combatant?.stats?.maxHp
                ?: commandActor?.hp?.coerceAtLeast(1)
                ?: 1
            val commandCurrentHp = commandActorState?.hp ?: commandMaxHp
            val partyDockHeightPx = remember(playerParty.size) { mutableStateOf(0) }
            val partyDockHeight = with(density) { partyDockHeightPx.value.toDp() }
            val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                BattleStageBackdrop(
                    accentColor = accentColor,
                    borderColor = borderColor,
                    panelColor = panelColor,
                    highContrastMode = highContrastMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 92.dp, bottom = partyDockHeight + 76.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CombatEncounterHeader(
                            locationTitle = viewModel.locationTitle,
                            statusText = targetPromptText ?: "Hostile Contact",
                            targetMode = targetMode,
                            onCancelTarget = if (pendingTargetRequest != null) {
                                { clearPendingRequest() }
                            } else null,
                            theme = viewModel.theme,
                            highContrastMode = highContrastMode,
                            modifier = Modifier.fillMaxWidth()
                        )
                        EnemyRoster(
                            enemies = enemies,
                            combatantIds = enemyCombatantIds,
                            combatState = state,
                            activeId = activeId,
                            selectedEnemyIds = selectedEnemyIds,
                            labelBorderColor = accentColor,
                            damageFx = damageFx,
                            attackFx = attackHitFx,
                            healFx = healFx,
                            statusFx = statusFx,
                            shieldBreakFx = shieldBreakFx,

                            knockoutFx = knockoutFx,
                            telegraphFx = telegraphFx,
                            delayedDeathTargets = delayedDeathTargets,
                            onEnemyTap = { handleEnemyTap(it) },
                            onEnemyLongPress = {
                                if (pendingTargetRequest == null && !combatLocked) {
                                    viewModel.toggleEnemyTarget(it)
                                }
                            },
                            atbMeters = atbMeters,
                            lungeActorId = lungeActorId,
                            lungeToken = lungeToken,
                            missLungeActorId = missLungeActorId,
                            missLungeToken = missLungeToken,
                            onLungeFinished = viewModel::onLungeFinished,
                            onMissLungeFinished = viewModel::onMissLungeFinished,
                            showTargetPrompt = false,
                            lungeStyle = lungeStyle
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = partyDockHeight + 8.dp)
                ) {
                    if (!commandPaletteVisible && !targetMode) {
                        CombatLogPanel(
                            bannerMessage = if (showCombatActionText) combatBanner else null,
                            instruction = null,
                            showCancel = false,
                            instructionShownAbove = false,
                            highContrastMode = highContrastMode,
                            theme = viewModel.theme,
                            onCancel = null
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                PartyRoster(
                    party = playerParty,
                    combatState = state,
                    activeId = activeId,
                    damageFx = damageFx,
                    attackFx = attackHitFx,
                    healFx = healFx,
                    statusFx = statusFx,
                    shieldBreakFx = shieldBreakFx,

                    knockoutFx = knockoutFx,
                    supportFx = supportFx,
                    telegraphFx = telegraphFx,
                    onMemberTap = { handlePartyMemberTap(it) },
                    allowNonReadySelection = allowAllySelection,
                    victoryEmotes = victoryEmotes,
                    atbMeters = atbMeters,
                    lungeActorId = lungeActorId,
                    lungeToken = lungeToken,
                    lungeStyle = lungeStyle,
                    onLungeFinished = viewModel::onLungeFinished,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 4.dp)
                        .onGloballyPositioned { coords ->
                            val height = coords.size.height
                            if (height > 0 && partyDockHeightPx.value != height) {
                                partyDockHeightPx.value = height
                            }
                        }
                )
            }

            CommandPalette(
                visible = commandPaletteVisible,
                actor = commandActor,
                currentHp = commandCurrentHp,
                maxHp = commandMaxHp,
                atbProgress = commandActor?.let { atbMeters[it.id] } ?: 0f,
                canAttack = hasTargets,
                hasSkills = menuActorSkills.isNotEmpty(),
                hasItems = battleUsableItems.isNotEmpty(),
                onAttack = {
                    if (viewModel.onCombatTutorialCommand("Attack")) {
                        requestTarget(PendingTargetRequest.Attack)
                    }
                },
                onSkills = {
                    if (viewModel.onCombatTutorialCommand("Skills")) {
                        showSkillsDialog.value = true
                        pendingTargetRequest = null
                        pendingInstruction = null
                    }
                },
                onItems = {
                    showItemsDialog.value = true
                    pendingTargetRequest = null
                    pendingInstruction = null
                },
                snackLabel = snackLabel,
                canSnack = canSnack,
                snackCooldown = snackCooldown,
                onSnack = {
                    when (snackRequirement) {
                        TargetRequirement.ENEMY -> requestTarget(
                            PendingTargetRequest.SnackRequest(
                                snackName = snackBaseLabel,
                                filter = TargetFilter.ENEMY,
                                instruction = "Choose an enemy for $snackBaseLabel"
                            )
                        )
                        TargetRequirement.ALLY -> requestTarget(
                            PendingTargetRequest.SnackRequest(
                                snackName = snackBaseLabel,
                                filter = TargetFilter.ALLY,
                                instruction = "Choose an ally for $snackBaseLabel"
                            )
                        )
                        TargetRequirement.ANY -> requestTarget(
                            PendingTargetRequest.SnackRequest(
                                snackName = snackBaseLabel,
                                filter = TargetFilter.ANY,
                                instruction = "Choose a target"
                            )
                        )
                        TargetRequirement.NONE -> {
                            pendingTargetRequest = null
                            pendingInstruction = null
                            viewModel.useSnack()
                        }
                    }
                },
                onRetreat = {
                    pendingTargetRequest = null
                    pendingInstruction = null
                    viewModel.attemptRetreat()
                },
                highContrastMode = highContrastMode,
                largeTouchTargets = largeTouchTargets,
                theme = viewModel.theme,
                targetInstruction = null,
                onCancelTarget = null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(contentPadding)
                    .padding(top = 138.dp)
                    .zIndex(6f)
            )
            outcomeFx?.let { OutcomeOverlay(it, playerParty) }
            timedPromptState?.let { prompt ->
                TimedPromptOverlay(
                    prompt = prompt,
                    onTap = { viewModel.registerTimedPromptTap() }
                )
            }
            combatTutorial?.let { tutorial ->
                CombatTutorialOverlay(
                    tutorial = tutorial,
                    theme = viewModel.theme,
                    highContrastMode = highContrastMode,
                    onContinue = viewModel::onCombatTutorialContinue,
                    onSkip = viewModel::skipCombatTutorial,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .zIndex(22f)
                )
            }
            val victoryPayload = pendingVictoryPayload
            if (victoryPayload != null) {
                victoryDialogStage?.let { stage ->
                    val portraitById = remember(playerParty) {
                        playerParty.associate { member -> member.id to member.miniIconPath }
                    }
                    VictoryDialog(
                        stage = stage,
                        payload = victoryPayload,
                        itemNameResolver = viewModel::itemDisplayName,
                        portraitById = portraitById,
                        highContrastMode = highContrastMode,
                        theme = viewModel.theme,
                        onContinue = { advanceVictoryDialog() }
                    )
                }
            }
            CinematicOverlayHost(
                state = cinematicPlayback?.toUiState(),
                onAdvance = { onAdvanceCinematic?.invoke() },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(80f)
            )
            
            CombatTransitionOverlay(
                visible = exitTransitionVisible,
                theme = viewModel.theme,
                suppressFlashes = suppressFlashes,
                highContrastMode = highContrastMode,
                mode = TransitionMode.EXIT,
                onFinished = { exitTransitionVisible = false },
                modifier = Modifier.zIndex(100f)
            )

            if (isExiting) {
                CombatTransitionOverlay(
                    visible = true,
                    theme = viewModel.theme,
                    suppressFlashes = suppressFlashes,
                    highContrastMode = highContrastMode,
                    mode = TransitionMode.ENTER,
                    mainText = exitMainText,
                    subText = "",
                    onFinished = { navController.popBackStack() },
                    modifier = Modifier.zIndex(100f)
                )
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
internal fun CombatImpactBanner(
    message: CombatBannerMessage?,
    hasInstruction: Boolean,
    persistent: Boolean = false,
    highContrastMode: Boolean,
    theme: Theme?,
    modifier: Modifier = Modifier
) {
    if (message == null || message.primary.isBlank()) return
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(14f) }
    val scale = remember { Animatable(0.98f) }
    val glow = remember { Animatable(0f) }
    val lastId = remember { mutableStateOf<String?>(null) }
    val tagKey = remember(message.tags) { message.tags.joinToString("|") }

    LaunchedEffect(message.id, message.primary, message.secondary, tagKey, persistent) {
        val isNew = message.id != lastId.value
        if (isNew) {
            lastId.value = message.id
            alpha.snapTo(0f)
            offsetY.snapTo(14f)
            scale.snapTo(0.98f)
            glow.snapTo(0f)
            launch {
                offsetY.animateTo(0f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
            }
            launch {
                scale.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing))
            }
            alpha.animateTo(1f, tween(durationMillis = 140, easing = LinearEasing))
        } else {
            if (alpha.value < 1f) {
                alpha.animateTo(1f, tween(durationMillis = 80, easing = LinearEasing))
            }
            glow.snapTo(1f)
            glow.animateTo(0f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
        }
        if (!persistent) {
            val baseHoldMs = if (message.importance == CombatBannerImportance.IMPORTANT) 1700L else 1400L
            val holdMs = when {
                message.icon == CombatBannerIcon.OUTCOME -> 2200L
                message.icon == CombatBannerIcon.BURST -> 2000L
                message.tags.any { it.equals("KO", ignoreCase = true) } -> 2000L
                else -> baseHoldMs
            }
            delay(holdMs)
            alpha.animateTo(0f, tween(durationMillis = 200, easing = LinearEasing))
        }
    }

    if (alpha.value <= 0.001f) return
    val accent = bannerAccentColor(message.accent, theme)
    val tightMode = hasInstruction
    val icon = if (tightMode) null else bannerIcon(message.icon)
    val primaryStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = 16.sp,
        fontFamily = CombatNameFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 2f)
    )
    val secondaryStyle = MaterialTheme.typography.bodyMedium.copy(
        color = Color.White.copy(alpha = 0.85f),
        fontWeight = FontWeight.Medium,
        shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 1f)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value
                scaleX = scale.value
                scaleY = scale.value
            }
            .fillMaxWidth()
            .background(
                color = Color(0xFF0A0C10).copy(alpha = 0.85f), // Solid dark backing for legibility
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        // Subtle Gradient Highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.35f),
                            accent.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 300f
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
        )

        Row(
            modifier = Modifier
                .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )

            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = message.primary,
                    style = primaryStyle,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun bannerIcon(icon: CombatBannerIcon?): ImageVector? =
    when (icon) {
        null -> null
        CombatBannerIcon.ATTACK -> Icons.Rounded.Bolt
        CombatBannerIcon.SKILL -> Icons.Rounded.AutoAwesome
        CombatBannerIcon.ITEM -> Icons.Rounded.Inventory2
        CombatBannerIcon.SNACK -> Icons.Rounded.Restaurant
        CombatBannerIcon.GUARD -> Icons.Rounded.Shield
        CombatBannerIcon.RETREAT -> Icons.Rounded.ExitToApp
        CombatBannerIcon.HEAL -> Icons.Filled.CheckCircle
        CombatBannerIcon.STATUS -> Icons.Filled.Flag
        CombatBannerIcon.BURST -> Icons.Rounded.Whatshot
        CombatBannerIcon.OUTCOME -> Icons.Filled.EmojiEvents
        CombatBannerIcon.MISS -> Icons.Outlined.RadioButtonUnchecked
    }

private fun bannerAccentColor(accent: CombatBannerAccent, theme: Theme?): Color =
    when (accent) {
        CombatBannerAccent.DEFAULT -> themeColor(theme?.accent, Color(0xFF2D9CFF))
        CombatBannerAccent.MISS -> Color(0xFFB0BEC5)
        CombatBannerAccent.HEAL -> Color(0xFF81C784)
        CombatBannerAccent.BURN -> Color(0xFFFF7043)
        CombatBannerAccent.FREEZE -> Color(0xFF90CAF9)
        CombatBannerAccent.SHOCK -> Color(0xFF42A5F5)
        CombatBannerAccent.ACID -> Color(0xFF81C784)
        CombatBannerAccent.SOURCE -> Color(0xFFBA68C8)
        CombatBannerAccent.PHYSICAL -> Color(0xFFFFB74D)
        CombatBannerAccent.NOVA -> Color(0xFF7BE4FF)
        CombatBannerAccent.ZEKE -> Color(0xFFFFB74D)
        CombatBannerAccent.ORION -> Color(0xFFB388FF)
        CombatBannerAccent.GHOST -> Color(0xFF80D8FF)
        CombatBannerAccent.ENEMY -> themeColor(theme?.accent, Color(0xFFFF6A5F))
    }

internal fun titleCaseName(name: String): String {
    val cleaned = name.replace('_', ' ').trim()
    if (cleaned.isEmpty()) return name
    return cleaned.split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            val lower = token.lowercase(Locale.getDefault())
            lower.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
}

private fun InventoryEntry.targetFilter(): TargetFilter? {
    val effect = item.effect ?: return null
    val declared = effect.target?.lowercase()
    return when {
        declared == "enemy" || declared == "single_enemy" -> TargetFilter.ENEMY
        declared == "ally" || declared == "single_ally" -> TargetFilter.ALLY
        declared == "any" -> TargetFilter.ANY
        declared == "self" || declared == "party" || declared == "enemy_group" || declared == "all_enemies" -> null
        effect.damage?.let { it > 0 } == true -> TargetFilter.ENEMY
        effect.restoreHp?.let { it > 0 } == true -> TargetFilter.ALLY
        effect.singleBuff != null || !effect.buffs.isNullOrEmpty() -> TargetFilter.ALLY
        else -> null
    }
}

private fun InventoryEntry.isBattleUsableItem(): Boolean {
    if (quantity <= 0) return false
    val effect = item.effect ?: return false
    return effect.restoreHp?.let { it > 0 } == true ||
        effect.damage?.let { it > 0 } == true ||
        effect.singleBuff != null ||
        !effect.buffs.isNullOrEmpty()
}
