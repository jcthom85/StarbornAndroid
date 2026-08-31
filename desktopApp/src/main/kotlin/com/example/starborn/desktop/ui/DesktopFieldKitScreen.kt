package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Item

private val NeonCyan = Color(0xFF00F5D4)
private val NeonPink = Color(0xFFFF007F)
private val NeonAmber = Color(0xFFFFB703)
private val DeepSpaceDark = Color(0xFF05070D)
private val PanelDark = Color(0xFF090E18)
private val PanelBorder = Color(0xFF1B283E)
private val TextWhite = Color(0xFFF0F4FA)
private val TextMuted = Color(0xFF8FA1B7)

enum class FieldKitTab {
    CARGO, CRAFTING, PROGRESSION
}

@Composable
fun DesktopFieldKitScreen(
    services: DesktopAppServices,
    onClose: () -> Unit
) {
    val items = remember { services.itemRepository.allItems() }
    var selectedTab by remember { mutableStateOf(FieldKitTab.CARGO) }
    var selectedItemIndex by remember { mutableStateOf(0) }
    val selectedItem = items.getOrNull(selectedItemIndex) ?: items.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceDark)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            onClose()
                            true
                        }
                        Key.Tab -> {
                            selectedTab = if (selectedTab == FieldKitTab.CARGO) FieldKitTab.CRAFTING else FieldKitTab.CARGO
                            true
                        }
                        Key.DirectionDown -> {
                            if (items.isNotEmpty()) {
                                selectedItemIndex = (selectedItemIndex + 1) % items.size
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (items.isNotEmpty()) {
                                selectedItemIndex = if (selectedItemIndex - 1 < 0) items.size - 1 else selectedItemIndex - 1
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF0050812))
                    .border(BorderStroke(1.dp, PanelBorder))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "FIELD KIT & WORKBENCH",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopFieldKitTabButton("CARGO HOLD", selectedTab == FieldKitTab.CARGO) { selectedTab = FieldKitTab.CARGO }
                        DesktopFieldKitTabButton("ENGINEERING", selectedTab == FieldKitTab.CRAFTING) { selectedTab = FieldKitTab.CRAFTING }
                    }
                }

                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141F32), contentColor = NeonCyan),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Text(text = "CLOSE [ESC]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Main 2-Column Workbench Layout
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Item / Recipe List
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelDark.copy(alpha = 0.94f))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(14.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = if (selectedTab == FieldKitTab.CARGO) "CARGO MANIFEST (${items.size} ITEMS)" else "AVAILABLE SCHEMATICS",
                            color = NeonAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(items) { index, item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (index == selectedItemIndex) NeonCyan.copy(alpha = 0.16f) else Color(0xFF0C1322))
                                        .border(
                                            BorderStroke(1.dp, if (index == selectedItemIndex) NeonCyan else PanelBorder),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedItemIndex = index }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                color = if (index == selectedItemIndex) NeonCyan else TextWhite,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = item.description.orEmpty(),
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            text = "${item.value} CR",
                                            color = NeonAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column: Selected Item Inspection, Stats & Action Panel
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PanelDark.copy(alpha = 0.94f))
                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(14.dp))
                        .padding(24.dp)
                ) {
                    if (selectedItem != null) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "ITEM SCHEMATIC & TELEMETRY",
                                    color = NeonPink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = selectedItem.name,
                                    color = TextWhite,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = selectedItem.description.orEmpty(),
                                    color = TextMuted,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Item Specifications Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0C1322))
                                        .border(BorderStroke(1.dp, PanelBorder), RoundedCornerShape(10.dp))
                                        .padding(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "Base Market Value", color = TextMuted, fontSize = 12.sp)
                                            Text(text = "${selectedItem.value} Credits", color = NeonAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "Rarity Tier", color = TextMuted, fontSize = 12.sp)
                                            Text(text = "Standard Issue", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Action Button
                            Button(
                                onClick = { /* Equip / Use */ },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text(
                                    text = if (selectedTab == FieldKitTab.CARGO) "EQUIP TO VANGUARD [ENTER]" else "SYNTHESIZE SCHEMATIC [ENTER]",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF04060E))
                    .border(BorderStroke(1.dp, PanelBorder))
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTROLS: [↑/↓] Select Item  •  [TAB] Switch Tab  •  [ENTER] Action  •  [ESC] Close",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "ENGINEERING MODULE ACTIVE",
                    color = TextMuted.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DesktopFieldKitTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                BorderStroke(1.dp, if (isSelected) NeonCyan else PanelBorder),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) NeonCyan else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
