package com.example.starborn.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.ui.events.UiEvent
import com.example.starborn.ui.events.UiEventBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

private data class ToastItem(
    val dedupKey: String,
    val text: String,
    val instanceId: Long
)

@Composable
fun ProgressToastOverlay(
    uiEventBus: UiEventBus,
    modifier: Modifier = Modifier,
    autoHideMillis: Long = 1_800
) {
    val toasts = remember { mutableStateListOf<ToastItem>() }
    val lastByKey = remember { mutableStateMapOf<String, Long>() }
    val mergeWindow = 800L

    LaunchedEffect(uiEventBus) {
        fun enqueueToast(
            key: String,
            label: String,
            merge: Boolean
        ) {
            val now = System.currentTimeMillis()
            val existingIndex = toasts.indexOfLast { it.dedupKey == key }
            val lastShown = lastByKey[key] ?: 0L
            if (merge && existingIndex >= 0 && now - lastShown <= mergeWindow) {
                toasts[existingIndex] = ToastItem(key, label, now)
            } else {
                if (toasts.size >= 5) {
                    toasts.removeAt(0)
                }
                toasts.add(ToastItem(key, label, now))
            }
            lastByKey[key] = now
        }

        uiEventBus.events.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    val key = "toast:${event.id}"
                    val label = event.text.ifBlank { event.id }
                    enqueueToast(key, label, merge = false)
                }
                else -> Unit
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            toasts.takeLast(3).forEach { toast ->
                ToastCard(
                    toast = toast,
                    autoHideMillis = autoHideMillis,
                    onTimeout = { id -> toasts.removeAll { it.instanceId == id } }
                )
            }
        }
    }
}

@Composable
private fun ToastCard(
    toast: ToastItem,
    autoHideMillis: Long,
    onTimeout: (Long) -> Unit
) {
    var visible by remember(toast.instanceId) { mutableStateOf(true) }
    val instanceId = toast.instanceId
    LaunchedEffect(instanceId) {
        delay(autoHideMillis)
        visible = false
        delay(150)
        onTimeout(instanceId)
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .padding(top = 8.dp)
                .wrapContentWidth()
        ) {
            Text(
                text = toast.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
