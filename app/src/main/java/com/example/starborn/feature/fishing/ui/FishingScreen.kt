package com.example.starborn.feature.fishing.ui

import com.example.starborn.ui.fishing.*

import com.example.starborn.domain.fishing.handlingSummary
import com.example.starborn.domain.fishing.catchSummary

import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.starborn.R
import com.example.starborn.domain.fishing.MinigameResult
import com.example.starborn.domain.fishing.FishingFightPhase
import com.example.starborn.domain.fishing.FishingJournalEntry
import com.example.starborn.domain.fishing.isNativeFish
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import com.example.starborn.feature.common.ui.StationBackground
import com.example.starborn.feature.common.ui.StationHeader
import com.example.starborn.feature.fishing.sensors.HookMotionDetector
import com.example.starborn.feature.fishing.viewmodel.FishingHookState
import com.example.starborn.feature.fishing.viewmodel.FishingReelState
import com.example.starborn.feature.fishing.viewmodel.FishingState
import com.example.starborn.feature.fishing.viewmodel.FishingUiState
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel
import com.example.starborn.feature.fishing.viewmodel.FishingController.FishingEvent
import com.example.starborn.feature.fishing.viewmodel.FishingWaitingState
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import kotlinx.coroutines.flow.collectLatest

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
    val active = uiState.fishingState in setOf(FishingState.WAITING, FishingState.HOOKSET, FishingState.READY, FishingState.REELING)
    val pageScroll = rememberScrollState()
    val fightScroll = rememberScrollState()
    LaunchedEffect(uiState.fishingState) {
        pageScroll.scrollTo(0)
        fightScroll.scrollTo(0)
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
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp

    val context = LocalContext.current
    val sensorManager = remember(context) { context.getSystemService(SensorManager::class.java) }
    val hookDetector = remember(sensorManager) { HookMotionDetector(sensorManager) }
    val haptics = remember(context) { FishingHaptics(context) }

    val currentFishingCue by rememberUpdatedState(onFishingCue)
    LaunchedEffect(haptics, disableHaptics) {
        viewModel.events.collectLatest { event ->
            val cue = when (event) {
                FishingEvent.Cast -> "fishing_cast"
                FishingEvent.Nibble, FishingEvent.LineSnap -> "fishing_splash"
                FishingEvent.Bite -> "fishing_bite"
                FishingEvent.CatchSuccess -> "fishing_catch"
                FishingEvent.Warning -> null
            }
            cue?.let(currentFishingCue)
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
            val active = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            viewModel.setPaused(!active)
            if (active && uiState.motionEnabled && uiState.fishingState == FishingState.HOOKSET && hookDetector.isSupported()) {
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

    FishingTheme {
    StationBackground(
        highContrastMode = highContrastMode,
        backgroundRes = R.drawable.beach_bg,
        backgroundPainter = rememberRoomBackgroundPainter(zone?.backgroundImage),
        vignetteRes = R.drawable.fishing_vignette
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(if (active) Modifier else Modifier.verticalScroll(pageScroll))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StationHeader(
                title = zone?.name ?: "Fishing",
                iconRes = R.drawable.fishing_icon,
                onBack = onBack,
                highContrastMode = highContrastMode,
                largeTouchTargets = largeTouchTargets
            )
            if (uiState.fishingState == FishingState.SETUP) {
                FishingHero(
                    zone = zone,
                    state = uiState.fishingState,
                    journal = uiState.journal,
                    itemDisplayName = viewModel::catchDisplayName,
                    highContrastMode = highContrastMode
                )
            }
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().then(if (active) Modifier.weight(1f) else Modifier),
                color = if (highContrastMode) Color(0xFF0B1119) else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (active) Modifier.fillMaxSize() else Modifier)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    when (uiState.fishingState) {
                        FishingState.SETUP -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (uiState.needsReelBriefing) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "Tutorial · Fishing",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Cast, then swipe up or tap to hook. Hold to reel; release when the fish surges. Your first bite and reel lesson wait for you.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            FishingSetupSection(
                                state = uiState,
                                itemDisplayName = viewModel::catchDisplayName,
                                onSelectRod = viewModel::selectRod,
                                onSelectLure = viewModel::selectLure,
                                onSelectSensitivity = viewModel::selectSensitivity,
                                onStart = viewModel::startFishing,
                                onCancel = onBack,
                                highContrastMode = highContrastMode,
                                largeTouchTargets = largeTouchTargets,
                                buttonHeight = buttonHeight
                            )
                            OutlinedButton(onClick = viewModel::toggleMotion, enabled = uiState.motionSupported) {
                                Text(if (uiState.motionEnabled) "Motion hook: On" else "Motion hook: Off · touch ready")
                            }
                            var showLog by remember { mutableStateOf(false) }
                            OutlinedButton(onClick = { showLog = !showLog }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (showLog) "Close catch log" else "Catch log")
                            }
                            if (showLog) FishingJournal(uiState.journal)
                        }

                        FishingState.READY, FishingState.WAITING, FishingState.HOOKSET, FishingState.REELING ->
                            FishingWaterScene(uiState, viewModel::onHookButtonPressed)

                        FishingState.RESULT -> FishingResultSection(
                            state = uiState,
                            onFishAgain = viewModel::resetFishing,
                            onFinish = onFishingComplete,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            buttonHeight = buttonHeight
                        )
                    }
                }
            }
            if (active) FishingActiveControls(
                state = uiState,
                onStrike = viewModel::onHookButtonPressed,
                onBeginReeling = viewModel::beginReeling,
                onReelPressed = viewModel::onReelPressed,
                onReelReleased = viewModel::onReelReleased,
                onCancel = viewModel::cancelFishing,
                largeTouchTargets = largeTouchTargets,
            )
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
fun FishingJournal(entries: List<FishingJournalEntry>, initiallyExpanded: Boolean = false) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initiallyExpanded) }
    val waters = entries.count { entry -> entry.species.any { it.caught } }
    val cleanWaters = entries.count { entry -> entry.species.any { it.clean } }
    val species = entries.flatMap { it.species }.groupBy { it.itemId }
    val caughtSpecies = species.values.count { records -> records.any { it.caught } }
    val cleanSpecies = species.values.count { records -> records.any { it.clean } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Catch Log · $caughtSpecies/${species.size} species · $waters/${entries.size} waters")
        }
        if (expanded) {
            Text("Collection goals", style = MaterialTheme.typography.titleMedium)
            Text("First clean species: Glimmer Lure · ${cleanSpecies.coerceAtMost(1)}/1", style = MaterialTheme.typography.bodyMedium)
            Text("Three clean species: Ghost-Signal Lure · ${cleanSpecies.coerceAtMost(3)}/3", style = MaterialTheme.typography.bodyMedium)
            Text("Master Angler: fish in every water for the exclusive Six-Water Lure · $waters/${entries.size}", style = MaterialTheme.typography.bodyMedium)
            Text("Every species: Angler's Field Medallion · $caughtSpecies/${species.size}", style = MaterialTheme.typography.bodyMedium)
            Text("Clean-Water Angler: a clean catch in every water · $cleanWaters/${entries.size}", style = MaterialTheme.typography.bodyMedium)
            entries.forEach { entry ->
                val caught = entry.species.count { it.caught }
                Text("${entry.name} · $caught/${entry.species.size}", fontWeight = FontWeight.Bold)
                entry.species.forEach { record ->
                    Text(if (record.caught) "${record.name}${if (record.clean) " · Clean catch" else " · Caught"}"
                        else "Undiscovered species", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("Records remain after cooking or selling. The Colony drain pool becomes fishable after the Sector 9 pod examination; return through Astra when travel is available.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FishingHero(
    zone: com.example.starborn.domain.fishing.FishingZone?,
    state: FishingState,
    journal: List<FishingJournalEntry>,
    itemDisplayName: (String) -> String,
    highContrastMode: Boolean
) {
    val zoneJournal = remember(zone?.id, journal) {
        journal.firstOrNull { it.zoneId == zone?.id }?.species?.associateBy { it.itemId }.orEmpty()
    }
    val gradient = Brush.linearGradient(
        colors = if (highContrastMode) {
            listOf(Color(0xFF0A1320), Color(0xFF11263A))
        } else {
            listOf(Color(0xFF0A1A2D), Color(0xFF0F2F46), Color(0x000A1A2D))
        }
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp,
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (highContrastMode) Color(0xFF1D8BF2) else Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = zone?.name ?: "Unknown Waters",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = zone?.description ?: "Watch the water for a bite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.item_icon_fishing),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(40.dp)
                            .padding(start = 8.dp)
                    )
                }

                // Native Species & Salvage Preview in this Water Body
                val catches = zone?.catches.orEmpty()
                if (catches.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "In these waters:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = if (highContrastMode) Color(0xFF8AC4FF) else MaterialTheme.colorScheme.primary
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
                                else -> "🐟"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$statusGlyph $fishName",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = if (isDiscovered) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.65f)
                                    )
                                    val rarityColor = when (catchDef.rarity) {
                                        com.example.starborn.domain.fishing.FishingRarity.COMMON -> Color(0xFFAAAAAA)
                                        com.example.starborn.domain.fishing.FishingRarity.UNCOMMON -> Color(0xFF81C784)
                                        com.example.starborn.domain.fishing.FishingRarity.RARE -> Color(0xFF64B5F6)
                                        com.example.starborn.domain.fishing.FishingRarity.EPIC -> Color(0xFFBA68C8)
                                        com.example.starborn.domain.fishing.FishingRarity.EXOTIC -> Color(0xFFFFD54F)
                                        else -> Color.Gray
                                    }
                                    Text(
                                        text = "• ${catchDef.rarity.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                        color = rarityColor
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
private fun FishingPhaseStepper(state: FishingState, highContrastMode: Boolean) {
    val steps = listOf("Setup", "Wait", "Hook", "Reel", "Results")
    val current = state.stepIndex()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        steps.forEachIndexed { index, label ->
            val active = index <= current
            val color = if (highContrastMode) Color(0xFF0D1724) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            val accent = if (highContrastMode) Color(0xFF8AC4FF) else MaterialTheme.colorScheme.primary
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = if (active) 2.dp else 0.dp,
                color = if (active) color.copy(alpha = 0.9f) else color
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) accent else if (highContrastMode) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun FishingState.stepIndex(): Int = when (this) {
    FishingState.SETUP -> 0
    FishingState.WAITING -> 1
    FishingState.HOOKSET, FishingState.READY -> 2
    FishingState.REELING -> 3
    FishingState.RESULT -> 4
}

private fun FishingState.label(): String = when (this) {
    FishingState.SETUP -> "Setup"
    FishingState.WAITING -> "Waiting"
    FishingState.HOOKSET -> "Hook"
    FishingState.READY -> "Learn to reel"
    FishingState.REELING -> "Reel"
    FishingState.RESULT -> "Results"
}

@Composable
private fun FishingSetupSection(
    state: FishingUiState,
    itemDisplayName: (String) -> String,
    onSelectRod: (com.example.starborn.domain.fishing.FishingRod) -> Unit,
    onSelectLure: (com.example.starborn.domain.fishing.FishingLure) -> Unit,
    onSelectSensitivity: (com.example.starborn.feature.fishing.viewmodel.HookSensitivity) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStart,
                enabled = state.selectedRod != null && state.selectedLure != null,
                modifier = Modifier.weight(1f).heightIn(min = maxOf(buttonHeight, 48.dp))
            ) {
                Text("Cast Line", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.heightIn(min = maxOf(buttonHeight, 48.dp))
            ) {
                Text("Leave")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Rod", style = MaterialTheme.typography.labelLarge)
            if (state.availableRods.isEmpty()) {
                Text(
                    text = "You don't have any fishing rods.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.availableRods.forEach { rod ->
                        val subtitle = rod.handlingSummary()
                        GearCard(
                            title = rod.name,
                            subtitle = subtitle,
                            description = rod.description,
                            selected = state.selectedRod?.id == rod.id,
                            onClick = { onSelectRod(rod) },
                            highContrastMode = highContrastMode,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Lure", style = MaterialTheme.typography.labelLarge)
            if (state.availableLures.isEmpty()) {
                Text(
                    text = "You don't have any lures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.availableLures.forEach { lure ->
                        val subtitle = lure.catchSummary(state.currentZone, itemDisplayName)
                        GearCard(
                            title = lure.name,
                            subtitle = subtitle,
                            description = lure.description,
                            selected = state.selectedLure?.id == lure.id,
                            onClick = { onSelectLure(lure) },
                            highContrastMode = highContrastMode,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            }
        }

        if (state.motionEnabled) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Motion hook sensitivity", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.starborn.feature.fishing.viewmodel.HookSensitivity.entries.forEach { sensitivity ->
                    val selected = state.hookSensitivity == sensitivity
                    val surfaceColor = if (selected) {
                        if (highContrastMode) Color(0xFF1D5A91) else MaterialTheme.colorScheme.primaryContainer
                    } else {
                        if (highContrastMode) Color(0xFF111A25) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                    val textColor = if (selected) {
                        if (highContrastMode) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        if (highContrastMode) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val border = if (selected) BorderStroke(1.dp, if (highContrastMode) Color(0xFF8AC4FF) else MaterialTheme.colorScheme.primary) else null
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectSensitivity(sensitivity) },
                        shape = RoundedCornerShape(10.dp),
                        color = surfaceColor,
                        border = border
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = sensitivity.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = textColor
                                )
                                Text(
                                    text = when(sensitivity) {
                                        com.example.starborn.feature.fishing.viewmodel.HookSensitivity.HIGH -> "3.0 m/s²"
                                        com.example.starborn.feature.fishing.viewmodel.HookSensitivity.MEDIUM -> "4.5 m/s²"
                                        com.example.starborn.feature.fishing.viewmodel.HookSensitivity.LOW -> "6.0 m/s²"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.75f)
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
private fun FishingActiveControls(
    state: FishingUiState,
    onStrike: () -> Unit,
    onBeginReeling: () -> Unit,
    onReelPressed: () -> Unit,
    onReelReleased: () -> Unit,
    onCancel: () -> Unit,
    largeTouchTargets: Boolean
) {
    val reelState = state.reelState
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.fishingState == FishingState.READY) {
            Button(onClick = onBeginReeling, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("I'm ready — start reeling", fontWeight = FontWeight.Bold)
            }
        }
        if (state.fishingState == FishingState.HOOKSET) {
            Button(onClick = onStrike, modifier = Modifier.fillMaxWidth().heightIn(min = if (largeTouchTargets) 64.dp else 52.dp)) {
                Text("Set hook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        if (state.fishingState == FishingState.REELING) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fishing-reel-control")
                    .heightIn(min = if (largeTouchTargets) 76.dp else 60.dp)
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
                            try { waitForUpOrCancellation() } finally { onReelReleased() }
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                color = if (reelState?.isReeling == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        when {
                            reelState?.isReeling == true && reelState.salvage -> "Retrieving..."
                            reelState?.isReeling == true -> "Reeling..."
                            reelState?.salvage == true -> "Hold to Retrieve"
                            else -> "Hold to Reel"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (reelState?.isReeling == true) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = if (largeTouchTargets) 52.dp else 48.dp)) {
            Text(if (state.fishingState == FishingState.REELING) "Put the line down" else "Cancel cast")
        }
    }
}

@Composable
private fun FishingResultSection(
    state: FishingUiState,
    onFishAgain: () -> Unit,
    onFinish: (FishingResultPayload?) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
) {
    val catch = state.lastCatchResult
    val success = state.lastResult != MinigameResult.FAIL && (catch?.quantity ?: 0) > 0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (success) "Back on shore" else "The water settles",
            style = MaterialTheme.typography.headlineSmall, color = FishingGold)
        if (success && catch != null) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF18353F), border = BorderStroke(1.dp, FishingGold.copy(alpha = .5f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${catch.quantity} × ${catch.displayName.ifBlank { catch.itemId.replace('_', ' ') }}",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = FishingGold)
                    catch.flavorText?.takeIf { it.isNotBlank() }?.let { Text(it, color = FishingMist) }
                    Text(if (catch.secured) "Secured in your inventory." else "Catch ready to collect.", color = FishingMist)
                    if (catch.cleanCatch) Text("Clean catch · recorded in your catch log", style = MaterialTheme.typography.labelLarge, color = FishingGold)
                }
            }
            catch.uses.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            catch.rewards.forEach { Text(it, color = FishingGold) }
        } else Text(catch?.message ?: "It slipped away. You can cast again whenever you're ready.")
        Button(onClick = onFishAgain, modifier = Modifier.fillMaxWidth().heightIn(min = if (largeTouchTargets) 56.dp else 48.dp)) {
            Text("Cast again", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = { onFinish(FishingResultPayload(catch?.itemId?.takeIf { it.isNotBlank() },
            catch?.quantity, catch?.message, success, catch?.secured == true)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Return to exploration") }
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
    val surfaceColor = if (highContrastMode) Color(0xFF111A25) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (selected) 0.75f else 0.55f)
    val border = if (selected) BorderStroke(1.dp, if (highContrastMode) Color(0xFF8AC4FF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
    Surface(
        modifier = modifier
            .clickable(enabled = !selected, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (selected) 3.dp else 1.dp,
        color = surfaceColor,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highContrastMode) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
