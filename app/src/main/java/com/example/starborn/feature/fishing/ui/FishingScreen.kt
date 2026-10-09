package com.example.starborn.feature.fishing.ui

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
    onFishingCue: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val active = uiState.fishingState in setOf(FishingState.WAITING, FishingState.HOOKSET, FishingState.REELING)
    val pageScroll = rememberScrollState()
    val fightScroll = rememberScrollState()
    LaunchedEffect(uiState.fishingState) {
        pageScroll.scrollTo(0)
        fightScroll.scrollTo(0)
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
            if (!active) {
                FishingHero(
                    zone = zone,
                    state = uiState.fishingState,
                    highContrastMode = highContrastMode
                )
                FishingPhaseStepper(state = uiState.fishingState, highContrastMode = highContrastMode)
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
                        .then(if (active) Modifier.verticalScroll(fightScroll) else Modifier)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    when (uiState.fishingState) {
                        FishingState.SETUP -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (uiState.needsReelBriefing) {
                                Text("Your first cast", style = MaterialTheme.typography.titleMedium)
                                Text("Tap Strike when the bite arrives. Reel while the fish is calm; release before its surge. Your first bite waits for you to tap Strike, then pauses for a safe reel explanation.")
                            }
                            FishingSetupSection(
                                state = uiState,
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
                            FishingJournal(uiState.journal)
                        }

                        FishingState.READY -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Your line is set — take your time", style = MaterialTheme.typography.titleLarge)
                            Text("Hold the reel during CALM or RECOVERY. Watch PULL and your tension. When SURGE COMING appears, release and give the fish slack until it settles.")
                            Text("Slack protects your progress during a surge. Reeling safely and giving slack through surges tire the fish. Keeping tension safe and giving slack through surges earns a clean catch.")
                            Text("Salvage uses a short, steady retrieval instead of a fish fight. Release the reel if its tension climbs.")
                            Button(onClick = viewModel::beginReeling, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                Text("Ready — start reeling")
                            }
                        }

                        FishingState.WAITING -> FishingWaitingSection(
                            state = uiState.waitingState,
                            highContrastMode = highContrastMode
                        )

                        FishingState.HOOKSET -> FishingHookSection(
                            hookState = uiState.hookState,
                            highContrastMode = highContrastMode
                        )

                        FishingState.REELING -> FishingReelSection(
                            reelState = uiState.reelState,
                            highContrastMode = highContrastMode
                        )

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
                onReelPressed = viewModel::onReelPressed,
                onReelReleased = viewModel::onReelReleased,
                onCancel = viewModel::cancelFishing,
                largeTouchTargets = largeTouchTargets,
            )
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
    onFishingCue: (String) -> Unit = {}
) {
    FishingScreen(
        viewModel = viewModel,
        onBack = onBack,
        onFishingComplete = onFinish,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets,
        disableHaptics = disableHaptics,
        onFishingCue = onFishingCue
    )
}

@Composable
fun FishingJournal(entries: List<FishingJournalEntry>) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val waters = entries.count { entry -> entry.species.any { it.caught } }
    val cleanWaters = entries.count { entry -> entry.species.any { it.clean } }
    val species = entries.flatMap { it.species }.groupBy { it.itemId }
    val caughtSpecies = species.values.count { records -> records.any { it.caught } }
    val cleanSpecies = species.values.count { records -> records.any { it.clean } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Fishing journal · $caughtSpecies/${species.size} species · $waters/${entries.size} waters")
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
    highContrastMode: Boolean
) {
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
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
                        Text(
                            text = "Current phase: ${state.label()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.item_icon_fishing),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(48.dp)
                            .padding(start = 8.dp)
                    )
                }

                // Native Species Preview in this Water Body
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
                            val fishName = catchDef.itemId.replace('_', ' ')
                                .split(' ')
                                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
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
                                        text = "${if (catchDef.isNativeFish()) "🐟" else "◇"} $fishName",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color.White.copy(alpha = 0.9f)
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Gear Loadout",
            style = MaterialTheme.typography.titleMedium,
            color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
        )
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
                        val speed = (((rod.fishingPower - 1.0) / 2.05).coerceIn(0.0, 1.0) * 18).toInt()
                        val forgiveness = (((rod.stability - 1.0) / 1.8).coerceIn(0.0, 1.0) * 30).toInt()
                        val subtitle = if (speed == 0 && forgiveness == 0) "Starter handling - watch your tension" else "$speed% faster reeling - $forgiveness% more tension resistance"
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
                        val formattedAttracts = lure.attracts.joinToString { raw ->
                            raw.replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        }.ifBlank { "General fish" }
                        val subtitle = buildString {
                            append("Favors: $formattedAttracts")
                            if ((lure.zoneBonuses[state.currentZone?.id] ?: 0) > 0 && state.currentZone?.catches?.any { it.itemId in lure.attracts } == true) append(" - Local match")
                            if (lure.rarityBonus > 0) append(" - +${(lure.rarityBonus * 100).toInt()}% rare catch weight")
                        }
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStart, enabled = state.selectedRod != null && state.selectedLure != null, modifier = Modifier.heightIn(min = buttonHeight)) {
                Text("Cast Line")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = buttonHeight)) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun FishingWaitingSection(
    state: FishingWaitingState?,
    highContrastMode: Boolean
) {
    val progress = state?.let { (it.elapsedMs.toFloat() / it.targetMs.toFloat()).coerceIn(0f, 1f) } ?: 0f
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Waiting for a bite…",
            style = MaterialTheme.typography.titleMedium,
            color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Text(
            text = "Watch for the bite signal. The Strike button appears when it is time to hook.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )

    }
}

@Composable
private fun FishingHookSection(
    hookState: FishingHookState?,
    highContrastMode: Boolean
) {
    val remaining = hookState?.timeRemainingMs ?: 0L
    val seconds = remaining.coerceAtLeast(0L) / 1000f

    val infiniteTransition = rememberInfiniteTransition(label = "arrowBypass")
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowOffset"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (hookState?.practiceHook == true) "Your first bite · take your time" else "A bite! Set the hook",
            style = MaterialTheme.typography.titleMedium,
            color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
        )

        // Bouncing Upward Arrow Animation
        Box(
            modifier = Modifier
                .height(40.dp)
                .width(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    val startY = size.height * 0.7f + arrowOffset
                    moveTo(size.width * 0.1f, startY)
                    lineTo(size.width * 0.5f, startY - 30f)
                    lineTo(size.width * 0.9f, startY)

                    moveTo(size.width * 0.1f, startY + 20f)
                    lineTo(size.width * 0.5f, startY - 10f)
                    lineTo(size.width * 0.9f, startY + 20f)
                }
                drawPath(
                    path = path,
                    color = if (highContrastMode) Color(0xFF8AC4FF) else Color(0xFF1D8BF2),
                    style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
            }
        }

        Text(
            text = if (hookState?.gyroAvailable == true) {
                "Jerk the device upward quickly as if striking a rod, or tap the button below!"
            } else {
                if (hookState?.practiceHook == true) "Tap Strike below to hook your catch. This first bite has no countdown." else "Tap Strike below before the bite is gone."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        val gyroLabel = if (hookState?.gyroAvailable == true) "⚡ Motion Strike + Touch Ready" else "Touch Strike Ready"
        Text(
            text = gyroLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = if (hookState?.practiceHook == true) "Practice bite · no time limit" else "Time remaining: ${String.format(java.util.Locale.ROOT, "%.1f", seconds)}s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )

    }
}

@Composable
private fun FishingReelSection(
    reelState: FishingReelState?,
    highContrastMode: Boolean
) {
    val progress = reelState?.progress ?: 0f
    val animated = animateFloatAsState(targetValue = progress, label = "reelProgress")
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (reelState?.salvage == true) "Retrieve the salvage" else "Reel it in!",
            style = MaterialTheme.typography.titleMedium,
            color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = reelState?.fishName ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        val frameColor = MaterialTheme.colorScheme.surfaceVariant
        val progressColor = MaterialTheme.colorScheme.primary
        val infiniteFishWiggle = rememberInfiniteTransition(label = "fishWiggle")
        val fishWiggleOffset by infiniteFishWiggle.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(300, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fishWiggleOffset"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                // Background Track
                drawRoundRect(
                    color = frameColor.copy(alpha = 0.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
                drawRoundRect(
                    color = frameColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = 6f)
                )
                // Filled progress
                drawRect(
                    color = progressColor.copy(alpha = 0.35f),
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.width * animated.value, size.height)
                )
            }
            // Dynamic Fish swimming across the progress track toward the catch boundary
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Surface Target / Boat Anchor on the right
                Text(
                            text = "LAND",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
                // Swimming fish marker that advances with progress
                val clampedProgress = animated.value.coerceIn(0f, 1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth(fraction = clampedProgress.coerceAtLeast(0.08f)),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = fishWiggleOffset.dp)
                    ) {
                        Text(
                            text = if (reelState?.salvage == true) "⚙" else "🐟",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "${(clampedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
        Text(
            text = if (reelState?.salvage == true) "Hold to retrieve it. Release if tension climbs." else reelState?.phase?.instruction ?: "Reel when the fish settles; give slack during a surge.",
            style = MaterialTheme.typography.bodySmall,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        LinearProgressIndicator(
            progress = { (reelState?.tension ?: 0f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if ((reelState?.tension ?: 0f) > 0.75f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = "Tension ${(reelState?.tension ?: 0f).coerceIn(0f, 1f).times(100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (reelState?.salvage == true) "STEADY RETRIEVAL" else when (reelState?.phase) {
                FishingFightPhase.WARNING -> "SURGE COMING — RELEASE"
                FishingFightPhase.SURGE -> "SURGE — GIVE SLACK"
                FishingFightPhase.PULL -> "PULL — WATCH TENSION"
                FishingFightPhase.RECOVERY -> "RECOVERY — REEL"
                else -> "CALM — REEL"
            },
            fontWeight = FontWeight.Bold,
            color = if (reelState?.phase in listOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE))
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        if (reelState?.salvage != true) Text(if ((reelState?.staminaRemaining ?: 1f) < 0.25f) "The fish is tiring — keep reading its surges."
            else "Fish energy ${((reelState?.staminaRemaining ?: 1f) * 100).toInt()}% · tire it by reeling safely", style = MaterialTheme.typography.labelMedium)

    }
}

@Composable
private fun FishingActiveControls(
    state: FishingUiState,
    onStrike: () -> Unit,
    onReelPressed: () -> Unit,
    onReelReleased: () -> Unit,
    onCancel: () -> Unit,
    largeTouchTargets: Boolean
) {
    val reelState = state.reelState
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.fishingState == FishingState.HOOKSET) {
            Button(onClick = onStrike, modifier = Modifier.fillMaxWidth().heightIn(min = if (largeTouchTargets) 64.dp else 52.dp)) {
                Text("Strike & set hook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        if (state.fishingState == FishingState.REELING) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
    val resultType = state.lastResult ?: MinigameResult.FAIL
    val success = resultType != MinigameResult.FAIL && (catch?.quantity ?: 0) > 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = catch?.message ?: if (success) "Catch secured!" else "Nothing biting this time.",
            style = MaterialTheme.typography.titleMedium,
            color = if (success) MaterialTheme.colorScheme.primary else if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
        )
        catch?.takeIf { it.itemId.isNotBlank() }?.let {
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(14.dp),
                color = if (highContrastMode) Color(0xFF111A25) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${it.quantity} × ${it.displayName.ifBlank { it.itemId.replace('_', ' ') }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    it.rarity?.let { rarity ->
                        val rarityLabel = rarity.name.lowercase().replaceFirstChar { ch -> ch.titlecase() }
                        Text(
                            text = "Rarity: $rarityLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    it.flavorText?.takeIf { text -> text.isNotBlank() }?.let { flavor ->
                        Text(
                            text = flavor,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highContrastMode) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (success) {
            Text("Added to your inventory.", style = MaterialTheme.typography.bodyMedium)
            catch?.uses?.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            catch?.rewards?.forEach { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            if (catch?.cleanCatch == true) Text("Clean catch recorded in your field journal.")
        }
        FishingJournal(state.journal)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onFishAgain, modifier = Modifier.heightIn(min = buttonHeight)) { Text("Fish Again") }
            val payload = FishingResultPayload(
                itemId = catch?.itemId?.takeIf { it.isNotBlank() },
                quantity = catch?.quantity,
                message = catch?.message,
                success = success,
                secured = catch?.secured == true
            )
            Button(onClick = { onFinish(payload) }, modifier = Modifier.heightIn(min = buttonHeight)) { Text("Return to Explore") }
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
