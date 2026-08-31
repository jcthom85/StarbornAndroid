package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.desktop.DesktopSaveSlotInfo

private val TitleCyan = Color(0xFF63E6FF)
private val TitleGold = Color(0xFFFFC857)
private val TitleAmber = Color(0xFFFF9F2E)
private val GlassDark = Color(0xFF07111A)
private val PanelBorder = Color(0xFF1E283C)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFFA0B3C6)

enum class SaveDialogMode {
    SAVE, LOAD
}

@Composable
fun DesktopSaveLoadDialog(
    services: DesktopAppServices,
    initialMode: SaveDialogMode = SaveDialogMode.SAVE,
    currentRoomTitle: String? = null,
    onLoadState: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(initialMode) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var confirmPrompt by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    // Slot 0 (Autosave), Slot 1, Slot 2, Slot 3
    var slotMetas by remember {
        mutableStateOf(
            (0..3).map { slot -> slot to services.saveManager.getSlotMetadata(slot) }.toMap()
        )
    }

    fun refreshMetas() {
        slotMetas = (0..3).map { slot -> slot to services.saveManager.getSlotMetadata(slot) }.toMap()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(880.dp)
                .height(600.dp),
            shape = RoundedCornerShape(20.dp),
            color = GlassDark,
            border = BorderStroke(1.5.dp, if (mode == SaveDialogMode.SAVE) TitleCyan.copy(alpha = 0.8f) else TitleGold.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                (if (mode == SaveDialogMode.SAVE) TitleCyan else TitleGold).copy(alpha = 0.12f),
                                Color(0xFF05080D)
                            )
                        )
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background((if (mode == SaveDialogMode.SAVE) TitleCyan else TitleGold).copy(alpha = 0.16f))
                                .border(1.dp, (if (mode == SaveDialogMode.SAVE) TitleCyan else TitleGold).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (mode == SaveDialogMode.SAVE) Icons.Rounded.Save else Icons.Rounded.Download,
                                contentDescription = null,
                                tint = if (mode == SaveDialogMode.SAVE) TitleCyan else TitleGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (mode == SaveDialogMode.SAVE) "CRYOGENIC STASIS ARCHIVE // SAVE GAME" else "CRYOGENIC STASIS ARCHIVE // LOAD GAME",
                                color = if (mode == SaveDialogMode.SAVE) TitleCyan else TitleGold,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (mode == SaveDialogMode.SAVE) "Choose a memory stasis sector to archive current progress." else "Choose a stasis archive to restore your journey.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Mode Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopSaveTabButton("SAVE DATA", mode == SaveDialogMode.SAVE, accent = TitleCyan) {
                        mode = SaveDialogMode.SAVE
                        statusMessage = null
                    }
                    DesktopSaveTabButton("LOAD JOURNEY", mode == SaveDialogMode.LOAD, accent = TitleGold) {
                        mode = SaveDialogMode.LOAD
                        statusMessage = null
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Slot List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val slotsToDisplay = if (mode == SaveDialogMode.LOAD) listOf(0, 1, 2, 3) else listOf(1, 2, 3)

                    items(slotsToDisplay) { slotIdx ->
                        val meta = slotMetas[slotIdx]
                        val isAutosave = slotIdx == 0
                        val isOccupied = meta != null
                        val slotName = if (isAutosave) "AUTOSAVE RECOVERY" else "STASIS SLOT $slotIdx"

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF090E17),
                            border = BorderStroke(1.dp, if (isOccupied) (if (mode == SaveDialogMode.SAVE) TitleCyan else TitleGold).copy(alpha = 0.4f) else PanelBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                if (isOccupied) (if (mode == SaveDialogMode.SAVE) TitleCyan else TitleGold).copy(alpha = 0.08f) else Color.Transparent,
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = slotName,
                                            color = if (mode == SaveDialogMode.SAVE) TitleCyan else TitleGold,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (meta != null) {
                                            Text(
                                                text = "• ${meta.formattedDate}",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (meta != null) {
                                        Text(
                                            text = "Sector: ${meta.roomTitle}  |  Level ${meta.playerLevel}  |  ${meta.credits} CR  |  ${meta.activeQuestsCount} Active Quests",
                                            color = TextWhite.copy(alpha = 0.90f),
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Text(
                                            text = "— Empty Memory Sector —",
                                            color = TextMuted.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (mode == SaveDialogMode.SAVE) {
                                        Button(
                                            onClick = {
                                                if (isOccupied) {
                                                    confirmPrompt = "Overwrite Stasis Slot $slotIdx?\nPrevious save data will be permanently overwritten." to {
                                                        val state = services.sessionStore.state.value
                                                        val success = services.saveManager.saveGame(slotIdx, state, currentRoomTitle)
                                                        if (success) {
                                                            refreshMetas()
                                                            statusMessage = "✓ STASIS ARCHIVED TO SLOT $slotIdx"
                                                        }
                                                    }
                                                } else {
                                                    val state = services.sessionStore.state.value
                                                    val success = services.saveManager.saveGame(slotIdx, state, currentRoomTitle)
                                                    if (success) {
                                                        refreshMetas()
                                                        statusMessage = "✓ STASIS ARCHIVED TO SLOT $slotIdx"
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TitleCyan, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(if (isOccupied) "OVERWRITE" else "SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                val loaded = services.loadSlot(slotIdx)
                                                if (loaded) {
                                                    statusMessage = "✓ JOURNEY RESTORED"
                                                    onLoadState()
                                                    onDismiss()
                                                } else {
                                                    statusMessage = "✗ UNABLE TO LOAD SLOT $slotIdx"
                                                }
                                            },
                                            enabled = isOccupied,
                                            colors = ButtonDefaults.buttonColors(containerColor = TitleGold, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("LOAD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    if (isOccupied && !isAutosave) {
                                        IconButton(
                                            onClick = {
                                                confirmPrompt = "Delete Slot $slotIdx?\nSaved progress will be permanently erased." to {
                                                    services.saveManager.deleteSlot(slotIdx)
                                                    refreshMetas()
                                                    statusMessage = "✓ SLOT $slotIdx CLEARED"
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Status Message Footer
                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (msg.startsWith("✓")) Color(0xFF00E676) else Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        confirmPrompt?.let { (prompt, action) ->
            AlertDialog(
                onDismissRequest = { confirmPrompt = null },
                title = {
                    Text("Confirm Stasis Operation", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(prompt, color = Color.White.copy(alpha = 0.85f))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            confirmPrompt = null
                            action()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmPrompt = null }) {
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
private fun DesktopSaveTabButton(
    title: String,
    isSelected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accent.copy(alpha = 0.16f) else Color(0xFF141924))
            .border(
                BorderStroke(1.dp, if (isSelected) accent else PanelBorder),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) accent else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
