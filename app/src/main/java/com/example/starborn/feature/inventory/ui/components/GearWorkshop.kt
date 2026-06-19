package com.example.starborn.feature.inventory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.feature.inventory.ui.*

@Composable
fun GearWorkshop(
    modifier: Modifier,
    slots: List<String>,
    selectedSlot: String,
    onSelectSlot: (String) -> Unit,
    equippedItemName: String?,
    equippedItemNames: Map<String, String?>,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    availableItems: List<InventoryEntry>,
    selectedEntry: InventoryEntry?,
    equippedEntry: InventoryEntry?,
    onSelectEntry: (InventoryEntry) -> Unit,
    onEquip: (() -> Unit)?,
    onUnequip: (() -> Unit)?,
    onSelectModSlot: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Column(
        modifier = modifier,
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

        ComparisonPanel(
            slotLabel = slotDisplayName(selectedSlot),
            selectedEntry = selectedEntry,
            equippedEntry = equippedEntry,
            equippedItemName = equippedItemName,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets,
            onEquip = onEquip,
            onUnequip = onUnequip
        )

        InventoryItemsColumn(
            modifier = Modifier.weight(1f),
            items = availableItems,
            selectedItem = selectedEntry,
            onSelectItem = onSelectEntry,
            accentColor = accentColor,
            borderColor = borderColor,
            equippedItemId = equippedEntry?.item?.id,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets,
            statSummary = { entry -> primaryStatSummary(entry.item) }
        )
    }
}
