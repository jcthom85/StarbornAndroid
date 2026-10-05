package com.example.starborn.feature.fishing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starborn.domain.fishing.FishingEncounter
import com.example.starborn.domain.fishing.FishingFight
import com.example.starborn.domain.fishing.FishingFightPhase
import com.example.starborn.domain.fishing.FishingLure
import com.example.starborn.domain.fishing.FishingResult
import com.example.starborn.domain.fishing.FishingRod
import com.example.starborn.domain.fishing.FishingService
import com.example.starborn.domain.fishing.MinigameResult
import com.example.starborn.domain.fishing.isNativeFish
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
    private var isReeling: Boolean = false
    private var fight: FishingFight? = null
    private var paused = false
    private var lastWarningMs = -2_000L

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
                    selectedRod = fishingService.preferredRod(),
                    selectedLure = fishingService.preferredLure(),
                    journal = fishingService.getJournal(),
                    needsReelBriefing = !fishingService.hasReelBriefing(),
                    needsHookBriefing = !fishingService.hasHookBriefing()
                )
            }
        }
    }

    fun setGyroAvailable(enabled: Boolean) {
        gyroAvailable = enabled
        _uiState.update { it.copy(motionSupported = enabled) }
        if (_uiState.value.fishingState == FishingState.HOOKSET) {
            _uiState.update { state ->
                state.copy(
                    hookState = state.hookState?.copy(gyroAvailable = gyroAvailable && state.motionEnabled)
                )
            }
        }
    }

    fun selectRod(rod: FishingRod) {
        if (_uiState.value.fishingState != FishingState.SETUP || rod !in _uiState.value.availableRods) return
        fishingService.rememberGear("rod", rod.id)
        _uiState.update { it.copy(selectedRod = rod) }
    }

    fun selectLure(lure: FishingLure) {
        if (_uiState.value.fishingState != FishingState.SETUP || lure !in _uiState.value.availableLures) return
        fishingService.rememberGear("lure", lure.id)
        _uiState.update { it.copy(selectedLure = lure) }
    }

    fun selectSensitivity(sensitivity: HookSensitivity) {
        _uiState.update { it.copy(hookSensitivity = sensitivity) }
    }

    fun toggleMotion() {
        _uiState.update { it.copy(motionEnabled = it.motionSupported && !it.motionEnabled) }
    }

    fun setPaused(value: Boolean) {
        paused = value
        if (value) onReelReleased()
    }

    fun beginReeling() {
        if (_uiState.value.fishingState != FishingState.READY) return
        fishingService.markReelBriefing()
        _uiState.update { it.copy(needsReelBriefing = false) }
        startReelingPhase()
    }

    fun startFishing() {
        if (_uiState.value.fishingState != FishingState.SETUP) return
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
        viewModelScope.launch { _events.emit(FishingEvent.Cast) }
        beginWaitingPhase()
    }

    fun cancelFishing() {
        waitingJob?.cancel()
        hookJob?.cancel()
        reelJob?.cancel()
        currentEncounter = null
        fight = null
        isReeling = false
        val rods = fishingService.getAvailableRods()
        val lures = fishingService.getAvailableLures()
        _uiState.update {
            it.copy(
                fishingState = FishingState.SETUP,
                waitingState = null,
                hookState = null,
                reelState = null,
                lastCatchResult = null,
                lastResult = null,
                availableRods = rods,
                availableLures = lures,
                selectedRod = rods.firstOrNull { rod -> rod.id == it.selectedRod?.id } ?: fishingService.preferredRod(),
                selectedLure = lures.firstOrNull { lure -> lure.id == it.selectedLure?.id } ?: fishingService.preferredLure(),
                journal = fishingService.getJournal()
            )
        }
    }

    fun resetFishing() {
        cancelFishing()
    }

    fun onHookMotionDetected() {
        if (!_uiState.value.motionEnabled || paused) return
        attemptHook()
    }

    fun onHookButtonPressed() {
        if (paused) return
        attemptHook()
    }

    fun onReelPressed() {
        if (paused || _uiState.value.fishingState != FishingState.REELING) return
        isReeling = true
        updateReelState()
    }

    fun onReelReleased() {
        if (_uiState.value.fishingState != FishingState.REELING) return
        isReeling = false
        updateReelState()
    }

    fun cancelWaiting() {
        cancelFishing()
    }

    private fun beginWaitingPhase() {
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
                if (paused) continue
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
            gyroAvailable = gyroAvailable && _uiState.value.motionEnabled,
            fallbackVisible = true,
            practiceHook = _uiState.value.needsHookBriefing
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
            if (hookState.practiceHook) return@launch
            var remaining = HOOK_WINDOW_MS
            while (isActive && remaining > 0) {
                delay(HOOK_TICK_MS)
                if (paused) continue
                remaining -= HOOK_TICK_MS
                _uiState.update { state ->
                    state.copy(
                        hookState = state.hookState?.copy(timeRemainingMs = remaining.coerceAtLeast(0L))
                    )
                }
            }
            if (isActive) {
                failHook("The bite was gone before you set the hook.")
            }
        }
    }

    private fun attemptHook() {
        if (_uiState.value.fishingState != FishingState.HOOKSET) return
        fishingService.markHookBriefing()
        _uiState.update { it.copy(needsHookBriefing = false) }
        if (_uiState.value.needsReelBriefing) {
            hookJob?.cancel()
            _uiState.update { it.copy(fishingState = FishingState.READY, hookState = null) }
            return
        }
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
        fight = FishingFight(encounter.behavior, rod, salvage = !encounter.catch.isNativeFish())
        isReeling = false
        lastWarningMs = -2_000L
        val fishName = fishingService.catchDisplayName(encounter.catch.itemId)
        _uiState.update {
            it.copy(
                fishingState = FishingState.REELING,
                hookState = null,
                waitingState = null,
                reelState = FishingReelState(
                    progress = fight!!.progress,
                    tension = fight!!.tension,
                    isReeling = isReeling,
                    fishName = fishName,
                    behavior = encounter.behavior,
                    salvage = !encounter.catch.isNativeFish()
                )
            )
        }
        reelJob = viewModelScope.launch {
            while (isActive) {
                delay(REEL_TICK_MS)
                if (paused) continue
                applyReelTick()
            }
        }
    }

    private fun applyReelTick() {
        val current = fight ?: return
        val previousPhase = current.phase
        val previousTension = current.tension
        current.tick(isReeling)
        val warning = current.phase == FishingFightPhase.WARNING && previousPhase != current.phase ||
            current.tension >= 0.75f && previousTension < 0.75f
        if (warning && current.elapsedMs - lastWarningMs >= 1_200L) {
            lastWarningMs = current.elapsedMs
            viewModelScope.launch { _events.emit(FishingEvent.Warning) }
        }
        if (current.tension >= 1f) {
            finishReeling(success = false, message = "The line snapped under too much tension.")
            return
        }
        if (current.failed) {
            finishReeling(success = false)
            return
        }
        if (current.successful) {
            finishReeling(success = true)
            return
        }
        updateReelState()
    }

    private fun updateReelState() {
        _uiState.update {
            it.copy(
                reelState = it.reelState?.copy(
                    progress = fight?.progress ?: 0f,
                    tension = fight?.tension ?: 0f,
                    isReeling = isReeling,
                    phase = fight?.phase ?: FishingFightPhase.CALM,
                    staminaRemaining = fight?.staminaRemaining ?: 1f
                )
            )
        }
    }

    private fun finishReeling(success: Boolean, message: String = "The line went slack.") {
        reelJob?.cancel()
        isReeling = false
        val encounter = currentEncounter
        if (!success || encounter == null) {
            val isSnap = message.contains("tension")
            viewModelScope.launch {
                if (isSnap) _events.emit(FishingEvent.LineSnap)
            }
            emitResult(
                FishingResult(itemId = "", quantity = 0, message = message),
                MinigameResult.FAIL
            )
            currentEncounter = null
            return
        }
        val resultType = if (fight?.perfect == true) {
            MinigameResult.PERFECT
        } else {
            MinigameResult.SUCCESS
        }
        val result = fishingService.resolveEncounter(encounter, resultType).copy(
            zoneId = zoneId, cleanCatch = resultType == MinigameResult.PERFECT
        )
        viewModelScope.launch {
            _events.emit(FishingEvent.CatchSuccess)
        }
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
        val securedResult = fishingService.secureCatch(result)
        _uiState.update {
            it.copy(
                fishingState = FishingState.RESULT,
                waitingState = null,
                hookState = null,
                reelState = null,
                lastCatchResult = securedResult,
                lastResult = minigameResult,
                journal = fishingService.getJournal()
            )
        }
    }

    sealed interface FishingEvent {
        object Cast : FishingEvent
        object Nibble : FishingEvent
        object Bite : FishingEvent
        object Warning : FishingEvent
        object CatchSuccess : FishingEvent
        object LineSnap : FishingEvent
    }

    companion object {
        private const val MIN_BITE_DELAY_MS = 1_800L
        private const val MAX_BITE_DELAY_MS = 3_500L
        private const val WAIT_TICK_MS = 150L
        private const val NIBBLE_WINDOW_MS = 1_500L
        private const val NIBBLE_INTERVAL_MS = 600L
        private const val HOOK_WINDOW_MS = 2_400L
        private const val HOOK_TICK_MS = 100L
        private const val REEL_TICK_MS = FishingFight.TICK_MS
    }
}
