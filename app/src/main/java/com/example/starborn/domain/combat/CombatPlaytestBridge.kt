package com.example.starborn.domain.combat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global playtest bridge for live combat developer controls.
 * Allows toggling OP / God Mode during battle encounters to expedite playtesting
 * without corrupting baseline balancing mechanics when toggled off.
 */
object CombatPlaytestBridge {
    private val _isOpMode = MutableStateFlow(false)
    val isOpModeFlow: StateFlow<Boolean> = _isOpMode.asStateFlow()

    var isOpMode: Boolean
        get() = _isOpMode.value
        set(value) {
            _isOpMode.value = value
        }

    fun toggleOpMode(): Boolean {
        val next = !_isOpMode.value
        _isOpMode.value = next
        return next
    }

    fun reset() {
        _isOpMode.value = false
    }
}
