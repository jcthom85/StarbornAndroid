package com.example.starborn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.mainmenu.SaveSlotSummary
import java.util.Locale

@Composable
fun SaveLoadDialog(
    mode: String,
    slots: List<SaveSlotSummary>,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    panelColor: Color,
    borderColor: Color,
    textColor: Color
) {
    val isSave = mode.equals("save", ignoreCase = true)
    val title = if (isSave) "SELECT SLOT TO SAVE" else "SELECT SLOT TO LOAD"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            color = panelColor,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = accentColor,
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onRefresh) {
                        Text("Refresh", color = accentColor)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(slots) { summary ->
                        SaveSlotRow(
                            summary = summary,
                            isSave = isSave,
                            onSave = onSave,
                            onLoad = onLoad,
                            onDelete = onDelete,
                            accent = accentColor,
                            textColor = textColor,
                            borderColor = borderColor
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun SaveSlotRow(
    summary: SaveSlotSummary,
    isSave: Boolean,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    accent: Color,
    textColor: Color,
    borderColor: Color
) {
    Surface(
        tonalElevation = 6.dp,
        color = Color(0xFF121A24),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val slotLabel = when {
                summary.isQuickSave -> "Quicksave"
                summary.isAutosave -> "Autosave"
                else -> "Slot ${summary.slot}"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = slotLabel, color = accent, style = MaterialTheme.typography.titleSmall)
                if (summary.state != null && !summary.isEmpty) {
                    Text(
                        text = "Lv ${summary.state.playerLevel}".uppercase(Locale.getDefault()),
                        color = textColor.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(text = summary.title, color = textColor, maxLines = 1)
            Text(
                text = summary.subtitle,
                color = textColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSave && !summary.isAutosave) {
                    Button(onClick = { onSave(summary.slot) }) {
                        Text(if (summary.isEmpty) "Save" else "Overwrite")
                    }
                }
                if (!isSave) {
                    Button(
                        onClick = { onLoad(summary.slot) },
                        enabled = !summary.isEmpty
                    ) {
                        Text(
                            when {
                                summary.isAutosave -> "Load Autosave"
                                summary.isQuickSave -> "Load Quicksave"
                                else -> "Load"
                            }
                        )
                    }
                }
                val deleteEnabled = !summary.isEmpty || summary.isQuickSave || summary.isAutosave
                TextButton(
                    onClick = { onDelete(summary.slot) },
                    enabled = deleteEnabled
                ) {
                    Text(
                        text = if (summary.isAutosave) "Clear" else "Delete",
                        color = accent
                    )
                }
            }
        }
    }
}
