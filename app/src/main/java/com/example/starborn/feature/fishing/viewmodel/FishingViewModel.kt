package com.example.starborn.feature.fishing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.fishing.FishingLure
import com.example.starborn.domain.fishing.FishingResult
import com.example.starborn.domain.fishing.FishingRod
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.fishing.FishingZone
import com.example.starborn.domain.fishing.MinigameResult
import com.example.starborn.domain.fishing.MinigameRules
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FishingViewModel(
    private val fishingService: FishingService,
    private val zoneId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FishingUiState())
    val uiState: StateFlow<FishingUiState> = _uiState.asStateFlow()

    private var minigameJob: Job? = null
    private var minigameConfig: FishingMinigameConfig? = null

    init {
        loadFishingData()
    }

    private fun loadFishingData() {
        viewModelScope.launch {
            val availableRods = fishingService.getAvailableRods()
            val availableLures = fishingService.getAvailableLures()
            val zone = fishingService.getFishingZone(zoneId)
            _uiState.update {
                it.copy(
                    availableRods = availableRods,
                    availableLures = availableLures,
                    currentZone = zone,
                    selectedRod = availableRods.firstOrNull(),
                    selectedLure = availableLures.firstOrNull()
                )
            }
        }
    }

    fun selectRod(rod: FishingRod) {
        _uiState.update { it.copy(selectedRod = rod) }
    }

    fun selectLure(lure: FishingLure) {
        _uiState.update { it.copy(selectedLure = lure) }
    }

    fun startFishing() {
        val zone = _uiState.value.currentZone ?: return
        val rod = _uiState.value.selectedRod ?: return
        val lure = _uiState.value.selectedLure ?: return

        if (_uiState.value.fishingState == FishingState.MINIGAME) return

        val config = buildMinigameConfig(zone.minigameRules)
        minigameConfig = config
        val initialUi = FishingMinigameUi(
            progress = 0f,
            successStart = config.successStart,
            successEnd = config.successEnd,
            perfectStart = config.perfectStart,
            perfectEnd = config.perfectEnd,
            timeRemainingSeconds = config.durationMs / 1000f,
            durationSeconds = config.durationMs / 1000f,
            isRunning = true
        )

        minigameJob?.cancel()
        _uiState.update {
            it.copy(
                fishingState = FishingState.MINIGAME,
                minigame = initialUi,
                lastCatchResult = null,
                lastResult = null
            )
        }

        minigameJob = viewModelScope.launch {
            var progress = 0f
            var direction = 1f
            var elapsed = 0L

            while (isActive && elapsed < config.durationMs) {
                delay(TICK_MS)
                progress += direction * config.progressPerMs * TICK_MS
                if (progress >= 1f) {
                    progress = 1f
                    direction = -1f
                } else if (progress <= 0f) {
                    progress = 0f
                    direction = 1f
                }
                elapsed += TICK_MS
                val remaining = ((config.durationMs - elapsed).coerceAtLeast(0L)) / 1000f
                _uiState.update { state ->
                    state.copy(
                        minigame = state.minigame?.copy(
                            progress = progress,
                            timeRemainingSeconds = remaining,
                            isRunning = elapsed < config.durationMs
                        )
                    )
                }
            }

            if (isActive) {
                finishMinigame(MinigameResult.FAIL)
            }
        }
    }

    fun reelIn() {
        val config = minigameConfig ?: return
        val minigame = _uiState.value.minigame ?: return
        if (!minigame.isRunning) return

        val result = when {
            minigame.progress in config.perfectStart..config.perfectEnd -> MinigameResult.PERFECT
            minigame.progress in config.successStart..config.successEnd -> MinigameResult.SUCCESS
            else -> MinigameResult.FAIL
        }
        finishMinigame(result)
    }

    fun cancelMinigame() {
        minigameJob?.cancel()
        minigameJob = null
        minigameConfig = null
        _uiState.update {
            it.copy(
                fishingState = FishingState.SETUP,
                minigame = null,
                lastCatchResult = null,
                lastResult = null
            )
        }
    }

    fun resetFishing() {
        cancelMinigame()
    }

    private fun finishMinigame(result: MinigameResult) {
        val zone = _uiState.value.currentZone ?: return
        val rod = _uiState.value.selectedRod ?: return
        val lure = _uiState.value.selectedLure ?: return

        minigameJob?.cancel()
        minigameJob = null

        val catch = fishingService.getCatchResult(zone, rod, lure, result)
        val fallbackMessage = when (result) {
            MinigameResult.PERFECT -> "Perfect catch!"
            MinigameResult.SUCCESS -> "You reeled in a fish."
            MinigameResult.FAIL -> "The fish got away."
        }
        val finalResult: FishingResult = catch?.let {
            if (!it.message.isNullOrBlank()) it else it.copy(message = fallbackMessage)
        } ?: FishingResult(itemId = "", quantity = 0, message = fallbackMessage)

        minigameConfig = null
        _uiState.update {
            it.copy(
                fishingState = FishingState.RESULT,
                minigame = null,
                lastCatchResult = finalResult,
                lastResult = result
            )
        }
    }

    private fun buildMinigameConfig(rules: MinigameRules): FishingMinigameConfig {
        val speedFactor = rules.speed.coerceIn(MIN_SPEED_FACTOR, MAX_SPEED_FACTOR)
        val durationMs = (BASE_DURATION_MS / speedFactor).toLong().coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)

        val successWidth = rules.successWindow.toFloat().coerceIn(0.1f, 0.85f)
        val perfectWidth = rules.perfectWindow.toFloat().coerceIn(0.05f, successWidth)
        val successStart = ((0.5f - successWidth / 2f).coerceAtLeast(0f)).coerceAtMost(1f - successWidth)
        val successEnd = successStart + successWidth
        val perfectStart = ((0.5f - perfectWidth / 2f).coerceAtLeast(successStart)).coerceAtMost(successEnd - perfectWidth)
        val perfectEnd = perfectStart + perfectWidth

        val progressPerMs = BASE_PROGRESS_PER_MS * speedFactor.toFloat()

        return FishingMinigameConfig(
            successStart = successStart,
            successEnd = successEnd,
            perfectStart = perfectStart,
            perfectEnd = perfectEnd,
            durationMs = durationMs,
            progressPerMs = progressPerMs
        )
    }

    companion object {
        private const val TICK_MS = 16L
        private const val BASE_DURATION_MS = 8000L
        private const val MIN_DURATION_MS = 5000L
        private const val MAX_DURATION_MS = 12000L
        private const val MIN_SPEED_FACTOR = 0.6
        private const val MAX_SPEED_FACTOR = 2.5
        private const val BASE_PROGRESS_PER_MS = 0.0012f
    }
}

private data class FishingMinigameConfig(
    val successStart: Float,
    val successEnd: Float,
    val perfectStart: Float,
    val perfectEnd: Float,
    val durationMs: Long,
    val progressPerMs: Float
)
