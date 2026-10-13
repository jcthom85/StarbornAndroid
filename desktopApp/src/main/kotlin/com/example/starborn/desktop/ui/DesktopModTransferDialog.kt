package com.example.starborn.desktop.ui

import androidx.compose.material3.*
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.starborn.domain.inventory.ModLocation
import com.example.starborn.domain.inventory.ModTransferRequest

@Composable
internal fun DesktopModTransferDialog(request: ModTransferRequest, name: String, onDismiss: () -> Unit, onMove: (ModLocation) -> Unit) {
    var selectedKey by remember(request) { mutableStateOf(request.sources.first().key) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Move $name?") },
        text = { Column {
            Text("Choose the equipped copy to move to ${request.target.label}. Its current slot will become empty.")
            request.sources.forEach { source ->
                FilterChip(selected = selectedKey == source.key, onClick = { selectedKey = source.key }, label = { Text(source.label) })
            }
        } },
        confirmButton = { TextButton(onClick = { request.sources.firstOrNull { it.key == selectedKey }?.let(onMove) },
            modifier = Modifier.testTag("mod-transfer-confirm")) { Text("Move mod") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
