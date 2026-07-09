package com.example.starborn.domain.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Process-wide foreground signal so playtime tracking works without a lifecycle dependency. */
object AppVisibility {
    private val _foreground = MutableStateFlow(false)
    val foreground: StateFlow<Boolean> = _foreground

    fun onStart() {
        _foreground.value = true
    }

    fun onStop() {
        _foreground.value = false
    }
}
