package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.feature.exploration.presentation.AstraArcadeDefinition
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

@Composable
internal fun CargoRecordTabs(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("cargo" to "Supplies", "catch" to "Catch Log", "relics" to "Relics").forEach { (id, label) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(label) })
        }
    }
}

@Composable
internal fun AstraRecreationDialog(games: List<AstraArcadeDefinition>, prizesAvailable: Boolean,
    onGame: (String) -> Unit, onFilms: () -> Unit, onPrizes: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(16.dp), color = FieldMenuDesign.shell,
            contentColor = FieldMenuDesign.text, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.heightIn(max = 600.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("Recreation", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onClose) { Text("Close") }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onFilms, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Great Frontier films") }
                    if (prizesAvailable) OutlinedButton(onClick = onPrizes, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Arcade prizes") }
                    Text("Restored cabinets", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.titleSmall)
                    if (games.isEmpty()) Text("Restore a cabinet at the workbench to add a game here.", style = MaterialTheme.typography.bodySmall)
                    games.forEach { game ->
                        Button(onClick = { onGame(game.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(game.title) }
                    }
                }
            }
        }
    }
}
