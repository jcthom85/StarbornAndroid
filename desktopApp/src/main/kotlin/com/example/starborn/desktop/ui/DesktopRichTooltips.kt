package com.example.starborn.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import java.util.Locale

/**
 * Rich desktop hover tooltip for inventory items.
 * Displays title, category/rarity tags, full description, stats/effects, value, and quantity.
 */
@Composable
internal fun DesktopItemTooltipContent(
    item: Item?,
    fallbackName: String,
    quantity: Int? = null,
    services: DesktopAppServices? = null
) {
    val name = item?.name ?: services?.contentName(fallbackName) ?: fallbackName
    val category = item?.categoryOverride ?: item?.type ?: "Item"
    val rarity = item?.rarity

    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header: Name and Category / Rarity tags
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = FieldMenuDesign.text,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = FieldMenuDesign.cyan.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.5f))
            ) {
                Text(
                    text = category.replace('_', ' ').uppercase(Locale.ROOT),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = FieldMenuDesign.cyan,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (rarity != null && !rarity.equals("common", ignoreCase = true)) {
            Text(
                text = rarity.uppercase(Locale.ROOT),
                color = FieldMenuDesign.gold,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider(color = FieldMenuDesign.border.copy(alpha = 0.25f))

        // Description
        item?.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = FieldMenuDesign.textMuted,
                lineHeight = 15.sp
            )
        }

        // Equipment Stats Preview
        item?.equipment?.let { eq ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (eq.damageMin != null || eq.damageMax != null) {
                    val min = eq.damageMin ?: 0
                    val max = eq.damageMax ?: min
                    StatPreviewRow("Damage", if (min == max) "$min" else "$min - $max", Color(0xFFFF9D80))
                }
                val defense = eq.defense
                if (defense != null && defense > 0) {
                    StatPreviewRow("Defense", "+$defense", Color(0xFF80E7A0))
                }
                val hpBonus = eq.hpBonus
                if (hpBonus != null && hpBonus > 0) {
                    StatPreviewRow("HP Bonus", "+$hpBonus", Color(0xFF80E7A0))
                }
                eq.accuracy?.let { acc ->
                    StatPreviewRow("Accuracy", String.format(Locale.ROOT, "%.0f%%", acc * 100), FieldMenuDesign.text)
                }
                eq.critRate?.let { crit ->
                    StatPreviewRow("Crit Chance", String.format(Locale.ROOT, "%.1f%%", crit * 100), Color(0xFFFFD700))
                }
                eq.statMods?.forEach { (stat, mod) ->
                    val statLabel = services?.contentName(stat) ?: stat.replace('_', ' ').replaceFirstChar { it.uppercase() }
                    StatPreviewRow(statLabel, if (mod >= 0) "+$mod" else "$mod", if (mod >= 0) Color(0xFF80E7A0) else Color(0xFFFF887F))
                }
            }
        }

        // Consumable Effect Preview
        item?.effect?.let { ef ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val restoreHp = ef.restoreHp
                if (restoreHp != null && restoreHp > 0) {
                    StatPreviewRow("Restores HP", "+$restoreHp HP", Color(0xFF80E7A0))
                }
                val damage = ef.damage
                if (damage != null && damage > 0) {
                    StatPreviewRow("Deals Damage", "$damage DMG", Color(0xFFFF887F))
                }
                ef.status?.let { st ->
                    StatPreviewRow("Effect", "Inflicts ${st.replace('_', ' ')}", FieldMenuDesign.cyan)
                }
                ef.buffs?.forEach { buff ->
                    StatPreviewRow("Buff", "+${buff.value} ${buff.stat.replace('_', ' ')} (${buff.duration ?: 3}t)", Color(0xFF80E7A0))
                }
            }
        }

        HorizontalDivider(color = FieldMenuDesign.border.copy(alpha = 0.2f))

        // Footer: Quantity & Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val valText = when {
                item?.unsellable == true -> "Unsellable"
                (item?.resaleValue ?: item?.value ?: 0) > 0 -> "${item?.resaleValue ?: item?.value} Credits"
                else -> "0 Credits"
            }
            Text(
                text = valText,
                style = MaterialTheme.typography.labelSmall,
                color = if (item?.unsellable == true) FieldMenuDesign.textMuted.copy(alpha = 0.6f) else FieldMenuDesign.gold
            )
            if (quantity != null) {
                Text(
                    text = "Owned: ×$quantity",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = FieldMenuDesign.cyan
                )
            }
        }
    }
}

/**
 * Rich desktop hover tooltip for comparing an equipment candidate against the currently equipped item.
 */
@Composable
internal fun DesktopEquipmentCompareTooltipContent(
    candidate: Item,
    equipped: Item?,
    slotLabel: String,
    services: DesktopAppServices
) {
    val isEquipped = candidate.id == equipped?.id
    val candEq = candidate.equipment
    val currEq = equipped?.equipment

    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = candidate.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = FieldMenuDesign.text,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isEquipped) FieldMenuDesign.gold.copy(alpha = 0.2f) else FieldMenuDesign.cyan.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, if (isEquipped) FieldMenuDesign.gold.copy(alpha = 0.6f) else FieldMenuDesign.cyan.copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (isEquipped) "EQUIPPED" else slotLabel.uppercase(Locale.ROOT),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = if (isEquipped) FieldMenuDesign.gold else FieldMenuDesign.cyan,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        candidate.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = FieldMenuDesign.textMuted,
                lineHeight = 14.sp
            )
        }

        HorizontalDivider(color = FieldMenuDesign.border.copy(alpha = 0.25f))

        Text(
            text = if (isEquipped) "Currently Equipped in $slotLabel" else "Compared with ${equipped?.name ?: "Empty Slot"}:",
            style = MaterialTheme.typography.labelSmall,
            color = FieldMenuDesign.gold,
            fontWeight = FontWeight.SemiBold
        )

        // Stat Deltas
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            CompareStatRow("Damage Min", candEq?.damageMin?.toDouble(), currEq?.damageMin?.toDouble())
            CompareStatRow("Damage Max", (candEq?.damageMax ?: candEq?.damageMin)?.toDouble(), (currEq?.damageMax ?: currEq?.damageMin)?.toDouble())
            CompareStatRow("Defense", candEq?.defense?.toDouble(), currEq?.defense?.toDouble(), additive = true)
            CompareStatRow("HP Bonus", candEq?.hpBonus?.toDouble(), currEq?.hpBonus?.toDouble(), additive = true)
            CompareStatRow("Accuracy", candEq?.accuracy, currEq?.accuracy, percent = true)
            CompareStatRow("Crit Chance", candEq?.critRate, currEq?.critRate, percent = true)

            val allStatKeys = (candEq?.statMods.orEmpty().keys + currEq?.statMods.orEmpty().keys).distinct()
            allStatKeys.forEach { stat ->
                val label = services.contentName(stat).replaceFirstChar { it.uppercase() }
                CompareStatRow(label, candEq?.statMods?.get(stat)?.toDouble(), currEq?.statMods?.get(stat)?.toDouble(), additive = true)
            }
        }

        HorizontalDivider(color = FieldMenuDesign.border.copy(alpha = 0.2f))

        Text(
            text = if (isEquipped) "Click to unequip" else "Click to select · Equip button to wear",
            style = MaterialTheme.typography.labelSmall,
            color = FieldMenuDesign.cyan.copy(alpha = 0.8f)
        )
    }
}

/**
 * Rich desktop hover tooltip for combat action commands.
 */
@Composable
internal fun DesktopCombatActionTooltipContent(
    title: String,
    shortcut: String,
    description: String,
    details: String? = null,
    accent: Color = Color(0xFF63E6FF)
) {
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accent.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
            ) {
                Text(
                    text = shortcut,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        HorizontalDivider(color = accent.copy(alpha = 0.25f))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.82f),
            lineHeight = 15.sp
        )
        if (!details.isNullOrBlank()) {
            Text(
                text = details,
                style = MaterialTheme.typography.labelSmall,
                color = FieldMenuDesign.gold,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Rich desktop hover tooltip for abilities and skills.
 */
@Composable
internal fun DesktopSkillTooltipContent(
    skillName: String,
    description: String,
    costAp: Int? = null,
    unlocked: Boolean? = null,
    cooldown: Int? = null,
    targeting: String? = null,
    keyGlyph: String? = null,
    accent: Color = Color(0xFF63E6FF)
) {
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = skillName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!keyGlyph.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accent.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = keyGlyph,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (unlocked != null) {
                    val statusColor = if (unlocked) FieldMenuDesign.gold else accent
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (unlocked) "UNLOCKED" else if (costAp != null) "$costAp AP" else "LOCKED",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = accent.copy(alpha = 0.25f))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            lineHeight = 15.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            targeting?.let { tgt ->
                Text(
                    text = "Target: ${tgt.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = FieldMenuDesign.textMuted
                )
            }
            if (cooldown != null && cooldown > 0) {
                Text(
                    text = "Cooldown: $cooldown turns",
                    style = MaterialTheme.typography.labelSmall,
                    color = FieldMenuDesign.gold
                )
            }
        }
    }
}

@Composable
private fun StatPreviewRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}

@Composable
private fun CompareStatRow(
    label: String,
    candidate: Double?,
    current: Double?,
    percent: Boolean = false,
    additive: Boolean = false
) {
    if (candidate == null && current == null) return
    val next = candidate ?: if (additive) 0.0 else null
    val old = current ?: if (additive) 0.0 else null
    val delta = if (next != null && old != null) next - old else null

    fun fmt(v: Double?): String = v?.let {
        if (percent) String.format(Locale.ROOT, "%.1f%%", it * 100)
        else String.format(Locale.ROOT, "%.1f", it).removeSuffix(".0")
    } ?: "—"

    val deltaText = delta?.takeIf { it != 0.0 }?.let { d ->
        val sign = if (d > 0) "+" else ""
        val num = if (percent) String.format(Locale.ROOT, "%.1f%%", d * 100)
        else String.format(Locale.ROOT, "%.1f", d).removeSuffix(".0")
        "$sign$num"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = FieldMenuDesign.textMuted
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${fmt(old)} → ${fmt(next)}",
                style = MaterialTheme.typography.bodySmall,
                color = FieldMenuDesign.text
            )
            if (deltaText != null) {
                Text(
                    text = if (delta!! > 0) "▲ $deltaText" else "▼ $deltaText",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (delta > 0) Color(0xFF80E7A0) else Color(0xFFFF887F)
                )
            }
        }
    }
}
