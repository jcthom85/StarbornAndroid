package com.example.starborn.feature.fishing.viewmodel

import com.example.starborn.domain.fishing.FishBehaviorDefinition
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
    val waitingState: FishingWaitingState? = null,
    val hookState: FishingHookState? = null,
    val reelState: FishingReelState? = null,
    val lastCatchResult: FishingResult? = null,
    val lastResult: MinigameResult? = null,
    val hookSensitivity: HookSensitivity = HookSensitivity.MEDIUM
)

enum class HookSensitivity(val label: String, val thresholdValue: Float) {
    HIGH("High Sensitivity (3.0 m/s²)", 3.0f),
    MEDIUM("Medium Sensitivity (4.5 m/s²)", 4.5f),
    LOW("Low Sensitivity (6.0 m/s²)", 6.0f)
}

enum class FishingState {
    SETUP,
    WAITING,
    HOOKSET,
    REELING,
    RESULT
}

data class FishingWaitingState(
    val elapsedMs: Long,
    val targetMs: Long
)

data class FishingHookState(
    val timeRemainingMs: Long,
    val gyroAvailable: Boolean,
    val fallbackVisible: Boolean
)

data class FishingReelState(
    val progress: Float,
    val tension: Float,
    val isReeling: Boolean,
    val fishName: String?,
    val behavior: FishBehaviorDefinition?
)
