package com.example.starborn.feature.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.feature.inventory.ui.*
import java.util.Locale

@Composable
fun InventoryDetailPanel(
    modifier: Modifier,
    entry: InventoryEntry?,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    equippedItemName: String? = null,
    primaryActionLabel: String? = null,
    primaryActionEnabled: Boolean = true,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    secondaryActionEnabled: Boolean = true,
    onSecondaryAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        if (entry == null) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Select an item to inspect its details.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = entry.item.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = foregroundColor
                )
                Text(
                    text = "Quantity: ${entry.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                entry.item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                equippedItemName?.takeIf { it.isNotBlank() }?.let { label ->
                    HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                    SectionHeading("Currently Equipped", accentColor)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                StatRow("Type", entry.item.type, accentColor)
                StatRow("Value", "${entry.item.value} c", accentColor)
                entry.item.rarity?.let { StatRow("Rarity", it, accentColor) }
                entry.item.buyPrice?.let { StatRow("Purchase", "${it} c", accentColor) }

                entry.item.equipment?.let { equipment ->
                    HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                    SectionHeading("Equipment Stats", accentColor)
                    equipment.weaponType?.let { StatRow("Weapon Type", it, accentColor) }
                    val damageMin = equipment.damageMin
                    val damageMax = equipment.damageMax
                    if (damageMin != null || damageMax != null) {
                        val label = when {
                            damageMin != null && damageMax != null && damageMin != damageMax ->
                                "${damageMin}–${damageMax}"
                            damageMin != null -> damageMin.toString()
                            else -> damageMax.toString()
                        }
                        StatRow("Damage", label, accentColor)
                    }
                    equipment.defense?.let { StatRow("Defense", it.toString(), accentColor) }
                    equipment.hpBonus?.let { StatRow("HP Bonus", "+$it", accentColor) }
                    equipment.accuracy?.let { StatRow("Accuracy", formatSignedDecimal(it), accentColor) }
                    equipment.critRate?.let { StatRow("Crit Rate", formatSignedDecimal(it), accentColor) }
                    equipment.statMods?.forEach { (stat, value) ->
                        StatRow(stat.uppercase(Locale.getDefault()), formatSigned(value), accentColor)
                    }
                }

                entry.item.effect?.let { effect ->
                    HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                    SectionHeading("Item Effect", accentColor)
                    EffectRows(effect, accentColor)
                }

                Spacer(modifier = Modifier.weight(1f))
                val buttonMinHeight = if (largeTouchTargets) 56.dp else 48.dp
                if (primaryActionLabel != null && onPrimaryAction != null) {
                    Button(
                        onClick = onPrimaryAction,
                        enabled = primaryActionEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = buttonMinHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF010308),
                            disabledContainerColor = Color.White.copy(alpha = 0.1f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(primaryActionLabel)
                    }
                }
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        enabled = secondaryActionEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = buttonMinHeight)
                    ) {
                        Text(secondaryActionLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor
        )
    }
}

@Composable
private fun EffectRows(effect: ItemEffect, accentColor: Color) {
    effect.restoreHp?.takeIf { it > 0 }?.let { StatRow("Restore HP", "+$it", accentColor) }
    effect.damage?.takeIf { it > 0 }?.let { StatRow("Damage", "$it", accentColor) }
    effect.learnSchematic?.let { StatRow("Schematic", it, accentColor) }
    effect.singleBuff?.let { buff ->
        StatRow("Buff", "${buff.stat}+${buff.value}", accentColor)
    }
    effect.buffs?.forEach { buff ->
        StatRow("Buff", "${buff.stat}+${buff.value}", accentColor)
    }
}
