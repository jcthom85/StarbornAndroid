package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun DesktopControlsDialog(onDismiss: () -> Unit, accentColor: Color = Color(0xFF63E6FF)) {
    val bindings = LocalDesktopKeyBindings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 640.dp),
            shape = MaterialTheme.shapes.large, color = Color(0xFF060E16),
            border = BorderStroke(1.dp, accentColor.copy(alpha = .6f))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Controls", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismiss, modifier = Modifier.desktopPointerHover()) { Text("Close") }
                }
                Text("Keyboard and mouse - Change exploration keys in Settings > Controls",
                    style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { ControlSectionHeading("Exploration", accentColor) }
                    items(explorationControlHints(bindings)) { ControlHintRow(it) }
                    item { ControlSectionHeading("Field menu", accentColor) }
                    items(fieldMenuControlHints) { ControlHintRow(it) }
                    item { ControlSectionHeading("Combat", accentColor) }
                    items(listOf(
                        DesktopControlHint("1 / 2 / 3 / 4", "Attack / Abilities / Items / Equipped snack"),
                        DesktopControlHint("R / L", "Retreat / Battle log"),
                        DesktopControlHint("Arrows / Enter", "Select / confirm target"),
                        DesktopControlHint("Tab", "Select next ready party member"),
                        DesktopControlHint("Space", "Respond to timing prompts"),
                        DesktopControlHint("Esc / Right click", "Cancel targeting or action menu")
                    )) { ControlHintRow(it) }
                    item { ControlSectionHeading("Dialogue, stations, and display", accentColor) }
                    items(listOf(
                        DesktopControlHint("Enter", "Reveal dialogue, then advance a line without choices"),
                        DesktopControlHint("1-9", "Choose a revealed dialogue response"),
                        DesktopControlHint("Fishing: Space", "Cast / hook / hold to reel"),
                        DesktopControlHint("Arcade: Enter / Esc", "Start or resume / pause"),
                        DesktopControlHint("Arcade: WASD / Arrows", "Move; Space / E are primary / secondary actions"),
                        DesktopControlHint("Harmonic Pulse: D/F/J/K", "Strike the four lanes"),
                        DesktopControlHint("Map: Mouse wheel", "Zoom; drag to pan"),
                        DesktopControlHint("F11 / Alt+Enter", "Toggle fullscreen / borderless window")
                    )) { ControlHintRow(it) }
                }
            }
        }
    }
}

@Composable
private fun ControlSectionHeading(label: String, accent: Color) {
    HorizontalDivider()
    Text(label, color = accent, style = MaterialTheme.typography.titleSmall)
}

@Composable
internal fun ControlHintRow(hint: DesktopControlHint) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(hint.keys, color = Color(0xFF63E6FF), style = MaterialTheme.typography.labelMedium)
        Text(hint.description, style = MaterialTheme.typography.bodySmall)
    }
}
