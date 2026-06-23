package com.example.starborn.feature.exploration.ui.tabs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.starborn.R
import com.example.starborn.domain.inventory.GearRules
import com.example.starborn.domain.model.Equipment
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.feature.exploration.ui.MenuSectionCard
import com.example.starborn.feature.exploration.ui.components.GearSelectionDialog
import com.example.starborn.feature.exploration.ui.components.previewItemIconRes
import com.example.starborn.feature.exploration.viewmodel.InventoryPreviewItemUi
import com.example.starborn.feature.exploration.viewmodel.PartyMemberStatusUi
import com.example.starborn.ui.background.rememberAssetPainter
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlin.math.roundToInt

private enum class InventoryCarouselPage { SUPPLIES, GEAR, KEY_ITEMS }

@Composable
fun InventoryTabContent(
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    unlockedWeapons: Set<String>,
    equippedWeapons: Map<String, String>,
    unlockedArmors: Set<String>,
    equippedArmors: Map<String, String>,
    partyMembers: List<PartyMemberStatusUi>,
    accentColor: Color,
    borderColor: Color,
    onEquipItem: (String, String?, String) -> Unit,
    onEquipMod: (String, String?, String) -> Unit,
    onEquipWeapon: (String, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?,
    onUseConsumable: (InventoryPreviewItemUi) -> Unit,
    creditsLabel: String
) {
    var page by rememberSaveable { mutableStateOf(InventoryCarouselPage.SUPPLIES) }
    val supplies = remember(inventoryItems) {
        inventoryItems.filterNot { it.isKeyItem() }
    }
    val keyItems = remember(inventoryItems) { inventoryItems.filter { it.isKeyItem() } }
    var detailItem by remember { mutableStateOf<InventoryPreviewItemUi?>(null) }
    MenuSectionCard(
        title = "Inventory Overview",
        accentColor = accentColor,
        borderColor = borderColor
    ) {
        InventoryCarouselToggle(
            current = page,
            onSelect = { page = it },
            accentColor = accentColor,
            borderColor = borderColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        when (page) {
            InventoryCarouselPage.SUPPLIES -> InventoryItemsPreview(
                items = supplies,
                accentColor = accentColor,
                borderColor = borderColor,
                emptyMessage = "No supplies collected yet. Explore rooms to gather materials.",
                onItemClick = onUseConsumable,
                onShowDetails = { detailItem = it }
            )
            InventoryCarouselPage.GEAR -> InventoryEquipmentPreview(
                inventoryItems = inventoryItems,
                equippedItems = equippedItems,
                completedMilestones = completedMilestones,
                unlockedWeapons = unlockedWeapons,
                equippedWeapons = equippedWeapons,
                unlockedArmors = unlockedArmors,
                equippedArmors = equippedArmors,
                partyMembers = partyMembers,
                borderColor = borderColor,
                accentColor = accentColor,
                onEquipItem = onEquipItem,
                onEquipMod = onEquipMod,
                onEquipWeapon = onEquipWeapon,
                resolveWeaponItem = resolveWeaponItem,
                onEquipArmor = onEquipArmor,
                resolveArmorItem = resolveArmorItem
            )
            InventoryCarouselPage.KEY_ITEMS -> InventoryItemsPreview(
                items = keyItems,
                accentColor = accentColor,
                borderColor = borderColor,
                emptyMessage = "No key items collected yet.",
                onShowDetails = { detailItem = it }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        WalletPill(
            creditsLabel = creditsLabel,
            accentColor = accentColor,
            borderColor = borderColor,
            modifier = Modifier.align(Alignment.End)
        )
    }
    detailItem?.let { item ->
        PreviewItemDetailsDialog(
            item = item,
            accentColor = accentColor,
            borderColor = borderColor,
            onUse = if (item.effect != null && !item.isKeyItem()) {
                {
                    detailItem = null
                    onUseConsumable(item)
                }
            } else null,
            onDismiss = { detailItem = null }
        )
    }
}

@Composable
private fun InventoryCarouselToggle(
    current: InventoryCarouselPage,
    onSelect: (InventoryCarouselPage) -> Unit,
    accentColor: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)), RoundedCornerShape(50.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        InventoryCarouselButton(
            label = "Supplies",
            selected = current == InventoryCarouselPage.SUPPLIES,
            onClick = { onSelect(InventoryCarouselPage.SUPPLIES) },
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
        InventoryCarouselButton(
            label = "Gear",
            selected = current == InventoryCarouselPage.GEAR,
            onClick = { onSelect(InventoryCarouselPage.GEAR) },
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
        InventoryCarouselButton(
            label = "Key Items",
            selected = current == InventoryCarouselPage.KEY_ITEMS,
            onClick = { onSelect(InventoryCarouselPage.KEY_ITEMS) },
            accentColor = accentColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InventoryCarouselButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val background = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .clickable { onClick() },
        color = background,
        shape = RoundedCornerShape(40.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun InventoryItemsPreview(
    items: List<InventoryPreviewItemUi>,
    accentColor: Color,
    borderColor: Color,
    emptyMessage: String,
    onItemClick: ((InventoryPreviewItemUi) -> Unit)? = null,
    onShowDetails: (InventoryPreviewItemUi) -> Unit
) {
    if (items.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 6.dp)
    ) {
        items(items, key = { it.id }) { item ->
            val isUsable = onItemClick != null && item.effect != null
            val shape = RoundedCornerShape(18.dp)
            val iconRes = remember(item.id + item.type) { previewItemIconRes(item.type) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (isUsable) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.35f)), shape)
                    .clickable(enabled = isUsable) { onItemClick?.invoke(item) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.type.readableInventoryLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = if (isUsable) 0.7f else 0.5f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "x${item.quantity}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { onShowDetails(item) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Item details",
                            tint = accentColor.copy(alpha = 0.94f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewItemDetailsDialog(
    item: InventoryPreviewItemUi,
    accentColor: Color,
    borderColor: Color,
    onUse: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF0061018),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.48f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.16f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Image(
                        painter = painterResource(previewItemIconRes(item.type)),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.type.readableInventoryLabel().uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor.copy(alpha = 0.9f)
                        )
                    }
                }
                Text(
                    text = item.description?.takeIf { it.isNotBlank() } ?: "No field description is available for this item.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
                HorizontalDivider(color = borderColor.copy(alpha = 0.24f))
                PreviewDetailRow("Quantity", "x${item.quantity}", accentColor)
                PreviewDetailRow("Type", item.type.readableInventoryLabel(), accentColor)
                item.effect?.let { effect ->
                    HorizontalDivider(color = borderColor.copy(alpha = 0.24f))
                    Text(
                        text = "EFFECTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    previewEffectRows(effect).forEach { (label, value) ->
                        PreviewDetailRow(label, value, accentColor)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.045f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Text(
                        text = previewFlavorText(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = { onUse?.invoke() },
                        enabled = onUse != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color(0xFF010308),
                            disabledContainerColor = Color.White.copy(alpha = 0.09f),
                            disabledContentColor = Color.White.copy(alpha = 0.38f)
                        )
                    ) {
                        Text("Use Item")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewDetailRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.62f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun WalletPill(
    creditsLabel: String,
    accentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val compactLabel = remember(creditsLabel) {
        creditsLabel.replace(Regex("\\s*credits", RegexOption.IGNORE_CASE), " c").trim()
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = compactLabel,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor.copy(alpha = 0.9f)
            )
        }
    }
}

private fun InventoryPreviewItemUi.isKeyItem(): Boolean {
    val normalized = type.lowercase(Locale.getDefault())
    return normalized == "key" || normalized == "key_item" || normalized == "quest"
}

private fun previewEffectRows(effect: ItemEffect): List<Pair<String, String>> = buildList {
    effect.type?.takeIf { it.isNotBlank() }?.let { add("Effect Type" to it.readableInventoryLabel()) }
    effect.target?.takeIf { it.isNotBlank() }?.let { add("Target" to it.readableInventoryLabel()) }
    effect.restoreHp?.takeIf { it > 0 }?.let { add("Restore HP" to "+$it") }
    effect.damage?.takeIf { it > 0 }?.let { add("Damage" to it.toString()) }
    effect.amount?.takeIf { it != 0 }?.let { add("Amount" to it.toString()) }
    effect.status?.takeIf { it.isNotBlank() }?.let { add("Status" to it.readableInventoryLabel()) }
    effect.duration?.takeIf { it > 0 }?.let { add("Duration" to "$it turn${if (it == 1) "" else "s"}") }
    effect.learnSchematic?.takeIf { it.isNotBlank() }?.let { add("Schematic" to it.readableInventoryLabel()) }
    effect.singleBuff?.let { add("Buff" to "${it.stat.readableInventoryLabel()} ${if (it.value >= 0) "+${it.value}" else it.value.toString()}") }
    effect.buffs?.forEach { buff ->
        add("Buff" to "${buff.stat.readableInventoryLabel()} ${if (buff.value >= 0) "+${buff.value}" else buff.value.toString()}")
    }
    if (isEmpty()) add("Effect" to "Usable item")
}

private fun String.readableInventoryLabel(): String =
    replace('_', ' ')
        .replace('-', ' ')
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase(Locale.getDefault()).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
        .ifBlank { this }

private fun previewFlavorText(item: InventoryPreviewItemUi): String {
    return when {
        item.isKeyItem() -> "Kept because someone, somewhere, will ask for it when the lights are low."
        item.effect != null -> "Small comfort, practical chemistry, or both."
        item.equipment != null -> "Tuned for survival, patched for the realities of the colony."
        item.type.contains("component", ignoreCase = true) || item.type.contains("material", ignoreCase = true) ->
            "The kind of part Jed would save twice before admitting it was useful."
        else -> "Another piece of the colony that found its way into your pack."
    }
}

@Composable
private fun InventoryEquipmentPreview(
    inventoryItems: List<InventoryPreviewItemUi>,
    equippedItems: Map<String, String>,
    completedMilestones: Set<String>,
    unlockedWeapons: Set<String>,
    equippedWeapons: Map<String, String>,
    unlockedArmors: Set<String>,
    equippedArmors: Map<String, String>,
    partyMembers: List<PartyMemberStatusUi>,
    borderColor: Color,
    accentColor: Color,
    onEquipItem: (String, String?, String) -> Unit,
    onEquipMod: (String, String?, String) -> Unit,
    onEquipWeapon: (String, String?) -> Unit,
    resolveWeaponItem: (String) -> Item?,
    onEquipArmor: (String, String?) -> Unit,
    resolveArmorItem: (String) -> Item?
) {
    var gearPicker by remember { mutableStateOf<Pair<String, String>?>(null) }
    var modPicker by remember { mutableStateOf<Pair<String, String>?>(null) }
    val itemNames = remember(inventoryItems) {
        inventoryItems.associate { it.id.lowercase(Locale.getDefault()) to it.name }
    }
    val weaponOptionIds = remember(unlockedWeapons, equippedWeapons) {
        val equippedIds = equippedWeapons.values.filter { it.isNotBlank() }
        (unlockedWeapons + equippedIds)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }
    val weaponItems = remember(weaponOptionIds, resolveWeaponItem) {
        weaponOptionIds.mapNotNull { weaponId ->
            resolveWeaponItem(weaponId)
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
    val weaponItemById = remember(weaponItems) {
        weaponItems.associateBy { it.id.lowercase(Locale.getDefault()) }
    }
    val weaponOptions = remember(weaponItems) {
        weaponItems.map { item ->
            InventoryPreviewItemUi(
                id = item.id,
                name = item.name,
                quantity = 1,
                type = item.type,
                effect = item.effect,
                equipment = item.equipment
            )
        }
    }
    val weaponNames = remember(weaponItems) {
        weaponItems.associate { it.id.lowercase(Locale.getDefault()) to it.name }
    }
    val armorOptionIds = remember(unlockedArmors, equippedArmors) {
        val equippedIds = equippedArmors.values.filter { it.isNotBlank() }
        (unlockedArmors + equippedIds)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }
    val armorItems = remember(armorOptionIds, resolveArmorItem) {
        armorOptionIds.mapNotNull { armorId ->
            resolveArmorItem(armorId)
        }.filter { item ->
            item.equipment?.slot?.equals("armor", true) == true ||
                item.type.equals("armor", ignoreCase = true)
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
    val armorItemById = remember(armorItems) {
        armorItems.associateBy { it.id.lowercase(Locale.getDefault()) }
    }
    val armorNames = remember(armorItems) {
        armorItems.associate { it.id.lowercase(Locale.getDefault()) to it.name }
    }
    val modOptions = remember(inventoryItems) {
        inventoryItems.filter { it.isModItem() }
    }
    val slots = remember { GearRules.equipSlots }
    if (partyMembers.isEmpty()) {
        Text(
            text = "No party members yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        partyMembers.forEach { member ->
            val portraitPainter = rememberAssetPainter(member.portraitPath, painterResource(R.drawable.main_menu_background))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = portraitPainter,
                        contentDescription = "${member.name} portrait",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slots.forEach { slot ->
                        val normalized = slot.lowercase(Locale.getDefault())
                        val normalizedMemberId = member.id.lowercase(Locale.getDefault())
                        val scopedKey = "$normalizedMemberId:$normalized"
                        val equippedId = when (normalized) {
                            "weapon" -> equippedWeapons[normalizedMemberId].orEmpty()
                            "armor" -> equippedArmors[normalizedMemberId].orEmpty()
                            else -> equippedItems[scopedKey].orEmpty()
                        }
                        val equippedName = when (normalized) {
                            "weapon" -> weaponNames[equippedId.lowercase(Locale.getDefault())]
                                ?: equippedId.ifBlank { "No weapon equipped" }
                            "armor" -> armorNames[equippedId.lowercase(Locale.getDefault())]
                                ?: equippedId.ifBlank { "No armor equipped" }
                            else -> itemNames[equippedId.lowercase(Locale.getDefault())]
                                ?: equippedId.ifBlank { "Unequipped" }
                        }
                        val iconRes = when (normalized) {
                            "weapon" -> {
                                val weaponItem = weaponItemById[equippedId.lowercase(Locale.getDefault())]
                                weaponItem?.let { weaponIconRes(it) }
                                    ?: weaponTypeIconRes(GearRules.allowedWeaponTypeFor(normalizedMemberId))
                            }
                            "armor" -> {
                                val armorItem = armorItemById[equippedId.lowercase(Locale.getDefault())]
                                armorItem?.let { armorIconRes(it) } ?: slotIconRes(normalized)
                            }
                            else -> slotIconRes(normalized)
                        }
                        val modSlots = modSlotsForBaseSlot(normalized)
                        val modNames = modSlots.associateWith { modSlot ->
                            val modKey = "$normalizedMemberId:$modSlot"
                            val modId = equippedItems[modKey].orEmpty()
                            val isUnlocked = GearRules.isModSlotUnlocked(modSlot, completedMilestones)
                            if (isUnlocked) {
                                itemNames[modId.lowercase(Locale.getDefault())] ?: modId.ifBlank { "Empty" }
                            } else {
                                "Locked"
                            }
                        }
                        GearSlotTile(
                            slot = normalized,
                            equippedName = equippedName,
                            modsLocked = modSlots.isNotEmpty() && equippedId.isBlank(),
                            modNames = modNames,
                            completedMilestones = completedMilestones,
                            iconRes = iconRes,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            onSelectSlot = { gearPicker = member.id to normalized },
                            onSelectMod = { modSlot ->
                                modPicker = member.id to modSlot
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    val picker = gearPicker
    if (picker != null) {
        val (characterId, slotId) = picker
        val normalizedSlot = slotId.lowercase(Locale.getDefault())
        val options = remember(inventoryItems, weaponOptions, picker) {
            when (normalizedSlot) {
                "weapon" -> filterGearOptionsForPreview(weaponOptions, normalizedSlot, characterId)
                "armor" -> emptyList()
                else -> filterGearOptionsForPreview(inventoryItems, normalizedSlot, characterId)
            }
        }
        val weaponItemsForCharacter = remember(weaponItems, options, picker) {
            if (normalizedSlot != "weapon") emptyList() else {
                val allowedIds = options.map { it.id.lowercase(Locale.getDefault()) }.toSet()
                weaponItems.filter { allowedIds.contains(it.id.lowercase(Locale.getDefault())) }
            }
        }
        val normalizedCharacter = characterId.lowercase(Locale.getDefault())
        val armorItemsForCharacter = remember(armorItems, picker, normalizedCharacter) {
            if (normalizedSlot != "armor") {
                emptyList()
            } else {
                val expectedType = GearRules.allowedArmorTypeFor(normalizedCharacter)
                if (expectedType == null) armorItems else armorItems.filter { item ->
                    item.type.trim().lowercase(Locale.getDefault()) == expectedType
                }
            }
        }
        val characterName = partyMembers.firstOrNull { it.id == characterId }?.name ?: characterId
        val equippedKey = "$normalizedCharacter:$normalizedSlot"
        val equippedId = when (normalizedSlot) {
            "weapon" -> equippedWeapons[normalizedCharacter].orEmpty()
            "armor" -> equippedArmors[normalizedCharacter].orEmpty()
            else -> equippedItems[equippedKey].orEmpty()
        }
        if (normalizedSlot == "weapon") {
            WeaponSelectionDialog(
                characterName = characterName,
                weapons = weaponItemsForCharacter,
                equippedWeaponId = equippedId,
                accentColor = accentColor,
                borderColor = borderColor,
                onSelect = { selection ->
                    onEquipWeapon(characterId, selection)
                    gearPicker = null
                },
                onDismiss = { gearPicker = null }
            )
        } else if (normalizedSlot == "armor") {
            ArmorSelectionDialog(
                characterName = characterName,
                armors = armorItemsForCharacter,
                equippedArmorId = equippedId,
                accentColor = accentColor,
                borderColor = borderColor,
                onSelect = { selection ->
                    onEquipArmor(characterId, selection)
                    gearPicker = null
                },
                onDismiss = { gearPicker = null }
            )
        } else {
            GearSelectionDialog(
                characterName = characterName,
                slotLabel = slotLabel(slotId),
                options = options,
                equippedId = equippedId,
                accentColor = accentColor,
                borderColor = borderColor,
                onSelect = { selection ->
                    onEquipItem(normalizedSlot, selection, characterId)
                    gearPicker = null
                },
                onUnequip = {
                    onEquipItem(normalizedSlot, null, characterId)
                    gearPicker = null
                },
                onDismiss = { gearPicker = null }
            )
        }
    }

    val modSelection = modPicker
    if (modSelection != null) {
        val (characterId, slotId) = modSelection
        val normalizedSlot = slotId.lowercase(Locale.getDefault())
        val equippedKey = "${characterId.lowercase(Locale.getDefault())}:$normalizedSlot"
        val equippedId = equippedItems[equippedKey].orEmpty()
        val characterName = partyMembers.firstOrNull { it.id == characterId }?.name ?: characterId
        GearSelectionDialog(
            characterName = characterName,
            slotLabel = slotLabel(slotId),
            options = modOptions,
            equippedId = equippedId,
            accentColor = accentColor,
            borderColor = borderColor,
            onSelect = { selection ->
                onEquipMod(normalizedSlot, selection, characterId)
                modPicker = null
            },
            onUnequip = {
                onEquipMod(normalizedSlot, null, characterId)
                modPicker = null
            },
            onDismiss = { modPicker = null }
        )
    }
}

@Composable
private fun GearSlotTile(
    slot: String,
    equippedName: String,
    modsLocked: Boolean,
    modNames: Map<String, String>,
    completedMilestones: Set<String>,
    @DrawableRes iconRes: Int,
    accentColor: Color,
    borderColor: Color,
    onSelectSlot: () -> Unit,
    onSelectMod: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val background = Color.Black.copy(alpha = 0.2f)
    val minHeight = if (modNames.isEmpty()) 76.dp else 108.dp
    Surface(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(shape)
            .clickable { onSelectSlot() },
        color = background,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slotLabel(slot),
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor
                    )
                    Text(
                        text = equippedName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (equippedName == "Unequipped" || equippedName == "No weapon equipped") {
                            accentColor.copy(alpha = 0.65f)
                        } else if (equippedName == "No armor equipped") {
                            accentColor.copy(alpha = 0.65f)
                        } else {
                            Color.White
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (modNames.isNotEmpty()) {
                val modSlots = modSlotsForBaseSlot(slot)
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modSlots.forEach { modSlot ->
                        val isUnlocked = GearRules.isModSlotUnlocked(modSlot, completedMilestones)
                        val name = modNames[modSlot].orEmpty()
                        val chipEnabled = !modsLocked && isUnlocked
                        ModChip(
                            label = if (modSlot.endsWith("1")) "M1" else "M2",
                            name = name,
                            enabled = chipEnabled,
                            accentColor = accentColor,
                            borderColor = borderColor,
                            onClick = { onSelectMod(modSlot) }
                        )
                    }
                }
                if (modsLocked) {
                    Spacer(modifier = Modifier.height(0.dp))
                } else {
                    val anyLockedByStory = modSlots.any { !GearRules.isModSlotUnlocked(it, completedMilestones) }
                    if (anyLockedByStory) {
                        Text(
                            text = "Unlocks after Main Story milestone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModChip(
    label: String,
    name: String,
    enabled: Boolean,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 30.dp)
            .clip(RoundedCornerShape(10.dp))
            .let { base -> if (enabled) base.clickable { onClick() } else base },
        color = Color.Black.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@DrawableRes
private fun slotIconRes(slot: String): Int {
    return when (slot.lowercase(Locale.getDefault())) {
        "weapon" -> R.drawable.item_icon_sword
        "armor" -> R.drawable.item_icon_armor
        "accessory" -> R.drawable.item_icon_accessory
        "snack" -> R.drawable.item_icon_food
        else -> R.drawable.item_icon_generic
    }
}

private fun modSlotsForBaseSlot(slot: String): List<String> {
    return when (slot.lowercase(Locale.getDefault())) {
        "weapon" -> listOf("weapon_mod1", "weapon_mod2")
        "armor" -> listOf("armor_mod1", "armor_mod2")
        else -> emptyList()
    }
}

private fun InventoryPreviewItemUi.isModItem(): Boolean {
    val normalizedType = type.lowercase(Locale.getDefault())
    return normalizedType == "mod" || equipment?.slot?.equals("mod", true) == true
}

private fun slotLabel(raw: String): String =
    raw.split('_', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { c ->
                if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
            }
        }

private fun filterGearOptionsForPreview(
    items: List<InventoryPreviewItemUi>,
    slotId: String,
    characterId: String
): List<InventoryPreviewItemUi> {
    val normalizedSlot = slotId.trim().lowercase(Locale.getDefault())
    return items.mapNotNull { item ->
        val equipment = item.equipment ?: synthesizePreviewEquipment(item) ?: return@mapNotNull null
        if (GearRules.matchesSlot(equipment, normalizedSlot, characterId, item.type)) item else null
    }.sortedBy { it.name.lowercase(Locale.getDefault()) }
}

private fun synthesizePreviewEquipment(item: InventoryPreviewItemUi): Equipment? {
    val normalizedType = item.type.trim().lowercase(Locale.getDefault())
    val slot = when {
        GearRules.equipSlots.contains(normalizedType) -> normalizedType
        GearRules.isWeaponType(normalizedType) -> "weapon"
        else -> null
    } ?: return null
    val weaponType = normalizedType.takeIf { GearRules.isWeaponType(it) }
    return Equipment(slot = slot, weaponType = weaponType)
}

@DrawableRes
private fun weaponIconRes(item: Item): Int {
    val weaponType = item.equipment?.weaponType?.lowercase(Locale.getDefault())
        ?: item.type.lowercase(Locale.getDefault())
    return when {
        weaponType.contains("gun") -> R.drawable.item_icon_gun
        weaponType.contains("glove") -> R.drawable.item_icon_gloves
        weaponType.contains("jewel") || weaponType.contains("pendant") -> R.drawable.item_icon_pendant
        weaponType.contains("sword") || weaponType.contains("blade") -> R.drawable.item_icon_sword
        else -> R.drawable.item_icon_generic
    }
}

@DrawableRes
private fun weaponTypeIconRes(type: String?): Int {
    val normalized = type?.lowercase(Locale.getDefault()) ?: return R.drawable.item_icon_generic
    return when {
        normalized.contains("gun") -> R.drawable.item_icon_gun
        normalized.contains("glove") -> R.drawable.item_icon_gloves
        normalized.contains("jewel") || normalized.contains("pendant") -> R.drawable.item_icon_pendant
        normalized.contains("sword") || normalized.contains("blade") -> R.drawable.item_icon_sword
        else -> R.drawable.item_icon_generic
    }
}

@DrawableRes
private fun armorIconRes(item: Item): Int {
    val name = item.name.lowercase(Locale.getDefault())
    return when {
        name.contains("pendant") -> R.drawable.item_icon_pendant
        name.contains("glove") -> R.drawable.item_icon_gloves
        else -> R.drawable.item_icon_armor
    }
}

private fun weaponAbilityLines(item: Item): List<String> {
    val equipment = item.equipment ?: return listOf("No weapon data available.")
    val lines = mutableListOf<String>()
    equipment.weaponType?.takeIf { it.isNotBlank() }?.let { lines += "Type: ${slotLabel(it)}" }
    val min = equipment.damageMin
    val max = equipment.damageMax
    if (min != null || max != null) {
        val minLabel = min?.toString() ?: "?"
        val maxLabel = max?.toString() ?: "?"
        lines += "Damage: $minLabel-$maxLabel"
    }
    attackStyleLabel(equipment.attackStyle)?.let { lines += "Style: $it" }
    equipment.attackPowerMultiplier?.let { lines += "Power: ${formatMultiplier(it)}" }
    equipment.attackElement?.takeIf { it.isNotBlank() }?.let { lines += "Element: ${slotLabel(it)}" }
    equipment.attackChargeTurns?.takeIf { it > 0 }?.let { turns ->
        lines += "Charge: $turns turn${if (turns == 1) "" else "s"}"
    }
    equipment.attackSplashMultiplier?.let { lines += "Splash: ${formatPercent(it)}" }
    equipment.statusOnHit?.takeIf { it.isNotBlank() }?.let { status ->
        val chance = equipment.statusChance?.takeIf { it > 0 }?.let { " (${formatPercent(it)})" } ?: ""
        lines += "Status: ${slotLabel(status)}$chance"
    }
    equipment.accuracy?.let { lines += "Accuracy: ${formatSignedNumber(it)}" }
    equipment.critRate?.let { lines += "Crit: ${formatPercent(it)}" }
    equipment.statMods?.forEach { (stat, value) ->
        val label = slotLabel(stat)
        val sign = if (value >= 0) "+" else ""
        lines += "$label: $sign$value"
    }
    return lines.ifEmpty { listOf("Standard issue weapon.") }
}

private fun armorAbilityLines(item: Item): List<String> {
    val equipment = item.equipment ?: return listOf("No armor data available.")
    val lines = mutableListOf<String>()
    equipment.defense?.let { lines += "Defense: ${if (it >= 0) "+$it" else it}" }
    equipment.hpBonus?.let { lines += "HP Bonus: ${if (it >= 0) "+$it" else it}" }
    equipment.accuracy?.let { lines += "Accuracy: ${formatSignedNumber(it)}" }
    equipment.critRate?.let { lines += "Crit Rate: ${formatPercent(it)}" }
    equipment.statMods?.forEach { (stat, value) ->
        val label = slotLabel(stat)
        val sign = if (value >= 0) "+" else ""
        lines += "$label: $sign$value"
    }
    return lines.ifEmpty { listOf("Standard issue armor.") }
}

private fun attackStyleLabel(raw: String?): String? {
    val normalized = raw?.trim()?.lowercase(Locale.getDefault()) ?: return null
    return when (normalized) {
        "single" -> "Single Target"
        "all" -> "All Enemies"
        "spread" -> "Spread Shot"
        "rocket" -> "Rocket Salvo"
        "charged_splash" -> "Charged Splash"
        else -> null
    }
}

private fun formatMultiplier(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    return "${text}x"
}

private fun formatPercent(value: Double): String {
    val percent = if (value <= 1.0) value * 100 else value
    return "${percent.roundToInt()}%"
}

private fun formatSignedNumber(value: Double): String {
    val rounded = if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.getDefault(), "%.1f", value)
    return if (value >= 0) "+$rounded" else rounded
}

@Composable
private fun WeaponSelectionDialog(
    characterName: String,
    weapons: List<Item>,
    equippedWeaponId: String?,
    accentColor: Color,
    borderColor: Color,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val equippedNormalized = remember(equippedWeaponId) { equippedWeaponId?.lowercase(Locale.getDefault()).orEmpty() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp),
            color = Color(0xFF060C14),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0A1422), Color(0xFF050A11))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Weapon Loadout",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = characterName.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onDismiss() }
                        )
                    }

                    if (weapons.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No weapons unlocked yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(weapons, key = { it.id }) { weapon ->
                                val isEquipped = weapon.id.lowercase(Locale.getDefault()) == equippedNormalized
                                WeaponFeatureCard(
                                    weapon = weapon,
                                    isEquipped = isEquipped,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    onSelect = { onSelect(weapon.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArmorSelectionDialog(
    characterName: String,
    armors: List<Item>,
    equippedArmorId: String?,
    accentColor: Color,
    borderColor: Color,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val equippedNormalized = remember(equippedArmorId) { equippedArmorId?.lowercase(Locale.getDefault()).orEmpty() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 18.dp),
            color = Color(0xFF060C14),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0A1422), Color(0xFF050A11))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Armor Loadout",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = characterName.uppercase(Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onDismiss() }
                        )
                    }

                    if (armors.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No armor unlocked yet.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(armors, key = { it.id }) { armor ->
                                val isEquipped = armor.id.lowercase(Locale.getDefault()) == equippedNormalized
                                ArmorFeatureCard(
                                    armor = armor,
                                    isEquipped = isEquipped,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    onSelect = { onSelect(armor.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponFeatureCard(
    weapon: Item,
    isEquipped: Boolean,
    accentColor: Color,
    borderColor: Color,
    onSelect: () -> Unit
) {
    val iconRes = remember(weapon.id) { weaponIconRes(weapon) }
    val abilities = remember(weapon.id) { weaponAbilityLines(weapon) }
    val description = weapon.description?.takeIf { it.isNotBlank() } ?: "No description available."
    val shape = RoundedCornerShape(22.dp)
    val background = if (isEquipped) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
    val outline = if (isEquipped) accentColor else borderColor.copy(alpha = 0.6f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onSelect() },
        color = background,
        border = BorderStroke(1.dp, outline),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = weapon.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    if (isEquipped) {
                        Surface(
                            color = accentColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "EQUIPPED",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Abilities",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    abilities.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArmorFeatureCard(
    armor: Item,
    isEquipped: Boolean,
    accentColor: Color,
    borderColor: Color,
    onSelect: () -> Unit
) {
    val iconRes = remember(armor.id) { armorIconRes(armor) }
    val abilities = remember(armor.id) { armorAbilityLines(armor) }
    val description = armor.description?.takeIf { it.isNotBlank() } ?: "No description available."
    val shape = RoundedCornerShape(22.dp)
    val background = if (isEquipped) accentColor.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
    val outline = if (isEquipped) accentColor else borderColor.copy(alpha = 0.6f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onSelect() },
        color = background,
        border = BorderStroke(1.dp, outline),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = armor.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    if (isEquipped) {
                        Surface(
                            color = accentColor.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "EQUIPPED",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Abilities",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor
                    )
                    abilities.forEach { line ->
                        Text(
                            text = "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }
    }
}
