package com.example.starborn.feature.exploration.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.R
import com.example.starborn.feature.exploration.viewmodel.InventoryPreviewItemUi
import java.util.Locale

@Composable
fun GearSelectionDialog(
    characterName: String,
    slotLabel: String,
    options: List<InventoryPreviewItemUi>,
    equippedId: String?,
    accentColor: Color,
    borderColor: Color,
    onSelect: (String?) -> Unit,
    onUnequip: () -> Unit,
    onDismiss: () -> Unit
) {
    val equippedNormalized = remember(equippedId) { equippedId?.lowercase(Locale.getDefault()).orEmpty() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = Color(0xFF0A0F18),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$characterName - $slotLabel",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onUnequip() },
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                    ) {
                        Text("Unequip")
                    }
                }

                if (options.isEmpty()) {
                    Text(
                        text = "No $slotLabel available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options, key = { it.id }) { option ->
                            val normalizedId = option.id.lowercase(Locale.getDefault())
                            val isEquipped = normalizedId == equippedNormalized
                            val shape = RoundedCornerShape(14.dp)
                            val iconRes = remember(option.id + option.type) { previewItemIconRes(option.type) }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .clickable { onSelect(option.id) },
                                color = if (isEquipped) accentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isEquipped) accentColor else borderColor.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = option.type,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (isEquipped) {
                                        Surface(
                                            color = accentColor.copy(alpha = 0.2f),
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
                            }
                        }
                    }
                }
            }
        }
    }
}

@DrawableRes
fun previewItemIconRes(type: String?): Int {
    val normalized = type?.lowercase(Locale.getDefault()) ?: return R.drawable.item_icon_generic
    return when {
        normalized.contains("food") || normalized == "snack" -> R.drawable.item_icon_food
        normalized in setOf("consumable", "medicine", "tonic", "drink") -> R.drawable.item_icon_consumable
        normalized.contains("fish") -> R.drawable.item_icon_fish
        normalized.contains("fishing") -> R.drawable.item_icon_fishing
        normalized.contains("lure") -> R.drawable.item_icon_lure
        normalized.contains("ingredient") ||
            normalized.contains("material") -> R.drawable.item_icon_ingredient
        normalized.contains("component") ||
            normalized.contains("resource") ||
            normalized.contains("part") -> R.drawable.item_icon_material
        normalized.contains("armor") -> R.drawable.item_icon_armor
        normalized.contains("accessory") -> R.drawable.item_icon_accessory
        normalized.contains("weapon") || normalized.contains("gear") -> R.drawable.item_icon_sword
        else -> R.drawable.item_icon_generic
    }
}
