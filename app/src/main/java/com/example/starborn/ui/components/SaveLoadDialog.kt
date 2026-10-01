package com.example.starborn.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.starborn.R
import com.example.starborn.feature.mainmenu.SaveSlotSummary
import com.example.starborn.ui.background.rememberAssetPainter
import java.util.Locale

@Composable
fun SaveLoadDialog(
    mode: String,
    slots: List<SaveSlotSummary>,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    panelColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    modern: Boolean = false,
    largeTargets: Boolean = false,
    highContrast: Boolean = false,
    confirmLoad: Boolean = false,
    busy: Boolean = false,
    errorMessage: String? = null
) {
    val requestSave: (Int) -> Unit = { if (!busy) onSave(it) }
    val requestLoad: (Int) -> Unit = { if (!busy) onLoad(it) }
    val requestDelete: (Int) -> Unit = { if (!busy) onDelete(it) }
    val isSave = mode.equals("save", ignoreCase = true)
    val title = if (isSave) "Save Data" else "Load Data"
    val kicker = if (isSave) "Choose a slot for your progress." else "Choose a saved game to resume."
    val actionLabel = if (isSave) "Save" else "Load"
    val solidPanel = if (highContrast) Color.Black else if (modern) panelColor else Color(0xFF07111A)
    var confirmAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
.padding(if (modern) 16.dp else 24.dp)
                .fillMaxWidth(if (modern) 1f else .92f)
                .fillMaxHeight(0.82f),
            color = solidPanel,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, if (highContrast) Color.White else borderColor.copy(alpha = if (modern) .25f else .9f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.2f).compositeOver(solidPanel),
                                panelColor.copy(alpha = 0.18f).compositeOver(solidPanel),
                                Color(0xFF05080D)
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = 0.16f))
                                .border(1.dp, accentColor.copy(alpha = 0.36f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSave) Icons.Rounded.Save else Icons.Rounded.Download,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = title.uppercase(Locale.getDefault()),
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = kicker,
                                color = textColor.copy(alpha = 0.68f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, textColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                            .semantics { contentDescription = "Close save menu" }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.86f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.72f),
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                if (busy) Text("Saving?", color = textColor, style = MaterialTheme.typography.bodyMedium)
                errorMessage?.let { Text(it, color = Color(0xFFFFAA90), style = MaterialTheme.typography.bodyMedium) }
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
                            onRequestSave = { slot, isOverwrite ->
                                if (isOverwrite) {
                                    confirmAction = "Overwrite Save Slot $slot?\nPrevious save data will be replaced." to { requestSave(slot) }
                                } else {
                                    requestSave(slot)
                                }
                            },
                            onLoad = { slot ->
                                if (confirmLoad) confirmAction = "Load Save Slot $slot?\nCurrent unsaved progress will be replaced." to { requestLoad(slot) }
                                else requestLoad(slot)
                            },
                            onRequestDelete = { slot, label ->
                                confirmAction = "$label?\nSaved progress will be permanently erased." to { requestDelete(slot) }
                            },
                            accent = accentColor,
                            textColor = textColor,
                            borderColor = borderColor,
                            actionLabel = actionLabel,
                            largeTargets = largeTargets,
                            highContrast = highContrast,
                            busy = busy
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            }
        }

        confirmAction?.let { (prompt, action) ->
            AlertDialog(
                onDismissRequest = { confirmAction = null },
                title = {
                    Text(
                        text = "Confirm Action",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            confirmAction = null
                            action()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5252),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmAction = null }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF141C24),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
private fun SaveSlotRow(
    summary: SaveSlotSummary,
    isSave: Boolean,
    onRequestSave: (Int, Boolean) -> Unit,
    onLoad: (Int) -> Unit,
    onRequestDelete: (Int, String) -> Unit,
    accent: Color,
    textColor: Color,
    borderColor: Color,
    actionLabel: String,
    largeTargets: Boolean = false,
    highContrast: Boolean = false,
    busy: Boolean = false
) {
    val slotLabel = when {
        summary.isQuickSave -> "Quicksave"
        summary.isAutosave -> "Autosave"
        else -> "Slot ${summary.slot}"
    }
    val occupied = summary.state != null && !summary.isEmpty

    Surface(
        tonalElevation = 6.dp,
        color = if (highContrast) Color.Black else solidSaveSlotCardColor(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (occupied) accent.copy(alpha = 0.36f) else borderColor.copy(alpha = 0.52f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            if (occupied) accent.copy(alpha = 0.13f).compositeOver(Color(0xFF0A141E)) else Color(0xFF0A141E),
                            Color(0xFF0A141E),
                            Color(0xFF070B10)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (occupied) accent else textColor.copy(alpha = 0.24f))
                )
                Text(
                    text = slotLabel,
                    color = if (occupied) Color.White else textColor.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = summary.title,
                color = if (occupied) textColor else textColor.copy(alpha = 0.58f),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (occupied) FontWeight.SemiBold else FontWeight.Normal),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = summary.subtitle,
                color = textColor.copy(alpha = if (occupied) 0.7f else 0.48f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (occupied && summary.partyPortraits.isNotEmpty()) {
                SaveSlotPartyStrip(
                    portraits = summary.partyPortraits
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSave && !summary.isAutosave) {
                    val isOverwrite = !summary.isEmpty
                    Button(
                        onClick = { onRequestSave(summary.slot, isOverwrite) },
                        enabled = !busy,
                        modifier = Modifier
                            .widthIn(min = 112.dp).heightIn(min = if (largeTargets) 56.dp else 48.dp)
                            .semantics { contentDescription = "Save $slotLabel" },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                    ) {
                        Text(if (summary.isEmpty) actionLabel else "Overwrite")
                    }
                }
                if (!isSave) {
                    Button(
                        onClick = { onLoad(summary.slot) },
                        enabled = !summary.isEmpty && !busy,
                        modifier = Modifier
                            .widthIn(min = 112.dp).heightIn(min = if (largeTargets) 56.dp else 48.dp)
                            .semantics { contentDescription = "Load $slotLabel" },
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                    ) {
                        Text(actionLabel)
                    }
                }
                val deleteEnabled = !summary.isEmpty || summary.isQuickSave || summary.isAutosave
                TextButton(
                    onClick = {
                        val actionText = if (summary.isAutosave) "Clear Autosave" else "Delete $slotLabel"
                        onRequestDelete(summary.slot, actionText)
                    },
                    enabled = deleteEnabled && !busy
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = if (deleteEnabled) accent.copy(alpha = 0.82f) else textColor.copy(alpha = 0.24f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (summary.isAutosave) "Clear" else "Delete",
                        color = if (deleteEnabled) accent.copy(alpha = 0.82f) else textColor.copy(alpha = 0.24f)
                    )
                }
            }
        }
    }
}

private fun solidSaveSlotCardColor(): Color = Color(0xFF0A141E)

@Composable
private fun SaveSlotPartyStrip(
    portraits: List<String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        portraits.take(MAX_SAVE_SLOT_PORTRAITS).forEach { portrait ->
            val painter = rememberAssetPainter(portrait, painterResource(R.drawable.main_menu_background))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private const val MAX_SAVE_SLOT_PORTRAITS = 4

@Composable
private fun SaveSlotChip(
    text: String,
    accent: Color,
    textColor: Color,
    filled: Boolean
) {
    val bg = if (filled) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
    val fg = if (filled) accent else textColor.copy(alpha = 0.64f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            color = fg,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}
