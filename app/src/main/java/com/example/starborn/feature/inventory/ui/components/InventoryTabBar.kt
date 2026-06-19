package com.example.starborn.feature.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.inventory.ui.InventoryTab
import java.text.NumberFormat
import java.util.Locale

@Composable
fun InventoryHeader(
    credits: Int,
    totalItems: Int,
    onBack: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val creditsLabel = remember(credits) { formatter.format(credits.coerceAtLeast(0)) }
    val itemsLabel = remember(totalItems) { formatter.format(totalItems.coerceAtLeast(0)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier
                .heightIn(min = if (largeTouchTargets) 56.dp else 48.dp)
                .widthIn(min = 120.dp)
                .clip(RoundedCornerShape(26.dp))
                .clickable { onBack() },
            color = Color.Transparent,
            border = BorderStroke(1.2.dp, borderColor.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = foregroundColor
                )
                Text(
                    text = "Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = foregroundColor
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Field Inventory",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = foregroundColor
            )
            Text(
                text = "Pack contents synced with crew manifest.",
                style = MaterialTheme.typography.bodySmall,
                color = foregroundColor.copy(alpha = 0.75f)
            )
        }
        InventorySummaryPill(
            label = "Credits",
            value = "$creditsLabel c",
            accentColor = accentColor,
            borderColor = borderColor
        )
        InventorySummaryPill(
            label = "Items",
            value = itemsLabel,
            accentColor = accentColor,
            borderColor = borderColor
        )
    }
}

@Composable
private fun InventorySummaryPill(
    label: String,
    value: String,
    accentColor: Color,
    borderColor: Color
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 60.dp)
            .widthIn(min = 120.dp),
        color = accentColor.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.9f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun InventoryTabRow(
    selectedTab: InventoryTab,
    onSelectTab: (InventoryTab) -> Unit,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InventoryTabButton(
            label = "Supplies",
            selected = selectedTab == InventoryTab.SUPPLIES,
            onClick = { onSelectTab(InventoryTab.SUPPLIES) },
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets,
            modifier = Modifier.weight(1f)
        )
        InventoryTabButton(
            label = "Gear",
            selected = selectedTab == InventoryTab.GEAR,
            onClick = { onSelectTab(InventoryTab.GEAR) },
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets,
            modifier = Modifier.weight(1f)
        )
        InventoryTabButton(
            label = "Key Items",
            selected = selectedTab == InventoryTab.KEY_ITEMS,
            onClick = { onSelectTab(InventoryTab.KEY_ITEMS) },
            accentColor = accentColor,
            borderColor = borderColor,
            largeTouchTargets = largeTouchTargets,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InventoryTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    largeTouchTargets: Boolean,
    modifier: Modifier = Modifier
) {
    val background = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent
    val textColor = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
    Surface(
        modifier = modifier
            .heightIn(min = if (largeTouchTargets) 58.dp else 48.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) accentColor else borderColor.copy(alpha = 0.6f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
        }
    }
}

@Composable
fun InventoryEmptyState(
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    onRefreshInventory: () -> Unit,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Inventory not loaded",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = foregroundColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sync from your save to see gear and supplies.",
                style = MaterialTheme.typography.bodyMedium,
                color = foregroundColor.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            val minHeight = if (largeTouchTargets) 56.dp else 48.dp
            Button(
                onClick = onRefreshInventory,
                modifier = Modifier.heightIn(min = minHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color(0xFF010308)
                )
            ) {
                Text("Reload Inventory")
            }
        }
    }
}
