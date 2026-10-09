package com.example.starborn.desktop.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import kotlinx.coroutines.*

sealed class DesktopHubTransitionType {
    object None : DesktopHubTransitionType()
    data class DeployToNode(
        val nodeTitle: String,
        val regionTitle: String?,
        val entryRoomTitle: String?,
        val description: String?
    ) : DesktopHubTransitionType()
    object ReturnToHub : DesktopHubTransitionType()
    object InstantFade : DesktopHubTransitionType()
}

/** Owned by the screen host, so transitions survive disposal of outgoing screens. */
internal class DesktopHubTravel(private val hostScope: CoroutineScope? = null) {
    val opacity = Animatable(0f)
    val cardAlpha = Animatable(0f)
    val cardScale = Animatable(0.96f)
    val beamProgress = Animatable(0f)

    var busy by mutableStateOf(false)
        private set

    var transitionType by mutableStateOf<DesktopHubTransitionType>(DesktopHubTransitionType.None)
        private set

    /** Standard quick travel transition, preserving 120ms behavior for compatibility. */
    fun request(scope: CoroutineScope, onFailure: (Exception) -> Unit = {}, action: () -> Unit): Boolean {
        if (busy) return false
        busy = true
        transitionType = DesktopHubTransitionType.InstantFade
        (hostScope ?: scope).launch {
            try {
                opacity.animateTo(1f, tween(120))
                action()
                withFrameNanos { }
                opacity.animateTo(0f, tween(120))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                onFailure(error)
            } finally {
                withContext(NonCancellable) {
                    opacity.snapTo(0f)
                    transitionType = DesktopHubTransitionType.None
                }
                busy = false
            }
        }
        return true
    }

    /**
     * Atmospheric node infiltration transition:
     * 1. Hub fades to black (240ms).
     * 2. Exploration screen loads underneath while the cyber arrival card fades in and beams expand (~700ms).
     * 3. Arrival card dissolves into the room exploration screen (260ms).
     */
    fun requestNodeDeployment(
        scope: CoroutineScope,
        nodeTitle: String,
        regionTitle: String?,
        entryRoomTitle: String?,
        description: String?,
        onFailure: (Exception) -> Unit = {},
        action: () -> Unit
    ): Boolean {
        if (busy) return false
        busy = true
        transitionType = DesktopHubTransitionType.DeployToNode(
            nodeTitle = nodeTitle,
            regionTitle = regionTitle,
            entryRoomTitle = entryRoomTitle,
            description = description
        )
        (hostScope ?: scope).launch {
            try {
                // Phase 1: Fade out outgoing screen
                cardAlpha.snapTo(0f)
                cardScale.snapTo(0.96f)
                beamProgress.snapTo(0f)
                opacity.animateTo(1f, tween(200, easing = FastOutSlowInEasing))

                // Swap screens while hidden behind black backdrop
                action()
                withFrameNanos { }

                // Phase 2: Reveal arrival card and cyber sweep
                coroutineScope {
                    launch { cardScale.animateTo(1.02f, tween(500, easing = LinearOutSlowInEasing)) }
                    launch { beamProgress.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
                    cardAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }

                // Short hold for reading and atmosphere
                delay(300)

                // Phase 3: Dissolve arrival card into live room
                coroutineScope {
                    launch { cardAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
                    opacity.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                onFailure(error)
            } finally {
                withContext(NonCancellable) {
                    opacity.snapTo(0f)
                    cardAlpha.snapTo(0f)
                    beamProgress.snapTo(0f)
                    transitionType = DesktopHubTransitionType.None
                }
                busy = false
            }
        }
        return true
    }

    /**
     * Subtle tactical disengage transition when returning to Hub from a room:
     * A clean, quick dissolve into dark with muted telemetry status (300ms total).
     */
    fun requestHubReturn(
        scope: CoroutineScope,
        onFailure: (Exception) -> Unit = {},
        action: () -> Unit
    ): Boolean {
        if (busy) return false
        busy = true
        transitionType = DesktopHubTransitionType.ReturnToHub
        (hostScope ?: scope).launch {
            try {
                cardAlpha.snapTo(0f)
                launch { cardAlpha.animateTo(1f, tween(160, easing = FastOutSlowInEasing)) }
                opacity.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                action()
                withFrameNanos { }
                delay(50)
                cardAlpha.animateTo(0f, tween(100, easing = FastOutSlowInEasing))
                opacity.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                onFailure(error)
            } finally {
                withContext(NonCancellable) {
                    opacity.snapTo(0f)
                    cardAlpha.snapTo(0f)
                    transitionType = DesktopHubTransitionType.None
                }
                busy = false
            }
        }
        return true
    }
}

internal val LocalHubTravel = staticCompositionLocalOf<DesktopHubTravel?> { null }
