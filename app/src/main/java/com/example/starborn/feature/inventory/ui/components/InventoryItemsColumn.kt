package com.example.starborn.feature.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.R
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.feature.inventory.ui.*
import java.util.Locale

@Composable
fun InventoryItemsColumn(
    modifier: Modifier,
    items: List<InventoryEntry>,
    selectedItem: InventoryEntry?,
    onSelectItem: (InventoryEntry) -> Unit,
    accentColor: Color,
    borderColor: Color,
    equippedItemId: String?,
    highContrastMode: Boolean,
    largeTouchTargets: Boolean,
    statSummary: (InventoryEntry) -> String? = { null }
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = if (highContrastMode) 0.35f else 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        val listState = rememberLazyListState()
        if (items.isEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "No items in this category yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                items(items, key = { it.item.id }) { entry ->
                    InventoryItemRow(
                        entry = entry,
                        selected = entry.item.id == selectedItem?.item?.id,
                        isEquipped = equippedItemId?.equals(entry.item.id, ignoreCase = true) == true,
                        accentColor = accentColor,
                        onClick = { onSelectItem(entry) },
                        largeTouchTargets = largeTouchTargets,
                        statSummary = statSummary(entry)
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryItemRow(
    entry: InventoryEntry,
    selected: Boolean,
    isEquipped: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    largeTouchTargets: Boolean,
    statSummary: String? = null
) {
    val background = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
    val borderColor = if (selected) accentColor else Color.White.copy(alpha = 0.25f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeTouchTargets) 76.dp else 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconRes = remember(entry.item.id) { itemIconRes(entry.item) }
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .heightIn(min = 48.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.item.description?.takeIf { it.isNotBlank() } ?: entry.item.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isEquipped) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "EQUIPPED",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                statSummary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = "x${entry.quantity}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                entry.item.rarity?.let {
                    Text(
                        text = it.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
