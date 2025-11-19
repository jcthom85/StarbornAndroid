package com.example.starborn.feature.fishing.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.domain.fishing.MinigameResult
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
    largeTouchTargets: Boolean,
    onHaptic: (FishingEvent) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val zone = uiState.currentZone
    val buttonHeight = if (largeTouchTargets) 52.dp else 0.dp

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest(onHaptic)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (highContrastMode) Color(0xFF02060A) else MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = zone?.name ?: "Fishing",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        zone?.let {
            Text(
                text = "Choose gear, wait for a bite, snap the hook, then tap to reel your catch in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }

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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rod", style = MaterialTheme.typography.titleMedium)
            if (state.availableRods.isEmpty()) {
                Text(
                    text = "You don't have any fishing rods.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                state.availableRods.forEach { rod ->
                    FilledTonalButton(
                        onClick = { onSelectRod(rod) },
                        enabled = state.selectedRod?.id != rod.id,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(rod.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Power ${rod.fishingPower.toInt()} | Stability ${"%.1f".format(rod.stability)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lure", style = MaterialTheme.typography.titleMedium)
            if (state.availableLures.isEmpty()) {
                Text(
                    text = "You don't have any lures.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                state.availableLures.forEach { lure ->
                    FilledTonalButton(
                        onClick = { onSelectLure(lure) },
                        enabled = state.selectedLure?.id != lure.id,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(lure.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Attracts: ${lure.attracts.joinToString().ifBlank { "General fish" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
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
    onCancel: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
) {
    val progress = state?.let { (it.elapsedMs.toFloat() / it.targetMs.toFloat()).coerceIn(0f, 1f) } ?: 0f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Waiting for a bite…", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Text(
            text = "Keep the line steady. You’ll feel a strong tug when it’s time to hook.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Fish on! Snap the hook!", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Jerk the device upward${if (hookState?.fallbackVisible == true) " or tap below" else ""}.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Reel it in!",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = reelState?.fishName ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = 6f)
                )
                drawRect(
                    color = MaterialTheme.colorScheme.primary,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(size.width * animated.value, size.height)
                )
            }
        }
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = catch?.message ?: if (success) "Catch secured!" else "Nothing biting this time.",
            style = MaterialTheme.typography.titleMedium,
            color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        catch?.takeIf { it.itemId.isNotBlank() }?.let {
            Text(
                text = "Loot: ${it.quantity} × ${it.itemId}",
                style = MaterialTheme.typography.bodyMedium
            )
            it.rarity?.let { rarity ->
                Text(
                    text = "Rarity: ${rarity.name.lowercase().replaceFirstChar { ch -> ch.titlecase() }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            it.flavorText?.takeIf { text -> text.isNotBlank() }?.let { flavor ->
                Text(
                    text = flavor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
