package com.example.starborn.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.starborn.domain.inventory.ModSlotComparison
import com.example.starborn.domain.crafting.MealRules

@Composable
internal fun DesktopModComparison(comparisons: List<ModSlotComparison>) {
    if (comparisons.isEmpty()) return
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val selected = comparisons.firstOrNull { it.target.key == selectedKey && it.blockedReason == null }
        ?: comparisons.firstOrNull { it.blockedReason == null }
    Text("Compare destination", style = MaterialTheme.typography.titleSmall)
    if (selected == null) {
        Text("Unlock a modification slot and equip its weapon or armor first.")
        return
    }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("mod-comparison-target")) { Text(selected.target.label) }
        DropdownMenu(expanded, { expanded = false }) {
            comparisons.forEach { comparison ->
                DropdownMenuItem(text = { Text(comparison.target.label + (comparison.blockedReason?.let { " - $it" } ?: "")) },
                    enabled = comparison.blockedReason == null,
                    onClick = { selectedKey = comparison.target.key; expanded = false })
            }
        }
    }
    Text("Replacing: ${selected.equippedItemName ?: "Empty slot"}")
    selected.movingFrom?.let { Text("Moves the existing copy from ${it.label}.") }
    Text(selected.statDeltas.entries.joinToString(" / ") { MealRules.bonusLabel(it.key, it.value) }.ifBlank { "No change to party stats" })
    if (selected.effectiveElementBefore != selected.effectiveElementAfter)
        Text("Weapon attack element: ${selected.effectiveElementBefore ?: "None"} -> ${selected.effectiveElementAfter ?: "None"}")
    else Text("Weapon attack element unchanged: ${selected.effectiveElementAfter ?: "None"}")
}
