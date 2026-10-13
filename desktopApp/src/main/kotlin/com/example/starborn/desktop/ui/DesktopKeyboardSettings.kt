package com.example.starborn.desktop.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.*
import kotlinx.coroutines.launch

internal val LocalDesktopKeyBindings = staticCompositionLocalOf { DesktopKeyBindings() }

internal data class DesktopControlHint(val keys: String, val description: String)

internal fun explorationControlHints(bindings: DesktopKeyBindings): List<DesktopControlHint> =
    DesktopAction.entries.map { action ->
        val alias = when (action) {
            DesktopAction.NORTH -> " / Up arrow"
            DesktopAction.SOUTH -> " / Down arrow"
            DesktopAction.WEST -> " / Left arrow"
            DesktopAction.EAST -> " / Right arrow"
            DesktopAction.MENU -> if (bindings.label(action) != "Esc") " / Esc" else ""
            else -> ""
        }
        DesktopControlHint(bindings.label(action) + alias, action.label)
    }

internal val fieldMenuControlHints = listOf(
    DesktopControlHint("0 / 1 / 2", "Stats / Inventory / Tinker"),
    DesktopControlHint("3 / 4 / 5", "Journal / Map / Settings"),
    DesktopControlHint("Alt+1–6", "Stats / Inventory / Tinker / Journal / Map / Settings"),
    DesktopControlHint("Ctrl+Tab", "Next page; Shift reverses"),
    DesktopControlHint("Tab / Shift+Tab", "Move keyboard focus"),
    DesktopControlHint("F5 / F9", "Quick save / load quicksave"),
    DesktopControlHint("Esc / Right click", "Back / close menu")
)

@Composable
internal fun DesktopKeyboardSettings(services: DesktopAppServices) {
    val bindings by services.userSettingsStore.keyBindings.collectAsState(initial = DesktopKeyBindings())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<DesktopAction?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Exploration key bindings", style = MaterialTheme.typography.titleSmall)
        Text("Choose a key to change it. Arrow keys, Esc, Tab, and Enter remain available for navigation.",
            style = MaterialTheme.typography.bodySmall)
        DesktopAction.entries.forEach { action ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(action.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { editing = action; message = null },
                    modifier = Modifier.desktopPointerHover().testTag("bind-${action.name}")) {
                    Text(bindings.label(action))
                }
            }
        }
        TextButton(onClick = { scope.launch { services.userSettingsStore.resetKeyBindings() } }) { Text("Restore default keys") }
    }
    editing?.let { action ->
        val focus = remember { FocusRequester() }
        LaunchedEffect(action) { withFrameNanos { }; focus.requestFocus() }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(action.label) },
            text = {
                Column(Modifier.focusRequester(focus).onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) false
                    else if (event.key == Key.Escape) { editing = null; true }
                    else {
                        val name = DesktopKeyBindings.nameOf(event.key)
                        val conflict = when {
                            event.isCtrlPressed || event.isAltPressed || event.isMetaPressed || event.isShiftPressed -> "Choose a single key without modifiers."
                            name == null -> "Choose a letter, number, Space, or function key."
                            else -> bindings.conflict(action, name)
                        }
                        if (conflict != null) message = conflict
                        else scope.launch {
                            runCatching { services.userSettingsStore.setKeyBinding(action, name!!) }
                                .onSuccess { editing = null }
                                .onFailure { message = it.message }
                        }
                        true
                    }
                }.focusable().testTag("key-binding-capture"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Press the new key. Esc cancels.")
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}
