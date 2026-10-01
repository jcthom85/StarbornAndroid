package com.example.starborn.feature.exploration.ui.components

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuLargeTargets
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuHighContrast
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.viewmodel.InventoryPreviewItemUi

@Composable
internal fun ModernGearPicker(
    characterName: String,
    slotLabel: String,
    options: List<InventoryPreviewItemUi>,
    equippedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    onUnequip: (() -> Unit)? = null
) {
    var selectedId by rememberSaveable(characterName, slotLabel) { mutableStateOf(equippedId) }
    val current = options.firstOrNull { it.id.equals(equippedId, true) }
    val selected = options.firstOrNull { it.id.equals(selectedId, true) }
    val isEquipped = selected != null && selected.id.equals(equippedId, true)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(16.dp),
            shape = RoundedCornerShape(20.dp), color = FieldMenuDesign.shell) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("$characterName · $slotLabel", color = FieldMenuDesign.text,
                            style = MaterialTheme.typography.titleLarge)
                        Text("Equipped: ${current?.name ?: equippedId?.takeIf { it.isNotBlank() } ?: "Empty"}",
                            color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                Text("Select an item to inspect it, then equip below.", color = FieldMenuDesign.textMuted,
                    style = MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (options.isEmpty()) item {
                        Text("No compatible items available.", color = FieldMenuDesign.textMuted)
                    }
                    items(options, key = { it.id }) { option ->
                        val inspecting = option.id.equals(selectedId, true)
                        Surface(onClick = { selectedId = option.id }, modifier = Modifier.semantics { this.selected = inspecting }, shape = RoundedCornerShape(12.dp),
                            color = if (inspecting) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel,
                            border = androidx.compose.foundation.BorderStroke(1.dp,
                                if (inspecting) FieldMenuDesign.gold else Color.White.copy(alpha = if (LocalFieldMenuHighContrast.current) .65f else .1f))) {
                            Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Image(painterResource(previewItemIconRes(option.type)), null, modifier = Modifier.size(32.dp))
                                    Text(option.name, color = FieldMenuDesign.text, style = MaterialTheme.typography.titleSmall)
                                }
                                if (option.id.equals(equippedId, true)) Text("Equipped", color = FieldMenuDesign.gold,
                                    style = MaterialTheme.typography.labelMedium)
                                if (inspecting) {
                                    option.description?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, color = FieldMenuDesign.textMuted, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    val gear = option.equipment
                                    fun readable(raw: String) = equipmentLabel(raw)
                                    fun percent(value: Double) = equipmentPercent(value)
                                    val attributes = buildList {
                                        gear?.damageMin?.let { add("Damage: $it–${gear.damageMax ?: it}") }
                                        gear?.defense?.let { add("Defense: $it") }
                                        gear?.hpBonus?.let { add("HP bonus: $it") }
                                        gear?.weaponType?.let { add("Weapon type: ${readable(it)}") }
                                        gear?.attackStyle?.let { add("Attack style: ${equipmentAttackStyle(it)}") }
                                        gear?.attackPowerMultiplier?.let { add("Attack power: ${it}×") }
                                        gear?.attackChargeTurns?.let { add("Charge: $it turns") }
                                        gear?.attackSplashMultiplier?.let { add("Splash damage: ${percent(it)}") }
                                        gear?.attackElement?.let { add("Element: ${readable(it)}") }
                                        gear?.accuracy?.let { add("Accuracy bonus: ${if (it > 0) "+" else ""}$it") }
                                        gear?.critRate?.let { add("Crit bonus: ${percent(it)}") }
                                        gear?.statusOnHit?.let { add("On hit: ${readable(it)}") }
                                        gear?.statusChance?.let { add("Status chance: ${percent(it)}") }
                                        gear?.statMods?.forEach { (stat, value) ->
                                            add("${readable(stat)}: ${if (value > 0) "+" else ""}$value")
                                        }
                                    }
                                    attributes.forEach { Text(it, color = FieldMenuDesign.text,
                                        style = MaterialTheme.typography.bodySmall) }
                                    if (!option.id.equals(equippedId, true)) EquipmentComparison(gear, current?.equipment,
                                        current?.name ?: equippedId?.takeIf { it.isNotBlank() })
                                }
                            }
                        }
                    }
                }
                if (onUnequip != null && !equippedId.isNullOrBlank()) {
                    OutlinedButton(onClick = onUnequip, modifier = Modifier.fillMaxWidth().heightIn(min = if (LocalFieldMenuLargeTargets.current) 56.dp else 48.dp)) {
                        Text("Unequip $slotLabel")
                    }
                }
                Button(onClick = { selected?.let { onSelect(it.id) } }, enabled = selected != null && !isEquipped,
                    modifier = Modifier.fillMaxWidth().heightIn(min = if (LocalFieldMenuLargeTargets.current) 56.dp else 48.dp)) {
                    Text(if (isEquipped) "Already equipped" else "Equip selected item")
                }
            }
        }
    }
}
