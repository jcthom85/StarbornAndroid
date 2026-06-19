package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.exploration.viewmodel.TogglePromptUi

@Composable
fun TogglePromptDialog(
    prompt: TogglePromptUi,
    onSelect: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(prompt.title) },
        text = { Text(prompt.message) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = { onSelect(true) },
                    enabled = !prompt.isOn
                ) {
                    Text(prompt.enableLabel)
                }
                TextButton(
                    onClick = { onSelect(false) },
                    enabled = prompt.isOn
                ) {
                    Text(prompt.disableLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
