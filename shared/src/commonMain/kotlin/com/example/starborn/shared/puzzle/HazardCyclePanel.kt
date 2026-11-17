package com.example.starborn.shared.puzzle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private val safeguards = listOf("Coolant spray", "Pressure vent", "Belt advance")
private val beatClues = mapOf(
    "heat" to "Hammer running. Heat climbs.",
    "pressure" to "Hammer paused. Pressure spikes.",
    "quiet" to "Hammer clear. The lane is open."
)
private val safeguardHelp = listOf("Cools a running hammer", "Releases trapped pressure", "Moves the belt through a clear lane")

@Composable
fun HazardCyclePanel(prompt: String, dials: List<SignalDial>, feedback: String?,
                     onChange: (String, Float) -> Unit, modifier: Modifier = Modifier) {
    val amber = Color(0xFFFFC47C)
    Column(modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).testTag("forge-hazard-panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(feedback ?: prompt, style = MaterialTheme.typography.bodyMedium)
        dials.forEachIndexed { index, dial ->
            var choosing by remember(dial.id) { mutableStateOf(false) }
            val selected = dial.value.toInt()
            val issue = feedback != null && !dial.matched
            val ready = feedback != null && dial.matched
            val signal = when {
                issue -> Color(0xFFFF9A84)
                ready -> Color(0xFFA1ECC5)
                else -> amber
            }
            Surface(Modifier.fillMaxWidth().testTag("forge-beat-${dial.id}"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF221A16), contentColor = Color(0xFFF8E6D7),
                border = BorderStroke(1.dp, signal.copy(alpha = .55f))) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Beat ${index + 1} · ${dial.id.replaceFirstChar { it.uppercaseChar() }}",
                        style = MaterialTheme.typography.labelLarge, color = signal)
                    Text(beatClues.getValue(dial.id), style = MaterialTheme.typography.bodySmall)
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { choosing = true },
                            modifier = Modifier.fillMaxWidth().testTag("forge-select-${dial.id}")) {
                            Text(safeguards.getOrNull(selected) ?: "Choose safeguard", color = signal)
                        }
                        DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                            safeguards.forEachIndexed { option, label ->
                                DropdownMenuItem(modifier = Modifier.testTag("forge-safeguard-$option"), text = {
                                    Column {
                                        Text(label)
                                        Text(safeguardHelp[option], style = MaterialTheme.typography.bodySmall)
                                    }
                                }, onClick = {
                                    choosing = false
                                    onChange(dial.id, option.toFloat())
                                })
                            }
                        }
                    }
                    if (feedback != null) {
                        Text(if (issue) "Check this beat" else "Safeguard accepted", color = signal,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
