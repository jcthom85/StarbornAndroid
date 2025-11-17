package com.example.starborn.feature.fishing.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.starborn.feature.fishing.viewmodel.FishingMinigameUi
import com.example.starborn.feature.fishing.viewmodel.FishingState
import com.example.starborn.feature.fishing.viewmodel.FishingUiState
import com.example.starborn.feature.fishing.viewmodel.FishingViewModel
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload

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
                text = "Select your gear and time the reel when the indicator is inside the highlighted zone.",
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

            FishingState.MINIGAME -> uiState.minigame?.let { minigame ->
                FishingMinigameSection(
                    state = minigame,
                    onReel = viewModel::reelIn,
                    onCancel = viewModel::cancelMinigame,
                    highContrastMode = highContrastMode,
                    largeTouchTargets = largeTouchTargets,
                    buttonHeight = buttonHeight
                )
            }

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
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rod", style = MaterialTheme.typography.titleMedium)
            if (state.availableRods.isEmpty()) {
                Text(
                    text = "You don't have a fishing rod yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.availableRods.forEach { rod ->
                        val selected = rod == state.selectedRod
                        FilledTonalButton(
                            onClick = { onSelectRod(rod) },
                            enabled = !selected,
                            modifier = Modifier.heightIn(min = buttonHeight)
                        ) {
                            Text(rod.name)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lure", style = MaterialTheme.typography.titleMedium)
            if (state.availableLures.isEmpty()) {
                Text(
                    text = "No lures available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.availableLures.forEach { lure ->
                        val selected = lure == state.selectedLure
                        FilledTonalButton(
                            onClick = { onSelectLure(lure) },
                            enabled = !selected,
                            modifier = Modifier.heightIn(min = buttonHeight)
                        ) {
                            Text(lure.name)
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onStart,
                enabled = state.selectedRod != null && state.selectedLure != null,
                modifier = Modifier.heightIn(min = buttonHeight)
            ) {
                Text("Cast Line")
            }
            Button(onClick = onCancel, modifier = Modifier.heightIn(min = buttonHeight)) {
                Text("Return")
            }
        }
    }
}

@Composable
private fun FishingMinigameSection(
    state: FishingMinigameUi,
    onReel: () -> Unit,
    onCancel: () -> Unit,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    buttonHeight: Dp
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tap Reel In while the indicator is inside the highlighted zone.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Time remaining: ${"%.1f".format(state.timeRemainingSeconds)}s",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        FishingMeter(state = state, modifier = Modifier.fillMaxWidth().height(140.dp), highContrastMode = highContrastMode)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onReel,
                enabled = state.isRunning,
                modifier = Modifier.heightIn(min = buttonHeight)
            ) {
                Text("Reel In")
            }
            Button(onClick = onCancel, modifier = Modifier.heightIn(min = buttonHeight)) {
                Text("Cancel")
            }
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
private fun FishingMeter(
    state: FishingMinigameUi,
    modifier: Modifier = Modifier,
    highContrastMode: Boolean
) {
    Canvas(modifier = modifier) {
        val trackHeight = size.height / 4f
        val trackTop = (size.height - trackHeight) / 2f
        val baseColor = if (highContrastMode) Color(0xFF0D1820) else Color(0xFF1B2735)
        val successColor = if (highContrastMode) Color(0xFF66BB6A).copy(alpha = 0.45f) else Color(0xFF4CAF50).copy(alpha = 0.35f)
        val perfectColor = if (highContrastMode) Color(0xFFFFEB3B).copy(alpha = 0.6f) else Color(0xFFFFC107).copy(alpha = 0.5f)

        drawRoundRect(
            color = baseColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, trackTop),
            size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(x = trackHeight / 2f, y = trackHeight / 2f)
        )

        val successStartX = state.successStart * size.width
        val successWidth = (state.successEnd - state.successStart) * size.width
        drawRoundRect(
            color = successColor,
            topLeft = androidx.compose.ui.geometry.Offset(successStartX, trackTop),
            size = androidx.compose.ui.geometry.Size(successWidth, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
        )

        val perfectStartX = state.perfectStart * size.width
        val perfectWidth = (state.perfectEnd - state.perfectStart) * size.width
        drawRoundRect(
            color = perfectColor,
            topLeft = androidx.compose.ui.geometry.Offset(perfectStartX, trackTop),
            size = androidx.compose.ui.geometry.Size(perfectWidth, trackHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)
        )

        val indicatorX = state.progress * size.width
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(indicatorX, trackTop - 8f),
            end = androidx.compose.ui.geometry.Offset(indicatorX, trackTop + trackHeight + 8f),
            strokeWidth = 6f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = androidx.compose.ui.geometry.Offset(indicatorX, trackTop - 16f),
            end = androidx.compose.ui.geometry.Offset(indicatorX, trackTop + trackHeight + 16f),
            strokeWidth = 2f
        )
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
