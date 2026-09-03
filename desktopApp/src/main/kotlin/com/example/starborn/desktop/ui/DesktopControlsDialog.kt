package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun DesktopControlsDialog(
    onDismiss: () -> Unit,
    accentColor: Color = Color(0xFF63E6FF)
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF060E16).copy(alpha = 0.96f),
            border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
            shadowElevation = 24.dp,
            modifier = Modifier.width(680.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor.copy(alpha = 0.10f), Color.Transparent)
                        )
                    )
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, accentColor)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Keyboard,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.padding(6.dp).size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TACTICAL CONTROLS & KEYBINDINGS",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Desktop Keyboard & Gamepad Direct Navigation",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                HorizontalDivider(color = accentColor.copy(alpha = 0.3f))

                // Control Sections Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Exploration & Navigation
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "EXPLORATION & TRAVERSAL",
                            color = Color(0xFFFFC857),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        KeybindRow("W / Up Arrow", "Travel North")
                        KeybindRow("S / Down Arrow", "Travel South")
                        KeybindRow("A / Left Arrow", "Travel West / Arcade")
                        KeybindRow("D / Right Arrow", "Travel East")
                        KeybindRow("[1] / [2] / [3]", "Talk to NPCs / Engage Threat")
                        KeybindRow("[E] / [Space]", "Interact with Room Objects")
                        KeybindRow("[M]", "Open Star Map / Hub")
                        KeybindRow("[I]", "Tinker Loadout & Cargo")
                    }

                    // Right Column: Stations & Quick Actions
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "STATIONS & QUICK ACTIONS",
                            color = Color(0xFFFFC857),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        KeybindRow("[F]", "Cast Angling Rod (Fishing)")
                        KeybindRow("[T]", "The Great Frontier Film Archive")
                        KeybindRow("[V]", "Outpost Merchant / Shop")
                        KeybindRow("[R]", "Stasis Rest Pod (Heal Squad)")
                        KeybindRow("[ESC]", "Field Menu / Pause / Back")
                        KeybindRow("[F5]", "Quick Save to Stasis Disk 1")
                        KeybindRow("[F11]", "Toggle Fullscreen / Windowed")
                        KeybindRow("[H] / [?]", "Toggle this Controls Guide")
                    }
                }

                HorizontalDivider(color = accentColor.copy(alpha = 0.2f))

                // Footer tip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Mouse cursor or touch clicks remain fully active alongside keyboard controls at all times.",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeybindRow(key: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(5.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
        ) {
            Text(
                text = key,
                color = Color(0xFF63E6FF),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
        Text(
            text = action,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.5.sp
        )
    }
}
