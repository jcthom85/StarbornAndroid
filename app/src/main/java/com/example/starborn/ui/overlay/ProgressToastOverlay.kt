package com.example.starborn.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** One passive notice at a time; its timer runs only while the scene is clear. */
@Composable
fun ProgressToastOverlay(
    state: com.example.starborn.ui.events.QuestPresentationState,
    modifier: Modifier = Modifier,
    autoHideMillis: Long = 1_800,
    deferShowing: Boolean = false
) {
    val current = state.toasts.firstOrNull()
    val visible = current != null && !deferShowing && state.important.isEmpty() && state.updates.isEmpty()
    fun dismiss() { if (state.toasts.firstOrNull() == current) state.toasts.removeAt(0) }
    LaunchedEffect(current, visible) {
        if (visible) { delay(autoHideMillis); dismiss() }
    }
    if (visible && current != null) Box(modifier, contentAlignment = Alignment.BottomStart) {
        Card(modifier = Modifier.padding(16.dp).wrapContentWidth().clickable(onClick = ::dismiss),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
            Text(current.text, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}
