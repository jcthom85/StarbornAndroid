package com.example.starborn.feature.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.inventory.ui.*
import java.util.Locale
import kotlin.math.roundToInt

internal data class StatComparisonRow(
    val label: String,
    val equippedValue: String?,
    val selectedValue: String?,
    val delta: Int?
)

internal data class StatValue(
    val display: String,
    val numeric: Int?
)

@Composable
fun ComparisonPanel(
    slotLabel: String,
    selectedEntry: InventoryEntry?,
    equippedEntry: InventoryEntry?,
    equippedItemName: String?,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onEquip: (() -> Unit)?,
    onUnequip: (() -> Unit)?
) {
    val equippedName = equippedEntry?.item?.name ?: equippedItemName
    val selectedName = selectedEntry?.item?.name
    val comparisonRows = remember(selectedEntry, equippedEntry) {
        buildComparisonRows(equippedEntry?.item, selectedEntry?.item)
    }
    val buttonMinHeight = if (largeTouchTargets) 56.dp else 48.dp

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeading(label = "Item Picker & Comparison", accentColor = accentColor)
            Text(
                text = slotLabel.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = foregroundColor.copy(alpha = 0.85f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ComparisonPill(
                    title = "Equipped",
                    value = equippedName ?: "Empty",
                    borderColor = borderColor,
                    accentColor = accentColor,
                    highlight = false,
                    valueColor = foregroundColor,
                    modifier = Modifier.weight(1f)
                )
                ComparisonPill(
                    title = "Selected",
                    value = selectedName ?: "Choose an item",
                    borderColor = borderColor,
                    accentColor = accentColor,
                    highlight = selectedEntry != null,
                    valueColor = foregroundColor,
                    modifier = Modifier.weight(1f)
                )
            }
            selectedEntry?.item?.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = foregroundColor.copy(alpha = 0.8f)
                )
            }
            HorizontalDivider(color = borderColor.copy(alpha = 0.35f))
            if (selectedEntry == null) {
                Text(
                    text = "Select an item on the right to compare with your current loadout.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = foregroundColor.copy(alpha = 0.75f)
                )
            } else if (comparisonRows.isEmpty()) {
                Text(
                    text = "No comparable stats for this item type.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = foregroundColor.copy(alpha = 0.75f)
                )
            } else {
                comparisonRows.forEach { row ->
                    ComparisonStatRow(row = row, accentColor = accentColor, textColor = foregroundColor)
                }
            }
            HorizontalDivider(color = borderColor.copy(alpha = 0.35f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onEquip?.invoke() },
                    enabled = onEquip != null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonMinHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF010308),
                        disabledContainerColor = Color.White.copy(alpha = 0.1f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text(text = "Equip to ${slotLabel.uppercase(Locale.getDefault())}")
                }
                if (onUnequip != null) {
                    OutlinedButton(
                        onClick = onUnequip,
                        modifier = Modifier
                            .weight(0.6f)
                            .heightIn(min = buttonMinHeight)
                    ) {
                        Text("Unequip")
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonPill(
    title: String,
    value: String,
    borderColor: Color,
    accentColor: Color,
    highlight: Boolean,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (highlight) accentColor.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(1.dp, if (highlight) accentColor else borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ComparisonStatRow(
    row: StatComparisonRow,
    accentColor: Color,
    textColor: Color
) {
    val deltaColor = when {
        row.delta == null || row.delta == 0 -> textColor.copy(alpha = 0.7f)
        row.delta > 0 -> accentColor
        else -> Color(0xFFFF6B6B)
    }
    val deltaLabel = row.delta?.takeIf { it != 0 }?.let { formatSigned(it) } ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = row.equippedValue ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor.copy(alpha = 0.8f),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.9f)
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = row.selectedValue ?: "—",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
            Text(
                text = deltaLabel,
                style = MaterialTheme.typography.labelSmall,
                color = deltaColor
            )
        }
    }
}

private fun buildComparisonRows(
    equippedItem: Item?,
    selectedItem: Item?
): List<StatComparisonRow> {
    val equippedStats = equipmentStats(equippedItem?.equipment)
    val selectedStats = equipmentStats(selectedItem?.equipment)
    if (equippedStats.isEmpty() && selectedStats.isEmpty()) return emptyList()

    val labels = linkedSetOf<String>()
    labels.addAll(listOf("Damage", "Defense", "HP Bonus", "Accuracy", "Crit Rate", "Weapon Type"))
    labels.addAll(selectedStats.keys)
    labels.addAll(equippedStats.keys)

    return labels.mapNotNull { label ->
        val equipped = equippedStats[label]
        val candidate = selectedStats[label]
        if (equipped == null && candidate == null) return@mapNotNull null
        val delta = when {
            candidate?.numeric != null || equipped?.numeric != null ->
                (candidate?.numeric ?: 0) - (equipped?.numeric ?: 0)
            else -> null
        }
        StatComparisonRow(
            label = label,
            equippedValue = equipped?.display,
            selectedValue = candidate?.display,
            delta = delta
        )
    }
}

private fun equipmentStats(equipment: Equipment?): Map<String, StatValue> {
    if (equipment == null) return emptyMap()
    val stats = mutableListOf<Pair<String, StatValue>>()
    formatDamageLabel(equipment)?.let { damage ->
        stats += "Damage" to StatValue(damage, averageDamageValue(equipment))
    }
    equipment.defense?.let { stats += "Defense" to StatValue(it.toString(), it) }
    equipment.hpBonus?.let { stats += "HP Bonus" to StatValue("+$it", it) }
    equipment.accuracy?.let { stats += "Accuracy" to StatValue(formatSignedDecimal(it), it.roundToInt()) }
    equipment.critRate?.let { stats += "Crit Rate" to StatValue(formatSignedDecimal(it), it.roundToInt()) }
    equipment.weaponType?.let { stats += "Weapon Type" to StatValue(it, null) }
    equipment.statMods?.forEach { (stat, value) ->
        val label = stat.uppercase(Locale.getDefault())
        stats += label to StatValue(formatSigned(value), value)
    }
    return stats.toMap()
}
