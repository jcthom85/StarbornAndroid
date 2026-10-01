package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.starborn.domain.model.Equipment
import com.example.starborn.feature.exploration.ui.menu.LocalModernFieldMenu

/** Compares item data, rather than predicting final combat stats or mod effects. */
@Composable
internal fun EquipmentComparison(candidate: Equipment?, current: Equipment?, currentName: String?) {
    if (!LocalModernFieldMenu.current || candidate == null) return
    if (current == null) {
        Text(if (currentName == null) "No item equipped" else "Comparison unavailable for $currentName",
            color = Color.White.copy(alpha = .65f), style = MaterialTheme.typography.bodySmall)
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Compared with ${currentName ?: "equipped gear"}",
            color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
        if (changed.isEmpty()) Text("Same listed attributes", color = Color.White.copy(alpha = .7f),
            style = MaterialTheme.typography.bodySmall)
        changed.forEach { label ->
            val oldValue = numericBefore[label]
            val newValue = numericAfter[label]
            val difference = if (oldValue != null && newValue != null) newValue - oldValue else null
            val delta = difference?.let { " (${if (it > 0) "+" else ""}$it)" }.orEmpty()
            Text("$label: ${before[label] ?: "Not listed"} → ${after[label] ?: "Not listed"}$delta",
                color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun equipmentNumber(value: Double): String = String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
internal fun equipmentPercent(value: Double): String = "${equipmentNumber(if (kotlin.math.abs(value) <= 1.0) value * 100 else value)}%"
internal fun equipmentLabel(raw: String): String = raw.split('_', ' ').filter { it.isNotBlank() }
    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
internal fun equipmentAttackStyle(raw: String): String = when (raw.lowercase(Locale.ROOT)) {
    "single" -> "Single target"
    "all" -> "All enemies"
    "spread" -> "Spread shot"
    "rocket" -> "Rocket salvo"
    "charged_splash" -> "Charged splash"
    else -> equipmentLabel(raw)
}
