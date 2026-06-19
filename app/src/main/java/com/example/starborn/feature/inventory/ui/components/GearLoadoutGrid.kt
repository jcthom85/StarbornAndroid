package com.example.starborn.feature.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.feature.inventory.ui.*
import java.util.Locale

@Composable
fun GearLoadoutGrid(
    slots: List<String>,
    selectedSlot: String,
    equippedItemNames: Map<String, String?>,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    onSelectSlot: (String) -> Unit,
    onSelectModSlot: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val preferredOrder = listOf("weapon", "armor", "accessory", "snack")
    val orderedSlots = remember(slots) {
        val normalized = slots.map { it.lowercase(Locale.getDefault()) }
        val ordered = preferredOrder.filter { normalized.contains(it) }
        ordered + normalized.filterNot { preferredOrder.contains(it) }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        orderedSlots.forEach { slot ->
            LoadoutSlotCard(
                slot = slot,
                isSelected = slot.equals(selectedSlot, ignoreCase = true),
                equippedItemName = equippedItemNames[slot],
                equippedItems = equippedItems,
                equippedItemNames = equippedItemNames,
                completedMilestones = completedMilestones,
                onClick = { onSelectSlot(slot) },
                onSelectModSlot = onSelectModSlot,
                accentColor = accentColor,
                borderColor = borderColor,
                foregroundColor = foregroundColor,
                largeTouchTargets = largeTouchTargets,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoadoutSlotCard(
    slot: String,
    isSelected: Boolean,
    equippedItemName: String?,
    equippedItems: Map<String, String>,
    equippedItemNames: Map<String, String?>,
    completedMilestones: Set<String>,
    onClick: () -> Unit,
    onSelectModSlot: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    modifier: Modifier
) {
    val background = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.28f)
    val border = if (isSelected) accentColor else borderColor.copy(alpha = 0.5f)
    val slotLabel = slotDisplayName(slot)
    val name = equippedItemName?.takeIf { it.isNotBlank() } ?: "Empty"
    val cardHeight = if (largeTouchTargets) 104.dp else 96.dp
    val normalizedSlot = slot.lowercase(Locale.getDefault())
    val supportsMods = normalizedSlot == "weapon" || normalizedSlot == "armor"
    val modSlots = if (normalizedSlot == "weapon") {
        listOf("weapon_mod1", "weapon_mod2")
    } else if (normalizedSlot == "armor") {
        listOf("armor_mod1", "armor_mod2")
    } else {
        emptyList()
    }
    val baseEquippedId = equippedItems[normalizedSlot]
    val modsLocked = supportsMods && baseEquippedId.isNullOrBlank()

    Surface(
        modifier = modifier
            .heightIn(min = cardHeight)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(slotIconRes(slot)),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slotLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = foregroundColor.copy(alpha = if (equippedItemName.isNullOrBlank()) 0.6f else 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isSelected) {
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (supportsMods) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (modsLocked) 0.6f else 1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mods",
                        style = MaterialTheme.typography.labelSmall,
                        color = foregroundColor.copy(alpha = 0.7f)
                    )
                    modSlots.forEach { modSlot ->
                        val isUnlocked = GearRules.isModSlotUnlocked(modSlot, completedMilestones)
                        val modName = if (isUnlocked) {
                            equippedItemNames[modSlot]?.takeIf { it.isNotBlank() } ?: "Empty"
                        } else {
                            "Locked"
                        }
                        val chipEnabled = !modsLocked && isUnlocked
                        ModSlotChip(
                            label = if (modSlot.endsWith("1")) "M1" else "M2",
                            name = modName,
                            enabled = chipEnabled,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            foregroundColor = foregroundColor,
                            onClick = { onSelectModSlot(modSlot) }
                        )
                    }
                }
                if (modsLocked) {
                    Text(
                        text = "Equip ${slotLabel.lowercase(Locale.getDefault())} to unlock mods.",
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.6f)
                    )
                } else {
                    val anyLockedByStory = modSlots.any { !GearRules.isModSlotUnlocked(it, completedMilestones) }
                    if (anyLockedByStory) {
                        Text(
                            text = "Unlocks after Main Story milestone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = foregroundColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModSlotChip(
    label: String,
    name: String,
    enabled: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(12.dp))
            .let { base -> if (enabled) base.clickable { onClick() } else base },
        color = Color.Black.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = foregroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
