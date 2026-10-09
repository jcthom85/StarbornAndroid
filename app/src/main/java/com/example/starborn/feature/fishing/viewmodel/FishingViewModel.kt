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
    val runtime = FishingController(fishingService, zoneId, random, parentScope = viewModelScope)
    val uiState get() = runtime.uiState
    val events get() = runtime.events
    fun setGyroAvailable(enabled: Boolean) = runtime.setGyroAvailable(enabled)
    fun selectRod(rod: FishingRod) = runtime.selectRod(rod)
    fun selectLure(lure: FishingLure) = runtime.selectLure(lure)
    fun selectSensitivity(sensitivity: HookSensitivity) = runtime.selectSensitivity(sensitivity)
    fun toggleMotion() = runtime.toggleMotion()
    fun setPaused(value: Boolean) = runtime.setPaused(value)
    fun beginReeling() = runtime.beginReeling()
    fun startFishing() = runtime.startFishing()
    fun cancelFishing() = runtime.cancelFishing()
    fun resetFishing() = runtime.resetFishing()
    fun onHookMotionDetected() = runtime.onHookMotionDetected()
    fun onHookButtonPressed() = runtime.onHookButtonPressed()
    fun onReelPressed() = runtime.onReelPressed()
    fun onReelReleased() = runtime.onReelReleased()
    fun cancelWaiting() = runtime.cancelWaiting()
    override fun onCleared() { runtime.close() }
}

