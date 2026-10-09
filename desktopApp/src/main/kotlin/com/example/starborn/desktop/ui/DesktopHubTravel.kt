package com.example.starborn.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import kotlinx.coroutines.*

/** Owned by the screen host, so the fade survives disposal of the outgoing hub. */
internal class DesktopHubTravel(private val hostScope: CoroutineScope?=null) {
    val opacity=Animatable(0f)
    var busy by mutableStateOf(false)
        private set
    fun request(scope: CoroutineScope, onFailure: (Exception) -> Unit = {}, action: () -> Unit): Boolean {
        if(busy) return false
        busy=true
        (hostScope ?: scope).launch {
            try {
                opacity.animateTo(1f,tween(120))
                action()
                withFrameNanos { }
                opacity.animateTo(0f,tween(120))
            } catch(error: Exception) {
                if(error is CancellationException) throw error
                onFailure(error)
            } finally {
                withContext(NonCancellable) { opacity.snapTo(0f) }
                busy=false
            }
        }
        return true
    }
}
internal val LocalHubTravel=staticCompositionLocalOf<DesktopHubTravel?> { null }
