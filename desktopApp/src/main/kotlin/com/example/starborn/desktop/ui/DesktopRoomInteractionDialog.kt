package com.example.starborn.desktop.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.actionKey
import com.example.starborn.feature.exploration.presentation.InlineActionTarget
import com.example.starborn.feature.exploration.presentation.resolveRoomDarkness
import com.example.starborn.feature.exploration.viewmodel.ExplorationUiState

@Composable
internal fun DesktopRoomInteractionDialog(services: DesktopAppServices, ui: ExplorationUiState,
    onDismiss: () -> Unit, onSelect: (InlineActionTarget) -> Unit) {
    val targets = buildList {
        ui.actions.distinctBy { it.actionKey() }.forEach { add(InlineActionTarget.Room(it)) }
        if (!resolveRoomDarkness(ui)) {
            ui.npcs.distinct().forEach { add(InlineActionTarget.Npc(it)) }
            ui.enemies.distinct().forEach { add(InlineActionTarget.Enemy(it, services.enemyDefinitions[it]?.name ?: it.replace('_', ' '))) }
        }
    }
    fun enabled(target: InlineActionTarget): Boolean = target !is InlineActionTarget.Room || ui.actionHints[target.action.actionKey()]?.locked != true
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; focus.requestFocus() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.widthIn(max = 580.dp).fillMaxWidth().heightIn(max = 520.dp)
            .focusRequester(focus).onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false
                else if (event.key == Key.Escape) { onDismiss(); true }
                else {
                    val index = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine).indexOf(event.key)
                    if (index >= 0 && index < targets.size) {
                        targets[index].takeIf(::enabled)?.let(onSelect); true
                    } else false
                }
            }.focusable(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Room interactions", style = MaterialTheme.typography.titleLarge)
                Text("Choose a number, or use Tab and Enter. Esc closes this list.", style = MaterialTheme.typography.bodySmall)
                if (targets.isEmpty()) Text("There are no interactions available here.")
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(targets) { index, target ->
                        val label = when (target) {
                            is InlineActionTarget.Room -> target.action.name
                            is InlineActionTarget.Npc -> "Talk to ${target.name}"
                            is InlineActionTarget.Enemy -> "Engage ${target.label}"
                        }
                        OutlinedButton(onClick = { onSelect(target) }, enabled = enabled(target),
                            modifier = Modifier.fillMaxWidth().desktopPointerHover(enabled(target)).testTag("room-interaction-$index")) {
                            Text(if (index < 9) "${index + 1}. $label" else label)
                        }
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}

@Composable
internal fun DesktopQuickLoadDialog(services: DesktopAppServices, onDismiss: () -> Unit, onLoad: () -> Unit) {
    val info = remember { services.saveManager.getSlotMetadata(-1) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (info == null) "No quicksave available" else "Load quicksave?") },
        text = { Text(if (info == null) "Create a quicksave before loading it."
            else "Return to ${info.roomTitle ?: "your saved location"}? Progress since the quicksave will be replaced.") },
        confirmButton = { if (info != null) TextButton(onClick = onLoad) { Text("Load quicksave") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (info == null) "Close" else "Cancel") } })
}
