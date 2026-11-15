package com.example.starborn.ui.cinematic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** A tap and an autoplay timer can both fire before Compose shows the next step. */
@Composable
fun rememberCinematicAdvance(sceneId: String, stepIndex: Int, onAdvance: () -> Unit): () -> Unit {
    val latestOnAdvance = rememberUpdatedState(onAdvance)
    return remember(sceneId, stepIndex) {
        var advanced = false
        val advance: () -> Unit = {
            if (!advanced) {
                advanced = true
                latestOnAdvance.value()
            }
        }
        advance
    }
}
