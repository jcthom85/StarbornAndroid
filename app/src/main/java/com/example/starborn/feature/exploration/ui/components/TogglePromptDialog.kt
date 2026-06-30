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
    ChoicePromptDialog(
        title = prompt.title,
        message = prompt.message,
        primaryLabel = prompt.enableLabel,
        secondaryLabel = prompt.disableLabel,
        dismissLabel = "Cancel",
        primaryEnabled = !prompt.isOn,
        secondaryEnabled = prompt.isOn,
        onPrimary = { onSelect(true) },
        onSecondary = { onSelect(false) },
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
fun ChoicePromptDialog(
    title: String,
    message: String,
    primaryLabel: String,
    secondaryLabel: String,
    dismissLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = onPrimary,
                    enabled = primaryEnabled
                ) {
                    Text(primaryLabel)
                }
                TextButton(
                    onClick = onSecondary,
                    enabled = secondaryEnabled
                ) {
                    Text(secondaryLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel)
            }
        }
    )
}
