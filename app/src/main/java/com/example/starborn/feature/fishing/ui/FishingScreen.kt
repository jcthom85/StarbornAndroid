package com.example.starborn.feature.fishing.ui

import com.example.starborn.ui.fishing.*

import com.example.starborn.domain.fishing.handlingSummary
import com.example.starborn.domain.fishing.catchSummary

import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.domain.fishing.FishingFightPhase
import com.example.starborn.domain.fishing.FishingJournalEntry
import com.example.starborn.domain.fishing.FishingRarity
import com.example.starborn.domain.fishing.MinigameResult
import com.example.starborn.domain.fishing.isNativeFish
import com.example.starborn.feature.common.ui.StationBackground
import com.example.starborn.feature.common.ui.StationHeader
import com.example.starborn.feature.fishing.sensors.HookMotionDetector
import com.example.starborn.feature.fishing.viewmodel.FishingController.FishingEvent
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import com.example.starborn.feature.fishing.viewmodel.FishingState
import com.example.starborn.feature.fishing.viewmodel.FishingUiState
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.ui.vfx.WeatherOverlay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun weatherForZone(zoneId: String?): String? = when (zoneId?.lowercase()) {
    "sector9_stream" -> "fog"
    "colony_pit_drain" -> "cave_drip"
    "spire_runoff" -> "rain"
    "foundry_cooling_runoff" -> "steam"
    "orbital_false_tide" -> "resonance"
    "singularity_ether_well" -> "starfall"
    else -> "fog"
}

private enum class FishingModalSheet {
    NONE,
    ROD,
    LURE,
    LOG,
    CATCH_DETAILS
}

@Composable
fun FishingScreen(
    viewModel: FishingViewModel,
    onBack: () -> Unit,
    onFishingComplete: (FishingResultPayload?) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    disableHaptics: Boolean = false,
    onFishingCue: (String) -> Unit = {},
    onCatchResult: (FishingResultPayload) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activeModal by remember { mutableStateOf(FishingModalSheet.NONE) }

    LaunchedEffect(uiState.fishingState) {
        if (uiState.fishingState != FishingState.SETUP && uiState.fishingState != FishingState.RESULT) {
            activeModal = FishingModalSheet.NONE
        }
        if (uiState.fishingState == FishingState.RESULT) {
            val catch = uiState.lastCatchResult
            val resultType = uiState.lastResult ?: MinigameResult.FAIL
            val success = resultType != MinigameResult.FAIL && (catch?.quantity ?: 0) > 0
            onCatchResult(
                FishingResultPayload(
                    itemId = catch?.itemId?.takeIf { it.isNotBlank() },
                    quantity = catch?.quantity,
                    message = catch?.message,
                    success = success,
                    secured = catch?.secured == true
                )
            )
        }
    }

    val zone = uiState.currentZone
    val context = LocalContext.current
    val sensorManager = remember(context) { context.getSystemService(SensorManager::class.java) }
    val hookDetector = remember(sensorManager) { HookMotionDetector(sensorManager) }
    val haptics = remember(context) { FishingHaptics(context) }

    val currentFishingCue by rememberUpdatedState(onFishingCue)
    LaunchedEffect(haptics, disableHaptics) {
        viewModel.events.collectLatest { event ->
            val cue = when (event) {
                FishingEvent.Cast -> "fishing_cast"
                FishingEvent.Nibble -> "fishing_splash"
                FishingEvent.Bite -> "fishing_bite"
                FishingEvent.Warning -> "fishing_warning"
                FishingEvent.LineSnap -> "fishing_snap"
                FishingEvent.CatchSuccess -> "fishing_catch"
            }
            currentFishingCue(cue)
            if (disableHaptics) return@collectLatest
            when (event) {
                FishingEvent.Cast -> Unit
                FishingEvent.Nibble -> haptics.nibble()
                FishingEvent.Bite -> haptics.bite()
                FishingEvent.Warning -> haptics.warning()
                FishingEvent.LineSnap -> haptics.lineSnap()
                FishingEvent.CatchSuccess -> haptics.catchSuccess()
            }
        }
    }

    LaunchedEffect(hookDetector, uiState.hookSensitivity) {
        hookDetector.threshold = uiState.hookSensitivity.thresholdValue
    }

    LaunchedEffect(hookDetector) {
        viewModel.setGyroAvailable(hookDetector.isSupported())
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(uiState.fishingState, uiState.motionEnabled, lifecycleOwner, hookDetector) {
        fun synchronize() {
            val resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            viewModel.setPaused(!resumed)
            if (resumed && uiState.motionEnabled && uiState.fishingState == FishingState.HOOKSET && hookDetector.isSupported()) {
                hookDetector.start { viewModel.onHookMotionDetected() }
            } else hookDetector.stop()
        }
        val observer = LifecycleEventObserver { _, _ -> synchronize() }
        lifecycleOwner.lifecycle.addObserver(observer)
        synchronize()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            hookDetector.stop()
            viewModel.setPaused(true)
        }
    }

    val biome = remember(zone?.id) { biomeStyleForZone(zone?.id) }

    FishingTheme {
        StationBackground(
            highContrastMode = highContrastMode,
            backgroundRes = R.drawable.beach_bg,
            backgroundPainter = rememberRoomBackgroundPainter(zone?.backgroundImage),
            vignetteRes = R.drawable.fishing_vignette
        ) {
            if (!highContrastMode) {
                WeatherOverlay(
                    weatherId = weatherForZone(zone?.id),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Fixed, non-scrolling single-screen layout across all states
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StationHeader(
                    title = zone?.name ?: "Fishing",
                    iconRes = R.drawable.fishing_icon,
                    onBack = onBack,
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets
                )

                // Compact 1-line "In these waters" species discovery strip (always fixed height)
                CompactZoneSpeciesBar(
                    zone = zone,
                    journal = uiState.journal,
                    itemDisplayName = viewModel::catchDisplayName,
                    highContrastMode = highContrastMode
                )

                // Center Water Viewport: constant weight(1f) so the water never jumps or shifts size
                Surface(
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = if (highContrastMode) Color(0xFF0B1119) else FishingInk.copy(alpha = 0.82f),
                    border = BorderStroke(
                        1.dp,
                        if (highContrastMode) Color(0xFF8AC4FF) else biome.bankRimTint.copy(alpha = 0.30f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        FishingWaterScene(
                            state = uiState,
                            onHook = viewModel::onHookButtonPressed,
                            showHeaderText = uiState.fishingState != FishingState.RESULT,
                            showTelemetryFooter = uiState.fishingState == FishingState.REELING
                        )

                        // Minimal Floating Catch / Miss Card over the lower water on RESULT
                        if (uiState.fishingState == FishingState.RESULT) {
                            CompactCatchOverlayCard(
                                state = uiState,
                                onOpenDetails = { activeModal = FishingModalSheet.CATCH_DETAILS },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }

                // Fixed-Height Bottom Control Dock: never shifts the water viewport when state changes
                FixedBottomFishingDock(
                    state = uiState,
                    onCast = viewModel::startFishing,
                    onStrike = viewModel::onHookButtonPressed,
                    onBeginReeling = viewModel::beginReeling,
                    onReelPressed = viewModel::onReelPressed,
                    onReelReleased = viewModel::onReelReleased,
                    onCancelCast = viewModel::cancelFishing,
                    onFishAgain = viewModel::resetFishing,
                    onQuickCast = viewModel::castAgain,
                    onOpenRodPicker = { activeModal = FishingModalSheet.ROD },
                    onOpenLurePicker = { activeModal = FishingModalSheet.LURE },
                    onOpenCatchLog = { activeModal = FishingModalSheet.LOG },
                    onFinish = {
                        val catch = uiState.lastCatchResult
                        val success = uiState.lastResult != MinigameResult.FAIL && (catch?.quantity ?: 0) > 0
                        onFishingComplete(
                            FishingResultPayload(
                                catch?.itemId?.takeIf { it.isNotBlank() },
                                catch?.quantity,
                                catch?.message,
                                success,
                                catch?.secured == true
                            )
                        )
                    },
                    largeTouchTargets = largeTouchTargets
                )
            }

            // On-demand lightweight modal sheets for Rod, Lure, Catch Log, and Catch Details
            when (activeModal) {
                FishingModalSheet.ROD -> GearPickerModal(
                    title = "Select Rod",
                    items = uiState.availableRods,
                    selectedId = uiState.selectedRod?.id,
                    itemTitle = { it.name },
                    itemSubtitle = { it.handlingSummary() },
                    itemDescription = { it.description },
                    onSelect = {
                        viewModel.selectRod(it)
                        activeModal = FishingModalSheet.NONE
                    },
                    motionSupported = uiState.motionSupported,
                    motionEnabled = uiState.motionEnabled,
                    onToggleMotion = viewModel::toggleMotion,
                    hookSensitivity = uiState.hookSensitivity,
                    onSelectSensitivity = viewModel::selectSensitivity,
                    onDismiss = { activeModal = FishingModalSheet.NONE },
                    highContrastMode = highContrastMode
                )
                FishingModalSheet.LURE -> GearPickerModal(
                    title = "Select Lure",
                    items = uiState.availableLures,
                    selectedId = uiState.selectedLure?.id,
                    itemTitle = { it.name },
                    itemSubtitle = { it.catchSummary(uiState.currentZone, viewModel::catchDisplayName) },
                    itemDescription = { it.description },
                    onSelect = {
                        viewModel.selectLure(it)
                        activeModal = FishingModalSheet.NONE
                    },
                    onDismiss = { activeModal = FishingModalSheet.NONE },
                    highContrastMode = highContrastMode
                )
                FishingModalSheet.LOG -> CatchLogModal(
                    entries = uiState.journal,
                    onDismiss = { activeModal = FishingModalSheet.NONE }
                )
                FishingModalSheet.CATCH_DETAILS -> CatchDetailsModal(
                    catch = uiState.lastCatchResult,
                    onDismiss = { activeModal = FishingModalSheet.NONE }
                )
                FishingModalSheet.NONE -> Unit
            }
        }
    }
}

@Composable
fun FishingRoute(
    viewModel: FishingViewModel,
    onBack: () -> Unit,
    onFinish: (FishingResultPayload?) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    disableHaptics: Boolean = false,
    onFishingCue: (String) -> Unit = {},
    onCatchResult: (FishingResultPayload) -> Unit = {}
) {
    FishingScreen(
        viewModel = viewModel,
        onBack = onBack,
        onFishingComplete = onFinish,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets,
        disableHaptics = disableHaptics,
        onFishingCue = onFishingCue,
        onCatchResult = onCatchResult
    )
}

@Composable
private fun CompactZoneSpeciesBar(
    zone: com.example.starborn.domain.fishing.FishingZone?,
    journal: List<FishingJournalEntry>,
    itemDisplayName: (String) -> String,
    highContrastMode: Boolean
) {
    val zoneJournal = remember(zone?.id, journal) {
        journal.firstOrNull { it.zoneId == zone?.id }?.species?.associateBy { it.itemId }.orEmpty()
    }
    val catches = zone?.catches.orEmpty()
    if (catches.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (highContrastMode) Color(0xFF0A1320) else FishingInk.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Waters:",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = FishingGold
            )
            catches.forEach { catchDef ->
                val record = zoneJournal[catchDef.itemId]
                val isNative = catchDef.isNativeFish()
                val isDiscovered = !isNative || record?.caught == true
                val fishName = if (isDiscovered) {
                    itemDisplayName(catchDef.itemId).ifBlank {
                        catchDef.itemId.replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    }
                } else {
                    "???"
                }
                val statusGlyph = when {
                    !isNative -> "◇"
                    record?.clean == true -> "★"
                    record?.caught == true -> "✓"
                    else -> "•"
                }
                val rarityColor = when (catchDef.rarity) {
                    FishingRarity.COMMON -> Color(0xFFB0BEC5)
                    FishingRarity.UNCOMMON -> Color(0xFF81C784)
                    FishingRarity.RARE -> Color(0xFF64B5F6)
                    FishingRarity.EPIC -> Color(0xFFCE93D8)
                    FishingRarity.EXOTIC -> Color(0xFFFFD54F)
                    else -> Color(0xFF90A4AE)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF102730),
                    border = BorderStroke(0.5.dp, rarityColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "$statusGlyph $fishName",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        color = if (isDiscovered) FishingMist else FishingMist.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Constant-height bottom dock so transitioning between SETUP -> WAITING -> HOOKSET -> REELING -> RESULT
 * never shifts the water canvas by a single pixel.
 */
@Composable
private fun FixedBottomFishingDock(
    state: FishingUiState,
    onCast: () -> Unit,
    onStrike: () -> Unit,
    onBeginReeling: () -> Unit,
    onReelPressed: () -> Unit,
    onReelReleased: () -> Unit,
    onCancelCast: () -> Unit,
    onFishAgain: () -> Unit,
    onQuickCast: () -> Unit,
    onOpenRodPicker: () -> Unit,
    onOpenLurePicker: () -> Unit,
    onOpenCatchLog: () -> Unit,
    onFinish: () -> Unit,
    largeTouchTargets: Boolean
) {
    val primarySlotHeight = if (largeTouchTargets) 88.dp else 76.dp
    val secondaryRowHeight = if (largeTouchTargets) 48.dp else 42.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Slot 1: Fixed-Height Primary Action Control (Cast / Set Hook / Ready / Reel Drum / Recast)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(primarySlotHeight)
        ) {
            when (state.fishingState) {
                FishingState.SETUP -> {
                    val canCast = state.selectedRod != null && state.selectedLure != null
                    Surface(
                        onClick = onCast,
                        enabled = canCast,
                        shape = RoundedCornerShape(20.dp),
                        color = if (canCast) Color(0xFF1C4E5E) else Color(0xFF10242C),
                        border = BorderStroke(1.8.dp, if (canCast) FishingGold else Color(0xFF35505A)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Cast Line",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (canCast) FishingMist else FishingMist.copy(alpha = 0.45f)
                            )
                        }
                    }
                }

                FishingState.WAITING -> {
                    // Always-present disabled "Set hook" button so the UI never jumps when the bite hits
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0E222A),
                        border = BorderStroke(1.2.dp, Color(0xFF2E4B56)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Set hook",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FishingMist.copy(alpha = 0.38f)
                            )
                            Text(
                                text = "Wait for the plunge...",
                                style = MaterialTheme.typography.bodySmall,
                                color = FishingMist.copy(alpha = 0.32f)
                            )
                        }
                    }
                }

                FishingState.HOOKSET -> {
                    val remainingFrac = if (state.hookState?.practiceHook == true) 1f
                    else ((state.hookState?.timeRemainingMs ?: 2400L) / 2400f).coerceIn(0f, 1f)
                    Surface(
                        onClick = onStrike,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF285866),
                        border = BorderStroke(2.2.dp, FishingGold),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Set hook",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = FishingMist
                            )
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { remainingFrac },
                                modifier = Modifier
                                    .fillMaxWidth(0.78f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = FishingGold,
                                trackColor = Color(0xFF102830)
                            )
                        }
                    }
                }

                FishingState.READY -> {
                    Surface(
                        onClick = onBeginReeling,
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E5260),
                        border = BorderStroke(1.8.dp, FishingGold),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "I'm ready — start reeling",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FishingMist
                            )
                        }
                    }
                }

                FishingState.REELING -> {
                    val reelState = state.reelState
                    val isReeling = reelState?.isReeling == true
                    val progress by animateFloatAsState(reelState?.progress ?: 0f, tween(110), label = "drumProgress")
                    val tension by animateFloatAsState((reelState?.tension ?: 0f).coerceIn(0f, 1f), tween(110), label = "drumTension")
                    val isUrgent = reelState?.phase == FishingFightPhase.SURGE ||
                        reelState?.phase == FishingFightPhase.WARNING ||
                        tension >= 0.75f
                    val drumBorderColor = when {
                        isUrgent -> FishingCoral
                        isReeling -> FishingGold
                        else -> Color(0xFF8ED9D3).copy(alpha = 0.75f)
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("fishing-reel-control")
                            .semantics {
                                role = Role.Button
                                onClick(label = "Toggle reeling and slack") {
                                    if (reelState?.isReeling == true) onReelReleased() else onReelPressed()
                                    true
                                }
                            }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown()
                                    onReelPressed()
                                    try {
                                        waitForUpOrCancellation()
                                    } finally {
                                        onReelReleased()
                                    }
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isUrgent && isReeling -> Color(0xFF441E20)
                            isReeling -> Color(0xFF1E4B56)
                            else -> Color(0xFF112A34)
                        },
                        border = BorderStroke(if (isReeling || isUrgent) 2.dp else 1.4.dp, drumBorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(54.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val strokeOuter = 5.dp.toPx()
                                    val strokeInner = 4.dp.toPx()
                                    val outerInset = strokeOuter / 2f
                                    val innerInset = strokeOuter + 4.dp.toPx()

                                    drawArc(
                                        color = Color.White.copy(alpha = 0.14f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = Offset(outerInset, outerInset),
                                        size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                                        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = Color(0xFF8ED9D3),
                                        startAngle = -90f,
                                        sweepAngle = (progress * 360f).coerceIn(0f, 360f),
                                        useCenter = false,
                                        topLeft = Offset(outerInset, outerInset),
                                        size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                                        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                                    )

                                    val tensionArcColor = when {
                                        tension >= 0.75f -> FishingCoral
                                        tension >= 0.45f -> FishingGold
                                        else -> Color(0xFF81C784)
                                    }
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.10f),
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = Offset(innerInset, innerInset),
                                        size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                                        style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = tensionArcColor,
                                        startAngle = -90f,
                                        sweepAngle = (tension * 360f).coerceIn(0f, 360f),
                                        useCenter = false,
                                        topLeft = Offset(innerInset, innerInset),
                                        size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                                        style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                                    )

                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val hubR = size.minDimension * 0.18f
                                    val spokeRotation = progress * 720f
                                    repeat(6) { idx ->
                                        val angleRad = ((idx * 60f + spokeRotation) * PI / 180.0).toFloat()
                                        val spokeEnd = center + Offset(cos(angleRad) * hubR, sin(angleRad) * hubR)
                                        drawLine(
                                            color = if (isReeling) FishingGold else FishingMist.copy(alpha = 0.45f),
                                            start = center,
                                            end = spokeEnd,
                                            strokeWidth = 1.6.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                    drawCircle(
                                        color = if (isReeling) FishingGold else FishingMist,
                                        radius = 3.dp.toPx(),
                                        center = center
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = when {
                                        isReeling && reelState?.salvage == true -> "Retrieving..."
                                        isReeling -> "Reeling..."
                                        reelState?.salvage == true -> "Hold to Retrieve"
                                        else -> "Hold to Reel"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUrgent) FishingCoral else FishingMist
                                )
                                Text(
                                    text = when {
                                        isUrgent -> "Release thumb to give slack!"
                                        isReeling -> "Distance ${(progress * 100).toInt()}% · Keep tension safe"
                                        else -> "Hold anywhere on this drum"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FishingMist.copy(alpha = 0.80f)
                                )
                            }
                        }
                    }
                }

                FishingState.RESULT -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = onQuickCast,
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1C4E5E),
                            border = BorderStroke(1.8.dp, FishingGold),
                            modifier = Modifier.weight(1f).fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "Quick cast",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FishingMist
                                )
                            }
                        }
                        Surface(
                            onClick = onFishAgain,
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF122C36),
                            border = BorderStroke(1.2.dp, Color(0xFF5D8996)),
                            modifier = Modifier.weight(1f).fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "Change gear",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FishingMist
                                )
                            }
                        }
                    }
                }
            }
        }

        // Slot 2: Fixed-Height Secondary Bar (Gear & Log pills in SETUP; Cancel/Leave in active/result)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(secondaryRowHeight)
        ) {
            when (state.fishingState) {
                FishingState.SETUP -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactDockPill(
                            label = "Rod",
                            onClick = onOpenRodPicker,
                            modifier = Modifier.weight(1f).fillMaxSize()
                        )
                        CompactDockPill(
                            label = "Lure",
                            onClick = onOpenLurePicker,
                            modifier = Modifier.weight(1f).fillMaxSize()
                        )
                        Surface(
                            onClick = onOpenCatchLog,
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0F252E),
                            border = BorderStroke(1.dp, FishingGold.copy(alpha = 0.55f)),
                            modifier = Modifier.size(secondaryRowHeight)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Catch Log",
                                    tint = FishingGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                FishingState.RESULT -> {
                    CompactDockPill(
                        label = "Pack up",
                        onClick = onFinish,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    CompactDockPill(
                        label = if (state.fishingState == FishingState.REELING) "Put the line down" else "Cancel cast",
                        onClick = onCancelCast,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactDockPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F252E),
        border = BorderStroke(1.dp, Color(0xFF3E616E)),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = FishingMist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Minimal, low-clutter catch result overlay card with an inline "Details" action.
 */
@Composable
private fun CompactCatchOverlayCard(
    state: FishingUiState,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catch = state.lastCatchResult
    val success = state.lastResult != MinigameResult.FAIL && (catch?.quantity ?: 0) > 0

    Surface(
        onClick = { if (success && catch != null) onOpenDetails() },
        enabled = success && catch != null,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xEE0D222B),
        border = BorderStroke(1.4.dp, if (success) FishingGold.copy(alpha = 0.75f) else FishingCoral.copy(alpha = 0.65f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (success && catch != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${catch.quantity} × ${catch.displayName.ifBlank { catch.itemId.replace('_', ' ') }}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = FishingMist,
                        modifier = Modifier.weight(1f)
                    )
                    if (catch.cleanCatch) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1F4742),
                            border = BorderStroke(1.dp, FishingGold.copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = "★ Clean",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FishingGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    catch.rarity?.let { rarity ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF15313B),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                        ) {
                            Text(
                                text = rarity.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FishingMist,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = catch.rewards.firstOrNull() ?: "Added to inventory",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (catch.rewards.isNotEmpty()) FishingGold else FishingMist.copy(alpha = 0.72f),
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = FishingGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Details ›",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = FishingGold
                        )
                    }
                }
            } else {
                Text(
                    text = "The water settles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FishingCoral
                )
                Text(
                    text = catch?.message ?: "It slipped away. Cast again whenever you're ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FishingMist
                )
            }
        }
    }
}

@Composable
private fun <T> GearPickerModal(
    title: String,
    items: List<T>,
    selectedId: String?,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String,
    itemDescription: (T) -> String?,
    onSelect: (T) -> Unit,
    motionSupported: Boolean = false,
    motionEnabled: Boolean = false,
    onToggleMotion: (() -> Unit)? = null,
    hookSensitivity: com.example.starborn.feature.fishing.viewmodel.HookSensitivity = com.example.starborn.feature.fishing.viewmodel.HookSensitivity.MEDIUM,
    onSelectSensitivity: ((com.example.starborn.feature.fishing.viewmodel.HookSensitivity) -> Unit)? = null,
    onDismiss: () -> Unit,
    highContrastMode: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (highContrastMode) Color(0xFF0B1119) else Color(0xFF0C2029),
            border = BorderStroke(1.2.dp, Color(0xFF4C7A89)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FishingGold
                    )
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close", color = FishingMist)
                    }
                }

                items.forEach { item ->
                    val name = itemTitle(item)
                    val isSelected = when (item) {
                        is com.example.starborn.domain.fishing.FishingRod -> item.id == selectedId
                        is com.example.starborn.domain.fishing.FishingLure -> item.id == selectedId
                        else -> false
                    }
                    GearCard(
                        title = name,
                        subtitle = itemSubtitle(item),
                        description = itemDescription(item),
                        selected = isSelected,
                        onClick = { onSelect(item) },
                        highContrastMode = highContrastMode,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (onToggleMotion != null) {
                    OutlinedButton(
                        onClick = onToggleMotion,
                        enabled = motionSupported,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (motionEnabled) "Motion hook: On" else "Motion hook: Off · touch ready",
                            color = FishingMist
                        )
                    }
                    if (motionEnabled && onSelectSensitivity != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.example.starborn.feature.fishing.viewmodel.HookSensitivity.entries.forEach { sens ->
                                val selected = hookSensitivity == sens
                                Surface(
                                    onClick = { onSelectSensitivity(sens) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Color(0xFF1F4E5C) else Color(0xFF112832),
                                    border = BorderStroke(1.dp, if (selected) FishingGold else Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = sens.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FishingMist,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

@Composable
private fun CatchDetailsModal(
    catch: com.example.starborn.domain.fishing.FishingResult?,
    onDismiss: () -> Unit
) {
    if (catch == null) return
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0C2029),
            border = BorderStroke(1.2.dp, FishingGold.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = catch.displayName.ifBlank { catch.itemId.replace('_', ' ') },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FishingGold
                    )
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close", color = FishingMist)
                    }
                }
                catch.flavorText?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = FishingMist)
                }
                if (catch.cleanCatch) {
                    Text(
                        text = "Clean catch · recorded in your catch log",
                        style = MaterialTheme.typography.labelLarge,
                        color = FishingGold
                    )
                }
                catch.cleanCatchProgress?.let { prog ->
                    Text(
                        text = "Clean-catch mastery: $prog/3 toward +1 bonus fish",
                        style = MaterialTheme.typography.bodySmall,
                        color = FishingMist
                    )
                }
                if (catch.uses.isNotEmpty()) {
                    Text("Uses", style = MaterialTheme.typography.labelLarge, color = FishingGold)
                    catch.uses.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall, color = FishingMist)
                    }
                }
                if (catch.rewards.isNotEmpty()) {
                    Text("Milestones", style = MaterialTheme.typography.labelLarge, color = FishingGold)
                    catch.rewards.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall, color = FishingGold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatchLogModal(
    entries: List<FishingJournalEntry>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0C2029),
            border = BorderStroke(1.2.dp, Color(0xFF4C7A89)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Catch Log",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FishingGold
                    )
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close", color = FishingMist)
                    }
                }
                FishingJournal(entries = entries, initiallyExpanded = true)
            }
        }
    }
}

@Composable
fun FishingJournal(entries: List<FishingJournalEntry>, initiallyExpanded: Boolean = false) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initiallyExpanded) }
    val waters = entries.count { entry -> entry.species.any { it.caught } }
    val cleanWaters = entries.count { entry -> entry.species.any { it.clean } }
    val species = entries.flatMap { it.species }.groupBy { it.itemId }
    val caughtSpecies = species.values.count { records -> records.any { it.caught } }
    val cleanSpecies = species.values.count { records -> records.any { it.clean } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(
                "Catch Log · $caughtSpecies/${species.size} species · $waters/${entries.size} waters",
                color = FishingMist
            )
        }
        if (expanded) {
            Text("Collection goals", style = MaterialTheme.typography.titleMedium, color = FishingGold)
            Text("First clean species: Glimmer Lure · ${cleanSpecies.coerceAtMost(1)}/1", style = MaterialTheme.typography.bodyMedium, color = FishingMist)
            Text("Three clean species: Ghost-Signal Lure · ${cleanSpecies.coerceAtMost(3)}/3", style = MaterialTheme.typography.bodyMedium, color = FishingMist)
            Text("Master Angler: fish in every water for the exclusive Six-Water Lure · $waters/${entries.size}", style = MaterialTheme.typography.bodyMedium, color = FishingMist)
            Text("Every species: Angler's Field Medallion · $caughtSpecies/${species.size}", style = MaterialTheme.typography.bodyMedium, color = FishingMist)
            Text("Clean-Water Angler: a clean catch in every water · $cleanWaters/${entries.size}", style = MaterialTheme.typography.bodyMedium, color = FishingMist)
            entries.forEach { entry ->
                val caught = entry.species.count { it.caught }
                Text("${entry.name} · $caught/${entry.species.size}", fontWeight = FontWeight.Bold, color = FishingGold)
                entry.species.forEach { record ->
                    Text(
                        if (record.caught) "${record.name}${if (record.clean) " · Clean catch" else " · Caught"}"
                        else "Undiscovered species",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FishingMist
                    )
                }
            }
        }
    }
}

@Composable
private fun GearCard(
    title: String,
    subtitle: String,
    description: String?,
    selected: Boolean,
    onClick: () -> Unit,
    highContrastMode: Boolean,
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (highContrastMode) Color(0xFF111A25) else if (selected) Color(0xFF1B4552) else Color(0xFF102832)
    val border = BorderStroke(1.dp, if (selected) FishingGold else Color.White.copy(alpha = 0.16f))
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (selected) 3.dp else 1.dp,
        color = surfaceColor,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FishingMist
                )
                if (selected) {
                    Text(
                        text = "Equipped",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FishingGold
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = FishingMist.copy(alpha = 0.85f)
            )
            description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = FishingMist.copy(alpha = 0.70f)
                )
            }
        }
    }
}
