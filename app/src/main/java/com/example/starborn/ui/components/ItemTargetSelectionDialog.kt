package com.example.starborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TargetSelectionOption(
    val id: String,
    val name: String,
    val detail: String?
)

@Composable
fun ItemTargetSelectionDialog(
    itemName: String,
    targets: List<TargetSelectionOption>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    backgroundColor: Color = Color.Black.copy(alpha = 0.4f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    textColor: Color = Color.White,
    accentColor: Color = Color.White
) {
    if (targets.isEmpty()) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = accentColor)
            }
        },
        title = { Text("Use $itemName") },
        text = {
            Column {
                Text(
                    text = "Choose a target:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                targets.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onSelect(option.id) },
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = backgroundColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = option.name,
                                color = textColor,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                            option.detail?.takeIf { it.isNotBlank() }?.let { detail ->
                                Text(
                                    text = detail,
                                    color = textColor.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
