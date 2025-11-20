package com.example.starborn.feature.fishing.ui

import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.R
import com.example.starborn.domain.fishing.MinigameResult
import com.example.starborn.feature.common.ui.StationBackground
import com.example.starborn.feature.common.ui.StationHeader
import com.example.starborn.feature.fishing.sensors.HookMotionDetector
import com.example.starborn.feature.fishing.viewmodel.FishingHookState
import com.example.starborn.feature.fishing.viewmodel.FishingReelState
import com.example.starborn.feature.fishing.viewmodel.FishingState
import com.example.starborn.feature.fishing.viewmodel.FishingUiState
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel.FishingEvent
import com.example.starborn.feature.fishing.viewmodel.FishingWaitingState
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FishingScreen(
    viewModel: FishingViewModel,
    onBack: () -> Unit,
    onFishingComplete: (FishingResultPayload?) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val zone = uiState.currentZone
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp

    val context = LocalContext.current
    val sensorManager = remember(context) { context.getSystemService(SensorManager::class.java) }
    val hookDetector = remember(sensorManager) { HookMotionDetector(sensorManager) }
    val haptics = remember(context) { FishingHaptics(context) }

    LaunchedEffect(haptics) {
        viewModel.events.collectLatest { event ->
            when (event) {
                FishingEvent.Nibble -> haptics.nibble()
                FishingEvent.Bite -> haptics.bite()
            }
        }
    }

    LaunchedEffect(hookDetector) {
        viewModel.setGyroAvailable(hookDetector.isSupported())
    }

    DisposableEffect(uiState.fishingState, hookDetector) {
        if (uiState.fishingState == FishingState.HOOKSET && hookDetector.isSupported()) {
            hookDetector.start { viewModel.onHookMotionDetected() }
        } else {
            hookDetector.stop()
        }
        onDispose {
            hookDetector.stop()
        }
    }

    StationBackground(
        highContrastMode = highContrastMode,
        backgroundRes = R.drawable.beach_bg,
        vignetteRes = R.drawable.fishing_vignette
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
            FishingHero(
                zoneName = zone?.name ?: "Unknown Waters",
                state = uiState.fishingState,
                highContrastMode = highContrastMode
            )
            FishingPhaseStepper(state = uiState.fishingState, highContrastMode = highContrastMode)
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                color = if (highContrastMode) Color(0xFF0B1119) else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    when (uiState.fishingState) {
                        FishingState.SETUP -> FishingSetupSection(
                            state = uiState,
                            onSelectRod = viewModel::selectRod,
                            onSelectLure = viewModel::selectLure,
                            onStart = viewModel::startFishing,
                            onCancel = onBack,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            buttonHeight = buttonHeight
                        )

                        FishingState.WAITING -> FishingWaitingSection(
                            state = uiState.waitingState,
                            onCancel = viewModel::cancelWaiting,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            buttonHeight = buttonHeight
                        )

                        FishingState.HOOKSET -> FishingHookSection(
                            hookState = uiState.hookState,
                            onSetHook = viewModel::onHookButtonPressed,
                            onCancel = viewModel::cancelFishing,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            buttonHeight = buttonHeight
                        )

                        FishingState.REELING -> FishingReelSection(
                            reelState = uiState.reelState,
                            onReelTap = viewModel::onReelTap,
                            onCancel = viewModel::cancelFishing,
                            highContrastMode = highContrastMode,
                            largeTouchTargets = largeTouchTargets,
                            buttonHeight = buttonHeight
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
        }
    }
}

@Composable
fun FishingRoute(
    viewModel: FishingViewModel,
    onBack: () -> Unit,
    onFinish: (FishingResultPayload?) -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    FishingScreen(
        viewModel = viewModel,
        onBack = onBack,
        onFishingComplete = onFinish,
        highContrastMode = highContrastMode,
        largeTouchTargets = largeTouchTargets
    )
}

@Composable
private fun FishingHero(
    zoneName: String,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = zoneName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Tide-smooth air, neon ripples, and distant biolights under the waterline.",
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
                    modifier = Modifier.height(48.dp)
                )
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
    FishingState.HOOKSET -> 2
    FishingState.REELING -> 3
    FishingState.RESULT -> 4
}

private fun FishingState.label(): String = when (this) {
    FishingState.SETUP -> "Setup"
    FishingState.WAITING -> "Waiting"
    FishingState.HOOKSET -> "Hook"
    FishingState.REELING -> "Reel"
    FishingState.RESULT -> "Results"
}

@Composable
private fun FishingSetupSection(
    state: FishingUiState,
    onSelectRod: (com.example.starborn.domain.fishing.FishingRod) -> Unit,
    onSelectLure: (com.example.starborn.domain.fishing.FishingLure) -> Unit,
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
            Text("Rods", style = MaterialTheme.typography.labelLarge)
            if (state.availableRods.isEmpty()) {
                Text(
                    text = "You don't have any fishing rods.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                state.availableRods.forEach { rod ->
                    val subtitle = "Power ${rod.fishingPower.toInt()} | Stability ${"%.1f".format(rod.stability)}"
                    GearCard(
                        title = rod.name,
                        subtitle = subtitle,
                        description = rod.description,
                        selected = state.selectedRod?.id == rod.id,
                        onClick = { onSelectRod(rod) },
                        highContrastMode = highContrastMode
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lures", style = MaterialTheme.typography.labelLarge)
            if (state.availableLures.isEmpty()) {
                Text(
                    text = "You don't have any lures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                state.availableLures.forEach { lure ->
                    val subtitle = buildString {
                        append("Rarity +${lure.rarityBonus.toInt()}%")
                        val attracts = lure.attracts.joinToString().ifBlank { "General fish" }
                        append(" • Attracts: $attracts")
                    }
                    GearCard(
                        title = lure.name,
                        subtitle = subtitle,
                        description = lure.description,
                        selected = state.selectedLure?.id == lure.id,
                        onClick = { onSelectLure(lure) },
                        highContrastMode = highContrastMode
                    )
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
    onCancel: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
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
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Text(
            text = "Keep the line steady. You’ll feel a strong tug when it’s time to hook.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = buttonHeight)) {
            Text("Cancel")
        }
    }
}

@Composable
private fun FishingHookSection(
    hookState: FishingHookState?,
    onSetHook: () -> Unit,
    onCancel: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
) {
    val remaining = hookState?.timeRemainingMs ?: 0L
    val seconds = remaining.coerceAtLeast(0L) / 1000f
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Fish on! Snap the hook!",
            style = MaterialTheme.typography.titleMedium,
            color = if (highContrastMode) Color.White else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Jerk the device upward${if (hookState?.fallbackVisible == true) " or tap below" else ""}.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        val gyroLabel = if (hookState?.gyroAvailable == true) "Gyro hookset active" else "Tap if motion failover"
        Text(
            text = gyroLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = "Time remaining: ${String.format("%.1f", seconds)}s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        if (hookState?.fallbackVisible == true) {
            Button(onClick = onSetHook, modifier = Modifier.heightIn(min = buttonHeight)) {
                Text("Set Hook")
            }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = buttonHeight)) {
            Text("Abort")
        }
    }
}

@Composable
private fun FishingReelSection(
    reelState: FishingReelState?,
    onReelTap: () -> Unit,
    onCancel: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
) {
    val progress = reelState?.progress ?: 0f
    val animated = animateFloatAsState(targetValue = progress, label = "reelProgress")
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Reel it in!",
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = frameColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = 6f)
                )
                drawRect(
                    color = progressColor,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.width * animated.value, size.height)
                )
            }
        }
        Text(
            text = "Keep tension balanced—rapid taps when the bar dips.",
            style = MaterialTheme.typography.bodySmall,
            color = if (highContrastMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onReelTap, modifier = Modifier.heightIn(min = buttonHeight)) {
            Text("Tap to Reel")
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = buttonHeight)) {
            Text("Give Up")
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
                        text = "Loot: ${it.quantity} × ${it.itemId}",
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onFishAgain, modifier = Modifier.heightIn(min = buttonHeight)) { Text("Fish Again") }
            val payload = FishingResultPayload(
                itemId = catch?.itemId?.takeIf { it.isNotBlank() },
                quantity = catch?.quantity,
                message = catch?.message,
                success = success
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
    highContrastMode: Boolean
) {
    val surfaceColor = if (highContrastMode) Color(0xFF111A25) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (selected) 0.75f else 0.55f)
    val border = if (selected) BorderStroke(1.dp, if (highContrastMode) Color(0xFF8AC4FF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
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
