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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
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
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.CombatViewModel.TimedPromptState
import com.example.starborn.feature.combat.viewmodel.CombatFxEvent
import com.example.starborn.feature.combat.ui.animations.CombatSide
import com.example.starborn.feature.combat.ui.animations.LungeAxis
import com.example.starborn.feature.combat.ui.animations.Lungeable
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import com.example.starborn.domain.cinematic.CinematicPlaybackState
import com.example.starborn.domain.cinematic.CinematicStepType
import com.example.starborn.feature.exploration.ui.CinematicOverlay
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

private data class AttackHitFxUi(
    val id: String,
    val targetId: String,
    val style: AttackFxStyle,
    val critical: Boolean
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
        override val instruction: String = "Choose an enemy to attack"
        override fun accepts(target: TargetFilter): Boolean = target == TargetFilter.ENEMY
    }

    data class SupportRequest(
        val abilityName: String,
        val filter: TargetFilter,
        override val instruction: String
    ) : PendingTargetRequest {
        override fun accepts(target: TargetFilter): Boolean =
            filter == TargetFilter.ANY || filter == target
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
    val lungeStyle by viewModel.lungeStyle.collectAsStateWithLifecycle(AttackLungeStyle.HIT)
    val missLungeActorId by viewModel.missLungeActorId.collectAsStateWithLifecycle(null)
    val missLungeToken by viewModel.missLungeToken.collectAsStateWithLifecycle(0L)
    val timedPromptState by viewModel.timedPrompt.collectAsStateWithLifecycle()
    val awaitingActionId by viewModel.awaitingAction.collectAsStateWithLifecycle()
    val combatMessage by viewModel.combatMessage.collectAsStateWithLifecycle()
    val cinematicPlayback = cinematicState
        ?.collectAsStateWithLifecycle(initialValue = null)
        ?.value
    val damageFx = remember { mutableStateListOf<DamageFxUi>() }
    val healFx = remember { mutableStateListOf<HealFxUi>() }
    val statusFx = remember { mutableStateListOf<StatusFxUi>() }
    val knockoutFx = remember { mutableStateListOf<KnockoutFxUi>() }
    val supportFx = remember { mutableStateListOf<SupportFxUi>() }
    val attackHitFx = remember { mutableStateListOf<AttackHitFxUi>() }
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
                            activeId = currentTurnActorId,
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
            when (request) {
                PendingTargetRequest.Attack -> {
                    viewModel.focusEnemyTarget(targetId)
                    viewModel.playerAttack(targetId)
                }
                is PendingTargetRequest.SupportRequest -> {
                    viewModel.useSupportAbility(targetId)
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
            }
            val commandActor = menuActor
            val commandPaletteVisible = commandActor != null &&
                !combatLocked &&
                !menuActorCannotAct &&
                pendingTargetRequest == null
            val hasTargets = enemyCombatantIds.any { state.combatants[it]?.isAlive == true }
            val snackBaseLabel = commandActor?.let { actor -> viewModel.snackLabel(actor.id) } ?: "Snack"
            val snackCooldown = commandActor?.let { actor -> viewModel.snackCooldownRemaining(actor.id) } ?: 0
            val snackLabel = when {
                snackCooldown > 0 -> "$snackBaseLabel ($snackCooldown)"
                else -> snackBaseLabel
            }
            val snackRequirement = commandActor?.let { actor -> viewModel.snackTargetRequirement(actor.id) } ?: TargetRequirement.NONE
            val snackUsable = commandActor?.let { actor -> viewModel.canUseSnack(actor.id) } == true
            val supportLabel = commandActor?.let { actor -> viewModel.supportAbilityLabel(actor.id) } ?: "Support"
            val supportRequirement = commandActor?.let { actor -> viewModel.supportTargetRequirement(actor.id) } ?: TargetRequirement.NONE
            val canSnack = snackUsable && when (snackRequirement) {
                TargetRequirement.ENEMY -> hasTargets
                TargetRequirement.ALLY -> playerParty.any { member -> state.combatants[member.id]?.isAlive == true }
                TargetRequirement.ANY -> hasTargets || playerParty.any { member -> state.combatants[member.id]?.isAlive == true }
                TargetRequirement.NONE -> true
            }
            val canSupport = when (supportRequirement) {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    EnemyRoster(
                        enemies = enemies,
                        combatantIds = enemyCombatantIds,
                        combatState = state,
                        activeId = activeId,
                        selectedEnemyIds = selectedEnemyIds,
                        labelBorderColor = themeColor(viewModel.theme?.accent, Color(0xFF2D9CFF)),
                        damageFx = damageFx,
                        attackFx = attackHitFx,
                        healFx = healFx,
                        statusFx = statusFx,
                        knockoutFx = knockoutFx,
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
                        showTargetPrompt = enemyTargetPrompt
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = partyDockHeight + 8.dp)
                ) {
                    CombatLogPanel(
                        flavorLine = combatMessage,
                        instruction = pendingInstruction,
                        showCancel = pendingTargetRequest != null,
                        instructionShownAbove = enemyTargetPrompt,
                        onCancel = if (pendingTargetRequest != null) {
                            { clearPendingRequest() }
                        } else null
                    )
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
                    knockoutFx = knockoutFx,
                    supportFx = supportFx,
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
                canAttack = hasTargets,
                hasSkills = menuActorSkills.isNotEmpty(),
                hasItems = inventoryEntries.isNotEmpty(),
                onAttack = { requestTarget(PendingTargetRequest.Attack) },
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
                snackLabel = snackLabel,
                canSnack = canSnack,
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
                                instruction = "Choose a target for $snackBaseLabel"
                            )
                        )
                        TargetRequirement.NONE -> {
                            pendingTargetRequest = null
                            pendingInstruction = null
                            viewModel.useSnack()
                        }
                    }
                },
                supportLabel = supportLabel,
                canSupport = canSupport,
                onSupport = {
                    when (supportRequirement) {
                        TargetRequirement.ENEMY -> requestTarget(
                            PendingTargetRequest.SupportRequest(
                                abilityName = supportLabel,
                                filter = TargetFilter.ENEMY,
                                instruction = "Choose an enemy for $supportLabel"
                            )
                        )
                        TargetRequirement.ALLY -> requestTarget(
                            PendingTargetRequest.SupportRequest(
                                abilityName = supportLabel,
                                filter = TargetFilter.ALLY,
                                instruction = "Choose an ally for $supportLabel"
                            )
                        )
                        TargetRequirement.ANY -> requestTarget(
                            PendingTargetRequest.SupportRequest(
                                abilityName = supportLabel,
                                filter = TargetFilter.ANY,
                                instruction = "Choose a target for $supportLabel"
                            )
                        )
                        TargetRequirement.NONE -> {
                            pendingTargetRequest = null
                            pendingInstruction = null
                            viewModel.useSupportAbility()
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
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
            cinematicPlayback?.toUiState()?.let { overlayState ->
                CinematicOverlay(
                    state = overlayState,
                    onAdvance = { onAdvanceCinematic?.invoke() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
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
private fun PartyRoster(
    party: List<Player>,
    combatState: CombatState,
    activeId: String?,
    damageFx: List<DamageFxUi>,
    attackFx: List<AttackHitFxUi>,
    healFx: List<HealFxUi>,
    statusFx: List<StatusFxUi>,
    knockoutFx: List<KnockoutFxUi>,
    supportFx: List<SupportFxUi>,
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
                            val cardShape = RoundedCornerShape(22.dp)
                            val isAlive = memberState?.isAlive != false
                            val memberLungeToken = if (member.id == lungeActorId) lungeToken else null
                            val isMissLunge = memberLungeToken != null && lungeStyle == AttackLungeStyle.MISS
                            val portraitPath = when {
                                !isAlive -> "images/characters/emotes/${member.id}_down.png"
                                isMissLunge -> "images/characters/emotes/${member.id}_confident.png"
                                memberLungeToken != null -> "images/characters/emotes/${member.id}_angry.png"
                                victoryEmotes -> "images/characters/emotes/${member.id}_cool.png"
                                else -> member.miniIconPath
                            }
                            val portraitPainter = rememberAssetPainter(portraitPath, R.drawable.main_menu_background)
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
                                                    val pulse = 1f + 0.06f * hitPulse
                                                    scaleX = pulse
                                                    scaleY = pulse
                                                    translationY = hitRecoilY
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val lungeDistance = if (suppressLunge) 0.dp else 24.dp
                                            val lungeAxis = if (isMissLunge) LungeAxis.X else LungeAxis.Y
                                            val lungeDirectionSign = if (isMissLunge) -1f else 1f
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
                                val elementStacks = memberState?.elementStacks.orEmpty()
                                if (statuses.isNotEmpty() || buffs.isNotEmpty() || elementStacks.values.any { it > 0 }) {
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
                                            ElementStackRow(elementStacks)
                                        }
                                    }
                                }
                                CombatFxOverlay(
                                    damageFx = damageFx.filter { it.targetId == member.id },
                                    attackFx = attackFx.filter { it.targetId == member.id },
                                    healFx = healFx.filter { it.targetId == member.id },
                                    statusFx = statusFx.filter { it.targetId == member.id },
                                    showKnockout = knockoutFx.any { it.targetId == member.id },
                                    shape = cardShape,
                                    supportFx = supportHighlights,
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
    knockoutFx: List<KnockoutFxUi>,
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
    showTargetPrompt: Boolean = false
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
            knockoutFx = knockoutFx,
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
            showTargetPrompt = showTargetPrompt
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
                        val isActive = activeId == combatantId
                        val isSelected = combatantId in selectedEnemyIds
                        val isAlive = enemyState?.isAlive != false
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
                            R.drawable.main_menu_background
                        )
                        val shape = RoundedCornerShape(28.dp)
                        val atbProgress = atbMeters[combatantId] ?: 0f
                        val spriteScale = when {
                            isElite -> 1.4f
                            isBoss -> 1f
                            else -> 1.1f
                        }
                        val enemyLungeToken = if (combatantId == lungeActorId) lungeToken else null
                        val enemyMissToken = if (combatantId == missLungeActorId) missLungeToken else null
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
                                    atbProgress = atbProgress,
                                    isAlive = isAlive,
                                    isActive = isActive,
                                    isSelected = isSelected,
                                    accentColor = labelBorderColor,
                                    barWidth = barWidth
                                )
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
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(bottom = innerPadding)
                                            .size(portraitSize)
                                            .graphicsLayer {
                                                scaleX = spriteScale
                                                scaleY = spriteScale
                                                translationY = hitRecoilY
                                            }
                                    ) {
                                        Lungeable(
                                            side = CombatSide.ENEMY,
                                            triggerToken = enemyMissToken,
                                            distance = 18.dp,
                                            axis = LungeAxis.X,
                                            directionSign = 1f,
                                            modifier = Modifier.matchParentSize(),
                                            onFinished = { enemyMissToken?.let(onMissLungeFinished) }
                                        ) {
                                            Lungeable(
                                                side = CombatSide.ENEMY,
                                                triggerToken = enemyLungeToken,
                                                distance = 24.dp,
                                                axis = LungeAxis.Y,
                                                directionSign = 1f,
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
                                    }
                                    CombatFxOverlay(
                                        damageFx = damageFx.filter { it.targetId == combatantId },
                                        attackFx = attackFx.filter { it.targetId == combatantId },
                                        healFx = healFx.filter { it.targetId == combatantId },
                                        statusFx = statusFx.filter { it.targetId == combatantId },
                                        showKnockout = knockoutFx.any { it.targetId == combatantId },
                                        shape = shape,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                                StatusBadges(
                                    statuses = enemyState?.statusEffects.orEmpty(),
                                    buffs = enemyState?.buffs.orEmpty()
                                )
                                ElementStackRow(enemyState?.elementStacks.orEmpty())
                            }
                        }
                    }
                }
            }
        }
        if (showTargetPrompt) {
            Box(modifier = Modifier.matchParentSize()) {
                TargetInstructionBadge(
                    text = "Choose Your Target",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )
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
    knockoutFx: List<KnockoutFxUi>,
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
    showTargetPrompt: Boolean = false
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = groupOffsetXDp, y = topLabelOffset + groupOffsetYDp)
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
                        R.drawable.main_menu_background
                    )
                    val shape = RoundedCornerShape(24.dp)
                    val enemyLungeToken = if (combatantId == lungeActorId) lungeToken else null
                    val enemyMissToken = if (combatantId == missLungeActorId) missLungeToken else null
                    val partWidth = baseSize * entry.layout.widthScale
                    val partHeight = baseSize * entry.layout.heightScale
                    val partOffsetX = baseSize * entry.layout.offsetX
                    val partOffsetY = baseSize * entry.layout.offsetY + groupOffsetYDp
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val glowScale = 1f + selectionGlow.value * 0.08f
                                    val hitScale = 1f + 0.04f * hitPulse
                                    val combined = glowScale * hitScale
                                    scaleX = combined
                                    scaleY = combined
                                    translationY = hitRecoilY
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
                            Lungeable(
                                side = CombatSide.ENEMY,
                                triggerToken = enemyMissToken,
                                distance = 18.dp,
                                axis = LungeAxis.X,
                                directionSign = 1f,
                                modifier = Modifier.matchParentSize(),
                                onFinished = { enemyMissToken?.let(onMissLungeFinished) }
                            ) {
                                Lungeable(
                                    side = CombatSide.ENEMY,
                                    triggerToken = enemyLungeToken,
                                    distance = 24.dp,
                                    axis = LungeAxis.Y,
                                    directionSign = 1f,
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
                            CombatFxOverlay(
                                damageFx = damageFx.filter { it.targetId == combatantId },
                                attackFx = attackFx.filter { it.targetId == combatantId },
                                healFx = healFx.filter { it.targetId == combatantId },
                                statusFx = statusFx.filter { it.targetId == combatantId },
                                showKnockout = knockoutFx.any { it.targetId == combatantId },
                                shape = shape,
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
        if (showTargetPrompt) {
            Box(modifier = Modifier.matchParentSize()) {
                TargetInstructionBadge(
                    text = "Choose Your Target",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )
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
    val isAlive = enemyState?.isAlive != false
    val isActive = combatantId == activeId
    val isSelected = combatantId in selectedEnemyIds
    val atbProgress = atbMeters[combatantId] ?: 0f
    EnemyStatusLabel(
        name = entry.enemy.name,
        currentHp = currentHp,
        maxHp = maxHp,
        atbProgress = atbProgress,
        isAlive = isAlive,
        isActive = isActive,
        isSelected = isSelected,
        accentColor = labelBorderColor,
        barWidth = 90.dp,
        onClick = { if (isAlive) onEnemyTap(combatantId) },
        onLongClick = { if (isAlive) onEnemyLongPress(combatantId) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnemyStatusLabel(
    name: String,
    currentHp: Int,
    maxHp: Int,
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
        modifier = interactionModifier.alpha(if (isAlive) 1f else 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                modifier = Modifier.width(barWidth)
            )
            StatBar(
                current = currentHp,
                max = maxHp,
                color = Color(0xFFFF5252),
                background = Color.Black.copy(alpha = 0.5f),
                height = 6.dp,
                modifier = Modifier.width(barWidth)
            )
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
private fun ElementStackRow(elementStacks: Map<String, Int>) {
    val entries = ElementalStackRules.displayOrder.mapNotNull { element ->
        val count = elementStacks[element] ?: 0
        if (count <= 0) null else element to count
    }
    if (entries.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        entries.forEach { (element, count) ->
            ElementStackBadge(element = element, stacks = count)
        }
    }
}

@Composable
private fun ElementStackBadge(element: String, stacks: Int) {
    val color = elementStackColor(element)
    val label = element.take(1).uppercase(Locale.getDefault())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, color.copy(alpha = 0.7f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            val threshold = ElementalStackRules.STACK_THRESHOLD
            repeat(threshold) { index ->
                val filled = index < stacks.coerceAtMost(threshold)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (filled) color else color.copy(alpha = 0.25f))
                )
            }
        }
    }
}

private fun elementStackColor(element: String): Color = when (element.lowercase(Locale.getDefault())) {
    "fire" -> Color(0xFFFF7043)
    "ice" -> Color(0xFF90CAF9)
    "lightning" -> Color(0xFF42A5F5)
    "poison" -> Color(0xFF81C784)
    "radiation" -> Color(0xFFFFD54F)
    else -> Color(0xFFD1C4E9)
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
    showKnockout: Boolean,
    shape: Shape,
    supportFx: List<SupportFxUi> = emptyList(),
    modifier: Modifier = Modifier.fillMaxSize()
) {
    if (damageFx.isEmpty() && attackFx.isEmpty() && healFx.isEmpty() && statusFx.isEmpty() && !showKnockout && supportFx.isEmpty()) return
    Box(
        modifier = modifier
            .clip(shape)
            .zIndex(1f)
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
        damageFx.filter { fx ->
            fx.amount > 0 && fx.element != "miss"
        }.forEach { fx ->
            ImpactBurst(
                fx = fx,
                modifier = Modifier.matchParentSize()
            )
        }
        damageFx.forEach { fx ->
            DamageNumberBubble(
                fx = fx,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        attackFx.forEach { fx ->
            AttackHitFx(
                fx = fx,
                modifier = Modifier.fillMaxSize()
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
    activeId: String?,
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
    val activeStyle = activeId
        ?.lowercase(Locale.getDefault())
        ?.let { attackFxStyleFor(it) ?: attackFxStyleForName(it) }
    if (activeStyle != null) return activeStyle
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
private fun DamageNumberBubble(
    fx: DamageFxUi,
    modifier: Modifier = Modifier
) {
    val isHealing = fx.amount < 0
    val displayAmount = abs(fx.amount)
    val verticalOffset = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    val scaleAnim = remember { Animatable(1f) }
    val driftX = remember(fx.id) { (Random.nextFloat() - 0.5f) * 48f }
    val tilt = remember(fx.id, isHealing) {
        if (isHealing) 0f else (Random.nextFloat() - 0.5f) * if (fx.critical) 18f else 10f
    }
    LaunchedEffect(fx.id) {
        verticalOffset.snapTo(0f)
        alphaAnim.snapTo(1f)
        val initialScale = when {
            isHealing -> 1.05f
            fx.critical -> 1.25f
            else -> 1.1f
        }
        scaleAnim.snapTo(initialScale)
        launch {
            verticalOffset.animateTo(
                targetValue = when {
                    isHealing -> -60f
                    fx.critical -> -96f
                    else -> -72f
                },
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
    val headline = when {
        isHealing -> "+$displayAmount"
        fx.critical -> "!$displayAmount"
        else -> "$displayAmount"
    }
    val (topColor, bottomColor) = damageNumberColors(fx.element, fx.critical, isHealing)
    Text(
        text = headline,
        style = MaterialTheme.typography.titleLarge.copy(
            shadow = Shadow(
                color = bottomColor.copy(alpha = 0.75f),
                offset = Offset(0f, 6f),
                blurRadius = 18f
            ),
            fontStyle = if (isHealing) FontStyle.Italic else FontStyle.Normal
        ),
        fontWeight = if (!isHealing && fx.critical) FontWeight.Black else FontWeight.SemiBold,
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
        "fire" -> Color(0xFFFF7043)
        "ice" -> Color(0xFF90CAF9)
        "lightning", "shock" -> Color(0xFF42A5F5)
        "poison" -> Color(0xFF81C784)
        "radiation" -> Color(0xFFFFD54F)
        "psychic", "psionic" -> Color(0xFFBA68C8)
        "void" -> Color(0xFF7E57C2)
        "physical" -> null
        "miss" -> Color(0xFFB0BEC5)
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
    val panelBase = themeColor(theme?.bg, Color(0xFF0F1118))
    val panelColor = panelBase.copy(alpha = if (highContrastMode) 0.98f else 0.95f)
    val borderColor = themeColor(theme?.border, Color.White.copy(alpha = if (highContrastMode) 0.65f else 0.45f))
    val accentColor = themeColor(
        theme?.accent,
        when (stage) {
            VictoryDialogStage.SPOILS -> Color(0xFFFFD54F)
            VictoryDialogStage.LEVEL_UPS -> Color(0xFF7CD8FF)
        }
    )
    val titleIcon = when (stage) {
        VictoryDialogStage.SPOILS -> Icons.Filled.EmojiEvents
        VictoryDialogStage.LEVEL_UPS -> Icons.Outlined.School
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = panelColor,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 18.dp,
            tonalElevation = 8.dp,
            border = BorderStroke(1.5.dp, borderColor),
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = titleIcon,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
                when (stage) {
                    VictoryDialogStage.SPOILS -> VictorySpoilsContent(payload, itemNameResolver)
                    VictoryDialogStage.LEVEL_UPS -> VictoryLevelUpContent(
                        levelUps = payload.levelUps,
                        portraitById = portraitById,
                        accentColor = accentColor,
                        borderColor = borderColor,
                        highContrastMode = highContrastMode
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onContinue) {
                        Text(buttonLabel)
                    }
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
private fun VictoryLevelUpContent(
    levelUps: List<LevelUpSummary>,
    portraitById: Map<String, String>,
    accentColor: Color,
    borderColor: Color,
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
            .heightIn(min = 140.dp, max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(levelUps, key = { summary -> summary.characterId }) { summary ->
            LevelUpCard(
                summary = summary,
                portraitPath = portraitById[summary.characterId],
                accentColor = accentColor,
                borderColor = borderColor,
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
    highContrastMode: Boolean
) {
    val portraitPainter = rememberAssetPainter(portraitPath, R.drawable.main_menu_background)
    val cardColor = Color(0xFF151C2A).copy(alpha = if (highContrastMode) 0.96f else 0.9f)
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.25.dp, borderColor.copy(alpha = if (highContrastMode) 0.75f else 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.16f), Color.Transparent)
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.9f))
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0B0F15)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = portraitPainter,
                        contentDescription = summary.characterName,
                        modifier = Modifier
                            .matchParentSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = summary.characterName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Text(
                        text = "Reached Lv. ${summary.newLevel} (+${summary.levelsGained})",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor.copy(alpha = 0.92f)
                    )
                }
            }

            if (summary.statChanges.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(summary.statChanges, key = { delta -> delta.label }) { delta ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "${delta.label} ${delta.value}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    flavorLine: String?,
    instruction: String?,
    showCancel: Boolean,
    instructionShownAbove: Boolean = false,
    onCancel: (() -> Unit)?
) {
    val instructionSlotHeight = 30.dp
    val cancelSlotHeight = 28.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AnimatedFlavorText(flavorLine)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(instructionSlotHeight)
        ) {
            if (!instructionShownAbove && !instruction.isNullOrBlank()) {
                TargetInstructionBadge(
                    text = instruction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp)
                )
            }
        }
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
}

@Composable
private fun TargetInstructionBadge(text: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, accent),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            textAlign = TextAlign.Center,
            letterSpacing = 0.8.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CommandPalette(
    visible: Boolean,
    actor: Player?,
    currentHp: Int,
    maxHp: Int,
    canAttack: Boolean,
    hasSkills: Boolean,
    hasItems: Boolean,
    onAttack: () -> Unit,
    onSkills: () -> Unit,
    onItems: () -> Unit,
    snackLabel: String,
    canSnack: Boolean,
    onSnack: () -> Unit,
    supportLabel: String,
    canSupport: Boolean,
    onSupport: () -> Unit,
    onRetreat: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    theme: Theme?,
    modifier: Modifier = Modifier
) {
    if (actor == null) return
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { full -> full }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { full -> full / 2 })
    ) {
        val portraitPainter = rememberAssetPainter(actor.miniIconPath, R.drawable.main_menu_background)
        val paletteBase = themeColor(theme?.bg, Color(0xFF0F1118))
        val paletteColor = paletteBase.copy(alpha = if (highContrastMode) 0.95f else 0.78f)
        val borderColor = themeColor(theme?.border, Color.White.copy(alpha = if (highContrastMode) 0.65f else 0.5f))
        val accentColor = themeColor(theme?.accent, Color(0xFFFF6A5F))
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = paletteColor,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 12.dp,
                tonalElevation = 6.dp,
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF1C1F24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = portraitPainter,
                                contentDescription = actor.name,
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(6.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = titleCaseName(actor.name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = CombatNameFont,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "${currentHp.coerceAtLeast(0)}/${maxHp.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.74f)
                            )
                        }
                    }
                    val commands = listOf(
                        CommandEntry("Attack", Icons.Rounded.Whatshot, canAttack, onAttack),
                        CommandEntry("Skills", Icons.Rounded.AutoAwesome, hasSkills, onSkills),
                        CommandEntry("Items", Icons.Rounded.Inventory2, hasItems, onItems),
                        CommandEntry(snackLabel, Icons.Rounded.Restaurant, canSnack, onSnack),
                        CommandEntry(supportLabel, Icons.Rounded.Shield, canSupport, onSupport),
                        CommandEntry("Retreat", Icons.Rounded.ExitToApp, true, onRetreat)
                    )
                    val rows = commands.chunked(2)
                    rows.forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            chunk.forEach { entry ->
                                CombatCommandButton(
                                    label = entry.label,
                                    icon = entry.icon,
                                    enabled = entry.enabled,
                                    onClick = entry.onClick,
                                    largeTouchTargets = largeTouchTargets,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
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
    val onClick: () -> Unit
)

@Composable
private fun CombatCommandButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    val minHeight = if (largeTouchTargets) 70.dp else 56.dp
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (enabled) Color(0xFF1E2534) else Color(0xFF1B1F29)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = background,
        tonalElevation = if (enabled) 4.dp else 0.dp,
        modifier = modifier
            .widthIn(min = 140.dp)
            .heightIn(min = minHeight * 2 / 3)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White
            )
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
