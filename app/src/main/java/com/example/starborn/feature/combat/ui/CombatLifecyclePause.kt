package com.example.starborn.feature.combat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CombatLifecyclePause(overlayPaused: Boolean = false, setPaused: (Boolean) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val callback = rememberUpdatedState(setPaused)
    DisposableEffect(owner, overlayPaused) {
        val lifecycle = owner.lifecycle
        fun sync() = callback.value(overlayPaused || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        val observer = LifecycleEventObserver { _, _ -> sync() }
        lifecycle.addObserver(observer)
        sync()
        onDispose {
            lifecycle.removeObserver(observer)
            callback.value(true)
        }
    }
}
