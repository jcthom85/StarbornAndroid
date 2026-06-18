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
import com.example.starborn.feature.combat.ui.animations.Lungeable
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
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

private val CombatNameFont = FontFamily(
    Font(R.font.orbitron_medium, weight = FontWeight.Medium)
)
private val TargetRippleColor = Color(0xFF3FE4FF)

private data class DamageFxUi(
    val id: String,
    val targetId: String,
    val amount: Int,
    val element: String?,
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

private data class TelegraphFxUi(
    val id: String,
    val actorId: String,
    val skillName: String,
    val targetIds: List<String>
)

private data class AttackHitFxUi(
    val id: String,
    val targetId: String,
    val style: AttackFxStyle,
    val critical: Boolean
)

private data class ShieldBreakFxUi(
    val id: String,
    val targetId: String
)



private data class CompositeEnemyEntry(
    val enemy: Enemy,
    val combatantId: String,
    val layout: com.example.starborn.domain.model.CompositePart
)

private enum class AttackFxStyle {
    NOVA_LASER,
    ZEKE_PUNCH,
    ORION_JEWEL,
    GHOST_SLASH
}

private enum class TargetFilter {
    ENEMY,
    ALLY,
    ANY
}

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

private enum class VictoryDialogStage {
    SPOILS,
    LEVEL_UPS
}

private sealed interface PendingTargetRequest {
    val instruction: String
    fun accepts(target: TargetFilter): Boolean

    data object Attack : PendingTargetRequest {
        override val instruction: String = "Choose a target"
        override fun accepts(target: TargetFilter): Boolean = target == TargetFilter.ENEMY
    }

    data class SnackRequest(
        val snackName: String,
        val filter: TargetFilter,
        override val instruction: String
    ) : PendingTargetRequest {
        override fun accepts(target: TargetFilter): Boolean =
            filter == TargetFilter.ANY || filter == target
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
                exitMainText = "The party collapses in defeat..."
                isExiting = true
            }
            CombatOutcome.Retreat -> {
                val payload = CombatResultPayload(
                    outcome = CombatResultPayload.Outcome.RETREAT,
                    enemyIds = viewModel.encounterEnemyIds
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
                items = inventoryEntries,
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
                hasItems = inventoryEntries.isNotEmpty(),
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
private fun CombatEncounterHeader(
    locationTitle: String?,
    statusText: String,
    targetMode: Boolean,
    onCancelTarget: (() -> Unit)?,
    theme: Theme?,
    highContrastMode: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = panel.copy(alpha = if (highContrastMode) 0.94f else 0.70f),
        border = BorderStroke(1.dp, border.copy(alpha = if (highContrastMode) 0.72f else 0.46f)),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = locationTitle?.uppercase(Locale.getDefault()) ?: "CURRENT AREA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        ),
                        color = accent.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (targetMode) Color(0xFFFFC8B8) else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (targetMode && onCancelTarget != null) {
                    TextButton(onClick = onCancelTarget) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = Color(0xFFFF7E78),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.78f),
                                border.copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun CombatTutorialOverlay(
    tutorial: CombatTutorialState,
    theme: Theme?,
    highContrastMode: Boolean,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    val title = when (tutorial.step) {
        CombatTutorialStep.BRIEF -> "Guard Break Training"
        CombatTutorialStep.BLOCKED_EXPLANATION -> "Direct Hit: Blocked"
        CombatTutorialStep.SUCCESS -> "Guard Broken"
        else -> "Training"
    }
    val message = when (tutorial.step) {
        CombatTutorialStep.BRIEF ->
            "That trainer eats direct hits. First, test the shield, then break its guard with Hydraulic Kick."
        CombatTutorialStep.SELECT_NOVA_ATTACK -> "Tap Nova when her action is ready."
        CombatTutorialStep.CHOOSE_ATTACK -> "Choose Attack. First, test the shield."
        CombatTutorialStep.TARGET_BASIC_ATTACK -> "Choose the Acoustic Bulwark."
        CombatTutorialStep.AWAIT_BASIC_RESULT -> "Watch how the shield handles a direct hit."
        CombatTutorialStep.BLOCKED_EXPLANATION ->
            "The shield reduced the attack to zero. Guard Break strips protection before you commit damage."
        CombatTutorialStep.SELECT_NOVA_SKILL -> "Nova is ready again. Tap Nova to break the guard."
        CombatTutorialStep.CHOOSE_SKILLS -> "Open Abilities to find a guard-breaking move."
        CombatTutorialStep.CHOOSE_HYDRAULIC_KICK -> "Use Hydraulic Kick."
        CombatTutorialStep.TARGET_HYDRAULIC_KICK -> "Choose an enemy for Hydraulic Kick."
        CombatTutorialStep.AWAIT_SHIELD_BREAK -> "Watch the guard break."
        CombatTutorialStep.SUCCESS ->
            "Hydraulic Kick stripped the shield. Now finish the fight."
    }
    if (tutorial.showsModal) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.98f else 0.94f),
                border = BorderStroke(1.dp, border.copy(alpha = 0.72f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onSkip) {
                            Text("Skip Training", color = Color.White.copy(alpha = 0.72f))
                        }
                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent.copy(alpha = 0.92f),
                                contentColor = Color(0xFF041018)
                            )
                        ) {
                            Text(
                                text = when (tutorial.step) {
                                    CombatTutorialStep.BRIEF -> "Start Training"
                                    CombatTutorialStep.BLOCKED_EXPLANATION -> "Break The Guard"
                                    CombatTutorialStep.SUCCESS -> "Finish The Fight"
                                    else -> "Continue"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 190.dp),
                shape = RoundedCornerShape(12.dp),
                color = panel.copy(alpha = if (highContrastMode) 0.96f else 0.88f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.62f)),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun BattleStageBackdrop(
    accentColor: Color,
    borderColor: Color,
    panelColor: Color,
    highContrastMode: Boolean,
    modifier: Modifier = Modifier
) {
    val motion = rememberInfiniteTransition(label = "battle_stage_backdrop")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "battle_stage_phase"
    )
    Canvas(modifier = modifier) {
        val railAlpha = if (highContrastMode) 0.34f else 0.22f
        val centerY = size.height * 0.47f
        val laneHeight = size.height * 0.18f
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    panelColor.copy(alpha = if (highContrastMode) 0.42f else 0.26f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(0f, centerY - laneHeight / 2f),
            size = Size(size.width, laneHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        )
        val pulse = 0.55f + 0.45f * sin(phase * 2f * PI).toFloat()
        drawLine(
            color = accentColor.copy(alpha = 0.18f + 0.10f * pulse),
            start = Offset(size.width * 0.08f, centerY),
            end = Offset(size.width * 0.92f, centerY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = borderColor.copy(alpha = railAlpha),
            start = Offset(size.width * 0.12f, size.height * 0.20f),
            end = Offset(size.width * 0.88f, size.height * 0.20f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = borderColor.copy(alpha = railAlpha),
            start = Offset(size.width * 0.12f, size.height * 0.76f),
            end = Offset(size.width * 0.88f, size.height * 0.76f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        val tickCount = 7
        repeat(tickCount) { index ->
            val x = size.width * (0.16f + index * 0.68f / (tickCount - 1).coerceAtLeast(1))
            val alpha = 0.08f + 0.08f * ((phase + index * 0.13f) % 1f)
            drawLine(
                color = accentColor.copy(alpha = alpha),
                start = Offset(x, centerY - laneHeight * 0.28f),
                end = Offset(x, centerY + laneHeight * 0.28f),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
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
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    shieldBreakFx: List<ShieldBreakFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    supportFx: List<SupportFxUi>,
    telegraphFx: List<TelegraphFxUi>,
    onMemberTap: ((String) -> Unit)? = null,
    allowNonReadySelection: Boolean = false,
    victoryEmotes: Boolean = false,
    atbMeters: Map<String, Float> = emptyMap(),
    lungeActorId: String?,
    lungeToken: Long,
    lungeStyle: AttackLungeStyle,
    onLungeFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (party.isEmpty()) return
    val rows = remember(party) { party.chunked(2) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { rowMembers ->
            key(rowMembers.joinToString(separator = "|") { member -> member.id }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                    rowMembers.forEach { member ->
                        key(member.id) {
                            val memberState = combatState.combatants[member.id]
                            val maxHp = memberState?.combatant?.stats?.maxHp ?: member.hp.coerceAtLeast(1)
                            val currentHp = memberState?.hp ?: maxHp
                            val isActive = member.id == activeId
                            val supportHighlights = supportFx.filter { member.id in it.targetIds }
                            val telegraphHighlights = telegraphFx.filter { member.id in it.targetIds }
                            val cardShape = RoundedCornerShape(22.dp)
                            val isAlive = memberState?.isAlive != false
                            val memberLungeToken = if (member.id == lungeActorId) lungeToken else null
                            val isMissLunge = memberLungeToken != null && lungeStyle == AttackLungeStyle.MISS
                            val portraitPath = when {
                                !isAlive -> "images/characters/emotes/${member.id}_down.png"
                                isMissLunge -> "images/characters/emotes/${member.id}_confident.png"
                                memberLungeToken != null -> "images/characters/emotes/${member.id}_angry.png"
                                victoryEmotes -> "images/characters/emotes/${member.id}_cool.png"
                                else -> member.combatIconPath.takeIf { it.isNotBlank() } ?: member.miniIconPath
                            }
                            val portraitPainter = rememberAssetPainter(portraitPath, painterResource(R.drawable.main_menu_background))
                            val portraitFrameSize = 114.dp
                            val atbProgress = atbMeters[member.id] ?: 0f
                            val readyToAct = atbProgress >= 0.999f

                            val baseModifier = Modifier.widthIn(min = 150.dp)
                            val canTap = onMemberTap != null && isAlive && (readyToAct || allowNonReadySelection)
                            val interactiveModifier = if (canTap) {
                                baseModifier.clickable { onMemberTap(member.id) }
                            } else {
                                baseModifier
                            }
                            val damageFlash = rememberDamageFlash(member.id, combatState.log)
                            val hitPulse = rememberHitPulse(member.id, combatState.log)
                            val hitRecoilY = rememberHitRecoil(
                                targetId = member.id,
                                log = combatState.log,
                                directionSign = 1f
                            )
                            val suppressLunge = damageFlash > 0f
                            Box(
                                modifier = interactiveModifier.padding(horizontal = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(portraitFrameSize),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (readyToAct) {
                                            ReadyAura(
                                                color = Color(0xFF4EE7FF),
                                                modifier = Modifier.matchParentSize()
                                            )
                                        }
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
                                                .size(100.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1C1F24))
                                                .graphicsLayer {
                                                    compositingStrategy = CompositingStrategy.Offscreen
                                                    val pulse = 1f + 0.06f * hitPulse
                                                    scaleX = pulse
                                                    scaleY = pulse
                                                    translationY = hitRecoilY
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val lungeDistance = when {
                                                suppressLunge -> 0.dp
                                                isMissLunge -> 18.dp
                                                lungeStyle == AttackLungeStyle.RANGED -> 12.dp
                                                lungeStyle == AttackLungeStyle.BUFF -> 16.dp
                                                lungeStyle == AttackLungeStyle.CAST -> 8.dp
                                                lungeStyle == AttackLungeStyle.ITEM -> 6.dp
                                                lungeStyle == AttackLungeStyle.SNACK -> 8.dp
                                                else -> 24.dp
                                            }
                                            val lungeAxis = if (isMissLunge || lungeStyle == AttackLungeStyle.ITEM) LungeAxis.X else LungeAxis.Y
                                            val lungeDirectionSign = when {
                                                isMissLunge -> -1f
                                                lungeStyle == AttackLungeStyle.RANGED -> -1f
                                                else -> 1f
                                            }
                                            Lungeable(
                                                side = CombatSide.PLAYER,
                                                triggerToken = memberLungeToken,
                                                distance = lungeDistance,
                                                axis = lungeAxis,
                                                directionSign = lungeDirectionSign,
                                                modifier = Modifier.matchParentSize(),
                                                onFinished = { memberLungeToken?.let(onLungeFinished) }
                                            ) {
                                                Image(
                                                    painter = portraitPainter,
                                                    contentDescription = member.name,
                                                    modifier = Modifier.matchParentSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            val shielded = memberState?.statusEffects.orEmpty().any { status ->
                                                isShieldVisualStatus(status.id)
                                            }
                                            if (shielded && isAlive) {
                                                ShieldFieldOverlay(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .clip(CircleShape)
                                                )
                                            }
                                            if (damageFlash > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.18f * damageFlash))
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = titleCaseName(member.name),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = CombatNameFont,
                                            fontWeight = FontWeight.Medium
                                        ),
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
                                }
                                val statuses = memberState?.statusEffects.orEmpty()
                                val buffs = memberState?.buffs.orEmpty()
                                if (statuses.isNotEmpty() || buffs.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .zIndex(0.5f),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) {
                                            StatusBadges(statuses = statuses, buffs = buffs)

                                        }
                                    }
                                }
                                CombatFxOverlay(
                                    damageFx = damageFx.filter { it.targetId == member.id },
                                    attackFx = attackFx.filter { it.targetId == member.id },
                                    healFx = healFx.filter { it.targetId == member.id },
                                    statusFx = statusFx.filter { it.targetId == member.id },
                                    shieldBreakFx = shieldBreakFx.filter { it.targetId == member.id },
                                    showKnockout = knockoutFx.any { it.targetId == member.id },
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 12.dp)
                                        .size(portraitFrameSize)
                                )
                                CombatFxOverlay(
                                    damageFx = emptyList(),
                                    attackFx = emptyList(),
                                    healFx = emptyList(),
                                    statusFx = emptyList(),
                                    shieldBreakFx = emptyList(),
                                    showKnockout = false,
                                    shape = cardShape,
                                    supportFx = supportHighlights,
                                    telegraphFx = telegraphHighlights,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                        }
                    }
                    if (rowMembers.size == 1 && party.size > 1) {
                        Spacer(modifier = Modifier.width(150.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnemyRoster(
    enemies: List<Enemy>,
    combatantIds: List<String>,
    combatState: CombatState,
    activeId: String?,
    selectedEnemyIds: Set<String>,
    labelBorderColor: Color,
    damageFx: List<DamageFxUi>,
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    shieldBreakFx: List<ShieldBreakFxUi>,

    knockoutFx: List<KnockoutFxUi>,
    telegraphFx: List<TelegraphFxUi>,
    delayedDeathTargets: Map<String, Long>,
    onEnemyTap: (String) -> Unit,
    onEnemyLongPress: (String) -> Unit,
    atbMeters: Map<String, Float> = emptyMap(),
    lungeActorId: String?,
    lungeToken: Long,
    missLungeActorId: String?,
    missLungeToken: Long,
    onLungeFinished: (Long) -> Unit,
    onMissLungeFinished: (Long) -> Unit,
    showTargetPrompt: Boolean = false,
    lungeStyle: AttackLungeStyle = AttackLungeStyle.MELEE
) {
    if (enemies.isEmpty()) return
    val compositeGroup = enemies.firstOrNull()?.composite?.group
    val useCompositeLayout = compositeGroup != null &&
        enemies.all { it.composite?.group == compositeGroup }
    if (useCompositeLayout) {
        CompositeEnemyRoster(
            enemies = enemies,
            combatantIds = combatantIds,
            combatState = combatState,
            activeId = activeId,
            selectedEnemyIds = selectedEnemyIds,
            labelBorderColor = labelBorderColor,
            damageFx = damageFx,
            attackFx = attackFx,
            healFx = healFx,
            statusFx = statusFx,
            shieldBreakFx = shieldBreakFx,

            knockoutFx = knockoutFx,
            telegraphFx = telegraphFx,
            delayedDeathTargets = delayedDeathTargets,
            onEnemyTap = onEnemyTap,
            onEnemyLongPress = onEnemyLongPress,
            atbMeters = atbMeters,
            lungeActorId = lungeActorId,
            lungeToken = lungeToken,
            missLungeActorId = missLungeActorId,
            missLungeToken = missLungeToken,
            onLungeFinished = onLungeFinished,
            onMissLungeFinished = onMissLungeFinished,
            showTargetPrompt = showTargetPrompt,
            lungeStyle = lungeStyle
        )
        return
    }
    val density = LocalDensity.current
    val totalEnemies = enemies.size
    val rowCounts = when {
        totalEnemies <= 3 -> listOf(totalEnemies)
        totalEnemies == 4 -> listOf(2, 2)
        else -> listOf(2, 3)
    }
    val indexedEnemies = enemies.mapIndexed { index, enemy -> index to enemy }
    var cursor = 0
    val rows = rowCounts.mapNotNull { count ->
        if (cursor >= indexedEnemies.size) return@mapNotNull null
        val end = (cursor + count).coerceAtMost(indexedEnemies.size)
        val slice = indexedEnemies.subList(cursor, end)
        cursor = end
        slice
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (rows.size > 1) 360.dp else 240.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val boxWidth = this.maxWidth
        val rowSpacing = 14.dp
        val columnSpacing = 12.dp
        val maxColumnsForSizing = 3
        val baseCardSize = 200.dp
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEachIndexed { rowIndex, row ->
                val columns = row.size.coerceAtLeast(1)
                val rowWidth = boxWidth
                val maxCardSizeForLayout =
                    (rowWidth - columnSpacing * (maxColumnsForSizing - 1)) / maxColumnsForSizing
                val cardSize = baseCardSize.coerceAtMost(maxCardSizeForLayout)
                val portraitSize = cardSize * 0.88f
                val barWidth = cardSize * 0.72f
                val innerPadding = if (cardSize < 150.dp) 6.dp else 10.dp
                val rowOffset = if (rowIndex == 0 && rows.size > 1) (-12).dp else 0.dp
                Row(
                    horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.offset(y = rowOffset)
                ) {
                    row.forEach { (index, enemy) ->
                        val combatantId = combatantIds.getOrElse(index) { enemy.id }
                        val enemyState = combatState.combatants[combatantId]
                        val maxHp = enemyState?.combatant?.stats?.maxHp ?: enemy.hp.coerceAtLeast(1)
                        val currentHp = enemyState?.hp ?: maxHp
                        val maxStability = enemyState?.combatant?.stats?.stability ?: 100
                        val currentStability = enemyState?.stability ?: maxStability
                        val isActive = activeId == combatantId
                        val isSelected = combatantId in selectedEnemyIds
                        val isAlive = enemyState?.isAlive != false
                        val telegraphHighlights = telegraphFx.filter { combatantId in it.targetIds }
                        val isVisible = isAlive || delayedDeathTargets.containsKey(combatantId)
                        val isElite = isEliteTier(enemy.tier)
                        val isBoss = isBossTier(enemy.tier)
                        val flash = rememberDamageFlash(combatantId, combatState.log)
                        val hitPulse = rememberHitPulse(combatantId, combatState.log)
                        val hitRecoilY = rememberHitRecoil(
                            targetId = combatantId,
                            log = combatState.log,
                            directionSign = -1f
                        )
                        val damageShake = rememberDamageShake(combatantId, combatState.log)
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
                            painterResource(R.drawable.main_menu_background)
                        )
                        val shape = RoundedCornerShape(28.dp)
                        val atbProgress = atbMeters[combatantId] ?: 0f
                        val spriteScale = when {
                            isElite -> 1.6f
                            isBoss -> 1f
                            else -> 1.4f
                        }
                        val rippleScale = 1f + (spriteScale - 1f) * 0.5f
                        val rippleSize = portraitSize * rippleScale
                        val labelSpacer = if (isElite) 8.dp else 4.dp
                        val enemyLungeToken = if (combatantId == lungeActorId) lungeToken else null
                        val enemyMissToken = if (combatantId == missLungeActorId) missLungeToken else null
                        val enemyInteractionSource = remember(combatantId) { MutableInteractionSource() }
                        val cardModifier = Modifier
                            .width(cardSize)
                            .graphicsLayer {
                                translationX = damageShake * with(density) { 6.dp.toPx() }
                            }
                            .zIndex(
                                when {
                                    isActive -> 3f
                                    isSelected -> 2.5f
                                    rowIndex == rows.lastIndex -> 2f
                                    else -> 1f
                                }
                            )
                            .combinedClickable(
                                interactionSource = enemyInteractionSource,
                                indication = null,
                                onClick = { if (isAlive) onEnemyTap(combatantId) },
                                onLongClick = { if (isAlive) onEnemyLongPress(combatantId) }
                            )

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(220)) +
                                scaleIn(initialScale = 0.9f, animationSpec = tween(260)),
                            exit = fadeOut(animationSpec = tween(360)) +
                                scaleOut(targetScale = 0.65f, animationSpec = tween(420)),
                            modifier = cardModifier,
                            label = "enemy_card_$combatantId"
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EnemyStatusLabel(
                                    name = enemy.name,
                                    currentHp = currentHp,
                                    maxHp = maxHp,
                                    currentStability = currentStability,
                                    maxStability = maxStability,
                                    breakTurns = enemyState?.breakTurns ?: 0,
                                    atbProgress = atbProgress,
                                    isAlive = isAlive,
                                    isActive = isActive,
                                    isSelected = isSelected,
                                    accentColor = labelBorderColor,
                                    barWidth = barWidth
                                )
                                Spacer(modifier = Modifier.height(labelSpacer))
                                Box(
                                    modifier = Modifier
                                        .size(cardSize)
                                        .graphicsLayer {
                                            val glowScale = 1f + selectionGlow.value * 0.08f
                                            val hitScale = 1f + 0.04f * hitPulse
                                            val combined = glowScale * hitScale
                                            scaleX = combined
                                            scaleY = combined
                                        }
                                ) {
                                    if (flash > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(shape)
                                                .background(Color.Black.copy(alpha = 0.18f * flash))
                                        )
                                    }
                                    SelectionRipple(
                                        isSelected = isSelected,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(bottom = innerPadding)
                                            .size(rippleSize)
                                    )
                                    val idleTransition = rememberInfiniteTransition(label = "enemy_idle_$combatantId")
                                    val idlePhase = remember(combatantId) { Random.nextFloat() * (2f * Math.PI).toFloat() }
                                    val idleWave by idleTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = (2f * Math.PI).toFloat(),
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(durationMillis = 2600, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "enemy_idle_wave"
                                    )
                                    val enemyBroken = (enemyState?.breakTurns ?: 0) > 0
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(bottom = innerPadding)
                                            .size(portraitSize)
                                            .graphicsLayer {
                                                compositingStrategy = CompositingStrategy.Offscreen
                                                if (enemyBroken) {
                                                    scaleX = spriteScale
                                                    scaleY = spriteScale
                                                    translationY = hitRecoilY
                                                } else {
                                                    val idleBreath = sin(idleWave + idlePhase)
                                                    val idleBob = idleBreath * 2.2f
                                                    val idlePuff = 1f + 0.012f * idleBreath
                                                    scaleX = spriteScale * (1f + 0.006f * idleBreath)
                                                    scaleY = spriteScale * idlePuff
                                                    translationY = hitRecoilY + idleBob
                                                }
                                            }
                                    ) {
                                        EnemyShadow(
                                            modifier = Modifier.matchParentSize()
                                        )
                                        Lungeable(
                                            side = CombatSide.ENEMY,
                                            triggerToken = enemyMissToken,
                                            distance = 18.dp,
                                            axis = LungeAxis.X,
                                            directionSign = 1f,
                                            modifier = Modifier.matchParentSize(),
                                            onFinished = { enemyMissToken?.let(onMissLungeFinished) }
                                        ) {
                                            val dist = when (lungeStyle) {
                                                AttackLungeStyle.RANGED -> 12.dp
                                                AttackLungeStyle.BUFF -> 16.dp
                                                AttackLungeStyle.CAST -> 8.dp
                                                AttackLungeStyle.ITEM -> 6.dp
                                                AttackLungeStyle.SNACK -> 8.dp
                                                else -> 24.dp
                                            }
                                            val sign = when {
                                                lungeStyle == AttackLungeStyle.RANGED || lungeStyle == AttackLungeStyle.BUFF || lungeStyle == AttackLungeStyle.SNACK -> -1f
                                                else -> 1f
                                            }
                                            Lungeable(
                                                side = CombatSide.ENEMY,
                                                triggerToken = enemyLungeToken,
                                                distance = dist,
                                                axis = if (lungeStyle == AttackLungeStyle.ITEM) LungeAxis.X else LungeAxis.Y,
                                                directionSign = sign,
                                                modifier = Modifier.matchParentSize(),
                                                onFinished = { enemyLungeToken?.let(onLungeFinished) }
                                            ) {
                                                Image(
                                                    painter = painter,
                                                    contentDescription = enemy.name,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.matchParentSize()
                                                )
                                            }
                                        }
                                        val enemyShielded = enemyState?.statusEffects.orEmpty().any { status ->
                                            isShieldVisualStatus(status.id)
                                        }
                                        val enemyBroken = (enemyState?.breakTurns ?: 0) > 0
                                        if (enemyShielded && isAlive) {
                                            ShieldFieldOverlay(modifier = Modifier.matchParentSize())
                                        }
                                        if (enemyBroken && isAlive) {
                                            BrokenFieldOverlay(modifier = Modifier.matchParentSize())
                                        }
                                    }
                                    CombatFxOverlay(
                                        damageFx = damageFx.filter { it.targetId == combatantId },
                                        attackFx = attackFx.filter { it.targetId == combatantId },
                                        healFx = healFx.filter { it.targetId == combatantId },
                                        statusFx = statusFx.filter { it.targetId == combatantId },
                                        shieldBreakFx = shieldBreakFx.filter { it.targetId == combatantId },
                                        showKnockout = knockoutFx.any { it.targetId == combatantId },
                                        shape = shape,
                                        telegraphFx = telegraphHighlights,
                                        modifier = Modifier.matchParentSize()
                                    )
                                    EnemyStatusRail(
                                        statuses = enemyState?.statusEffects.orEmpty(),
                                        buffs = enemyState?.buffs.orEmpty(),
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 4.dp)
                                            .zIndex(6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompositeEnemyRoster(
    enemies: List<Enemy>,
    combatantIds: List<String>,
    combatState: CombatState,
    activeId: String?,
    selectedEnemyIds: Set<String>,
    labelBorderColor: Color,
    damageFx: List<DamageFxUi>,
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    shieldBreakFx: List<ShieldBreakFxUi>,

    knockoutFx: List<KnockoutFxUi>,
    telegraphFx: List<TelegraphFxUi>,
    delayedDeathTargets: Map<String, Long>,
    onEnemyTap: (String) -> Unit,
    onEnemyLongPress: (String) -> Unit,
    atbMeters: Map<String, Float> = emptyMap(),
    lungeActorId: String?,
    lungeToken: Long,
    missLungeActorId: String?,
    missLungeToken: Long,
    onLungeFinished: (Long) -> Unit,
    onMissLungeFinished: (Long) -> Unit,
    showTargetPrompt: Boolean = false,
    lungeStyle: AttackLungeStyle = AttackLungeStyle.MELEE
) {
    if (enemies.isEmpty()) return
    val entries = enemies.mapIndexedNotNull { index, enemy ->
        val layout = enemy.composite ?: return@mapIndexedNotNull null
        val combatantId = combatantIds.getOrElse(index) { enemy.id }
        CompositeEnemyEntry(enemy = enemy, combatantId = combatantId, layout = layout)
    }
    if (entries.size != enemies.size) return
    val compositeGroup = entries.firstOrNull()?.layout?.group
    val underSpriteIds = if (compositeGroup == "driller") {
        setOf("driller_drill_arm", "driller_fire_arm")
    } else {
        emptySet()
    }
    val groupOffsetSource = entries.firstOrNull { entry ->
        entry.layout.role?.equals("core", ignoreCase = true) == true
    } ?: entries.firstOrNull()
    val underSpriteEntries = entries.filter { it.enemy.id in underSpriteIds }
    val topRowEntriesBase = entries.filterNot { it.enemy.id in underSpriteIds }
    val topRowEntries = if (compositeGroup == "driller") {
        topRowEntriesBase.sortedBy { it.layout.offsetX }
    } else {
        topRowEntriesBase
    }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val boxWidth = maxWidth
        val paddingFactor = 0.18f
        val extentFactorX = entries.maxOf { entry ->
            abs(entry.layout.offsetX) + (entry.layout.widthScale / 2f)
        }
        val widthLimit = boxWidth * 0.95f
        val sizeLimit = widthLimit / (extentFactorX * 2f + paddingFactor)
        val baseSize = minOf(minOf(220.dp, sizeLimit) * 1.08f, sizeLimit)
        val baseValue = baseSize.value
        val groupOffsetX = groupOffsetSource?.layout?.groupOffsetX ?: 0f
        val groupOffsetY = groupOffsetSource?.layout?.groupOffsetY ?: 0f
        val groupOffsetXValue = baseValue * groupOffsetX
        val groupOffsetYValue = baseValue * groupOffsetY
        val groupOffsetXDp = baseSize * groupOffsetX
        val groupOffsetYDp = baseSize * groupOffsetY
        var minX = 0f
        var maxX = 0f
        var minY = 0f
        var maxY = 0f
        entries.forEach { entry ->
            val width = baseValue * entry.layout.widthScale
            val height = baseValue * entry.layout.heightScale
            val labelReserve = if (entry.enemy.id in underSpriteIds) {
                baseValue * 0.44f
            } else {
                0f
            }
            val centerX = baseValue * entry.layout.offsetX + groupOffsetXValue
            val centerY = baseValue * entry.layout.offsetY + groupOffsetYValue
            minX = min(minX, centerX - width / 2f)
            maxX = max(maxX, centerX + width / 2f)
            minY = min(minY, centerY - height / 2f)
            maxY = max(maxY, centerY + height / 2f + labelReserve)
        }
        val padding = baseValue * paddingFactor
        val halfWidth = max(abs(minX), abs(maxX)) + padding
        val halfHeight = max(abs(minY), abs(maxY)) + padding
        val compositeWidth = (halfWidth * 2f).dp
        val compositeHeight = (halfHeight * 2f).dp
        val underLabelAnchorY = underSpriteEntries.maxOfOrNull { entry ->
            val partHeight = baseSize * entry.layout.heightScale
            val partOffsetY = baseSize * entry.layout.offsetY + groupOffsetYDp
            partOffsetY + partHeight / 2f
        } ?: 0.dp
        val underLabelOffsetY = underLabelAnchorY + 42.dp
        val topLabelOffset = 76.dp
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (topRowEntries.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .widthIn(max = 1200.dp)
                        .fillMaxWidth()
                        .offset(x = groupOffsetXDp, y = topLabelOffset + groupOffsetYDp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    items(topRowEntries) { entry ->
                        CompositePartStatus(
                            entry = entry,
                            combatState = combatState,
                            activeId = activeId,
                            selectedEnemyIds = selectedEnemyIds,
                            atbMeters = atbMeters,
                            labelBorderColor = labelBorderColor,
                            onEnemyTap = onEnemyTap,
                            onEnemyLongPress = onEnemyLongPress
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.size(compositeWidth, compositeHeight),
                contentAlignment = Alignment.Center
            ) {
                entries.forEach { entry ->
                    val enemy = entry.enemy
                    val combatantId = entry.combatantId
                    val enemyState = combatState.combatants[combatantId]
                    val isActive = activeId == combatantId
                    val isSelected = combatantId in selectedEnemyIds
                    val telegraphHighlights = telegraphFx.filter { combatantId in it.targetIds }
                    val isAlive = enemyState?.isAlive != false
                    val isVisible = isAlive || delayedDeathTargets.containsKey(combatantId)
                    val flash = rememberDamageFlash(combatantId, combatState.log)
                    val hitPulse = rememberHitPulse(combatantId, combatState.log)
                    val hitRecoilY = rememberHitRecoil(
                        targetId = combatantId,
                        log = combatState.log,
                        directionSign = -1f
                    )
                    val damageShake = rememberDamageShake(combatantId, combatState.log)
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
                        painterResource(R.drawable.main_menu_background)
                    )
                    val shape = RoundedCornerShape(24.dp)
                    val enemyLungeToken = if (combatantId == lungeActorId) lungeToken else null
                    val enemyMissToken = if (combatantId == missLungeActorId) missLungeToken else null
                    val partWidth = baseSize * entry.layout.widthScale
                    val partHeight = baseSize * entry.layout.heightScale
                    val partOffsetX = baseSize * entry.layout.offsetX
                    val partOffsetY = baseSize * entry.layout.offsetY + groupOffsetYDp
                    val enemyInteractionSource = remember(combatantId) { MutableInteractionSource() }
                    val partModifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = partOffsetX + groupOffsetXDp, y = partOffsetY)
                        .size(width = partWidth, height = partHeight)
                        .graphicsLayer {
                            translationX = damageShake * with(density) { 6.dp.toPx() }
                        }
                        .zIndex(
                            when {
                                isActive -> entry.layout.z + 0.4f
                                isSelected -> entry.layout.z + 0.2f
                                else -> entry.layout.z
                            }
                        )
                        .combinedClickable(
                            interactionSource = enemyInteractionSource,
                            indication = null,
                            onClick = { if (isAlive) onEnemyTap(combatantId) },
                            onLongClick = { if (isAlive) onEnemyLongPress(combatantId) }
                        )
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(220)) +
                            scaleIn(initialScale = 0.92f, animationSpec = tween(260)),
                        exit = fadeOut(animationSpec = tween(360)) +
                            scaleOut(targetScale = 0.65f, animationSpec = tween(420)),
                        modifier = partModifier,
                        label = "enemy_part_$combatantId"
                    ) {
                        val idleTransition = rememberInfiniteTransition(label = "enemy_idle_part_$combatantId")
                        val idlePhase = remember(combatantId) { Random.nextFloat() * (2f * Math.PI).toFloat() }
                        val idleWave by idleTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = (2f * Math.PI).toFloat(),
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 2600, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "enemy_idle_wave"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    val glowScale = 1f + selectionGlow.value * 0.08f
                                    val hitScale = 1f + 0.04f * hitPulse
                                    val combined = glowScale * hitScale
                                    val isBroken = (enemyState?.breakTurns ?: 0) > 0
                                    if (isBroken) {
                                        scaleX = combined
                                        scaleY = combined
                                        translationY = hitRecoilY
                                    } else {
                                        val idleBreath = sin(idleWave + idlePhase)
                                        val idleBob = idleBreath * 2.0f
                                        val idlePuff = 1f + 0.01f * idleBreath
                                        scaleX = combined * (1f + 0.006f * idleBreath)
                                        scaleY = combined * idlePuff
                                        translationY = hitRecoilY + idleBob
                                    }
                                }
                        ) {
                            if (entry.layout.role?.equals("core", ignoreCase = true) != false) {
                                EnemyShadow(
                                    modifier = Modifier.matchParentSize(),
                                    widthFraction = 0.7f,
                                    heightFraction = 0.2f
                                )
                            }
                            if (flash > 0f) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(shape)
                                        .background(Color.Black.copy(alpha = 0.18f * flash))
                                )
                            }
                            SelectionRipple(
                                isSelected = isSelected,
                                modifier = Modifier.matchParentSize()
                            )
                            Lungeable(
                                side = CombatSide.ENEMY,
                                triggerToken = enemyMissToken,
                                distance = 18.dp,
                                axis = LungeAxis.X,
                                directionSign = 1f,
                                modifier = Modifier.matchParentSize(),
                                onFinished = { enemyMissToken?.let(onMissLungeFinished) }
                            ) {
                                val dist = when (lungeStyle) {
                                    AttackLungeStyle.RANGED -> 12.dp
                                    AttackLungeStyle.BUFF -> 16.dp
                                    AttackLungeStyle.CAST -> 8.dp
                                    AttackLungeStyle.ITEM -> 6.dp
                                    AttackLungeStyle.SNACK -> 8.dp
                                    else -> 24.dp
                                }
                                val sign = when {
                                    lungeStyle == AttackLungeStyle.RANGED || lungeStyle == AttackLungeStyle.BUFF || lungeStyle == AttackLungeStyle.SNACK -> -1f
                                    else -> 1f
                                }
                                Lungeable(
                                    side = CombatSide.ENEMY,
                                    triggerToken = enemyLungeToken,
                                    distance = dist,
                                    axis = if (lungeStyle == AttackLungeStyle.ITEM) LungeAxis.X else LungeAxis.Y,
                                    directionSign = sign,
                                    modifier = Modifier.matchParentSize(),
                                    onFinished = { enemyLungeToken?.let(onLungeFinished) }
                                ) {
                                    Image(
                                        painter = painter,
                                        contentDescription = enemy.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                            }
                            val enemyShielded = enemyState?.statusEffects.orEmpty().any { status ->
                                isShieldVisualStatus(status.id)
                            }
                            val enemyBroken = (enemyState?.breakTurns ?: 0) > 0
                            if (enemyShielded && isAlive) {
                                ShieldFieldOverlay(modifier = Modifier.matchParentSize())
                            }
                            if (enemyBroken && isAlive) {
                                BrokenFieldOverlay(modifier = Modifier.matchParentSize())
                            }
                            CombatFxOverlay(
                                damageFx = damageFx.filter { it.targetId == combatantId },
                                attackFx = attackFx.filter { it.targetId == combatantId },
                                healFx = healFx.filter { it.targetId == combatantId },
                                statusFx = statusFx.filter { it.targetId == combatantId },
                                shieldBreakFx = shieldBreakFx.filter { it.targetId == combatantId },

                                showKnockout = knockoutFx.any { it.targetId == combatantId },
                                shape = shape,
                                telegraphFx = telegraphHighlights,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                    }
                }
                underSpriteEntries.forEach { entry ->
                        val enemy = entry.enemy
                        val combatantId = entry.combatantId
                        val enemyState = combatState.combatants[combatantId]
                        val isAlive = enemyState?.isAlive != false
                        val isVisible = isAlive || delayedDeathTargets.containsKey(combatantId)
                        val partOffsetX = baseSize * entry.layout.offsetX + groupOffsetXDp
                        val labelOffsetY = underLabelOffsetY
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(animationSpec = tween(180)) +
                                scaleIn(initialScale = 0.95f, animationSpec = tween(220)),
                            exit = fadeOut(animationSpec = tween(240)) +
                                scaleOut(targetScale = 0.9f, animationSpec = tween(260)),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = partOffsetX, y = labelOffsetY)
                                .zIndex(entry.layout.z + 1.2f),
                            label = "enemy_part_label_$combatantId"
                        ) {
                            CompositePartStatus(
                                entry = entry,
                                combatState = combatState,
                                activeId = activeId,
                                selectedEnemyIds = selectedEnemyIds,
                                atbMeters = atbMeters,
                                labelBorderColor = labelBorderColor,
                                onEnemyTap = onEnemyTap,
                                onEnemyLongPress = onEnemyLongPress
                            )
                        }
                    }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompositePartStatus(
    entry: CompositeEnemyEntry,
    combatState: CombatState,
    activeId: String?,
    selectedEnemyIds: Set<String>,
    atbMeters: Map<String, Float>,
    labelBorderColor: Color,
    onEnemyTap: (String) -> Unit,
    onEnemyLongPress: (String) -> Unit
) {
    val combatantId = entry.combatantId
    val enemyState = combatState.combatants[combatantId]
    val maxHp = enemyState?.combatant?.stats?.maxHp ?: entry.enemy.hp.coerceAtLeast(1)
    val currentHp = enemyState?.hp ?: maxHp
    val maxStability = enemyState?.combatant?.stats?.stability ?: 100
    val currentStability = enemyState?.stability ?: maxStability
    val isAlive = enemyState?.isAlive != false
    val isActive = combatantId == activeId
    val isSelected = combatantId in selectedEnemyIds
    val atbProgress = atbMeters[combatantId] ?: 0f
    Box(contentAlignment = Alignment.Center) {
        EnemyStatusLabel(
            name = entry.enemy.name,
            currentHp = currentHp,
            maxHp = maxHp,
            currentStability = currentStability,
            maxStability = maxStability,
            breakTurns = enemyState?.breakTurns ?: 0,
            atbProgress = atbProgress,
            isAlive = isAlive,
            isActive = isActive,
            isSelected = isSelected,
            accentColor = labelBorderColor,
            barWidth = 90.dp,
            onClick = { if (isAlive) onEnemyTap(combatantId) },
            onLongClick = { if (isAlive) onEnemyLongPress(combatantId) }
        )
        EnemyStatusRail(
            statuses = enemyState?.statusEffects.orEmpty(),
            buffs = enemyState?.buffs.orEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 18.dp)
                .zIndex(2f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnemyStatusLabel(
    name: String,
    currentHp: Int,
    maxHp: Int,
    currentStability: Int,
    maxStability: Int,
    breakTurns: Int,
    atbProgress: Float,
    isAlive: Boolean,
    isActive: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    barWidth: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val baseAccent = accentColor.copy(alpha = 1f)
    val borderColor = when {
        isActive -> baseAccent
        isSelected -> baseAccent.copy(alpha = 0.7f)
        else -> baseAccent.copy(alpha = 0.4f)
    }
    val isBroken = breakTurns > 0
    val displayedStability = remember { Animatable(currentStability.toFloat()) }
    LaunchedEffect(currentStability, maxStability, breakTurns) {
        val target = if (isBroken) 0f else currentStability.toFloat()
        if (isBroken) {
            displayedStability.snapTo(0f)
        } else {
            val start = displayedStability.value
            val clampedTarget = target.coerceIn(0f, maxStability.toFloat())
            if (start != clampedTarget) {
                displayedStability.animateTo(
                    clampedTarget,
                    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                )
            }
        }
    }
    val interactionModifier = if (onClick != null || onLongClick != null) {
        modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() }
        )
    } else {
        modifier
    }
    Surface(
        color = Color(0xFF0C0F14).copy(alpha = if (isSelected) 0.72f else 0.5f),
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = interactionModifier
            .height(66.dp)
            .alpha(if (isAlive) 1f else 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleCaseName(name),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AtbBar(
                progress = atbProgress,
                modifier = Modifier.width(barWidth),
                color = if (isActive) Color(0xFFFFD36E) else Color(0xFFFF9F43)
            )
            StatBar(
                current = currentHp,
                max = maxHp,
                color = Color(0xFFFF5252),
                background = Color.Black.copy(alpha = 0.5f),
                height = 6.dp,
                modifier = Modifier.width(barWidth)
            )
            Row(
                modifier = Modifier.width(barWidth),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (isBroken) Color(0xFFFF8A80) else Color(0xFFB39DDB).copy(alpha = 0.9f),
                    modifier = Modifier.size(12.dp)
                )
                StatBar(
                    current = displayedStability.value.roundToInt(),
                    max = maxStability,
                    color = if (isBroken) Color(0xFFE57373) else Color(0xFF7E57C2),
                    background = Color.Black.copy(alpha = 0.45f),
                    height = 5.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun titleCaseName(name: String): String {
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

private fun isEliteTier(tier: String): Boolean {
    val normalized = tier.trim()
        .lowercase(Locale.getDefault())
        .replace("_", "")
        .replace("-", "")
        .replace(" ", "")
    return normalized == "elite" || normalized == "miniboss"
}

private fun isBossTier(tier: String): Boolean {
    val normalized = tier.trim()
        .lowercase(Locale.getDefault())
        .replace("_", "")
        .replace("-", "")
        .replace(" ", "")
    return normalized == "boss"
}

@Composable
private fun rememberDamageFlash(
    targetId: String,
    log: List<com.example.starborn.domain.combat.CombatLogEntry>
): Float {
    val lastDamage = log.lastOrNull { entry ->
        entry is com.example.starborn.domain.combat.CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
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
private fun rememberDamageShake(
    targetId: String,
    log: List<com.example.starborn.domain.combat.CombatLogEntry>
): Float {
    val damageIndex = log.indexOfLast {
        it is com.example.starborn.domain.combat.CombatLogEntry.Damage &&
            it.targetId == targetId &&
            !(it.amount == 0 && it.element == "miss")
    }
    val lastIndex = remember { mutableStateOf(-1) }
    val anim = remember { Animatable(0f) }
    val direction = remember { mutableStateOf(1f) }
    LaunchedEffect(damageIndex) {
        if (damageIndex >= 0 && damageIndex > lastIndex.value) {
            lastIndex.value = damageIndex
            direction.value = if (Random.nextBoolean()) 1f else -1f
            anim.snapTo(1f)
            anim.animateTo(0f, animationSpec = tween(durationMillis = 320, easing = EaseOutBack))
        }
    }
    return anim.value * direction.value
}

@Composable
private fun rememberHitRecoil(
    targetId: String,
    log: List<com.example.starborn.domain.combat.CombatLogEntry>,
    directionSign: Float
): Float {
    val lastHit = log.lastOrNull { entry ->
        entry is com.example.starborn.domain.combat.CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    } as? com.example.starborn.domain.combat.CombatLogEntry.Damage
    val hitIndex = log.indexOfLast { entry ->
        entry is com.example.starborn.domain.combat.CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    }
    val lastIndex = remember { mutableStateOf(-1) }
    val anim = remember { Animatable(0f) }
    val density = LocalDensity.current
    val critical = lastHit?.critical == true
    val distance = if (critical) 12.dp else 8.dp
    val holdMs = if (critical) 90L else 60L
    LaunchedEffect(hitIndex) {
        if (hitIndex >= 0 && hitIndex > lastIndex.value) {
            lastIndex.value = hitIndex
            anim.snapTo(1f)
            delay(holdMs)
            anim.animateTo(0f, animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
        }
    }
    val distancePx = with(density) { distance.toPx() }
    return anim.value * distancePx * directionSign
}

@Composable
private fun rememberHitPulse(
    targetId: String,
    log: List<com.example.starborn.domain.combat.CombatLogEntry>
): Float {
    val lastHit = log.lastOrNull { entry ->
        entry is com.example.starborn.domain.combat.CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    } as? com.example.starborn.domain.combat.CombatLogEntry.Damage
    val hitIndex = log.indexOfLast { entry ->
        entry is com.example.starborn.domain.combat.CombatLogEntry.Damage &&
            entry.targetId == targetId &&
            !(entry.amount == 0 && entry.element == "miss")
    }
    val lastIndex = remember { mutableStateOf(-1) }
    val anim = remember { Animatable(0f) }
    val critical = lastHit?.critical == true
    val holdMs = if (critical) 90L else 60L
    val returnMs = if (critical) 240 else 200
    LaunchedEffect(hitIndex) {
        if (hitIndex >= 0 && hitIndex > lastIndex.value) {
            lastIndex.value = hitIndex
            anim.snapTo(1f)
            delay(holdMs)
            anim.animateTo(0f, animationSpec = tween(durationMillis = returnMs, easing = FastOutSlowInEasing))
        }
    }
    return anim.value
}

@Composable
private fun EnemyStatusRail(
    statuses: List<StatusEffect>,
    buffs: List<ActiveBuff>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3
) {
    val entries = statusChipsFor(statuses, buffs)
    if (entries.isEmpty()) return
    val visible = entries.take(maxVisible.coerceAtLeast(1))
    val overflow = entries.size - visible.size
    Surface(
        color = Color(0xFF070A10).copy(alpha = 0.74f),
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            visible.forEach { chip ->
                EnemyStatusPip(chip)
            }
            if (overflow > 0) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.height(22.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$overflow",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnemyStatusPip(chip: StatusChip) {
    Box(modifier = Modifier.size(24.dp)) {
        Surface(
            color = chip.tint.copy(alpha = 0.88f),
            contentColor = Color.White,
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
            modifier = Modifier.matchParentSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = chip.icon,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Surface(
            color = Color(0xFF05070B).copy(alpha = 0.96f),
            contentColor = Color.White,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.28f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 3.dp, y = 2.dp)
                .height(12.dp)
                .widthIn(min = 12.dp)
        ) {
            Text(
                text = statusChipLabel(chip),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun StatusBadges(statuses: List<StatusEffect>, buffs: List<ActiveBuff>) {
    val entries = statusChipsFor(statuses, buffs)
    if (entries.isEmpty()) return
    LazyRow(
        modifier = Modifier.widthIn(max = 1200.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(entries) { chip ->
            Surface(
                color = chip.tint.copy(alpha = 0.85f),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.height(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = chip.icon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = statusChipLabel(chip),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

private fun statusChipsFor(statuses: List<StatusEffect>, buffs: List<ActiveBuff>): List<StatusChip> =
    buildList {
        statuses.forEach { statusEffect ->
            add(
                StatusChip(
                    icon = iconForStatus(statusEffect.id),
                    tint = colorForStatus(statusEffect.id),
                    turns = statusEffect.remainingTurns
                )
            )
        }
        buffs.forEach { buff ->
            val isPositive = buff.effect.value >= 0
            val stat = buff.effect.stat.lowercase(Locale.getDefault())
            add(
                StatusChip(
                    icon = iconForStat(stat),
                    tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    turns = buff.remainingTurns,
                    value = buff.effect.value
                )
            )
        }
    }

private fun statusChipLabel(chip: StatusChip): String {
    val value = chip.value
    return if (value != null) {
        val magnitude = abs(value)
        val sign = when {
            value > 0 -> "+"
            value < 0 -> "-"
            else -> ""
        }
        if (magnitude > 9) "${sign}9+" else "$sign$magnitude"
    } else {
        val turns = chip.turns.coerceAtLeast(0)
        if (turns > 9) "9+" else turns.toString()
    }
}

private fun iconForStatus(statusId: String): ImageVector {
    return when (statusId.lowercase(Locale.getDefault())) {
        "burn", "meltdown" -> Icons.Rounded.Whatshot
        "shock", "static" -> Icons.Rounded.Bolt
        "freeze", "frozen" -> Icons.Rounded.AcUnit
        "wet", "water" -> Icons.Rounded.WaterDrop
        "acid", "poison", "erosion", "bleed" -> Icons.Rounded.WaterDrop // Reusing WaterDrop as generic liquid/blood
        "stun", "stagger" -> Icons.Rounded.Star
        "blind" -> Icons.Rounded.VisibilityOff
        "shield", "guard", "invulnerable" -> Icons.Rounded.Shield
        "weak", "brittle", "exposed", "discord" -> Icons.Rounded.BrokenImage
        "regen", "heal" -> Icons.Rounded.AutoAwesome
        "jammed", "silence" -> Icons.Rounded.micsOff() // Need to check if MicOff exists or use generic
        "overdrive", "charged" -> Icons.Rounded.Bolt
        else -> Icons.Rounded.Warning
    }
}

private fun Icons.Rounded.micsOff(): ImageVector = Icons.Rounded.VisibilityOff // Fallback

private fun iconForStat(stat: String): ImageVector {
    return when (stat) {
        "strength", "str", "atk" -> Icons.Rounded.Restaurant // Sword icon missing, using generic
        "vitality", "vit", "def" -> Icons.Rounded.Shield
        "agility", "agi", "spd", "speed" -> Icons.Rounded.AutoAwesome // Wing icon missing
        "focus", "int", "psi" -> Icons.Rounded.Bolt
        else -> Icons.Rounded.AutoAwesome
    }
}

private fun colorForStatus(statusId: String): Color {
    return when (statusId.lowercase(Locale.getDefault())) {
        "burn", "meltdown" -> Color(0xFFFF5722)
        "shock", "static", "overdrive" -> Color(0xFFFFC107)
        "freeze", "frozen" -> Color(0xFF03A9F4)
        "acid", "poison", "erosion" -> Color(0xFF8BC34A)
        "bleed" -> Color(0xFFE91E63)
        "stun", "stagger" -> Color(0xFF9C27B0)
        "blind" -> Color(0xFF607D8B)
        "shield", "guard", "invulnerable", "regen" -> Color(0xFF4CAF50)
        "weak", "brittle", "exposed" -> Color(0xFF795548)
        else -> Color(0xFF9E9E9E)
    }
}

@Composable
private fun ShieldFieldOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF42A5F5)
) {
    val transition = rememberInfiniteTransition(label = "shield_field")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shield_sweep"
    )
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas

        // Mask this overlay to whatever has already been drawn beneath it (enemy sprite/portrait),
        // so it reads as a "field over the target" instead of a new HUD bar.
        val maskBlend = BlendMode.SrcAtop

        val intensity = 0.65f + 0.35f * pulse
        val center = Offset(size.width * 0.5f, size.height * 0.35f)
        val radius = size.maxDimension * 0.9f
        val base = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.20f * intensity),
                color.copy(alpha = 0.10f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
        drawRect(brush = base, blendMode = maskBlend)

        val bandWidth = size.minDimension * 0.55f
        val x = (size.width + bandWidth) * sweep - bandWidth
        val shimmer = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.08f + 0.10f * pulse),
                Color.Transparent
            ),
            start = Offset(x - bandWidth, 0f),
            end = Offset(x + bandWidth, size.height)
        )
        drawRect(brush = shimmer, blendMode = maskBlend)
    }
}

@Composable
private fun BrokenFieldOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFB388FF)
) {
    val transition = rememberInfiniteTransition(label = "broken_field")
    val pulse by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "broken_pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "broken_sweep"
    )
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val maskBlend = BlendMode.SrcAtop
        val intensity = 0.6f + 0.4f * pulse
        val center = Offset(size.width * 0.5f, size.height * 0.6f)
        val radius = size.maxDimension * 0.95f
        val base = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.16f * intensity),
                color.copy(alpha = 0.08f * intensity),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
        drawRect(brush = base, blendMode = maskBlend)

        val bandWidth = size.minDimension * 0.45f
        val looped = (kotlin.math.sin(sweep) + 1f) * 0.5f
        val x = (size.width + bandWidth) * looped - bandWidth
        val fissure = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = 0.22f + 0.18f * pulse),
                Color.Transparent
            ),
            start = Offset(x - bandWidth, 0f),
            end = Offset(x + bandWidth, size.height)
        )
        drawRect(brush = fissure, blendMode = maskBlend)

        val crackStroke = size.minDimension * 0.024f
        val crackColor = Color(0xFFF3E5F5).copy(alpha = 0.48f + 0.28f * pulse)
        val crackHighlight = Color.White.copy(alpha = 0.28f + 0.2f * pulse)

        val shardCenter = Offset(size.width * 0.46f, size.height * 0.44f)
        val mainStart = Offset(size.width * 0.12f, size.height * 0.08f)
        val mainEnd = Offset(size.width * 0.9f, size.height * 0.86f)
        drawLine(color = crackColor, start = mainStart, end = shardCenter, strokeWidth = crackStroke, blendMode = maskBlend)
        drawLine(color = crackColor, start = shardCenter, end = mainEnd, strokeWidth = crackStroke, blendMode = maskBlend)
        drawLine(color = crackHighlight, start = mainStart, end = shardCenter, strokeWidth = crackStroke * 0.55f, blendMode = maskBlend)
        drawLine(color = crackHighlight, start = shardCenter, end = mainEnd, strokeWidth = crackStroke * 0.55f, blendMode = maskBlend)

        val branchStroke = crackStroke * 0.7f
        val branches = listOf(
            shardCenter to shardCenter + Offset(size.width * 0.26f, size.height * -0.14f),
            shardCenter to shardCenter + Offset(size.width * -0.18f, size.height * 0.26f),
            shardCenter to shardCenter + Offset(size.width * 0.12f, size.height * 0.3f)
        )
        branches.forEach { (start, end) ->
            drawLine(color = crackColor, start = start, end = end, strokeWidth = branchStroke, blendMode = maskBlend)
            drawLine(color = crackHighlight, start = start, end = end, strokeWidth = branchStroke * 0.55f, blendMode = maskBlend)
        }

        val burstRadius = size.minDimension * 0.08f
        val burstCount = 7
        repeat(burstCount) { idx ->
            val angle = (idx / burstCount.toFloat()) * 360f + 12f
            val length = burstRadius * (0.7f + 0.5f * (idx % 2))
            val rad = Math.toRadians(angle.toDouble())
            val end = Offset(
                shardCenter.x + (kotlin.math.cos(rad) * length).toFloat(),
                shardCenter.y + (kotlin.math.sin(rad) * length).toFloat()
            )
            drawLine(color = crackHighlight, start = shardCenter, end = end, strokeWidth = crackStroke * 0.45f, blendMode = maskBlend)
        }

        val glintRadius = size.minDimension * 0.18f
        val glintBrush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.12f + 0.08f * pulse),
                Color.Transparent,
                Color.Transparent
            ),
            center = shardCenter + Offset(glintRadius * 0.2f, -glintRadius * 0.2f),
            radius = glintRadius
        )
        drawCircle(
            brush = glintBrush,
            radius = glintRadius,
            center = shardCenter + Offset(glintRadius * 0.2f, -glintRadius * 0.2f),
            blendMode = maskBlend
        )
    }
}

private fun isShieldVisualStatus(statusId: String): Boolean {
    val normalized = statusId.trim().lowercase(Locale.getDefault())
    return normalized == "invulnerable" ||
        normalized == "shield" ||
        normalized == "guard" ||
        normalized == "defend"
}



@Composable
private fun EnemyShadow(
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.62f,
    heightFraction: Float = 0.18f,
    liftFraction: Float = 0.06f,
    alpha: Float = 0.35f
) {
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val shadowWidth = size.width * widthFraction
        val shadowHeight = size.height * heightFraction
        val topLeft = Offset(
            (size.width - shadowWidth) / 2f,
            size.height - shadowHeight - size.height * liftFraction
        )
        drawOval(
            color = Color.Black.copy(alpha = alpha * 0.45f),
            topLeft = topLeft,
            size = Size(shadowWidth, shadowHeight)
        )
        drawOval(
            color = Color.Black.copy(alpha = alpha),
            topLeft = topLeft + Offset(shadowWidth * 0.1f, shadowHeight * 0.12f),
            size = Size(shadowWidth * 0.8f, shadowHeight * 0.7f)
        )
    }
}

@Composable
private fun SelectionRipple(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    color: Color = TargetRippleColor
) {
    val rippleAnim = remember { Animatable(0f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(
                1f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
            )
        } else {
            rippleAnim.snapTo(0f)
        }
    }
    val progress = rippleAnim.value
    if (progress <= 0f) return
    Canvas(modifier = modifier) {
        val minDimension = size.minDimension
        if (minDimension <= 0f) return@Canvas
        val t = progress.coerceIn(0f, 1f)
        val center = Offset(size.width / 2f, size.height * 0.82f)
        val radius = minDimension * (0.16f + 0.34f * t)
        val stroke = minDimension * (0.024f - 0.012f * t)
        val alpha = (1f - t).coerceIn(0f, 1f)
        drawCircle(
            color = color.copy(alpha = 0.65f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = color.copy(alpha = 0.35f * alpha),
            radius = radius * 0.65f,
            center = center,
            style = Stroke(width = stroke * 0.6f)
        )
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
    val icon: ImageVector,
    val tint: Color,
    val turns: Int,
    val value: Int? = null
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
        effect.singleBuff != null || !effect.buffs.isNullOrEmpty() -> TargetFilter.ALLY
        else -> null
    }
}



@Composable
private fun CombatFxOverlay(
    damageFx: List<DamageFxUi>,
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    shieldBreakFx: List<ShieldBreakFxUi> = emptyList(),

    showKnockout: Boolean,
    shape: Shape,
    supportFx: List<SupportFxUi> = emptyList(),
    telegraphFx: List<TelegraphFxUi> = emptyList(),
    modifier: Modifier = Modifier.fillMaxSize()
) {
    if (damageFx.isEmpty() &&
        attackFx.isEmpty() &&
        healFx.isEmpty() &&
        statusFx.isEmpty() &&
        shieldBreakFx.isEmpty() &&
        !showKnockout &&
        supportFx.isEmpty() &&
        telegraphFx.isEmpty()
    ) {
        return
    }
    Box(
        modifier = modifier
            .clip(shape)
            .zIndex(1f)
    ) {
        val telegraphHighlight = telegraphFx.firstOrNull()
        if (telegraphHighlight != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF8E24AA).copy(alpha = 0.24f))
            )
            Text(
                text = "Incoming: ${telegraphHighlight.skillName}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        } else {
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

        shieldBreakFx.forEach { fx ->
            ShieldBreakBurst(
                fx = fx,
                modifier = Modifier.fillMaxSize().zIndex(9f)
            )
        }

        attackFx.forEach { fx ->
            AttackHitFx(
                fx = fx,
                modifier = Modifier.fillMaxSize().zIndex(10f)
            )
        }
        statusFx.forEach { fx ->
            StatusImpactFlash(
                fx = fx,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(14f)
            )
            StatusPulse(
                fx = fx,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .zIndex(15f)
            )
        }
        damageFx.forEach { fx ->
            DamageNumberBubble(
                fx = fx,
                modifier = Modifier.align(Alignment.Center).zIndex(100f)
            )
        }
        healFx.forEach { fx ->
            HealNumberBubble(
                fx = fx,
                modifier = Modifier.align(Alignment.Center).zIndex(100f)
            )
        }
    }
}

private fun attackFxStyleFor(sourceId: String): AttackFxStyle? = when (sourceId) {
    "nova" -> AttackFxStyle.NOVA_LASER
    "zeke" -> AttackFxStyle.ZEKE_PUNCH
    "orion" -> AttackFxStyle.ORION_JEWEL
    "gh0st", "ghost" -> AttackFxStyle.GHOST_SLASH
    else -> null
}

private fun resolveAttackStyle(
    sourceId: String,
    combatState: CombatState?,
    playerIdSet: Set<String>
): AttackFxStyle? {
    val normalized = normalizeAttackSourceId(sourceId)
    val direct = attackFxStyleFor(normalized) ?: attackFxStyleForName(normalized)
    if (direct != null) return direct
    val combatantMatch = combatState?.combatants?.get(sourceId)
        ?: combatState?.combatants?.get(normalized)
        ?: combatState?.combatants?.values?.firstOrNull { state ->
            val id = state.combatant.id.lowercase(Locale.getDefault())
            val name = state.combatant.name.lowercase(Locale.getDefault())
            normalized == id || normalized.contains(id) || normalized.contains(name)
        }
    val fromCombatant = combatantMatch?.combatant?.name?.let { attackFxStyleForName(it) }
        ?: combatantMatch?.combatant?.id?.lowercase(Locale.getDefault())?.let { attackFxStyleFor(it) }
    if (fromCombatant != null) return fromCombatant
    val matchedPlayer = playerIdSet.firstOrNull { playerId ->
        normalized.startsWith(playerId) || normalized.contains(playerId)
    }
    return matchedPlayer?.let { attackFxStyleFor(it) }
}

private fun attackFxStyleForName(name: String): AttackFxStyle? {
    val normalized = name.lowercase(Locale.getDefault())
    return when {
        "nova" in normalized -> AttackFxStyle.NOVA_LASER
        "zeke" in normalized -> AttackFxStyle.ZEKE_PUNCH
        "orion" in normalized -> AttackFxStyle.ORION_JEWEL
        "gh0st" in normalized || "ghost" in normalized -> AttackFxStyle.GHOST_SLASH
        else -> null
    }
}

private fun attackFxDurationFor(style: AttackFxStyle): Long = when (style) {
    AttackFxStyle.NOVA_LASER -> ATTACK_FX_DURATION_MS
    AttackFxStyle.ZEKE_PUNCH -> ATTACK_FX_DURATION_MS
    AttackFxStyle.ORION_JEWEL -> ATTACK_FX_DURATION_MS
    AttackFxStyle.GHOST_SLASH -> ATTACK_FX_DURATION_MS
}

private fun normalizeAttackSourceId(sourceId: String): String {
    val base = sourceId.substringBefore('#')
    val segment = base.substringAfterLast(':').substringAfterLast('/')
    return segment.lowercase(Locale.getDefault()).removePrefix("player_")
}

@Composable
private fun AttackHitFx(
    fx: AttackHitFxUi,
    modifier: Modifier = Modifier
) {
    val durationMillis = attackFxDurationFor(fx.style)
    val progress = remember { Animatable(0f) }
    val zekeWords = remember { listOf("BAM!", "WHAM!", "CRACK!", "POW!", "SMASH!") }
    val zekeTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.08f
        }
    }
    val seed = remember(fx.id) { kotlin.math.abs(fx.id.hashCode()) }
    val zekeWord = remember(fx.id) { zekeWords[seed % zekeWords.size] }
    LaunchedEffect(fx.id) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis.toInt(), easing = LinearEasing)
        )
    }
    val t = progress.value
    val fadeStart = 0.7f
    val fadeProgress = ((t - fadeStart) / (1f - fadeStart)).coerceIn(0f, 1f)
    val alpha = 1f - fadeProgress
    val scale = 1f + 0.12f * (1f - t)

    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val sizeMin = size.minDimension
        val color = when (fx.style) {
            AttackFxStyle.NOVA_LASER -> Color(0xFF7BE4FF)
            AttackFxStyle.ZEKE_PUNCH -> Color(0xFFFFB74D)
            AttackFxStyle.ORION_JEWEL -> Color(0xFFB388FF)
            AttackFxStyle.GHOST_SLASH -> Color(0xFF80D8FF)
        }
        withTransform({
            scale(scale, scale, center)
        }) {
            drawCircle(
                color = color.copy(alpha = 0.16f + 0.08f * (1f - t)),
                radius = sizeMin * 0.48f,
                center = center
            )
            when (fx.style) {
                AttackFxStyle.NOVA_LASER -> {
                    val beamRamp = (t / 0.18f).coerceIn(0f, 1f)
                    val beamFade = if (t < 0.65f) 1f else (1f - (t - 0.65f) / 0.35f).coerceIn(0f, 1f)
                    val beamAlpha = beamRamp * beamFade
                    val beamWidth = sizeMin * (0.1f - 0.02f * t)
                    val beamCore = beamWidth * 0.35f
                    val beamHeight = size.height * (0.5f + 0.25f * (1f - t))
                    val beamTop = center.y - beamHeight
                    val beamBottom = center.y + sizeMin * (0.08f + 0.04f * (1f - t))
                    val beamOuter = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            color.copy(alpha = 0.25f * beamAlpha),
                            color.copy(alpha = 0.75f * beamAlpha),
                            Color.Transparent
                        ),
                        startY = beamTop,
                        endY = beamBottom
                    )
                    drawRoundRect(
                        brush = beamOuter,
                        topLeft = Offset(center.x - beamWidth / 2f, beamTop),
                        size = Size(beamWidth, beamBottom - beamTop),
                        cornerRadius = CornerRadius(beamWidth, beamWidth)
                    )
                    val beamCoreBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.95f * beamAlpha),
                            color.copy(alpha = 0.45f * beamAlpha),
                            Color.Transparent
                        ),
                        startY = beamTop,
                        endY = beamBottom
                    )
                    drawRoundRect(
                        brush = beamCoreBrush,
                        topLeft = Offset(center.x - beamCore / 2f, beamTop),
                        size = Size(beamCore, beamBottom - beamTop),
                        cornerRadius = CornerRadius(beamCore, beamCore)
                    )

                    val flareRadius = sizeMin * (0.16f + 0.26f * t)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.9f * (1f - t)),
                                color.copy(alpha = 0.65f * (1f - t)),
                                Color.Transparent
                            ),
                            center = center,
                            radius = flareRadius
                        ),
                        radius = flareRadius,
                        center = center
                    )

                    val shock = ((t - 0.08f) / 0.92f).coerceIn(0f, 1f)
                    val ringRadius = sizeMin * (0.12f + 0.56f * shock)
                    val ringWidth = sizeMin * (0.035f - 0.015f * shock)
                    drawCircle(
                        color = color.copy(alpha = 0.6f * (1f - shock)),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = ringWidth)
                    )

                    val crossAlpha = (1f - t).coerceIn(0f, 1f)
                    val crossLength = sizeMin * (0.22f + 0.12f * sin(t * PI.toFloat()))
                    val crossWidth = sizeMin * 0.018f
                    val lineColor = lerp(color, Color.White, 0.4f)
                    drawLine(
                        color = lineColor.copy(alpha = 0.8f * crossAlpha),
                        start = Offset(center.x - crossLength, center.y),
                        end = Offset(center.x + crossLength, center.y),
                        strokeWidth = crossWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = lineColor.copy(alpha = 0.65f * crossAlpha),
                        start = Offset(center.x, center.y - crossLength),
                        end = Offset(center.x, center.y + crossLength),
                        strokeWidth = crossWidth * 0.75f,
                        cap = StrokeCap.Round
                    )

                    val sparkAlpha = (1f - t) * 0.6f
                    repeat(14) { index ->
                        val angle = (index * 26 + seed % 360) * (PI.toFloat() / 180f)
                        val distance = sizeMin * (0.12f + 0.42f * t)
                        val length = sizeMin * (0.05f + 0.04f * (1f - t))
                        val dir = Offset(cos(angle), sin(angle))
                        val start = center + dir * distance
                        val end = center + dir * (distance + length)
                        drawLine(
                            color = Color.White.copy(alpha = sparkAlpha),
                            start = start,
                            end = end,
                            strokeWidth = sizeMin * 0.012f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                AttackFxStyle.ZEKE_PUNCH -> {
                    val slamIn = (t / 0.22f).coerceIn(0f, 1f)
                    val slamOut = if (t < 0.6f) 1f else (1f - (t - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    val slamAlpha = slamIn * slamOut

                    val impactGlow = (1f - t).coerceIn(0f, 1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f * impactGlow),
                                color.copy(alpha = 0.6f * impactGlow),
                                Color.Transparent
                            ),
                            center = center,
                            radius = sizeMin * 0.22f
                        ),
                        radius = sizeMin * 0.22f,
                        center = center
                    )

                    val shock = ((t - 0.05f) / 0.95f).coerceIn(0f, 1f)
                    val ringRadius = sizeMin * (0.18f + 0.56f * shock)
                    val ringWidth = sizeMin * (0.05f - 0.02f * shock)
                    drawCircle(
                        color = color.copy(alpha = 0.55f * (1f - shock)),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = ringWidth)
                    )

                    val shardAlpha = (1f - shock) * 0.75f
                    repeat(12) { index ->
                        val angle = (index * 27 + seed % 360) * (PI.toFloat() / 180f)
                        val dir = Offset(cos(angle), sin(angle))
                        val start = center + dir * (sizeMin * (0.12f + 0.32f * shock))
                        val end = start + dir * (sizeMin * (0.08f + 0.06f * (1f - shock)))
                        drawLine(
                            color = Color.White.copy(alpha = shardAlpha),
                            start = start,
                            end = end,
                            strokeWidth = sizeMin * 0.02f,
                            cap = StrokeCap.Round
                        )
                    }

                    repeat(6) { index ->
                        val angleDeg = (index * 60 + seed % 45).toFloat()
                        val angle = angleDeg * (PI.toFloat() / 180f)
                        val dir = Offset(cos(angle), sin(angle))
                        val tileSize = sizeMin * (0.045f + 0.015f * (1f - t))
                        val dist = sizeMin * (0.16f + 0.36f * shock)
                        val pos = center + dir * dist
                        withTransform({
                            translate(pos.x - tileSize / 2f, pos.y - tileSize / 2f)
                            rotate(angleDeg + 45f, pivot = Offset(tileSize / 2f, tileSize / 2f))
                        }) {
                            drawRoundRect(
                                color = color.copy(alpha = 0.35f * (1f - shock)),
                                topLeft = Offset.Zero,
                                size = Size(tileSize, tileSize),
                                cornerRadius = CornerRadius(tileSize * 0.2f, tileSize * 0.2f)
                            )
                        }
                    }

                    val bubbleScale = 0.85f + 0.22f * sin(t * PI.toFloat())
                    val bubbleRotation = -6f + 12f * sin(t * PI.toFloat() * 1.2f)
                    val bubbleOuter = sizeMin * 0.26f
                    val bubbleInner = bubbleOuter * 0.68f
                    val spikes = 14
                    val bubblePath = Path().apply {
                        for (i in 0 until spikes) {
                            val angle = (i * (2f * PI.toFloat() / spikes)) + t * 0.6f
                            val radius = if (i % 2 == 0) bubbleOuter else bubbleInner
                            val x = center.x + cos(angle) * radius
                            val y = center.y + sin(angle) * radius
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    val bubbleFill = Color(0xFFFFF3E0).copy(alpha = 0.95f * slamAlpha)
                    val bubbleStroke = color.copy(alpha = 0.9f * slamAlpha)
                    withTransform({
                        scale(bubbleScale, bubbleScale, center)
                        rotate(bubbleRotation, center)
                    }) {
                        drawPath(bubblePath, bubbleFill)
                        drawPath(
                            bubblePath,
                            bubbleStroke,
                            style = Stroke(width = sizeMin * 0.02f)
                        )
                    }

                    val baseTextSize = sizeMin * 0.16f * bubbleScale
                    val textColor = Color(0xFF1B1B1B).copy(alpha = slamAlpha)
                    zekeTextPaint.textSize = baseTextSize
                    zekeTextPaint.color = textColor.toArgb()
                    zekeTextPaint.setShadowLayer(
                        sizeMin * 0.02f,
                        0f,
                        sizeMin * 0.01f,
                        color.copy(alpha = 0.5f * slamAlpha).toArgb()
                    )
                    val maxTextWidth = bubbleInner * 2f
                    val measuredWidth = zekeTextPaint.measureText(zekeWord)
                    if (measuredWidth > maxTextWidth) {
                        zekeTextPaint.textSize = baseTextSize * (maxTextWidth / measuredWidth)
                    }
                    val fm = zekeTextPaint.fontMetrics
                    val textY = center.y - (fm.ascent + fm.descent) / 2f
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        nativeCanvas.save()
                        nativeCanvas.rotate(bubbleRotation, center.x, center.y)
                        nativeCanvas.drawText(zekeWord, center.x, textY, zekeTextPaint)
                        nativeCanvas.restore()
                    }
                }
                AttackFxStyle.ORION_JEWEL -> {
                    val pulse = 0.5f + 0.5f * sin(t * PI.toFloat() * 2f)
                    val auraAlpha = (1f - t) * 0.55f
                    val coreColor = lerp(color, Color.White, 0.4f)
                    val accentColor = lerp(color, Color(0xFF7C4DFF), 0.5f)

                    val auraRadius = sizeMin * (0.28f + 0.18f * pulse)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = auraAlpha),
                                color.copy(alpha = 0.18f * auraAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = auraRadius
                        ),
                        radius = auraRadius,
                        center = center
                    )

                    val ringRadius = sizeMin * (0.18f + 0.48f * t)
                    val ringWidth = sizeMin * (0.022f + 0.01f * (1f - t))
                    val ringRect = Rect(center = center, radius = ringRadius)
                    val segmentSweep = 38f
                    repeat(6) { index ->
                        val start = index * 60f + t * 120f
                        drawArc(
                            color = coreColor.copy(alpha = 0.6f * (1f - t)),
                            startAngle = start,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = ringRect.topLeft,
                            size = ringRect.size,
                            style = Stroke(width = ringWidth, cap = StrokeCap.Round)
                        )
                    }

                    val beamLength = sizeMin * (0.34f + 0.16f * (1f - t))
                    val beamWidth = sizeMin * 0.022f
                    repeat(4) { index ->
                        val angle = (45f + index * 90f + t * 25f) * (PI.toFloat() / 180f)
                        val dir = Offset(cos(angle), sin(angle))
                        drawLine(
                            color = coreColor.copy(alpha = 0.7f * (1f - t)),
                            start = center - dir * (beamLength * 0.15f),
                            end = center + dir * beamLength,
                            strokeWidth = beamWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    val jewelSize = sizeMin * (0.12f + 0.02f * pulse)
                    val innerSize = jewelSize * 0.55f
                    val jewelPath = Path().apply {
                        moveTo(center.x, center.y - jewelSize)
                        lineTo(center.x + jewelSize, center.y)
                        lineTo(center.x, center.y + jewelSize)
                        lineTo(center.x - jewelSize, center.y)
                        close()
                    }
                    val innerPath = Path().apply {
                        moveTo(center.x, center.y - innerSize)
                        lineTo(center.x + innerSize, center.y)
                        lineTo(center.x, center.y + innerSize)
                        lineTo(center.x - innerSize, center.y)
                        close()
                    }
                    withTransform({
                        rotate(18f + 140f * t, center)
                    }) {
                        drawPath(jewelPath, coreColor.copy(alpha = 0.85f))
                        drawPath(
                            jewelPath,
                            color.copy(alpha = 0.4f),
                            style = Stroke(width = sizeMin * 0.012f)
                        )
                        drawPath(innerPath, Color.White.copy(alpha = 0.8f * (1f - t)))
                    }

                    val orbitBase = sizeMin * (0.2f + 0.06f * sin(t * PI.toFloat()))
                    repeat(12) { index ->
                        val angle = (index * 30 + seed % 360) * (PI.toFloat() / 180f) + t * 1.8f
                        val wobble = 0.85f + 0.2f * sin(t * PI.toFloat() * 2.6f + index)
                        val pos = center + Offset(cos(angle), sin(angle)) * (orbitBase * wobble)
                        val moteSize = sizeMin * (0.012f + 0.006f * (1f - t))
                        drawCircle(
                            color = accentColor.copy(alpha = 0.6f * (1f - t)),
                            radius = moteSize,
                            center = pos
                        )
                    }

                    val rippleRadius = sizeMin * (0.12f + 0.6f * t)
                    drawCircle(
                        color = accentColor.copy(alpha = 0.35f * (1f - t)),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = sizeMin * 0.018f)
                    )
                }
                AttackFxStyle.GHOST_SLASH -> {
                    val slashIn = (t / 0.18f).coerceIn(0f, 1f)
                    val slashOut = if (t < 0.55f) 1f else (1f - (t - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    val slashAlpha = slashIn * slashOut
                    val angleJitter = ((seed % 9) - 4) * 2.2f
                    val angleDeg = -35f + angleJitter
                    val theta = angleDeg * (PI.toFloat() / 180f)
                    val dir = Offset(cos(theta), sin(theta))
                    val normal = Offset(-dir.y, dir.x)

                    val length = sizeMin * (0.95f + 0.08f * (1f - t))
                    val start = center - dir * length * 0.5f - normal * sizeMin * 0.1f
                    val end = center + dir * length * 0.5f + normal * sizeMin * 0.16f
                    val control = center + normal * sizeMin * 0.28f
                    val slashPath = Path().apply {
                        moveTo(start.x, start.y)
                        quadraticBezierTo(control.x, control.y, end.x, end.y)
                    }

                    val trailBrush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to color.copy(alpha = 0.2f * slashAlpha),
                            1f to color.copy(alpha = 0.6f * slashAlpha)
                        ),
                        start = start,
                        end = end
                    )
                    val coreBrush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to color.copy(alpha = 0.2f * slashAlpha),
                            0.6f to color.copy(alpha = 0.85f * slashAlpha),
                            1f to Color.White.copy(alpha = 0.95f * slashAlpha)
                        ),
                        start = start,
                        end = end
                    )

                    val outerWidth = sizeMin * (0.16f - 0.04f * t)
                    val coreWidth = sizeMin * (0.08f - 0.02f * t)
                    val highlightWidth = sizeMin * 0.024f
                    drawPath(
                        path = slashPath,
                        brush = trailBrush,
                        style = Stroke(width = outerWidth, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = slashPath,
                        brush = coreBrush,
                        style = Stroke(width = coreWidth, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = slashPath,
                        color = Color.White.copy(alpha = 0.75f * slashAlpha),
                        style = Stroke(width = highlightWidth, cap = StrokeCap.Round)
                    )

                    val afterOffset = normal * sizeMin * 0.08f * (1f - t)
                    val afterPath = Path().apply {
                        moveTo(start.x + afterOffset.x, start.y + afterOffset.y)
                        quadraticBezierTo(
                            control.x + afterOffset.x,
                            control.y + afterOffset.y,
                            end.x + afterOffset.x,
                            end.y + afterOffset.y
                        )
                    }
                    drawPath(
                        path = afterPath,
                        color = color.copy(alpha = 0.2f * slashAlpha),
                        style = Stroke(width = sizeMin * 0.06f, cap = StrokeCap.Round)
                    )

                    val speedAlpha = (1f - t) * 0.4f
                    repeat(6) { index ->
                        val offset = (index - 2.5f) * sizeMin * 0.035f
                        val lineStart = center - dir * sizeMin * (0.22f + 0.06f * index) + normal * offset
                        val lineEnd = lineStart + dir * sizeMin * 0.12f
                        drawLine(
                            color = color.copy(alpha = speedAlpha),
                            start = lineStart,
                            end = lineEnd,
                            strokeWidth = sizeMin * 0.012f,
                            cap = StrokeCap.Round
                        )
                    }

                    val sparkBurst = ((t - 0.08f) / 0.35f).coerceIn(0f, 1f)
                    repeat(10) { index ->
                        val spread = -0.6f + index * (1.2f / 9f)
                        val sparkAngle = theta + spread
                        val sparkDir = Offset(cos(sparkAngle), sin(sparkAngle))
                        val sparkLen = sizeMin * (0.04f + 0.07f * (1f - t))
                        val sparkStart = end + sparkDir * sizeMin * 0.02f
                        val sparkEnd = sparkStart + sparkDir * sparkLen
                        drawLine(
                            color = Color.White.copy(alpha = 0.7f * sparkBurst),
                            start = sparkStart,
                            end = sparkEnd,
                            strokeWidth = sizeMin * 0.008f,
                            cap = StrokeCap.Round
                        )
                    }

                    val glintAlpha = (1f - t) * slashAlpha
                    val glintSize = sizeMin * 0.06f
                    drawLine(
                        color = Color.White.copy(alpha = 0.8f * glintAlpha),
                        start = end - normal * glintSize * 0.5f,
                        end = end + normal * glintSize * 0.5f,
                        strokeWidth = sizeMin * 0.012f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f * glintAlpha),
                        start = end - dir * glintSize * 0.35f,
                        end = end + dir * glintSize * 0.35f,
                        strokeWidth = sizeMin * 0.008f,
                        cap = StrokeCap.Round
                    )

                    val mistAlpha = (1f - t) * 0.25f
                    val mist = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f * mistAlpha),
                            color.copy(alpha = 0.12f * mistAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = sizeMin * 0.4f
                    )
                    drawCircle(brush = mist, radius = sizeMin * 0.4f, center = center)
                }
            }
        }
    }
}

@Composable
private fun ImpactBurst(
    fx: DamageFxUi,
    modifier: Modifier = Modifier
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fx.id) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (fx.critical) 280 else 220,
                easing = FastOutSlowInEasing
            )
        )
    }
    val t = anim.value
    val alpha = (1f - t).coerceIn(0f, 1f)
    val ringScale = if (fx.critical) 1.15f else 1f
    val (topColor, _) = damageNumberColors(fx.element, fx.critical, isHealing = false)
    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val minSize = size.minDimension
        if (minSize <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = minSize * 0.16f
        val radius = baseRadius * (1f + 0.55f * t) * ringScale
        val stroke = (minSize * 0.02f).coerceAtLeast(2f)
        drawCircle(
            color = topColor.copy(alpha = 0.75f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )
        val shardLength = minSize * 0.08f * ringScale
        val shardStroke = stroke * 0.6f
        val angleOffset = t * 0.9f
        repeat(4) { index ->
            val angle = angleOffset + index * (PI.toFloat() / 2f)
            val dir = Offset(
                cos(angle.toDouble()).toFloat(),
                sin(angle.toDouble()).toFloat()
            )
            val start = center + dir * radius * 0.55f
            val end = center + dir * (radius * 0.55f + shardLength)
            drawLine(
                color = topColor.copy(alpha = 0.55f * alpha),
                start = start,
                end = end,
                strokeWidth = shardStroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ShieldBreakBurst(
    fx: ShieldBreakFxUi,
    modifier: Modifier = Modifier
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fx.id) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
        )
    }
    val t = anim.value.coerceIn(0f, 1f)
    val alpha = (1f - t).coerceIn(0f, 1f)
    val shieldColor = Color(0xFF42A5F5)
    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        val minSize = size.minDimension
        if (minSize <= 0f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minSize * (0.18f + 0.42f * t)
        val stroke = (minSize * 0.018f).coerceAtLeast(2f)

        drawCircle(
            color = shieldColor.copy(alpha = 0.65f * alpha),
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )

        val arcSize = Size(radius * 2f, radius * 2f)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        drawArc(
            color = Color.White.copy(alpha = 0.35f * alpha),
            startAngle = 360f * t,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = stroke * 0.75f, cap = StrokeCap.Round)
        )

        val shardCount = 7
        repeat(shardCount) { index ->
            val angle = (index / shardCount.toFloat()) * (PI.toFloat() * 2f) + t * 1.1f
            val dir = Offset(cos(angle.toDouble()).toFloat(), sin(angle.toDouble()).toFloat())
            val start = center + dir * radius * 0.62f
            val end = center + dir * radius * (0.92f + 0.18f * t)
            drawLine(
                color = shieldColor.copy(alpha = 0.55f * alpha),
                start = start,
                end = end,
                strokeWidth = stroke * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun DamageNumberBubble(
    fx: DamageFxUi,
    modifier: Modifier = Modifier
) {
    val normalizedElement = fx.element?.trim()?.lowercase(Locale.getDefault())
    val isMiss = normalizedElement == "miss" && fx.amount == 0
    val isBlocked = normalizedElement == "blocked" && fx.amount == 0
    val isHealing = fx.amount < 0
    val displayAmount = abs(fx.amount)
    val verticalOffset = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    val scaleAnim = remember { Animatable(1f) }
    val driftX = remember(fx.id, isMiss, isBlocked) { (Random.nextFloat() - 0.5f) * if (isMiss || isBlocked) 22f else 48f }
    val tilt = remember(fx.id, isHealing, isMiss, isBlocked) {
        when {
            isHealing -> 0f
            isMiss -> 0f
            isBlocked -> 0f
            else -> (Random.nextFloat() - 0.5f) * if (fx.critical) 18f else 10f
        }
    }
    LaunchedEffect(fx.id) {
        verticalOffset.snapTo(0f)
        alphaAnim.snapTo(1f)
        val initialScale = when {
            isMiss -> 1.12f
            isBlocked -> 1.12f
            isHealing -> 1.05f
            fx.critical -> 1.25f
            else -> 1.1f
        }
        scaleAnim.snapTo(initialScale)
        launch {
            verticalOffset.animateTo(
                targetValue = when {
                    isMiss -> -64f
                    isBlocked -> -64f
                    isHealing -> -60f
                    fx.critical -> -96f
                    else -> -72f
                },
                animationSpec = tween(durationMillis = 680, easing = LinearEasing)
            )
        }
        launch {
            delay(350)
            alphaAnim.animateTo(0f, tween(durationMillis = 400, easing = LinearEasing))
        }
        launch {
            scaleAnim.animateTo(1f, tween(durationMillis = 420, easing = EaseOutBack))
        }
    }
    val headline = when {
        isMiss -> "MISS!"
        isBlocked -> "BLOCKED"
        isHealing -> "+$displayAmount"
        fx.critical -> "$displayAmount"
        else -> "$displayAmount"
    }
    val (topColor, bottomColor) = damageNumberColors(fx.element, fx.critical, isHealing)
    val bubbleModifier = modifier.graphicsLayer {
            translationY = verticalOffset.value
            translationX = driftX
            alpha = alphaAnim.value
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            rotationZ = tilt
        }
    Box(
        modifier = bubbleModifier,
        contentAlignment = Alignment.Center
    ) {
        if (isBlocked) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = bottomColor.copy(alpha = 0.55f),
                modifier = Modifier.size(46.dp)
            )
            Canvas(modifier = Modifier.size(46.dp)) {
                val w = size.width
                val h = size.height
                val crackColor = Color.White.copy(alpha = 0.62f)
                val stroke = (size.minDimension * 0.06f).coerceAtLeast(2f)
                drawLine(
                    color = crackColor,
                    start = Offset(w * 0.58f, h * 0.22f),
                    end = Offset(w * 0.46f, h * 0.52f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = crackColor,
                    start = Offset(w * 0.46f, h * 0.52f),
                    end = Offset(w * 0.62f, h * 0.78f),
                    strokeWidth = stroke * 0.8f,
                    cap = StrokeCap.Round
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center
        ) {
            val outlineColor = Color.Black.copy(alpha = 0.8f)
            val outlineOffset = 1.5.dp
            val textStyle = MaterialTheme.typography.titleLarge.copy(
                fontSize = if (fx.critical) 28.sp else 24.sp,
                fontFamily = CombatNameFont,
                letterSpacing = if (isMiss || isBlocked) 0.85.sp else 0.sp,
                fontStyle = if (isHealing) FontStyle.Italic else FontStyle.Normal,
                fontWeight = FontWeight.Black
            )

            // Outline layers
            listOf(
                Offset(-1f, -1f), Offset(1f, -1f),
                Offset(-1f, 1f), Offset(1f, 1f),
                Offset(0f, -1.2f), Offset(0f, 1.2f),
                Offset(-1.2f, 0f), Offset(1.2f, 0f)
            ).forEach { offset ->
                Text(
                    text = headline,
                    style = textStyle,
                    color = outlineColor,
                    modifier = Modifier.offset(
                        x = (offset.x * outlineOffset.value).dp,
                        y = (offset.y * outlineOffset.value).dp
                    )
                )
            }

            // Main Text
            Text(
                text = headline,
                style = textStyle.copy(
                    shadow = Shadow(
                        color = bottomColor.copy(alpha = 0.9f),
                        offset = Offset(0f, 4f),
                        blurRadius = 12f
                    )
                ),
                color = topColor,
                modifier = if (isBlocked) Modifier.padding(horizontal = 8.dp) else Modifier
            )
        }
    }
}

private fun damageNumberColors(
    element: String?,
    critical: Boolean,
    isHealing: Boolean
): Pair<Color, Color> {
    if (isHealing) {
        val base = Color(0xFF81C784)
        val highlight = if (critical) 0.45f else 0.3f
        val shadow = if (critical) 0.18f else 0.12f
        val top = lerp(base, Color.White, highlight)
        val bottom = lerp(base, Color.Black, shadow)
        return top to bottom
    }
    val normalized = element?.trim()?.lowercase(Locale.getDefault())
    val base = when (normalized) {
        "burn", "fire" -> Color(0xFFFF7043)
        "freeze", "ice" -> Color(0xFF90CAF9)
        "shock", "lightning" -> Color(0xFF42A5F5)
        "acid", "poison" -> Color(0xFF81C784)
        "source", "harmonic", "psychic", "psionic", "void" -> Color(0xFFBA68C8)
        "physical" -> null
        "miss" -> Color(0xFFB0BEC5)
        "blocked" -> Color(0xFF90A4AE)
        else -> null
    }
    if (base == null) {
        val top = if (critical) Color(0xFFFFF59D) else Color(0xFFFFB74D)
        val bottom = if (critical) Color(0xFFFFD740) else Color(0xFFFF7043)
        return top to bottom
    }
    val highlight = if (critical) 0.35f else 0.2f
    val shadow = if (critical) 0.22f else 0.12f
    val top = lerp(base, Color.White, highlight)
    val bottom = lerp(base, Color.Black, shadow)
    return top to bottom
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
        launch {
            verticalOffset.animateTo(-52f, tween(durationMillis = 560, easing = LinearEasing))
        }
        launch {
            delay(350)
            alphaAnim.animateTo(0f, tween(durationMillis = 400, easing = LinearEasing))
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            translationY = verticalOffset.value
            alpha = alphaAnim.value
        },
        contentAlignment = Alignment.Center
    ) {
        val text = "+${fx.amount}"
        val textStyle = MaterialTheme.typography.titleLarge.copy(
            fontSize = 22.sp,
            fontFamily = CombatNameFont,
            fontWeight = FontWeight.Black
        )
        val outlineColor = Color.Black.copy(alpha = 0.75f)
        val outlineOffset = 1.2.dp

        // Outline
        listOf(
            Offset(-1f, -1f), Offset(1f, -1f),
            Offset(-1f, 1f), Offset(1f, 1f)
        ).forEach { offset ->
            Text(
                text = text,
                style = textStyle,
                color = outlineColor,
                modifier = Modifier.offset(
                    x = (offset.x * outlineOffset.value).dp,
                    y = (offset.y * outlineOffset.value).dp
                )
            )
        }

        Text(
            text = text,
            style = textStyle,
            color = Color(0xFF81C784)
        )
    }
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
private fun StatusImpactFlash(
    fx: StatusFxUi,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fx.id) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 760, easing = FastOutSlowInEasing))
    }
    val normalized = fx.statusId.lowercase(Locale.getDefault())
    val isBlind = normalized == "blind"
    val statusColor = if (isBlind) Color(0xFFE9F5FF) else colorForStatus(fx.statusId)
    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        val t = progress.value.coerceIn(0f, 1f)
        val fade = 1f - t
        val center = Offset(size.width * 0.5f, size.height * 0.48f)
        val radius = size.minDimension * (0.18f + 0.72f * t)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isBlind) 0.62f * fade else 0.28f * fade),
                    statusColor.copy(alpha = if (isBlind) 0.34f * fade else 0.26f * fade),
                    Color.Transparent
                ),
                center = center,
                radius = radius.coerceAtLeast(1f)
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            color = statusColor.copy(alpha = 0.44f * fade),
            radius = size.minDimension * (0.28f + 0.44f * t),
            center = center,
            style = Stroke(width = size.minDimension * 0.035f)
        )
        if (isBlind) {
            val sweepY = size.height * (0.28f + 0.32f * t)
            drawLine(
                color = Color.White.copy(alpha = 0.62f * fade),
                start = Offset(size.width * 0.12f, sweepY),
                end = Offset(size.width * 0.88f, sweepY),
                strokeWidth = size.minDimension * 0.035f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun OutcomeOverlay(
    outcomeType: CombatFxEvent.CombatOutcomeFx.OutcomeType,
    party: List<Player>
) {
    val isVictory = outcomeType == CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY
    val isRetreat = outcomeType == CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT
    val accentColor = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> Color(0xFFFF922B)
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> Color(0xFFE65D5D)
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> Color(0xFF7CD8FF)
    }
    val eyebrow = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> "Combat Result"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> "Party Status"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> "Tactical Exit"
    }
    val title = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> "Victory"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> "Defeated"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> "Retreat"
    }
    val subtitle = when (outcomeType) {
        CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY -> "Hostile contact resolved"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT -> "The party collapses"
        CombatFxEvent.CombatOutcomeFx.OutcomeType.RETREAT -> "Disengaged from combat"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (isVictory) 0.62f else 0.52f))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF15100D).copy(alpha = 0.92f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.25.dp, accentColor.copy(alpha = 0.72f)),
            shadowElevation = 18.dp,
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = CombatNameFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = if (isVictory) Icons.Filled.EmojiEvents else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.2.dp)
                        .background(accentColor.copy(alpha = 0.44f))
                )
                if (!isRetreat) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        party.take(4).forEach { player ->
                            val emotePath = when (outcomeType) {
                                CombatFxEvent.CombatOutcomeFx.OutcomeType.VICTORY ->
                                    "images/characters/emotes/${player.id}_cool.png"
                                CombatFxEvent.CombatOutcomeFx.OutcomeType.DEFEAT ->
                                    "images/characters/emotes/${player.id}_down.png"
                                else -> null
                            }
                            if (emotePath != null) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.Black.copy(alpha = 0.32f),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.36f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(if (isVictory) 96.dp else 82.dp)
                                ) {
                                    Box {
                                        Image(
                                            painter = rememberAssetPainter(emotePath, painterResource(R.drawable.main_menu_background)),
                                            contentDescription = player.name,
                                            modifier = Modifier
                                                .matchParentSize()
                                                .graphicsLayer {
                                                    scaleX = if (isVictory) 1.18f else 1f
                                                    scaleY = if (isVictory) 1.18f else 1f
                                                },
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f))
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth()
                )
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
    portraitById: Map<String, String>,
    highContrastMode: Boolean,
    theme: Theme?,
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
    val eyebrow = when (stage) {
        VictoryDialogStage.SPOILS -> "Battle Rewards"
        VictoryDialogStage.LEVEL_UPS -> "Progression"
    }
    val panelColor = Color(0xFF21130D).copy(alpha = if (highContrastMode) 0.98f else 0.94f)
    val cardColor = Color(0xFF171A24).copy(alpha = if (highContrastMode) 0.98f else 0.92f)
    val borderColor = Color(0xFFFF922B)
    val accentColor = Color(0xFFFF922B)
    val titleIcon = when (stage) {
        VictoryDialogStage.SPOILS -> Icons.Filled.EmojiEvents
        VictoryDialogStage.LEVEL_UPS -> Icons.Outlined.School
    }
    val dialogMaxWidth = 470.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = panelColor,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 18.dp,
            tonalElevation = 8.dp,
            border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.74f)),
            modifier = Modifier.widthIn(max = dialogMaxWidth)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.66f)),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = titleIcon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = CombatNameFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(accentColor.copy(alpha = 0.48f))
                )
                when (stage) {
                    VictoryDialogStage.SPOILS -> VictorySpoilsContent(
                        payload = payload,
                        itemNameResolver = itemNameResolver,
                        cardColor = cardColor,
                        accentColor = accentColor
                    )
                    VictoryDialogStage.LEVEL_UPS -> VictoryLevelUpContent(
                        levelUps = payload.levelUps,
                        portraitById = portraitById,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        cardColor = cardColor,
                        highContrastMode = highContrastMode
                    )
                }
                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun VictorySpoilsContent(
    payload: CombatResultPayload,
    itemNameResolver: (String) -> String,
    cardColor: Color,
    accentColor: Color
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
            .heightIn(min = 120.dp, max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (resourceEntries.isNotEmpty()) {
            resourceEntries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (label, value) ->
                        RewardStatCard(
                            label = label,
                            value = value,
                            cardColor = cardColor,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
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
private fun RewardStatCard(
    label: String,
    value: String,
    cardColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = cardColor.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.1.dp, accentColor.copy(alpha = 0.54f)),
        modifier = modifier.heightIn(min = 74.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VictoryLevelUpContent(
    levelUps: List<LevelUpSummary>,
    portraitById: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
    cardColor: Color,
    highContrastMode: Boolean
) {
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
            .heightIn(min = 104.dp, max = 260.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(levelUps, key = { summary -> summary.characterId }) { summary ->
            LevelUpCard(
                summary = summary,
                portraitPath = portraitById[summary.characterId],
                accentColor = accentColor,
                borderColor = borderColor,
                cardColor = cardColor,
                highContrastMode = highContrastMode
            )
        }
    }
}

@Composable
private fun LevelUpCard(
    summary: LevelUpSummary,
    portraitPath: String?,
    accentColor: Color,
    borderColor: Color,
    cardColor: Color,
    highContrastMode: Boolean
) {
    val portraitPainter = rememberAssetPainter(portraitPath, painterResource(R.drawable.main_menu_background))
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.1.dp, borderColor.copy(alpha = if (highContrastMode) 0.78f else 0.54f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0B0F15))
                        .border(1.dp, accentColor.copy(alpha = 0.56f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = portraitPainter,
                        contentDescription = summary.characterName,
                        modifier = Modifier
                            .matchParentSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = summary.characterName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = CombatNameFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "LEVEL ${summary.newLevel}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }

            if (summary.unlockedSkills.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = Color(0xFFFFE082)
                        )
                        Text(
                            text = if (summary.unlockedSkills.size == 1) "New Skill" else "New Skills",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFE082)
                        )
                    }
                    LazyRow(
                        modifier = Modifier.widthIn(max = 1200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(summary.unlockedSkills, key = { skill -> skill.id }) { skill ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, Color(0xFFFFE082).copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = skill.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
private fun CombatLogPanel(
    bannerMessage: CombatBannerMessage?,
    instruction: String?,
    showCancel: Boolean,
    instructionShownAbove: Boolean = false,
    highContrastMode: Boolean,
    theme: Theme?,
    onCancel: (() -> Unit)?
) {
    val instructionSlotHeight = 30.dp
    val cancelSlotHeight = 28.dp
    val baseSpacing = 6.dp
    val baseHeight = instructionSlotHeight + cancelSlotHeight + baseSpacing
    val hasInstruction = instructionShownAbove || !instruction.isNullOrBlank()
    val displayMessage = bannerMessage ?: if (!instructionShownAbove && !instruction.isNullOrBlank()) {
        CombatBannerMessage(
            id = "instruction",
            primary = instruction,
            accent = CombatBannerAccent.DEFAULT
        )
    } else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(baseHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(baseSpacing)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(instructionSlotHeight)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cancelSlotHeight)
            ) {
                if (showCancel && onCancel != null) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(text = "Cancel target")
                    }
                }
            }
        }
        CombatImpactBanner(
            message = displayMessage,
            hasInstruction = hasInstruction,
            persistent = displayMessage?.id == "instruction",
            highContrastMode = highContrastMode,
            theme = theme,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-56).dp)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun TargetInstructionBadge(text: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF0A0C10).copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 4.dp)
    ) {
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

            Text(
                text = text.uppercase(Locale.getDefault()),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    fontFamily = CombatNameFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 2f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CommandPalette(
    visible: Boolean,
    actor: Player?,
    currentHp: Int,
    maxHp: Int,
    atbProgress: Float,
    canAttack: Boolean,
    hasSkills: Boolean,
    hasItems: Boolean,
    onAttack: () -> Unit,
    onSkills: () -> Unit,
    onItems: () -> Unit,
    snackLabel: String,
    canSnack: Boolean,
    snackCooldown: Int,
    onSnack: () -> Unit,
    onRetreat: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    theme: Theme?,
    targetInstruction: String? = null,
    onCancelTarget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (actor == null) return
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { full -> full }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { full -> full / 2 })
    ) {
        val portraitPainter = rememberAssetPainter(actor.miniIconPath, painterResource(R.drawable.main_menu_background))
        val paletteBase = themeColor(theme?.bg, Color(0xFF0F1118))
        val paletteColor = paletteBase.copy(alpha = if (highContrastMode) 0.96f else 0.90f)
        val borderColor = themeColor(theme?.border, Color.White.copy(alpha = if (highContrastMode) 0.65f else 0.5f))
        val accentColor = themeColor(theme?.accent, Color(0xFFFF6A5F))
        val isTargetMode = !targetInstruction.isNullOrBlank()
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = paletteColor,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 10.dp,
                tonalElevation = 6.dp,
                border = BorderStroke(1.25.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF1C1F24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = portraitPainter,
                                contentDescription = actor.name,
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = titleCaseName(actor.name),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "${currentHp.coerceAtLeast(0)}/${maxHp.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.74f)
                            )
                            AtbBar(
                                progress = atbProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            )
                        }
                        if (isTargetMode && onCancelTarget != null) {
                            TextButton(onClick = onCancelTarget) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = accentColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Cancel", color = Color.White)
                            }
                        }
                    }
                    if (isTargetMode) {
                        TargetInstructionBadge(
                            text = targetInstruction.orEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val commands = listOf(
                            CommandEntry("Attack", Icons.Rounded.Whatshot, canAttack, onAttack),
                            CommandEntry("Abilities", Icons.Rounded.AutoAwesome, hasSkills, onSkills),
                            CommandEntry("Items", Icons.Rounded.Inventory2, hasItems, onItems),
                            CommandEntry(snackLabel, Icons.Rounded.Restaurant, canSnack, onSnack, cooldown = snackCooldown),
                            CommandEntry("Retreat", Icons.Rounded.ExitToApp, true, onRetreat)
                        )
                        val rows = listOf(commands.take(3), commands.drop(3))
                        rows.forEach { chunk ->
                            if (chunk.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunk.forEach { entry ->
                                        CombatCommandButton(
                                            label = entry.label,
                                            icon = entry.icon,
                                            enabled = entry.enabled,
                                            onClick = entry.onClick,
                                            largeTouchTargets = largeTouchTargets,
                                            cooldownRemaining = entry.cooldown,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            CommandPaletteMarkers(
                color = accentColor,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

private data class CommandEntry(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val cooldown: Int = 0
)

@Composable
private fun CombatCommandButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    largeTouchTargets: Boolean,
    cooldownRemaining: Int = 0,
    modifier: Modifier = Modifier
) {
    val minHeight = if (largeTouchTargets) 58.dp else 48.dp
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (enabled) Color(0xFF1E2534) else Color(0xFF1B1F29)
    Box(
        modifier = modifier
            .widthIn(min = 88.dp)
            .heightIn(min = minHeight)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = background,
            tonalElevation = if (enabled) 4.dp else 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.45f)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (cooldownRemaining > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cooldownRemaining.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                )
            }
        }
    }
}

@Composable
private fun CommandPaletteMarkers(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 42.dp, vertical = 8.dp)
    ) {
        val strokeWidth = 4.dp.toPx()
        val markerLength = 38.dp.toPx()
        val y = strokeWidth / 2f
        val tint = color.copy(alpha = 0.9f)
        drawLine(
            color = tint,
            start = Offset(0f, y),
            end = Offset(markerLength, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(size.width - markerLength, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun CombatItemsDialog(
    items: List<InventoryEntry>,
    theme: Theme?,
    highContrastMode: Boolean,
    onItemSelected: (InventoryEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = themeColor(theme?.accent, Color(0xFF7BE4FF))
    val border = themeColor(theme?.border, Color(0xFF5CCBE8))
    val panel = themeColor(theme?.bg, Color(0xFF061018))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = panel.copy(alpha = if (highContrastMode) 0.98f else 0.94f),
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.88f),
        shape = RoundedCornerShape(18.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = CombatNameFont,
                        color = Color.White
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(accent.copy(alpha = 0.80f), Color.Transparent)
                            )
                        )
                )
            }
        },
        text = {
            if (items.isEmpty()) {
                Text(
                    text = "No usable items available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.76f)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { entry ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = if (highContrastMode) 0.10f else 0.07f),
                            border = BorderStroke(1.dp, border.copy(alpha = 0.34f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = entry.item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "x${entry.quantity}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent.copy(alpha = 0.86f)
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
private fun CombatImpactBanner(
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

@Composable
private fun ReadyAura(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ready_aura_transition")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ready_aura_pulse"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
                alpha = 0.42f + (pulse - 0.94f) * 1.4f
            }
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .border(BorderStroke(2.dp, color.copy(alpha = 0.65f)), CircleShape)
    )
}

@Composable
private fun AtbBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2D9CFF)
) {
    val clamped = progress.coerceIn(0f, 1f)
    val ready = clamped >= 0.999f
    val barTransition = rememberInfiniteTransition(label = "atb_bar_transition")
    val tipGlow by barTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atb_tip_glow"
    )
    val readyPulse by barTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "atb_ready_pulse"
    )
    val readyFlash = remember { Animatable(0f) }
    LaunchedEffect(ready) {
        if (ready) {
            readyFlash.snapTo(1f)
            readyFlash.animateTo(0f, tween(durationMillis = 360))
        } else {
            readyFlash.snapTo(0f)
        }
    }
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
    ) {
        if (ready) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.55f + 0.35f * readyPulse
                        scaleX = 1f + 0.04f * readyPulse
                        scaleY = 1f + 0.04f * readyPulse
                    }
            ) {
                val corner = CornerRadius(size.height, size.height)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.05f),
                            color.copy(alpha = 0.45f + 0.25f * readyPulse),
                            color.copy(alpha = 0.05f)
                        )
                    ),
                    size = size,
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f + 0.12f * readyPulse),
                    size = size,
                    cornerRadius = corner,
                    style = Stroke(width = size.height * 0.2f)
                )
            }
        }
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
        if (readyFlash.value > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f * readyFlash.value),
                    size = size,
                    cornerRadius = CornerRadius(size.height, size.height)
                )
            }
        }
    }
}

