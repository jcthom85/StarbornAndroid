package com.example.starborn.feature.fishing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.fishing.FishPattern
import com.example.starborn.domain.fishing.FishingEncounter
import com.example.starborn.domain.fishing.FishingLure
import com.example.starborn.domain.fishing.FishingResult
import com.example.starborn.domain.fishing.FishingRod
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.fishing.FishingZone
import com.example.starborn.domain.fishing.MinigameResult
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FishingViewModel(
    private val fishingService: FishingService,
    private val zoneId: String,
    private val random: Random = Random.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(FishingUiState())
    val uiState: StateFlow<FishingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FishingEvent>()
    val events: SharedFlow<FishingEvent> = _events.asSharedFlow()

    private var gyroAvailable: Boolean = false

    private var waitingJob: Job? = null
    private var hookJob: Job? = null
    private var reelJob: Job? = null
    private var currentEncounter: FishingEncounter? = null
    private var reelProgress: Float = 0f
    private var highestProgress: Float = 0f

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

    fun setGyroAvailable(enabled: Boolean) {
        gyroAvailable = enabled
        if (_uiState.value.fishingState == FishingState.HOOKSET) {
            _uiState.update { state ->
                state.copy(
                    hookState = state.hookState?.copy(gyroAvailable = gyroAvailable)
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
        val encounter = fishingService.prepareEncounter(zone, rod, lure)
        if (encounter == null) {
            emitResult(
                FishingResult(itemId = "", quantity = 0, message = "Nothing seems to be biting here."),
                MinigameResult.FAIL
            )
            return
        }
        currentEncounter = encounter
        beginWaitingPhase(zone, rod, lure)
    }

    fun cancelFishing() {
        waitingJob?.cancel()
        hookJob?.cancel()
        reelJob?.cancel()
        currentEncounter = null
        _uiState.update {
            it.copy(
                fishingState = FishingState.SETUP,
                waitingState = null,
                hookState = null,
                reelState = null,
                lastCatchResult = null,
                lastResult = null
            )
        }
    }

    fun resetFishing() {
        cancelFishing()
    }

    fun onHookMotionDetected() {
        attemptHook()
    }

    fun onHookButtonPressed() {
        attemptHook()
    }

    fun onReelTap() {
        if (_uiState.value.fishingState != FishingState.REELING) return
        val rod = _uiState.value.selectedRod ?: return
        reelProgress = (reelProgress + (0.08f + rod.fishingPower.toFloat() * 0.02f)).coerceAtMost(1.1f)
        highestProgress = max(highestProgress, reelProgress)
        updateReelState(rod)
        if (reelProgress >= 1f) {
            finishReeling(success = true)
        }
    }

    fun cancelWaiting() {
        cancelFishing()
    }

    private fun beginWaitingPhase(zone: FishingZone, rod: FishingRod, lure: FishingLure) {
        waitingJob?.cancel()
        hookJob?.cancel()
        reelJob?.cancel()
        val targetMs = random.nextLong(MIN_BITE_DELAY_MS, MAX_BITE_DELAY_MS)
        val initial = FishingWaitingState(elapsedMs = 0, targetMs = targetMs)
        _uiState.update {
            it.copy(
                fishingState = FishingState.WAITING,
                waitingState = initial,
                hookState = null,
                reelState = null,
                lastCatchResult = null,
                lastResult = null
            )
        }
        waitingJob = viewModelScope.launch {
            var elapsed = 0L
            while (isActive && elapsed < targetMs) {
                delay(WAIT_TICK_MS)
                elapsed += WAIT_TICK_MS
                val nibbleWindow = targetMs - elapsed <= NIBBLE_WINDOW_MS
                if (nibbleWindow && elapsed % (NIBBLE_INTERVAL_MS) == 0L) {
                    _events.emit(FishingEvent.Nibble)
                }
                _uiState.update { state ->
                    state.copy(waitingState = state.waitingState?.copy(elapsedMs = elapsed))
                }
            }
            if (isActive) {
                enterHooksetPhase()
            }
        }
    }

    private fun enterHooksetPhase() {
        waitingJob?.cancel()
        hookJob?.cancel()
        viewModelScope.launch { _events.emit(FishingEvent.Bite) }
        val hookState = FishingHookState(
            timeRemainingMs = HOOK_WINDOW_MS,
            gyroAvailable = gyroAvailable,
            fallbackVisible = true
        )
        _uiState.update {
            it.copy(
                fishingState = FishingState.HOOKSET,
                waitingState = null,
                hookState = hookState,
                reelState = null
            )
        }
        hookJob = viewModelScope.launch {
            var remaining = HOOK_WINDOW_MS
            while (isActive && remaining > 0) {
                delay(HOOK_TICK_MS)
                remaining -= HOOK_TICK_MS
                _uiState.update { state ->
                    state.copy(
                        hookState = state.hookState?.copy(timeRemainingMs = remaining.coerceAtLeast(0L))
                    )
                }
            }
            if (isActive) {
                failHook("The fish darted away before you set the hook.")
            }
        }
    }

    private fun attemptHook() {
        if (_uiState.value.fishingState != FishingState.HOOKSET) return
        startReelingPhase()
    }

    private fun startReelingPhase() {
        hookJob?.cancel()
        val encounter = currentEncounter ?: run {
            failHook("The fish slipped away.")
            return
        }
        val rod = _uiState.value.selectedRod
        val lure = _uiState.value.selectedLure
        if (rod == null || lure == null) {
            failHook("You lowered your rod.")
            return
        }
        reelProgress = 0.35f
        highestProgress = reelProgress
        val fishName = encounter.catch.itemId
        _uiState.update {
            it.copy(
                fishingState = FishingState.REELING,
                hookState = null,
                waitingState = null,
                reelState = FishingReelState(
                    progress = reelProgress,
                    tension = 0f,
                    fishName = fishName,
                    behavior = encounter.behavior
                )
            )
        }
        reelJob = viewModelScope.launch {
            while (isActive) {
                delay(REEL_TICK_MS)
                applyFishPull(encounter, rod)
            }
        }
    }

    private fun applyFishPull(encounter: FishingEncounter, rod: FishingRod) {
        val behavior = encounter.behavior
        val basePull = ((behavior?.basePull ?: 0.4) / rod.stability).toFloat()
        val burstPull = when (behavior?.pattern) {
            null -> 0f
            com.example.starborn.domain.fishing.FishPattern.SINE -> 0.05f * kotlin.math.sin(System.nanoTime().toDouble()).toFloat()
            com.example.starborn.domain.fishing.FishPattern.LINEAR -> 0.02f
            com.example.starborn.domain.fishing.FishPattern.BURST -> if (random.nextFloat() < 0.25f) (behavior.burstPull / rod.stability).toFloat() else 0f
        }
        val totalPull = (basePull + burstPull).coerceAtLeast(0.02f) * PULL_SCALE
        reelProgress = (reelProgress - totalPull).coerceAtLeast(0f)
        if (reelProgress <= 0f) {
            finishReeling(success = false)
            return
        }
        updateReelState(rod, totalPull)
    }

    private fun updateReelState(rod: FishingRod, tension: Float = 0f) {
        _uiState.update {
            it.copy(
                reelState = it.reelState?.copy(
                    progress = reelProgress,
                    tension = tension
                )
            )
        }
    }

    private fun finishReeling(success: Boolean) {
        reelJob?.cancel()
        val encounter = currentEncounter
        if (!success || encounter == null) {
            emitResult(
                FishingResult(itemId = "", quantity = 0, message = "The line went slack."),
                MinigameResult.FAIL
            )
            currentEncounter = null
            return
        }
        val resultType = if (highestProgress >= PERFECT_THRESHOLD && reelProgress >= 1f) {
            MinigameResult.PERFECT
        } else {
            MinigameResult.SUCCESS
        }
        val result = fishingService.resolveEncounter(encounter, resultType)
        emitResult(result, resultType)
        currentEncounter = null
    }

    private fun failHook(message: String) {
        hookJob?.cancel()
        emitResult(FishingResult(itemId = "", quantity = 0, message = message), MinigameResult.FAIL)
        currentEncounter = null
    }

    private fun emitResult(result: FishingResult, minigameResult: MinigameResult) {
        waitingJob?.cancel()
        hookJob?.cancel()
        reelJob?.cancel()
        _uiState.update {
            it.copy(
                fishingState = FishingState.RESULT,
                waitingState = null,
                hookState = null,
                reelState = null,
                lastCatchResult = result,
                lastResult = minigameResult
            )
        }
    }

    sealed interface FishingEvent {
        object Nibble : FishingEvent
        object Bite : FishingEvent
    }

    companion object {
        private const val MIN_BITE_DELAY_MS = 3_000L
        private const val MAX_BITE_DELAY_MS = 6_500L
        private const val WAIT_TICK_MS = 150L
        private const val NIBBLE_WINDOW_MS = 1_500L
        private const val NIBBLE_INTERVAL_MS = 600L
        private const val HOOK_WINDOW_MS = 1_500L
        private const val HOOK_TICK_MS = 100L
        private const val REEL_TICK_MS = 200L
        private const val PULL_SCALE = 0.05f
        private const val PERFECT_THRESHOLD = 0.85f
    }
}
