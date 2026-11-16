package com.example.starborn.feature.crafting.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.crafting.MealRules
import com.example.starborn.domain.inventory.ModSlotComparison
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModComparisonPanel(comparisons: List<ModSlotComparison>) {
    if (comparisons.isEmpty()) return
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val selected = comparisons.firstOrNull { it.target.key == selectedKey && it.blockedReason == null }
        ?: comparisons.firstOrNull { it.blockedReason == null }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = FieldMenuDesign.deepCard,
        border = BorderStroke(1.dp, FieldMenuDesign.subtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPARE DESTINATION",
                    color = FieldMenuDesign.cyan,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                if (selected != null) {
                    Box {
                        Surface(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = FieldMenuDesign.elevatedPanel,
                            border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.65f)),
                            modifier = Modifier.testTag("mod-comparison-target")
                        ) {
                            Text(
                                text = "${selected.target.label} ▾",
                                color = FieldMenuDesign.gold,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            comparisons.forEach { comparison ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            comparison.target.label +
                                                (comparison.blockedReason?.let { " - $it" } ?: "")
                                        )
                                    },
                                    enabled = comparison.blockedReason == null,
                                    onClick = {
                                        selectedKey = comparison.target.key
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (selected == null) {
                Text(
                    text = "Unlock a modification slot and equip its weapon or armor first.",
                    color = FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                return@Column
            }

            // Quick Crew Slot Selector Pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                comparisons.forEach { comp ->
                    val isCurrent = comp.target.key == selected.target.key
                    val unlocked = comp.blockedReason == null
                    Surface(
                        onClick = { if (unlocked) selectedKey = comp.target.key },
                        enabled = unlocked,
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isCurrent -> FieldMenuDesign.primaryActionFill
                            unlocked -> FieldMenuDesign.panel
                            else -> FieldMenuDesign.shell
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCurrent -> FieldMenuDesign.gold
                                unlocked -> FieldMenuDesign.subtleBorder
                                else -> FieldMenuDesign.subtleBorder.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Text(
                            text = if (unlocked) comp.target.label else "${comp.target.label} 🔒",
                            color = when {
                                isCurrent -> FieldMenuDesign.text
                                unlocked -> FieldMenuDesign.textMuted
                                else -> FieldMenuDesign.textMuted.copy(alpha = 0.5f)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Before -> After Replacement Line
            Text(
                text = "Replacing: ${selected.equippedItemName ?: "Empty slot"}",
                color = FieldMenuDesign.text,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
            selected.movingFrom?.let {
                Text(
                    text = "Moves the existing copy from ${it.label}.",
                    color = FieldMenuDesign.gold,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Stat Delta Pills
            if (selected.statDeltas.isEmpty()) {
                Text(
                    text = "No change to party stats",
                    color = FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    selected.statDeltas.entries.forEach { (stat, delta) ->
                        val positive = delta > 0
                        val pillColor = if (positive) FieldMenuDesign.mint else FieldMenuDesign.coral
                        val arrow = if (positive) "▲" else "▼"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = pillColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, pillColor.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "${MealRules.bonusLabel(stat, delta)} $arrow",
                                color = pillColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (selected.effectiveElementBefore != selected.effectiveElementAfter) {
                Text(
                    text = "Weapon attack element: ${selected.effectiveElementBefore ?: "None"} → ${selected.effectiveElementAfter ?: "None"}",
                    color = FieldMenuDesign.cyan,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

