package com.example.starborn.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.desktop.DesktopSaveSlotInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SaveDialogMode { SAVE, LOAD }

@Composable
fun DesktopSaveLoadDialog(services: DesktopAppServices, initialMode: SaveDialogMode = SaveDialogMode.SAVE,
    currentRoomTitle: String? = null, onLoadState: () -> Unit, onDismiss: () -> Unit, allowLoad: Boolean = true) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(initialMode) }
    var metas by remember { mutableStateOf<Map<Int, DesktopSaveSlotInfo?>>(emptyMap()) }
    var occupied by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    suspend fun refresh() {
        withContext(Dispatchers.IO) {
            occupied = (-1..3).filter(services.saveManager::hasSave).toSet()
            metas = (-1..3).associateWith(services.saveManager::getSlotMetadata)
        }
    }
    LaunchedEffect(services) { refresh() }
    fun save(slot: Int) {
        val snapshot = services.sessionStore.state.value
        scope.launch {
            busy = true
            val success = withContext(Dispatchers.IO) { services.saveManager.saveGame(slot, snapshot, currentRoomTitle) }
            message = if (success) "Saved successfully." else services.saveManager.lastError.value ?: "Unable to save."
            refresh(); busy = false
        }
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(Modifier.widthIn(max = 880.dp).fillMaxWidth(.94f).fillMaxHeight(.85f), color = FieldMenuDesign.shell, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = .3f))) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Save archive", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { mode = SaveDialogMode.SAVE }, enabled = !busy) { Text("Save") }
                    if (allowLoad) TextButton(onClick = { mode = SaveDialogMode.LOAD }, enabled = !busy) { Text("Load") }
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Close") }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val slots = if (mode == SaveDialogMode.LOAD) listOf(0, -1, 1, 2, 3) else listOf(-1, 1, 2, 3)
                    slots.forEach { slot -> item {
                        val meta = metas[slot]
                        val name = when (slot) { -1 -> "Quick save"; 0 -> "Autosave"; else -> "Slot $slot" }
                        DesktopMenuCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(name, style = MaterialTheme.typography.titleMedium, color = FieldMenuDesign.gold)
                            Text(meta?.let { "${it.roomTitle} · Level ${it.playerLevel} · ${it.credits} credits\n${it.formattedDate}" }
                                ?: if (slot in occupied) "This save could not be read. Original files are retained for recovery." else "Empty")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (mode == SaveDialogMode.SAVE) Button(onClick = {
                                    if (slot in occupied) confirmation = "Overwrite $name?" to { save(slot) } else save(slot)
                                }, enabled = !busy) { Text(if (slot in occupied) "Overwrite" else "Save") }
                                else Button(onClick = { scope.launch {
                                    busy = true
                                    val loaded = withContext(Dispatchers.IO) { services.loadSlot(slot) }
                                    busy = false
                                    if (loaded) { onLoadState(); onDismiss() }
                                    else message = services.saveManager.lastError.value ?: "Unable to load this save."
                                } }, enabled = !busy && slot in occupied) { Text("Load") }
                                if (slot in occupied) TextButton(onClick = {
                                    confirmation = "Delete $name?" to { scope.launch {
                                        busy = true
                                        val deleted = withContext(Dispatchers.IO) { services.saveManager.deleteSlot(slot) }
                                        message = if (deleted) "Save deleted." else services.saveManager.lastError.value
                                        refresh(); busy = false
                                    } }
                                }, enabled = !busy) { Text("Delete") }
                            }
                        } }
                    } }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it) }
            }
        }
        confirmation?.let { (prompt, action) -> AlertDialog(onDismissRequest = { confirmation = null },
            text = { Text(prompt) }, confirmButton = { TextButton(onClick = { confirmation = null; action() }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } }) }
    }
}
