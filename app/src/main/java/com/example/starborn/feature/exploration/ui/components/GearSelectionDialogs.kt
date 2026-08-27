package com.example.starborn.feature.exploration.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            color = Color(0xFF071018),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.65f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$slotLabel Loadout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = characterName.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Unequip Button Row
                if (equippedId != null && equippedId.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENTLY EQUIPPED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        OutlinedButton(
                            onClick = { onUnequip() },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Unequip",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                if (options.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF040A10).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.15f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "No compatible $slotLabel found in inventory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options, key = { it.id }) { option ->
                            val normalizedId = option.id.lowercase(Locale.getDefault())
                            val isEquipped = normalizedId == equippedNormalized
                            val shape = RoundedCornerShape(12.dp)
                            val iconRes = remember(option.id + option.type) { previewItemIconRes(option.type) }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .clickable { onSelect(option.id) },
                                shape = shape,
                                color = if (isEquipped) accentColor.copy(alpha = 0.15f) else Color(0xFF0A1624).copy(alpha = 0.7f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isEquipped) Color(0xFFFFC857) else borderColor.copy(alpha = 0.35f)
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
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            if (option.quantity > 1) {
                                                Text(
                                                    text = "x${option.quantity}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = accentColor
                                                )
                                            }
                                        }
                                        Text(
                                            text = option.type.uppercase(Locale.getDefault()),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = accentColor.copy(alpha = 0.75f)
                                        )
                                        itemDetailSummary(option)?.let { effect ->
                                            Text(
                                                text = effect,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = Color.White.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (isEquipped) {
                                        Surface(
                                            color = Color(0xFFFFC857).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFFFFC857).copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "EQUIPPED",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                color = Color(0xFFFFC857),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        normalized.contains("mod") -> R.drawable.item_icon_material
        else -> R.drawable.item_icon_generic
    }
}

private fun itemDetailSummary(option: InventoryPreviewItemUi): String? {
    val eff = option.effect
    if (eff != null) {
        val parts = mutableListOf<String>()
        eff.restoreHp?.let { parts.add("Restores $it HP") }
        eff.amount?.let { parts.add("Amount: $it") }
        eff.damage?.let { parts.add("Deals $it DMG") }
        eff.singleBuff?.let { parts.add("+${it.value} ${it.stat.uppercase(Locale.getDefault())}") }
        eff.buffs?.forEach { parts.add("+${it.value} ${it.stat.uppercase(Locale.getDefault())}") }
        if (parts.isNotEmpty()) return parts.joinToString(" • ")
    }
    val eq = option.equipment
    if (eq != null) {
        val parts = mutableListOf<String>()
        eq.defense?.let { parts.add("DEF ${if (it >= 0) "+$it" else "$it"}") }
        eq.hpBonus?.let { parts.add("HP ${if (it >= 0) "+$it" else "$it"}") }
        eq.statMods?.forEach { (k, v) -> parts.add("${k.uppercase(Locale.getDefault())} ${if (v >= 0) "+$v" else "$v"}") }
        if (parts.isNotEmpty()) return parts.joinToString(" • ")
    }
    return null
}
