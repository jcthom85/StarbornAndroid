package com.example.starborn.feature.inventory.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.feature.inventory.ui.components.InventoryDetailPanel
import com.example.starborn.feature.inventory.ui.components.InventoryItemsColumn

@Composable
fun KeyItemsTabContent(
    items: List<InventoryEntry>,
    selectedItem: InventoryEntry?,
    onSelectItem: (InventoryEntry) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InventoryItemsColumn(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            items = items,
            selectedItem = selectedItem,
            onSelectItem = onSelectItem,
            accentColor = accentColor,
            borderColor = borderColor,
            equippedItemId = null,
            highContrastMode = highContrastMode,
            largeTouchTargets = largeTouchTargets
        )
        InventoryDetailPanel(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight(),
            entry = selectedItem,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets
        )
    }
}
