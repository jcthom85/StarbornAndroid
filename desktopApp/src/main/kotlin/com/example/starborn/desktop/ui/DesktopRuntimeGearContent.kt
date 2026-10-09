package com.example.starborn.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.presentation.ItemDetails

@Composable
internal fun DesktopRuntimeGearContent(services: DesktopAppServices, characterId: String? = null) {
    val session by services.sessionStore.state.collectAsState()
    val inventory by services.inventoryService.state.collectAsState()
    val catalog = remember(services) { services.itemRepository.allItems().associateBy { it.id } }
    val character = characterId ?: session.partyMembers.firstOrNull() ?: return
    if (character.equals("ollie", true)) return
    var slot by rememberSaveable(character) { mutableStateOf(GearRules.equipSlots.first()) }
    var selection by rememberSaveable(character, slot) { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    val modSlots = listOf("weapon_mod1", "weapon_mod2", "armor_mod1", "armor_mod2")
    fun equippedId(target: String): String? = when (target) {
        "weapon" -> session.equippedWeapons[character]
        "armor" -> session.equippedArmors[character]
        else -> session.equippedItems["$character:$target"] ?: session.equippedItems[target]
    }
    fun slotLabel(target: String) = target.replace('_', ' ').replaceFirstChar { it.uppercase() }
    val equipped = equippedId(slot)
    val mod = slot in modSlots
    val slotUnlocked = !mod || GearRules.isModSlotUnlocked(slot, session.completedMilestones)
    val owned = (inventory.map { it.item.id } + session.unlockedWeapons + session.unlockedArmors + listOfNotNull(equipped))
        .distinct().mapNotNull(catalog::get).filter {
            (if (mod) it.type == "mod" || it.equipment?.slot == "mod" else GearRules.matchesSlot(it.equipment, slot, character, it.type)) && it.name.contains(search, true)
        }.sortedBy { it.name }
    val selected = owned.firstOrNull { it.id == selection } ?: owned.firstOrNull { it.id == equipped } ?: owned.firstOrNull()
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val short = maxHeight < 320.dp
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.width(140.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (GearRules.equipSlots.toList() + modSlots).forEach { target ->
                val currentItem = equippedId(target)?.let(catalog::get)
                val tooltipText = buildString {
                    append(slotLabel(target))
                    if (currentItem != null) {
                        append(": ").append(currentItem.name)
                        currentItem.description?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
                    } else if (target in modSlots && !GearRules.isModSlotUnlocked(target, session.completedMilestones)) {
                        append(" (Locked by story progress)")
                    } else {
                        append(" (Empty)")
                    }
                }
                DesktopTooltip(tooltipText) {
                    Surface(onClick = { slot = target }, shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().desktopPointerHover(),
                        color = if (slot == target) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel,
                        border = BorderStroke(1.dp, if (slot == target) FieldMenuDesign.gold else FieldMenuDesign.border.copy(alpha = .3f))) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DesktopMenuItemArt(services, currentItem, Modifier.size(40.dp), target)
                            Text(slotLabel(target), color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelLarge)
                            Text(if (target in modSlots && !GearRules.isModSlotUnlocked(target, session.completedMilestones)) "Locked by story progress"
                                else currentItem?.name ?: "None", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!short || owned.size > 6 || search.isNotBlank()) OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search equipment") }, singleLine = true)
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!slotUnlocked) item { Text("This slot is locked by story progress.", color = FieldMenuDesign.textMuted) }
                    else if (owned.isEmpty()) item { Text("No compatible equipment.", color = FieldMenuDesign.textMuted) }
                    if (slotUnlocked) items(owned, key = { it.id }) { item ->
                        val unlocked = when (slot) { "weapon" -> item.id in session.unlockedWeapons; "armor" -> item.id in session.unlockedArmors; else -> true }
                        val canEquip = unlocked || item.id == equipped
                        fun equipItem() {
                            val id = item.id.takeUnless { it == equipped }
                            when {
                                mod -> services.exploration.equipInventoryMod(slot, id, character)
                                slot == "weapon" -> services.exploration.equipWeapon(character, id)
                                slot == "armor" -> services.exploration.equipArmor(character, id)
                                else -> services.exploration.equipInventoryItem(slot, id, character)
                            }
                        }
                        val rowTooltip = item.description?.takeIf { it.isNotBlank() } ?: item.name
                        DesktopTooltip(rowTooltip) {
                            Surface(onClick = { selection = item.id }, shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().desktopPointerHover(),
                                color = if (selected?.id == item.id) FieldMenuDesign.elevatedPanel else FieldMenuDesign.panel) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    DesktopMenuItemArt(services, item, Modifier.size(40.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.labelLarge)
                                        if (item.id == equipped) Text("Equipped", color = FieldMenuDesign.gold, style = MaterialTheme.typography.labelSmall)
                                    }
                                    OutlinedButton(
                                        onClick = ::equipItem,
                                        enabled = canEquip,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.desktopPointerHover(canEquip)
                                    ) {
                                        Text("${item.name} · equip", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
                if (slotUnlocked) selected?.let { item ->
                    Surface(Modifier.weight(1.25f).fillMaxHeight(), color = FieldMenuDesign.panel, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = .16f))) {
                        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DesktopMenuScrollPane(Modifier.weight(1f).fillMaxWidth()) {
                            DesktopMenuItemArt(services, item, Modifier.size(if (short) 48.dp else 80.dp))
                            Text(item.name, style = MaterialTheme.typography.titleLarge, color = FieldMenuDesign.gold)
                            item.description?.let { Text(it) }
                            val current = equipped?.let { catalog[it]?.equipment }
                            val candidate = item.equipment
                            HorizontalDivider()
                            Text("Compared with ${equipped?.let { catalog[it]?.name } ?: "empty slot"}", style = MaterialTheme.typography.labelLarge)
                            Text("Item stats (before character bonuses)", style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted)
                            DesktopGearComparison("Damage minimum", candidate?.damageMin?.toDouble(), current?.damageMin?.toDouble())
                            DesktopGearComparison("Damage maximum", (candidate?.damageMax ?: candidate?.damageMin)?.toDouble(), (current?.damageMax ?: current?.damageMin)?.toDouble())
                            DesktopGearComparison("Defense", candidate?.defense?.toDouble(), current?.defense?.toDouble(), additive = true)
                            DesktopGearComparison("HP bonus", candidate?.hpBonus?.toDouble(), current?.hpBonus?.toDouble(), additive = true)
                            DesktopGearComparison("Accuracy", candidate?.accuracy, current?.accuracy, percent = true)
                            DesktopGearComparison("Critical chance", candidate?.critRate, current?.critRate, percent = true)
                            (candidate?.statMods.orEmpty().keys + current?.statMods.orEmpty().keys).distinct().forEach { stat ->
                                DesktopGearComparison(services.contentName(stat), candidate?.statMods?.get(stat)?.toDouble(), current?.statMods?.get(stat)?.toDouble(), additive = true)
                            }
                            Text("Features", style = MaterialTheme.typography.titleSmall, color = FieldMenuDesign.gold)
                            Text("Candidate", style = MaterialTheme.typography.labelMedium)
                            ItemDetails.lines(item, services::contentName).filterNot { it.startsWith("Damage:") || it.startsWith("Defense:") || it.startsWith("HP bonus:") || it.startsWith("Accuracy:") || it.startsWith("Critical chance:") }.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                            equipped?.let { catalog[it] }?.takeIf { it.id != item.id }?.let { existing ->
                                Text("Currently equipped", style = MaterialTheme.typography.labelMedium)
                                ItemDetails.lines(existing, services::contentName).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = FieldMenuDesign.textMuted) }
                            }
                        }
                            val unlocked = when (slot) { "weapon" -> item.id in session.unlockedWeapons; "armor" -> item.id in session.unlockedArmors; else -> true }
                            if (!unlocked) Text("Not unlocked for use yet", color = FieldMenuDesign.gold)
                            Button(onClick = {
                                val id = item.id.takeUnless { it == equipped }
                                when {
                                    mod -> services.exploration.equipInventoryMod(slot, id, character)
                                    slot == "weapon" -> services.exploration.equipWeapon(character, id)
                                    slot == "armor" -> services.exploration.equipArmor(character, id)
                                    else -> services.exploration.equipInventoryItem(slot, id, character)
                                }
                            }, enabled = unlocked || item.id == equipped, modifier = Modifier.fillMaxWidth().desktopPointerHover(unlocked || item.id == equipped)) { Text(if (item.id == equipped) "Unequip" else "Equip") }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun DesktopGearComparison(label: String, candidate: Double?, current: Double?, percent: Boolean = false, additive: Boolean = false) {
    if (candidate == null && current == null) return
    fun number(value: Double?): String = value?.let {
        if (percent) String.format(java.util.Locale.ROOT, "%.1f%%", it * 100)
        else String.format(java.util.Locale.ROOT, "%.1f", it).removeSuffix(".0")
    } ?: "Not specified"
    val next = candidate ?: if (additive) 0.0 else null
    val old = current ?: if (additive) 0.0 else null
    val delta = if (next != null && old != null) next - old else null
    val difference = delta?.let { " (${if (it > 0) "+" else ""}${number(it).removeSuffix("%")}${if (percent) " percentage points" else ""})" }.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = FieldMenuDesign.textMuted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(number(old), style = MaterialTheme.typography.bodySmall)
            Text("${number(next)}$difference", style = MaterialTheme.typography.bodySmall,
                color = when { delta == null || delta == 0.0 -> FieldMenuDesign.text; delta > 0 -> Color(0xFF80E7A0); else -> Color(0xFFFF887F) })
        }
    }
}
