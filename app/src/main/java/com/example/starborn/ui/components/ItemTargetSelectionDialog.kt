package com.example.starborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
    val shape = RoundedCornerShape(18.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp),
            shape = shape,
            color = Color(0xF0061018),
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.48f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.16f),
                                backgroundColor.copy(alpha = 0.82f),
                                Color.Black.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .border(1.dp, accentColor.copy(alpha = 0.44f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = accentColor,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "USE ITEM",
                            color = accentColor.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = itemName,
                            color = textColor,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "Choose who receives the effect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.74f)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    targets.forEach { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(option.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF071018).copy(alpha = 0.76f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.38f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                accentColor.copy(alpha = 0.13f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
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
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.42f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
