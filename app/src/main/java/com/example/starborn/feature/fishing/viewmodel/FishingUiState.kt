package com.example.starborn.feature.fishing.viewmodel

import com.example.starborn.domain.fishing.FishingLure
import com.example.starborn.domain.fishing.FishingRod
import com.example.starborn.domain.fishing.FishingResult
import com.example.starborn.domain.fishing.FishingZone
import com.example.starborn.domain.fishing.MinigameResult

data class FishingUiState(
    val availableRods: List<FishingRod> = emptyList(),
    val availableLures: List<FishingLure> = emptyList(),
    val selectedRod: FishingRod? = null,
    val selectedLure: FishingLure? = null,
    val currentZone: FishingZone? = null,
    val fishingState: FishingState = FishingState.SETUP,
    val minigame: FishingMinigameUi? = null,
    val lastCatchResult: FishingResult? = null,
    val lastResult: MinigameResult? = null
)

enum class FishingState {
    SETUP,
    MINIGAME,
    RESULT
}

data class FishingMinigameUi(
    val progress: Float,
    val successStart: Float,
    val successEnd: Float,
    val perfectStart: Float,
    val perfectEnd: Float,
    val timeRemainingSeconds: Float,
    val durationSeconds: Float,
    val isRunning: Boolean
)
