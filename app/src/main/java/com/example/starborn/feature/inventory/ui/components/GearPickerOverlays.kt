package com.example.starborn.feature.inventory.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.model.Item
import com.example.starborn.feature.inventory.ui.*
import androidx.compose.ui.draw.clip
import java.util.Locale

@Composable
fun ModPickerDialog(
    slotLabel: String,
    mods: List<InventoryEntry>,
    selectedModId: String?,
    onSelect: (InventoryEntry) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val selectedName = mods.firstOrNull { it.item.id == selectedModId }?.item?.name
        ?: selectedModId?.humanizeId()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            color = Color(0xFF0B0F16),
            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select Mod",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = foregroundColor
                        )
                        Text(
                            text = slotLabel.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = foregroundColor.copy(alpha = 0.6f)
                        )
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                Text(
                    text = "Current: ${selectedName ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = foregroundColor.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRemove,
                        enabled = !selectedModId.isNullOrBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (largeTouchTargets) 52.dp else 44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.18f),
                            contentColor = accentColor,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Remove Mod")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.8f)
                            .heightIn(min = if (largeTouchTargets) 52.dp else 44.dp)
                    ) {
                        Text("Cancel")
                    }
                }
                if (mods.isEmpty()) {
                    Text(
                        text = "No mods in inventory yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = foregroundColor.copy(alpha = 0.7f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mods, key = { it.item.id }) { entry ->
                            InventoryItemRow(
                                entry = entry,
                                selected = entry.item.id == selectedModId,
                                isEquipped = entry.item.id == selectedModId,
                                accentColor = accentColor,
                                onClick = { onSelect(entry) },
                                largeTouchTargets = largeTouchTargets,
                                statSummary = primaryStatSummary(entry.item)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeaponPickerDialog(
    characterName: String,
    weapons: List<WeaponOption>,
    equippedWeaponId: String?,
    selectedWeaponId: String?,
    onSelectWeapon: (String) -> Unit,
    onEquipWeapon: (String) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF050B12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weapon Loadout",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = foregroundColor
                        )
                        Text(
                            text = characterName.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = foregroundColor.copy(alpha = 0.6f)
                        )
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                if (weapons.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No weapons unlocked yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = foregroundColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val selectedOption = weapons.firstOrNull { it.id.equals(selectedWeaponId, ignoreCase = true) }
                        ?: weapons.firstOrNull()
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val isWide = maxWidth > 720.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                WeaponOptionsColumn(
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .fillMaxHeight(),
                                    weapons = weapons,
                                    selectedWeaponId = selectedOption?.id,
                                    equippedWeaponId = equippedWeaponId,
                                    onSelectWeapon = onSelectWeapon,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                                WeaponDetailPanel(
                                    modifier = Modifier
                                        .weight(0.55f)
                                        .fillMaxHeight(),
                                    option = selectedOption,
                                    equippedWeaponId = equippedWeaponId,
                                    onEquipWeapon = onEquipWeapon,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                WeaponDetailPanel(
                                    modifier = Modifier.fillMaxWidth(),
                                    option = selectedOption,
                                    equippedWeaponId = equippedWeaponId,
                                    onEquipWeapon = onEquipWeapon,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                                WeaponOptionsColumn(
                                    modifier = Modifier.weight(1f),
                                    weapons = weapons,
                                    selectedWeaponId = selectedOption?.id,
                                    equippedWeaponId = equippedWeaponId,
                                    onSelectWeapon = onSelectWeapon,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
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
fun ArmorPickerDialog(
    characterName: String,
    armors: List<ArmorOption>,
    equippedArmorId: String?,
    selectedArmorId: String?,
    onSelectArmor: (String) -> Unit,
    onEquipArmor: (String) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF050B12)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Armor Loadout",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = foregroundColor
                        )
                        Text(
                            text = characterName.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            color = foregroundColor.copy(alpha = 0.6f)
                        )
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                if (armors.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No armor unlocked yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = foregroundColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val selectedOption = armors.firstOrNull { it.id.equals(selectedArmorId, ignoreCase = true) }
                        ?: armors.firstOrNull()
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val isWide = maxWidth > 720.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ArmorOptionsColumn(
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .fillMaxHeight(),
                                    armors = armors,
                                    selectedArmorId = selectedOption?.id,
                                    equippedArmorId = equippedArmorId,
                                    onSelectArmor = onSelectArmor,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                                ArmorDetailPanel(
                                    modifier = Modifier
                                        .weight(0.55f)
                                        .fillMaxHeight(),
                                    option = selectedOption,
                                    equippedArmorId = equippedArmorId,
                                    onEquipArmor = onEquipArmor,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ArmorDetailPanel(
                                    modifier = Modifier.fillMaxWidth(),
                                    option = selectedOption,
                                    equippedArmorId = equippedArmorId,
                                    onEquipArmor = onEquipArmor,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
                                )
                                ArmorOptionsColumn(
                                    modifier = Modifier.weight(1f),
                                    armors = armors,
                                    selectedArmorId = selectedOption?.id,
                                    equippedArmorId = equippedArmorId,
                                    onSelectArmor = onSelectArmor,
                                    accentColor = accentColor,
                                    borderColor = borderColor,
                                    foregroundColor = foregroundColor,
                                    largeTouchTargets = largeTouchTargets
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
private fun WeaponOptionsColumn(
    modifier: Modifier,
    weapons: List<WeaponOption>,
    selectedWeaponId: String?,
    equippedWeaponId: String?,
    onSelectWeapon: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(weapons, key = { it.id }) { option ->
                WeaponOptionRow(
                    option = option,
                    selected = option.id.equals(selectedWeaponId, ignoreCase = true),
                    isEquipped = option.id.equals(equippedWeaponId, ignoreCase = true),
                    accentColor = accentColor,
                    borderColor = borderColor,
                    foregroundColor = foregroundColor,
                    largeTouchTargets = largeTouchTargets,
                    onClick = { onSelectWeapon(option.id) }
                )
            }
        }
    }
}

@Composable
private fun ArmorOptionsColumn(
    modifier: Modifier,
    armors: List<ArmorOption>,
    selectedArmorId: String?,
    equippedArmorId: String?,
    onSelectArmor: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(armors, key = { it.id }) { option ->
                ArmorOptionRow(
                    option = option,
                    selected = option.id.equals(selectedArmorId, ignoreCase = true),
                    isEquipped = option.id.equals(equippedArmorId, ignoreCase = true),
                    accentColor = accentColor,
                    borderColor = borderColor,
                    foregroundColor = foregroundColor,
                    largeTouchTargets = largeTouchTargets,
                    onClick = { onSelectArmor(option.id) }
                )
            }
        }
    }
}

@Composable
private fun WeaponOptionRow(
    option: WeaponOption,
    selected: Boolean,
    isEquipped: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
    val outline = if (selected) accentColor else borderColor.copy(alpha = 0.6f)
    val summary = weaponSummaryLine(option.item)
    val iconRes = remember(option.id) { itemIconRes(option.item) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeTouchTargets) 80.dp else 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = foregroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isEquipped) {
                Surface(
                    color = accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
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
    }
}

@Composable
private fun ArmorOptionRow(
    option: ArmorOption,
    selected: Boolean,
    isEquipped: Boolean,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) accentColor.copy(alpha = 0.18f) else Color.Transparent
    val outline = if (selected) accentColor else borderColor.copy(alpha = 0.6f)
    val summary = armorSummaryLine(option.item)
    val iconRes = remember(option.id) { itemIconRes(option.item) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeTouchTargets) 80.dp else 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = background,
        border = BorderStroke(1.dp, outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = foregroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isEquipped) {
                Surface(
                    color = accentColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp)
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
    }
}

@Composable
private fun WeaponDetailPanel(
    modifier: Modifier,
    option: WeaponOption?,
    equippedWeaponId: String?,
    onEquipWeapon: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val weapon = option?.item
    val isEquipped = option?.id?.equals(equippedWeaponId, ignoreCase = true) == true
    val detailRows = weapon?.let { weaponDetailRows(it) }.orEmpty()
    val buttonHeight = if (largeTouchTargets) 56.dp else 48.dp
    val iconRes = remember(option?.id) {
        weapon?.let { itemIconRes(it) } ?: slotIconRes("weapon")
    }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Weapon Details", accentColor = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (largeTouchTargets) 96.dp else 84.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = weapon?.name ?: "Select a weapon",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = weapon?.description?.takeIf { it.isNotBlank() } ?: "Choose a weapon to see details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (detailRows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detailRows.forEach { (label, value) ->
                        StatRow(label, value, accentColor)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { option?.let { onEquipWeapon(it.id) } },
                    enabled = option != null && !isEquipped,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text(if (isEquipped) "Equipped" else "Equip Weapon")
                }
            }
        }
    }
}

@Composable
private fun ArmorDetailPanel(
    modifier: Modifier,
    option: ArmorOption?,
    equippedArmorId: String?,
    onEquipArmor: (String) -> Unit,
    accentColor: Color,
    borderColor: Color,
    foregroundColor: Color,
    largeTouchTargets: Boolean
) {
    val armor = option?.item
    val isEquipped = option?.id?.equals(equippedArmorId, ignoreCase = true) == true
    val detailRows = armor?.let { armorDetailRows(it) }.orEmpty()
    val buttonHeight = if (largeTouchTargets) 56.dp else 48.dp
    val iconRes = remember(option?.id) {
        armor?.let { itemIconRes(it) } ?: slotIconRes("armor")
    }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeading(label = "Armor Details", accentColor = accentColor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(if (largeTouchTargets) 96.dp else 84.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = armor?.name ?: "Select armor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = foregroundColor
                    )
                    Text(
                        text = armor?.description?.takeIf { it.isNotBlank() } ?: "Choose armor to see details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = foregroundColor.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (detailRows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detailRows.forEach { (label, value) ->
                        StatRow(label, value, accentColor)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { option?.let { onEquipArmor(it.id) } },
                    enabled = option != null && !isEquipped,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.18f),
                        contentColor = accentColor,
                        disabledContainerColor = Color.White.copy(alpha = 0.08f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Text(if (isEquipped) "Equipped" else "Equip Armor")
                }
            }
        }
    }
}
