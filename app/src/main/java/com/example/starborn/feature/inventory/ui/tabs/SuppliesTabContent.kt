package com.example.starborn.feature.inventory.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.feature.inventory.PartyMemberStatus
import com.example.starborn.feature.inventory.ui.*
import com.example.starborn.feature.inventory.ui.components.InventoryCategoryColumn
import com.example.starborn.feature.inventory.ui.components.InventoryDetailPanel
import com.example.starborn.feature.inventory.ui.components.InventoryItemsColumn
import com.example.starborn.feature.inventory.ui.components.PartyMemberSidebar

@Composable
fun SuppliesTabContent(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    items: List<InventoryEntry>,
    selectedItem: InventoryEntry?,
    partyMembers: List<PartyMemberStatus>,
    selectedCharacterId: String?,
    healPulses: Map<String, InventoryHealPulse>,
    onSelectItem: (InventoryEntry) -> Unit,
    onSelectCharacter: (String) -> Unit,
    onUseItem: (InventoryEntry) -> Unit,
    credits: Int,
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
        InventoryCategoryColumn(
            categories = categories,
            selectedCategory = selectedCategory,
            onSelectCategory = onSelectCategory,
            credits = credits,
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets
        )
        InventoryItemsColumn(
            modifier = Modifier
                .weight(0.42f)
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
                .weight(0.34f)
                .fillMaxHeight(),
            entry = selectedItem,
            accentColor = accentColor,
            borderColor = borderColor,
            foregroundColor = foregroundColor,
            largeTouchTargets = largeTouchTargets,
            primaryActionLabel = selectedItem?.let { "Use Item" },
            primaryActionEnabled = selectedItem?.item?.effect != null,
            onPrimaryAction = selectedItem?.let { entry -> { onUseItem(entry) } }
        )
        PartyMemberSidebar(
            modifier = Modifier
                .weight(0.24f)
                .fillMaxHeight(),
            partyMembers = partyMembers,
            selectedCharacterId = selectedCharacterId,
            healPulses = healPulses,
            onSelectCharacter = onSelectCharacter,
            accentColor = accentColor,
            borderColor = borderColor
        )
    }
}
