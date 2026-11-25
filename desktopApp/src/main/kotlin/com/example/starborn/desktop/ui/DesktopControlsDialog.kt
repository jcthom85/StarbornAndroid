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

import com.example.starborn.desktop.DesktopAction

@Composable
fun DesktopControlsDialog(onDismiss: () -> Unit, accentColor: Color = Color(0xFF63E6FF)) {
    val bindings = LocalDesktopKeyBindings.current
    val moveLabel = listOf(DesktopAction.NORTH, DesktopAction.WEST, DesktopAction.SOUTH, DesktopAction.EAST).joinToString("") { bindings.label(it) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.widthIn(max = 760.dp).fillMaxWidth().heightIn(max = 640.dp),
            shape = MaterialTheme.shapes.large, color = Color(0xFF060E16),
            border = BorderStroke(1.dp, accentColor.copy(alpha = .6f))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Controls", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismiss, modifier = Modifier.desktopPointerHover()) { Text("Close") }
                }
                Text("Keyboard and mouse - Customize Exploration, Combat, and Arcade keys in Settings > Controls",
                    style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { ControlSectionHeading("Exploration", accentColor) }
                    items(explorationControlHints(bindings)) { ControlHintRow(it) }
                    item { ControlSectionHeading("Field menu", accentColor) }
                    items(fieldMenuControlHints) { ControlHintRow(it) }
                    item { ControlSectionHeading("Combat", accentColor) }
                    items(combatControlHints(bindings)) { ControlHintRow(it) }
                    item { ControlSectionHeading("Gamepad & Steam Deck", accentColor) }
                    items(listOf(
                        DesktopControlHint("Left Stick / D-Pad", "Travel rooms, navigate menus, aim in Arcade"),
                        DesktopControlHint("A (✕) / B (○)", "Confirm, Interact, Attack, Reel / Cancel, Back"),
                        DesktopControlHint("X (□) / Y (△)", "Room Actions or Combat Abilities / Inventory or Combat Items"),
                        DesktopControlHint("LB (L1) / RB (R1)", "Combat Guard & Next Ready Crew · Menu/Shop Tab Cycling · Exploration Quick Save & Map"),
                        DesktopControlHint("LT (L2) / RT (R2)", "Combat Snack & Retreat · Sub-category Switch · Harmonic Pulse Lanes 2 & 3"),
                        DesktopControlHint("Menu (+) / View (-)", "Field Menu or Combat Pause / Controls Guide or Battle Log")
                    )) { ControlHintRow(it) }
                    item { ControlSectionHeading("Dialogue, stations, and display", accentColor) }
                    items(listOf(
                        DesktopControlHint("Enter", "Reveal dialogue, then advance a line without choices"),
                        DesktopControlHint("1-9", "Choose a revealed dialogue response"),
                        DesktopControlHint("Shop: 1 / 2 / 3 · Q / E · Arrows / Enter", "Switch Buy/Sell/Chat · Filter category · Select & transact"),
                        DesktopControlHint("Fishing: Space", "Cast / hook / hold to reel"),
                        DesktopControlHint("Arcade: Enter / Esc", "Start or resume / pause"),
                        DesktopControlHint("Arcade: $moveLabel / Arrows", "Move; Space / ${bindings.label(DesktopAction.INTERACT)} are primary / secondary actions"),
                        DesktopControlHint("Harmonic Pulse: ${arcadeControlHints(bindings)}", "Strike the four lanes"),
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
