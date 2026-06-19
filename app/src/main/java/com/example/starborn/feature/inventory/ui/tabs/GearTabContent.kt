package com.example.starborn.feature.inventory.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.inventory.PartyMemberStatus
import com.example.starborn.feature.inventory.ui.*
import com.example.starborn.feature.inventory.ui.components.*

@Composable
fun GearTabContent(
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    healPulses: Map<String, InventoryHealPulse>,
    slots: List<String>,
    selectedSlot: String,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    availableItems: List<InventoryEntry>,
    selectedEntry: InventoryEntry?,
    equippedEntry: InventoryEntry?,
    equippedItemName: String?,
    equippedItemNames: Map<String, String?>,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    onSelectCharacter: (String) -> Unit,
    onSelectSlot: (String) -> Unit,
    onSelectEntry: (InventoryEntry) -> Unit,
    onEquip: (() -> Unit)?,
    onUnequip: (() -> Unit)?,
    onSelectModSlot: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PartyMemberSidebar(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            partyMembers = partyMembers,
            selectedCharacterId = selectedCharacterId,
            healPulses = healPulses,
            onSelectCharacter = onSelectCharacter,
            accentColor = accentColor,
            borderColor = borderColor
        )
        GearWorkshop(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            slots = slots,
            selectedSlot = selectedSlot,
            onSelectSlot = onSelectSlot,
            equippedItemName = equippedItemName,
            equippedItemNames = equippedItemNames,
            equippedItems = equippedItems,
            completedMilestones = completedMilestones,
            availableItems = availableItems,
            selectedEntry = selectedEntry,
            equippedEntry = equippedEntry,
            onSelectEntry = onSelectEntry,
            onEquip = onEquip,
            onUnequip = onUnequip,
            onSelectModSlot = onSelectModSlot,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets
        )
    }
}

@Composable
fun WeaponGearTabContent(
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    healPulses: Map<String, InventoryHealPulse>,
    slots: List<String>,
    selectedSlot: String,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    equippedItemNames: Map<String, String?>,
    equippedWeaponItem: Item?,
    equippedWeaponName: String?,
    equippedWeaponSummary: String?,
    hasUnlockedWeapons: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onSelectCharacter: (String) -> Unit,
    onSelectSlot: (String) -> Unit,
    onSelectModSlot: (String) -> Unit,
    onOpenWeaponPicker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PartyMemberSidebar(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            partyMembers = partyMembers,
            selectedCharacterId = selectedCharacterId,
            healPulses = healPulses,
            onSelectCharacter = onSelectCharacter,
            accentColor = accentColor,
            borderColor = borderColor
        )
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GearLoadoutGrid(
                slots = slots,
                selectedSlot = selectedSlot,
                equippedItemNames = equippedItemNames,
                equippedItems = equippedItems,
                completedMilestones = completedMilestones,
                onSelectSlot = onSelectSlot,
                onSelectModSlot = onSelectModSlot,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets
            )
            WeaponSlotPanel(
                equippedWeaponItem = equippedWeaponItem,
                equippedWeaponName = equippedWeaponName,
                equippedWeaponSummary = equippedWeaponSummary,
                hasUnlockedWeapons = hasUnlockedWeapons,
                onOpenWeaponPicker = onOpenWeaponPicker,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets
            )
        }
    }
}

@Composable
fun ArmorGearTabContent(
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    healPulses: Map<String, InventoryHealPulse>,
    slots: List<String>,
    selectedSlot: String,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    equippedItemNames: Map<String, String?>,
    equippedArmorItem: Item?,
    equippedArmorName: String?,
    equippedArmorSummary: String?,
    hasUnlockedArmors: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onSelectCharacter: (String) -> Unit,
    onSelectSlot: (String) -> Unit,
    onSelectModSlot: (String) -> Unit,
    onOpenArmorPicker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PartyMemberSidebar(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight(),
            partyMembers = partyMembers,
            selectedCharacterId = selectedCharacterId,
            healPulses = healPulses,
            onSelectCharacter = onSelectCharacter,
            accentColor = accentColor,
            borderColor = borderColor
        )
        Column(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GearLoadoutGrid(
                slots = slots,
                selectedSlot = selectedSlot,
                equippedItemNames = equippedItemNames,
                equippedItems = equippedItems,
                completedMilestones = completedMilestones,
                onSelectSlot = onSelectSlot,
                onSelectModSlot = onSelectModSlot,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets
            )
            ArmorSlotPanel(
                equippedArmorItem = equippedArmorItem,
                equippedArmorName = equippedArmorName,
                equippedArmorSummary = equippedArmorSummary,
                hasUnlockedArmors = hasUnlockedArmors,
                onOpenArmorPicker = onOpenArmorPicker,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets
            )
        }
    }
}

@Composable
private fun WeaponSlotPanel(
    equippedWeaponItem: Item?,
    equippedWeaponName: String?,
    equippedWeaponSummary: String?,
    hasUnlockedWeapons: Boolean,
    onOpenWeaponPicker: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val weaponLabel = equippedWeaponName?.takeIf { it.isNotBlank() } ?: "No weapon equipped"
    val description = equippedWeaponItem?.description?.takeIf { it.isNotBlank() }
        ?: if (equippedWeaponItem == null) "No weapon equipped yet." else "No description available."
    val iconRes = remember(equippedWeaponItem?.id) {
        equippedWeaponItem?.let { itemIconRes(it) } ?: slotIconRes("weapon")
    }
    val buttonHeight = if (largeTouchTargets) 56.dp else 48.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Weapon Loadout", accentColor = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (largeTouchTargets) 88.dp else 76.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weaponLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    equippedWeaponSummary?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                }
            }
            if (!hasUnlockedWeapons) {
                Text(
                    text = "No weapons unlocked yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.7f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenWeaponPicker,
                    enabled = hasUnlockedWeapons,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Choose Weapon")
                }
            }
        }
    }
}

@Composable
private fun ArmorSlotPanel(
    equippedArmorItem: Item?,
    equippedArmorName: String?,
    equippedArmorSummary: String?,
    hasUnlockedArmors: Boolean,
    onOpenArmorPicker: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val armorLabel = equippedArmorName?.takeIf { it.isNotBlank() } ?: "No armor equipped"
    val description = equippedArmorItem?.description?.takeIf { it.isNotBlank() }
        ?: if (equippedArmorItem == null) "No armor equipped yet." else "No description available."
    val iconRes = remember(equippedArmorItem?.id) {
        equippedArmorItem?.let { itemIconRes(it) } ?: slotIconRes("armor")
    }
    val buttonHeight = if (largeTouchTargets) 56.dp else 48.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Armor Loadout", accentColor = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (largeTouchTargets) 88.dp else 76.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = armorLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    equippedArmorSummary?.let { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                }
            }
            if (!hasUnlockedArmors) {
                Text(
                    text = "No armor unlocked yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.7f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenArmorPicker,
                    enabled = hasUnlockedArmors,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text("Choose Armor")
                }
            }
        }
    }
}
