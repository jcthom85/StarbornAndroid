package com.example.starborn.ui.exploration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*

const val TEMPLE_GATE_ROOM_ID = "sector9_temple_gate"
const val TEMPLE_GATE_CLOSED_ART = "images/rooms/world_2/temple_gate.webp"

data class TempleGateRevealState(val active: Boolean, val progress: Float)

/** A saved open gate stays open; only a witnessed closed-to-open change plays the reveal. */
@Composable
fun rememberTempleGateReveal(roomId: String?, open: Boolean, reducedMotion: Boolean): TempleGateRevealState {
    val progress = remember { Animatable(1f) }
    var active by remember { mutableStateOf(false) }
    var wasOpen by remember(roomId) { mutableStateOf(open) }
    val pending = roomId == TEMPLE_GATE_ROOM_ID && open && !wasOpen && !reducedMotion
    LaunchedEffect(roomId, open, reducedMotion) {
        val shouldPlay = roomId == TEMPLE_GATE_ROOM_ID && open && !wasOpen && !reducedMotion
        wasOpen = open
        if (shouldPlay) {
            active = true
            try {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(2400, easing = LinearEasing))
            } finally {
                active = false
            }
        }
    }
    // Suppress notices immediately, including the frame before the effect starts.
    return TempleGateRevealState(roomId == TEMPLE_GATE_ROOM_ID && open && (active || pending), if (pending) 0f else progress.value)
}
