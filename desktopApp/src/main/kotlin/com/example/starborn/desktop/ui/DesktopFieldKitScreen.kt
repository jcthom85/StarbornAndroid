package com.example.starborn.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

enum class FieldKitTab { LOADOUT, CARGO, CRAFTING }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopFieldKitScreen(services: DesktopAppServices, onClose: () -> Unit) {
    var tab by remember { mutableStateOf(if (services.openTinkeringOnNextFieldKit) FieldKitTab.CRAFTING else FieldKitTab.LOADOUT) }
    val focus = remember { FocusRequester() }
    val ui by services.exploration.uiState.collectAsState()
    LaunchedEffect(Unit) { services.openTinkeringOnNextFieldKit = false; focus.requestFocus() }
    Column(Modifier.fillMaxSize().background(FieldMenuDesign.shell).padding(24.dp).focusRequester(focus).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) false else when (event.key) {
            Key.Escape -> { onClose(); true }
            Key.One -> { tab = FieldKitTab.LOADOUT; true }
            Key.Two -> { tab = FieldKitTab.CARGO; true }
            Key.Three -> { tab = FieldKitTab.CRAFTING; true }
            else -> false
        }
    }.focusable(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Field kit", color = FieldMenuDesign.cyan, style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onClose) { Text("Back") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldKitTab.entries.forEachIndexed { index, value -> OutlinedButton(onClick = { tab = value }) {
                Text("${value.name.lowercase().replaceFirstChar { it.uppercase() }}${if (tab == value) " ✓" else ""}")
            } }
        }
        ui.menuFeedback?.let { Text(it, color = FieldMenuDesign.gold) }
        Surface(Modifier.weight(1f).fillMaxWidth(), color = FieldMenuDesign.elevatedPanel) {
            Box(Modifier.padding(16.dp)) { when (tab) {
                FieldKitTab.LOADOUT -> DesktopRuntimeGearContent(services)
                FieldKitTab.CARGO -> DesktopFieldCargoContent(services)
                FieldKitTab.CRAFTING -> DesktopTinkeringContent(services)
            } }
        }
    }
}

@Composable
private fun DesktopFieldCargoContent(services: DesktopAppServices) {
    val inventory by services.inventoryService.state.collectAsState()
    val session by services.sessionStore.state.collectAsState()
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (inventory.isEmpty()) item { Text("No cargo collected yet.", color = FieldMenuDesign.text) }
        items(inventory, key = { it.item.id }) { entry ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${entry.item.name} ×${entry.quantity}", style = MaterialTheme.typography.titleMedium)
                Text(entry.item.description.orEmpty())
                if (services.exploration.isPreparedMeal(entry.item.id)) {
                    Button(onClick = { services.exploration.useInventoryItem(entry.item.id, replaceMeal = true) }) {
                        Text(if (session.activeMealBuff == null) "Eat meal" else "Eat and replace current meal")
                    }
                } else if (entry.item.effect != null && entry.item.type == "consumable") {
                    session.partyMembers.forEach { member -> TextButton(onClick = { services.exploration.useInventoryItem(entry.item.id, member) }) { Text("Use on $member") } }
                }
            } }
        }
    }
}
