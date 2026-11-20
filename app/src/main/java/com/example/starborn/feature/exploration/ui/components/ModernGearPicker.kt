package com.example.starborn.feature.exploration.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.starborn.feature.exploration.ui.menu.FieldMenuDesign
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuHighContrast
import com.example.starborn.feature.exploration.ui.menu.LocalFieldMenuLargeTargets
import com.example.starborn.feature.exploration.viewmodel.InventoryPreviewItemUi
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
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
    val highContrast = LocalFieldMenuHighContrast.current
    val largeTargets = LocalFieldMenuLargeTargets.current
    val initialSelected = remember(characterName, slotLabel, equippedId, options) {
        equippedId?.takeIf { id -> options.any { it.id.equals(id, ignoreCase = true) } }
            ?: options.firstOrNull()?.id
    }
    var selectedId by rememberSaveable(characterName, slotLabel) { mutableStateOf(initialSelected) }
    LaunchedEffect(options) {
        if (selectedId != null && options.none { it.id.equals(selectedId, ignoreCase = true) }) {
            selectedId = initialSelected
        }
    }

    val current = options.firstOrNull { it.id.equals(equippedId, ignoreCase = true) }
    val selected = options.firstOrNull { it.id.equals(selectedId, ignoreCase = true) } ?: current
    val isEquipped = selected != null && selected.id.equals(equippedId, ignoreCase = true)
    val closeButtonSize = if (largeTargets) 52.dp else 44.dp
    val actionButtonHeight = if (largeTargets) 54.dp else 48.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = if (highContrast) Color.Black else FieldMenuDesign.shell,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (highContrast) Color.White else FieldMenuDesign.cyan.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    FieldMenuDesign.panel.copy(alpha = if (highContrast) 0f else 0.72f),
                                    Color.Transparent
                                )
                            )
                        )
                        .navigationBarsPadding()
                ) {
                    // Top atmospheric accent bar matching ModernFieldMenu
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        FieldMenuDesign.cyan.copy(alpha = 0.75f),
                                        FieldMenuDesign.gold.copy(alpha = 0.65f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Tactical Sheet Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${characterName.uppercase(Locale.getDefault())} · LOADOUT",
                                color = FieldMenuDesign.cyan.copy(alpha = 0.88f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = slotLabel,
                                color = FieldMenuDesign.text,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            color = FieldMenuDesign.elevatedPanel,
                            border = BorderStroke(1.dp, FieldMenuDesign.cyan.copy(alpha = 0.45f)),
                            modifier = Modifier.size(closeButtonSize)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close loadout picker",
                                    tint = FieldMenuDesign.text,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = if (highContrast) Color.White else FieldMenuDesign.cyan.copy(alpha = 0.20f))

                    if (options.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No compatible ${slotLabel.lowercase(Locale.getDefault())}s in pack",
                                    color = FieldMenuDesign.text,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Loot, craft, or unequip from another crew member to assign here.",
                                    color = FieldMenuDesign.textMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        // 1. Pinned Inspection & Comparison Deck
                        selected?.let { inspected ->
                            val deckScroll = rememberScrollState()
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .heightIn(max = 310.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = if (highContrast) Color.Black else FieldMenuDesign.panel,
                                border = BorderStroke(
                                    1.dp,
                                    if (isEquipped) FieldMenuDesign.gold.copy(alpha = 0.55f)
                                    else FieldMenuDesign.cyan.copy(alpha = 0.35f)
                                )
                            ) {
                                AnimatedContent(
                                    targetState = inspected,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "gearInspectorDeck"
                                ) { item ->
                                    val itemIsEquipped = item.id.equals(equippedId, ignoreCase = true)
                                    val chips = remember(item) { buildItemStatChips(item) }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(deckScroll)
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        Brush.radialGradient(
                                                            listOf(
                                                                (if (itemIsEquipped) FieldMenuDesign.gold else FieldMenuDesign.cyan).copy(alpha = 0.22f),
                                                                FieldMenuDesign.deepCard
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = rememberItemIconPainter(item.id, item.type),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(46.dp)
                                                )
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = item.name,
                                                        color = FieldMenuDesign.text,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                                                    )
                                                    if (itemIsEquipped) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = FieldMenuDesign.gold.copy(alpha = 0.16f),
                                                            border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.65f))
                                                        ) {
                                                            Text(
                                                                text = "EQUIPPED",
                                                                color = FieldMenuDesign.gold,
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    letterSpacing = 0.6.sp
                                                                ),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else if (item.quantity > 1) {
                                                        Text(
                                                            text = "×${item.quantity}",
                                                            color = FieldMenuDesign.textMuted,
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = (item.equipment?.weaponType?.let(::equipmentLabel)
                                                        ?: slotLabel).uppercase(Locale.getDefault()),
                                                    color = (if (itemIsEquipped) FieldMenuDesign.gold else FieldMenuDesign.cyan).copy(alpha = 0.85f),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.7.sp
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                                            Text(
                                                text = desc,
                                                color = FieldMenuDesign.textMuted,
                                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        if (chips.isNotEmpty()) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                chips.forEach { (label, tint) ->
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = tint.copy(alpha = 0.13f),
                                                        border = BorderStroke(1.dp, tint.copy(alpha = 0.42f))
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = tint,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (!itemIsEquipped && item.equipment != null && current?.equipment != null) {
                                            EquipmentComparison(
                                                candidate = item.equipment,
                                                current = current.equipment,
                                                currentName = current.name
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Scannable Uniform Candidate List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(options, key = { it.id }) { option ->
                                val inspecting = option.id.equals(selected?.id, ignoreCase = true)
                                val optionEquipped = option.id.equals(equippedId, ignoreCase = true)
                                val headline = remember(option) { buildHeadlineSummary(option) }
                                Surface(
                                    onClick = { selectedId = option.id },
                                    modifier = Modifier.semantics { this.selected = inspecting },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        inspecting -> FieldMenuDesign.elevatedPanel
                                        highContrast -> Color.Black
                                        else -> FieldMenuDesign.panel.copy(alpha = 0.78f)
                                    },
                                    border = BorderStroke(
                                        width = if (inspecting) 1.5.dp else 1.dp,
                                        color = when {
                                            inspecting -> FieldMenuDesign.gold
                                            optionEquipped -> FieldMenuDesign.gold.copy(alpha = 0.42f)
                                            highContrast -> Color.White.copy(alpha = 0.6f)
                                            else -> FieldMenuDesign.subtleBorder
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = if (largeTargets) 68.dp else 60.dp)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(FieldMenuDesign.deepCard),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberItemIconPainter(option.id, option.type),
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = option.name,
                                                color = if (inspecting) FieldMenuDesign.gold else FieldMenuDesign.text,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (headline.isNotBlank()) {
                                                Text(
                                                    text = headline,
                                                    color = FieldMenuDesign.textMuted,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (optionEquipped) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = FieldMenuDesign.gold.copy(alpha = 0.14f),
                                                border = BorderStroke(1.dp, FieldMenuDesign.gold.copy(alpha = 0.55f))
                                            ) {
                                                Text(
                                                    text = "EQUIPPED",
                                                    color = FieldMenuDesign.gold,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        } else if (option.quantity > 1) {
                                            Text(
                                                text = "×${option.quantity}",
                                                color = FieldMenuDesign.textMuted,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Docked Split Action Bar
                    HorizontalDivider(color = if (highContrast) Color.White else FieldMenuDesign.cyan.copy(alpha = 0.18f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (highContrast) Color.Black else FieldMenuDesign.panel)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onUnequip != null && !equippedId.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = onUnequip,
                                modifier = Modifier.heightIn(min = actionButtonHeight),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, FieldMenuDesign.coral.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FieldMenuDesign.coral)
                            ) {
                                Text("Unequip", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                        Button(
                            onClick = { selected?.let { onSelect(it.id) } },
                            enabled = selected != null && !isEquipped,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = actionButtonHeight),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (selected != null && !isEquipped) FieldMenuDesign.gold else FieldMenuDesign.subtleBorder
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FieldMenuDesign.primaryActionFill,
                                contentColor = FieldMenuDesign.text,
                                disabledContainerColor = FieldMenuDesign.deepCard,
                                disabledContentColor = FieldMenuDesign.textMuted
                            )
                        ) {
                            Text(
                                text = when {
                                    isEquipped -> "Currently Equipped"
                                    selected != null -> "Equip ${selected.name}"
                                    else -> "Select an Item"
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildItemStatChips(item: InventoryPreviewItemUi): List<Pair<String, Color>> = buildList {
    val gear = item.equipment
    if (gear != null) {
        gear.damageMin?.let { min -> add("DMG $min–${gear.damageMax ?: min}" to FieldMenuDesign.coral) }
        gear.defense?.let { add("DEF +$it" to FieldMenuDesign.cyan) }
        gear.hpBonus?.let { add("HP +$it" to FieldMenuDesign.mint) }
        gear.attackElement?.let { add(equipmentLabel(it) to FieldMenuDesign.ember) }
        gear.attackStyle?.let { add(equipmentAttackStyle(it) to FieldMenuDesign.cyan) }
        gear.attackPowerMultiplier?.let { add("${equipmentNumber(it)}× Power" to FieldMenuDesign.gold) }
        gear.attackChargeTurns?.let { add("${it}T Charge" to FieldMenuDesign.ember) }
        gear.attackSplashMultiplier?.let { add("Splash ${equipmentPercent(it)}" to FieldMenuDesign.ember) }
        gear.accuracy?.let { add("ACC ${if (it > 0) "+" else ""}${equipmentNumber(it)}" to FieldMenuDesign.text) }
        gear.critRate?.let { add("CRIT ${equipmentPercent(it)}" to FieldMenuDesign.gold) }
        gear.statusOnHit?.let { status ->
            val pct = gear.statusChance?.let { " ${equipmentPercent(it)}" }.orEmpty()
            add("${equipmentLabel(status)}$pct" to FieldMenuDesign.ember)
        }
        gear.statMods?.forEach { (stat, value) ->
            add("${equipmentLabel(stat)} ${if (value > 0) "+" else ""}$value" to if (value >= 0) FieldMenuDesign.mint else FieldMenuDesign.coral)
        }
    }
    val effect = item.effect
    if (effect != null) {
        effect.restoreHp?.takeIf { it > 0 }?.let { add("Heal +$it HP" to FieldMenuDesign.mint) }
        effect.damage?.takeIf { it > 0 }?.let { add("DMG $it" to FieldMenuDesign.coral) }
        effect.status?.takeIf { it.isNotBlank() }?.let { add(equipmentLabel(it) to FieldMenuDesign.cyan) }
        effect.singleBuff?.let { buff ->
            val dur = buff.duration?.let { " (${it}T)" }.orEmpty()
            add("${equipmentLabel(buff.stat)} ${if (buff.value >= 0) "+${buff.value}" else buff.value}$dur" to FieldMenuDesign.gold)
        }
        effect.buffs?.forEach { buff ->
            val dur = buff.duration?.let { " (${it}T)" }.orEmpty()
            add("${equipmentLabel(buff.stat)} ${if (buff.value >= 0) "+${buff.value}" else buff.value}$dur" to FieldMenuDesign.gold)
        }
        effect.cooldown?.takeIf { it > 0 }?.let { add("${it}T Cooldown" to FieldMenuDesign.ember) }
        effect.usesPerBattle?.takeIf { it > 0 }?.let { add("$it / battle" to FieldMenuDesign.textMuted) }
    }
}

private fun buildHeadlineSummary(item: InventoryPreviewItemUi): String {
    val parts = buildItemStatChips(item).take(3).map { it.first }
    return if (parts.isNotEmpty()) parts.joinToString(" · ")
    else item.description?.takeIf { it.isNotBlank() }.orEmpty()
}
