package com.example.starborn.feature.fishing.ui

import com.example.starborn.ui.fishing.*

import com.example.starborn.domain.fishing.handlingSummary
import com.example.starborn.domain.fishing.catchSummary

import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .then(if (active) Modifier else Modifier.verticalScroll(pageScroll))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        selectedRodName = uiState.selectedRod?.name,
                        selectedLureName = uiState.selectedLure?.name,
                        itemDisplayName = viewModel::catchDisplayName,
                        highContrastMode = highContrastMode
                    )
                }
                Surface(
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth().then(if (active) Modifier.weight(1f) else Modifier),
                    color = if (highContrastMode) Color(0xFF0B1119) else FishingInk.copy(alpha = 0.80f),
                    border = BorderStroke(
                        1.dp,
                        if (highContrastMode) Color(0xFF8AC4FF) else biome.bankRimTint.copy(alpha = 0.28f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (active) Modifier.fillMaxSize() else Modifier)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        when (uiState.fishingState) {
                            FishingState.SETUP -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Live Water-First Basin Preview in Setup
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(176.dp)
                                ) {
                                    FishingWaterScene(
                                        state = uiState,
                                        onHook = viewModel::onHookButtonPressed,
                                        showHeaderText = false,
                                        showTelemetryFooter = false
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = FishingInk.copy(alpha = 0.78f),
                                        border = BorderStroke(1.dp, FishingGold.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.selectedRod?.name ?: "No Rod"} · ${uiState.selectedLure?.name ?: "No Lure"}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FishingGold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

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
                                onQuickCast = viewModel::castAgain,
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
                    Text(
                        if (record.caught) "${record.name}${if (record.clean) " · Clean catch" else " · Caught"}"
                        else "Undiscovered species",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                "Records remain after cooking or selling. The Colony drain pool becomes fishable after the Sector 9 pod examination; return through Astra when travel is available.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun FishingHero(
    zone: com.example.starborn.domain.fishing.FishingZone?,
    state: FishingState,
    journal: List<FishingJournalEntry>,
    selectedRodName: String?,
    selectedLureName: String?,
    itemDisplayName: (String) -> String,
    highContrastMode: Boolean
) {
    val zoneJournal = remember(zone?.id, journal) {
        journal.firstOrNull { it.zoneId == zone?.id }?.species?.associateBy { it.itemId }.orEmpty()
    }
    val biome = remember(zone?.id) { biomeStyleForZone(zone?.id) }
    val gradient = Brush.linearGradient(
        colors = if (highContrastMode) {
            listOf(Color(0xFF0A1320), Color(0xFF11263A))
        } else {
            listOf(biome.deepWater.copy(alpha = 0.88f), biome.shallowWater.copy(alpha = 0.82f), biome.deepWater.copy(alpha = 0.65f))
        }
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp,
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (highContrastMode) Color(0xFF1D8BF2) else biome.bankRimTint.copy(alpha = 0.30f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = zone?.name ?: "Unknown Waters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = zone?.description ?: "Watch the water for a bite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.item_icon_fishing),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(38.dp)
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
                            color = if (highContrastMode) Color(0xFF8AC4FF) else FishingGold
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
                                color = Color.Black.copy(alpha = 0.42f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$statusGlyph $fishName",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = if (isDiscovered) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.65f)
                                    )
                                    val rarityColor = when (catchDef.rarity) {
                                        FishingRarity.COMMON -> Color(0xFFAAAAAA)
                                        FishingRarity.UNCOMMON -> Color(0xFF81C784)
                                        FishingRarity.RARE -> Color(0xFF64B5F6)
                                        FishingRarity.EPIC -> Color(0xFFBA68C8)
                                        FishingRarity.EXOTIC -> Color(0xFFFFD54F)
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
                modifier = Modifier.weight(1f).heightIn(min = maxOf(buttonHeight, 52.dp))
            ) {
                Text("Cast Line", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.heightIn(min = maxOf(buttonHeight, 52.dp))
            ) {
                Text("Leave")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Select Rod", style = MaterialTheme.typography.labelLarge, color = FishingGold)
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

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Select Lure", style = MaterialTheme.typography.labelLarge, color = FishingGold)
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
                                    text = when (sensitivity) {
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
            Button(
                onClick = onBeginReeling,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
            ) {
                Text("I'm ready — start reeling", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
        if (state.fishingState == FishingState.HOOKSET) {
            val remainingFrac = if (state.hookState?.practiceHook == true) 1f
            else ((state.hookState?.timeRemainingMs ?: 2400L) / 2400f).coerceIn(0f, 1f)
            Button(
                onClick = onStrike,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FishingGold,
                    contentColor = FishingInk
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = if (largeTouchTargets) 68.dp else 58.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set hook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    LinearProgressIndicator(
                        progress = { remainingFrac },
                        modifier = Modifier.fillMaxWidth(0.75f).height(4.dp).clip(RoundedCornerShape(999.dp)),
                        color = FishingInk,
                        trackColor = FishingInk.copy(alpha = 0.25f)
                    )
                }
            }
        }
        if (state.fishingState == FishingState.REELING) {
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
                    .fillMaxWidth()
                    .testTag("fishing-reel-control")
                    .heightIn(min = if (largeTouchTargets) 92.dp else 78.dp)
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
                shape = RoundedCornerShape(22.dp),
                color = when {
                    isUrgent && isReeling -> Color(0xFF421C1B)
                    isReeling -> Color(0xFF1E444D)
                    else -> Color(0xFF102832)
                },
                border = BorderStroke(if (isReeling || isUrgent) 2.dp else 1.2.dp, drumBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tactile Dual-Arc Winch Drum Gauge
                    Box(
                        modifier = Modifier.size(58.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val strokeOuter = 5.dp.toPx()
                            val strokeInner = 4.dp.toPx()
                            val outerInset = strokeOuter / 2f
                            val innerInset = strokeOuter + 4.dp.toPx()

                            // Outer Track & Landing Progress Arc
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

                            // Inner Track & Tension Arc
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

                            // Rotating Winch Spokes inside the Drum
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
                            color = if (isUrgent) FishingCoral else if (isReeling) FishingGold else FishingMist
                        )
                        Text(
                            text = when {
                                isUrgent -> "Release thumb to give slack!"
                                isReeling -> "Distance ${(progress * 100).toInt()}% · Keep tension safe"
                                else -> "Press & hold anywhere on this drum"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = FishingMist.copy(alpha = 0.82f)
                        )
                    }
                }
            }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = if (largeTouchTargets) 52.dp else 46.dp)) {
            Text(if (state.fishingState == FishingState.REELING) "Put the line down" else "Cancel cast")
        }
    }
}

@Composable
private fun FishingResultSection(
    state: FishingUiState,
    onFishAgain: () -> Unit,
    onQuickCast: () -> Unit,
    onFinish: (FishingResultPayload?) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
) {
    val catch = state.lastCatchResult
    val success = state.lastResult != MinigameResult.FAIL && (catch?.quantity ?: 0) > 0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Keep the settling water basin visible at the top of the result card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            FishingWaterScene(
                state = state,
                onHook = {},
                showHeaderText = false,
                showTelemetryFooter = false
            )
        }

        Text(
            text = if (success) "Back on shore" else "The water settles",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (success) FishingGold else FishingCoral
        )
        if (success && catch != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF132D36),
                border = BorderStroke(1.2.dp, FishingGold.copy(alpha = 0.6f))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${catch.quantity} × ${catch.displayName.ifBlank { catch.itemId.replace('_', ' ') }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = FishingGold,
                            modifier = Modifier.weight(1f)
                        )
                        catch.rarity?.let { rarity ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, FishingGold.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = rarity.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = FishingGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    catch.flavorText?.takeIf { it.isNotBlank() }?.let { Text(it, color = FishingMist) }
                    Text(
                        text = if (catch.secured) "Secured in your inventory." else "Catch ready to collect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FishingMist.copy(alpha = 0.85f)
                    )
                    if (catch.cleanCatch) {
                        Text(
                            text = "Clean catch · recorded in your catch log",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FishingGold
                        )
                    }
                    catch.cleanCatchProgress?.let { prog ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Clean-catch mastery:",
                                style = MaterialTheme.typography.labelMedium,
                                color = FishingMist
                            )
                            repeat(3) { pip ->
                                Text(
                                    text = if (pip < prog) "●" else "○",
                                    color = if (pip < prog) FishingGold else FishingMist.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "($prog/3 toward +1 bonus fish)",
                                style = MaterialTheme.typography.labelSmall,
                                color = FishingMist.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            catch.uses.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = FishingMist) }
            catch.rewards.forEach { Text(it, color = FishingGold, fontWeight = FontWeight.SemiBold) }
        } else {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF23181B),
                border = BorderStroke(1.dp, FishingCoral.copy(alpha = 0.45f))
            ) {
                Text(
                    text = catch?.message ?: "It slipped away. You can cast again whenever you're ready.",
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    color = FishingMist
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onFishAgain,
                modifier = Modifier.weight(1f).heightIn(min = if (largeTouchTargets) 56.dp else 48.dp)
            ) {
                Text("Cast again", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onQuickCast,
                modifier = Modifier.heightIn(min = if (largeTouchTargets) 56.dp else 48.dp)
            ) {
                Text("Quick cast", fontWeight = FontWeight.SemiBold)
            }
        }
        OutlinedButton(
            onClick = {
                onFinish(
                    FishingResultPayload(
                        catch?.itemId?.takeIf { it.isNotBlank() },
                        catch?.quantity,
                        catch?.message,
                        success,
                        catch?.secured == true
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text("Return to exploration")
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
