package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

enum class FieldKitTab {
    LOADOUT, CARGO, CRAFTING
}

@Composable
fun DesktopFieldKitScreen(
    services: DesktopAppServices,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableStateOf(FieldKitTab.LOADOUT) }
    val allItems = remember { services.itemRepository.allItems().associateBy { it.id } }
    val sessionState by services.sessionStore.state.collectAsState()

    // Equip State from session
    val equippedWeapons = sessionState.equippedWeapons
    val equippedArmors = sessionState.equippedArmors
    val equippedItems = sessionState.equippedItems

    val activeWeaponId = equippedWeapons["nova"] ?: sessionState.equippedItems["weapon"]
    val activeArmorId = equippedArmors["nova"] ?: sessionState.equippedItems["armor"]
    val activeAccessoryId = sessionState.equippedItems["accessory"]
    val activeSnackId = sessionState.equippedItems["snack"]

    val activeWeapon = activeWeaponId?.let { allItems[it] }
    val activeArmor = activeArmorId?.let { allItems[it] }
    val activeAccessory = activeAccessoryId?.let { allItems[it] }
    val activeSnack = activeSnackId?.let { allItems[it] }

    var selectedItemForDetail by remember { mutableStateOf<Item?>(activeWeapon ?: allItems.values.firstOrNull()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04060E))
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            onClose()
                            true
                        }
                        Key.One -> {
                            activeTab = FieldKitTab.LOADOUT
                            true
                        }
                        Key.Two -> {
                            activeTab = FieldKitTab.CARGO
                            true
                        }
                        Key.Three -> {
                            activeTab = FieldKitTab.CRAFTING
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
                    .background(FieldMenuDesign.shell)
                    .border(BorderStroke(1.dp, FieldMenuDesign.border.copy(alpha = 0.3f)))
                    .padding(horizontal = 28.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "FIELD KIT & LOADOUT MANAGER",
                        color = FieldMenuDesign.cyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopKitTabButton("[1] LOADOUT & GEAR", activeTab == FieldKitTab.LOADOUT) { activeTab = FieldKitTab.LOADOUT }
                        DesktopKitTabButton("[2] CARGO HOLD", activeTab == FieldKitTab.CARGO) { activeTab = FieldKitTab.CARGO }
                        DesktopKitTabButton("[3] FABRICATION", activeTab == FieldKitTab.CRAFTING) { activeTab = FieldKitTab.CRAFTING }
                    }
                }

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = FieldMenuDesign.elevatedPanel),
                    border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "[ESC] RETURN", color = FieldMenuDesign.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Main Body Area
            when (activeTab) {
                FieldKitTab.LOADOUT -> {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left Paper Doll Character Card
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
                                .background(FieldMenuDesign.panel)
                                .border(BorderStroke(1.2.dp, FieldMenuDesign.border.copy(alpha = 0.4f)), RoundedCornerShape(FieldMenuDesign.cardRadius))
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Character Portrait & Vitals
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val portrait = rememberDesktopAssetPainter("nova_portrait", services.assetProvider)
                                    Image(
                                        painter = portrait,
                                        contentDescription = "Nova",
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(CircleShape)
                                            .border(BorderStroke(2.dp, FieldMenuDesign.cyan), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )

                                    Text(text = "NOVA // EXPEDITION SPECIALIST", color = FieldMenuDesign.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Level ${sessionState.playerLevel}  •  ${sessionState.playerCredits} CR  •  ${sessionState.playerXp} XP", color = FieldMenuDesign.gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // 4 Paper Doll Equipment Slots (Weapon, Armor, Accessory, Snack)
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DesktopPaperDollSlot(
                                        slotName = "PRIMARY WEAPON",
                                        item = activeWeapon,
                                        defaultName = "Standard Kinetic Blaster",
                                        accentColor = FieldMenuDesign.gold,
                                        onClick = { selectedItemForDetail = activeWeapon }
                                    )

                                    DesktopPaperDollSlot(
                                        slotName = "BODY ARMOR",
                                        item = activeArmor,
                                        defaultName = "Nano-Weave Field Suit",
                                        accentColor = Color(0xFF2979FF),
                                        onClick = { selectedItemForDetail = activeArmor }
                                    )

                                    DesktopPaperDollSlot(
                                        slotName = "TACTICAL ACCESSORY",
                                        item = activeAccessory,
                                        defaultName = "Sensor Targeting Matrix",
                                        accentColor = FieldMenuDesign.cyan,
                                        onClick = { selectedItemForDetail = activeAccessory }
                                    )

                                    DesktopPaperDollSlot(
                                        slotName = "FIELD SNACK / STIMPACK",
                                        item = activeSnack,
                                        defaultName = "Nutrient Ration Block",
                                        accentColor = Color(0xFF00E676),
                                        onClick = { selectedItemForDetail = activeSnack }
                                    )
                                }
                            }
                        }

                        // Right Inventory Equip Drawer
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(FieldMenuDesign.cardRadius))
                                .background(FieldMenuDesign.panel)
                                .border(BorderStroke(1.2.dp, FieldMenuDesign.border.copy(alpha = 0.4f)), RoundedCornerShape(FieldMenuDesign.cardRadius))
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "AVAILABLE FIELD GEAR // CLICK TO EQUIP",
                                    color = FieldMenuDesign.gold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                val equippableItems = sessionState.inventory.keys
                                    .mapNotNull { allItems[it] }
                                    .filter { it.equipment != null || it.type == "weapon" || it.type == "armor" || it.type == "snack" }

                                if (equippableItems.isEmpty()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(text = "— No compatible gear modules in cargo. Fabricate schematics or scavenge sectors. —", color = FieldMenuDesign.textMuted, fontSize = 12.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(equippableItems) { item ->
                                            val isEquipped = activeWeaponId == item.id || activeArmorId == item.id || activeAccessoryId == item.id || activeSnackId == item.id
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isEquipped) FieldMenuDesign.cyan.copy(alpha = 0.15f) else FieldMenuDesign.elevatedPanel)
                                                    .border(BorderStroke(1.dp, if (isEquipped) FieldMenuDesign.cyan else FieldMenuDesign.border.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedItemForDetail = item
                                                        // Toggle Equip
                                                        val slot = item.equipment?.slot ?: if (item.type == "weapon") "weapon" else if (item.type == "armor") "armor" else "accessory"
                                                        val updatedEquipped = sessionState.equippedItems.toMutableMap()
                                                        if (isEquipped) {
                                                            updatedEquipped.remove(slot)
                                                        } else {
                                                            updatedEquipped[slot] = item.id
                                                        }
                                                        services.sessionStore.restore(sessionState.copy(equippedItems = updatedEquipped))
                                                    }
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(text = item.name, color = FieldMenuDesign.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = item.description.orEmpty(), color = FieldMenuDesign.textMuted, fontSize = 11.sp, maxLines = 1)
                                                    }

                                                    Text(
                                                        text = if (isEquipped) "EQUIPPED" else "EQUIP",
                                                        color = if (isEquipped) Color(0xFF00E676) else FieldMenuDesign.cyan,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Detail Inspector Footer
                                selectedItemForDetail?.let { item ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF060D18))
                                            .border(BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(text = item.name.uppercase(), color = FieldMenuDesign.cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(text = item.description.orEmpty(), color = FieldMenuDesign.textMuted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                FieldKitTab.CARGO -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        DesktopInventoryTabBody(sessionState = sessionState, allItems = allItems)
                    }
                }
                FieldKitTab.CRAFTING -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        DesktopTinkeringFabricatorTabContent(services = services, sessionState = sessionState, allItems = allItems)
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopPaperDollSlot(
    slotName: String,
    item: Item?,
    defaultName: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(FieldMenuDesign.elevatedPanel)
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = slotName, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(text = item?.name ?: defaultName, color = FieldMenuDesign.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (item != null) Color(0xFF00E676) else FieldMenuDesign.textMuted.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun DesktopKitTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(FieldMenuDesign.controlRadius))
            .background(if (isSelected) FieldMenuDesign.cyan.copy(alpha = 0.2f) else Color.Transparent)
            .border(BorderStroke(1.dp, if (isSelected) FieldMenuDesign.cyan else Color(0x337FE6FF)), RoundedCornerShape(FieldMenuDesign.controlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) FieldMenuDesign.cyan else FieldMenuDesign.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
