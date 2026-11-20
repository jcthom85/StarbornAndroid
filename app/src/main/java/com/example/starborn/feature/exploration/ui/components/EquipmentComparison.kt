package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.model.Equipment
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.ui.menu.LocalModernFieldMenu
import java.util.Locale

/** Compares item data, rather than predicting final combat stats or mod effects. */
@Composable
internal fun EquipmentComparison(candidate: Equipment?, current: Equipment?, currentName: String?) {
    if (!LocalModernFieldMenu.current || candidate == null) return
    if (current == null) {
        Text(
            text = if (currentName == null) "No item equipped" else "Comparison unavailable for $currentName",
            color = FieldMenuDesign.textMuted,
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    fun number(value: Double) = equipmentNumber(value)
    fun percent(value: Double) = equipmentPercent(value)
    fun signed(value: Double) = "${if (value > 0) "+" else ""}${equipmentNumber(value)}"
    fun label(raw: String) = equipmentLabel(raw)
    fun style(raw: String) = equipmentAttackStyle(raw)
    fun values(equipment: Equipment): Map<String, String> = buildMap {
        if (equipment.damageMin != null || equipment.damageMax != null)
            put("Damage", "${equipment.damageMin ?: "?"}–${equipment.damageMax ?: "?"}")
        equipment.defense?.let { put("Defense", it.toString()) }
        equipment.hpBonus?.let { put("HP bonus", it.toString()) }
        equipment.accuracy?.let { put("Accuracy bonus", signed(it)) }
        equipment.critRate?.let { put("Crit rate bonus", percent(it)) }
        equipment.attackStyle?.let { put("Attack style", style(it)) }
        equipment.attackPowerMultiplier?.let { put("Attack power", "${number(it)}x") }
        equipment.attackChargeTurns?.let { put("Charge turns", it.toString()) }
        equipment.attackSplashMultiplier?.let { put("Splash damage", percent(it)) }
        equipment.attackElement?.let { put("Element", label(it)) }
        equipment.statusOnHit?.let { put("Status on hit", label(it)) }
        equipment.statusChance?.let { put("Status chance", percent(it)) }
        equipment.statMods?.forEach { (stat, value) -> put(label(stat), "${if (value > 0) "+" else ""}$value") }
    }
    val before = values(current)
    val after = values(candidate)
    val changed = (before.keys + after.keys).filter { before[it] != after[it] }
    val numericBefore = buildMap<String, Int> {
        current.defense?.let { put("Defense", it) }
        current.hpBonus?.let { put("HP bonus", it) }
        current.statMods?.forEach { (stat, value) -> put(label(stat), value) }
    }
    val numericAfter = buildMap<String, Int> {
        candidate.defense?.let { put("Defense", it) }
        candidate.hpBonus?.let { put("HP bonus", it) }
        candidate.statMods?.forEach { (stat, value) -> put(label(stat), value) }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = FieldMenuDesign.deepCard,
        border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Compared with ${currentName ?: "equipped gear"}",
                color = FieldMenuDesign.gold,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (changed.isEmpty()) {
                Text(
                    text = "Same listed attributes",
                    color = FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            changed.forEach { statName ->
                val oldValue = numericBefore[statName] ?: 0
                val newValue = numericAfter[statName] ?: 0
                val hasNumeric = statName in numericBefore || statName in numericAfter
                val difference = if (hasNumeric) newValue - oldValue else null
                val deltaText = difference?.let {
                    val arrow = if (it > 0) "▲" else if (it < 0) "▼" else ""
                    " (${if (it > 0) "+" else ""}$it $arrow)".trimEnd()
                }.orEmpty()
                val deltaColor = when {
                    difference != null && difference > 0 -> FieldMenuDesign.mint
                    difference != null && difference < 0 -> FieldMenuDesign.coral
                    else -> FieldMenuDesign.cyan
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statName,
                        color = FieldMenuDesign.textMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${before[statName] ?: "None"} → ${after[statName] ?: "None"}$deltaText",
                        color = deltaColor,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

internal fun equipmentNumber(value: Double): String = String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
internal fun equipmentPercent(value: Double): String = "${equipmentNumber(if (kotlin.math.abs(value) <= 1.0) value * 100 else value)}%"
internal fun equipmentLabel(raw: String): String = when (raw.trim().lowercase(Locale.ROOT)) {
    "atk" -> "Attack"
    "str" -> "Strength"
    "vit" -> "Vitality"
    "spd" -> "Speed"
    "agi" -> "Agility"
    "foc" -> "Focus"
    "int" -> "Intelligence"
    "lck" -> "Luck"
    "def" -> "Defense"
    "eva" -> "Evasion"
    "acc" -> "Accuracy"
    "crit_rate", "crit" -> "Crit Rate"
    else -> raw.split('_', ' ').filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}
internal fun equipmentAttackStyle(raw: String): String = when (raw.lowercase(Locale.ROOT)) {
    "single" -> "Single target"
    "all" -> "All enemies"
    "spread" -> "Spread shot"
    "rocket" -> "Rocket salvo"
    "charged_splash" -> "Charged splash"
    else -> equipmentLabel(raw)
}
